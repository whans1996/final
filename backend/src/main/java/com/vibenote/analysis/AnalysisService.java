package com.vibenote.analysis;

import com.vibenote.analysis.domain.Analysis;
import com.vibenote.analysis.domain.AnalysisDetail;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.dto.AnalysisDetailResponse;
import com.vibenote.analysis.dto.AnalysisFileResponse;
import com.vibenote.analysis.dto.AnalysisFileSummaryResponse;
import com.vibenote.analysis.dto.AnalysisStatusResponse;
import com.vibenote.analysis.dto.AnalysisSummaryResponse;
import com.vibenote.analysis.dto.CreateAnalysisRequest;
import com.vibenote.analysis.dto.CreateAnalysisResponse;
import com.vibenote.analysis.dto.UpdateAnalysisRequest;
import com.vibenote.analysis.dto.VisibilityResponse;
import com.vibenote.analysis.explain.AnalysisOverview;
import com.vibenote.analysis.explain.FileExplanation;
import com.vibenote.analysis.pipeline.SelectedFile;
import com.vibenote.common.Level;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.feed.ActivityService;
import com.vibenote.feed.ActivityType;
import com.vibenote.quiz.QuizProperties;
import com.vibenote.quiz.QuizStatus;
import com.vibenote.user.UserService;
import com.vibenote.user.domain.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 분석 요청 접수, 내 분석 목록, 진행 상태 (F-05a), 결과 조회 (F-06), 수정·공개·삭제와 공개 분석 열람 (F-19), 실패 분석 재실행과 퀴즈 상태
 * (L-1a). 파이프라인 실행은 AnalysisPipeline이 한다.
 */
@Service
public class AnalysisService {

    /** 탈퇴로 중단한 분석의 error_message. 주인이 탈퇴해 화면에 나오지 않는다. */
    private static final String WITHDRAWN_MESSAGE = "회원 탈퇴로 분석을 중단했습니다.";

    /** analyses.title 길이. project_name(200)보다 짧으므로 둘 다 이 길이로 자른다. */
    static final int MAX_NAME_LENGTH = 100;

    static final String DEFAULT_PROJECT_NAME = "project";

    static final int DONE_PROGRESS = 100;

    /** analysis_files INSERT 한 번의 행 수 (F-26a 5.5절). */
    static final int INSERT_CHUNK = 100;

    /**
     * analysis_files INSERT 한 번의 content 글자 수 상한. 행이 100개가 안 돼도 이 값을 넘기 전에 끊는다. 글자당 최대 3바이트(U+FFFD
     * 포함)에 드라이버 이스케이프까지 더해도 MySQL 기본 {@code max_allowed_packet}(64MB) 안에 든다.
     */
    static final int INSERT_CHUNK_CHARS = 4_000_000;

    private final AnalysisMapper analysisMapper;
    private final UserService userService;
    private final AnalysisTempFiles tempFiles;
    private final AnalysisProperties properties;
    private final ApplicationEventPublisher events;
    private final ActivityService activityService;
    private final QuizProperties quizProperties;
    private final Clock clock;

    public AnalysisService(
            AnalysisMapper analysisMapper,
            UserService userService,
            AnalysisTempFiles tempFiles,
            AnalysisProperties properties,
            ApplicationEventPublisher events,
            ActivityService activityService,
            QuizProperties quizProperties,
            Clock clock) {
        this.analysisMapper = analysisMapper;
        this.userService = userService;
        this.tempFiles = tempFiles;
        this.properties = properties;
        this.events = events;
        this.activityService = activityService;
        this.quizProperties = quizProperties;
        this.clock = clock;
    }

    /**
     * 한도 확인 → 행 INSERT → (ZIP만) 임시 파일 저장을 한 트랜잭션으로 하고, 커밋된 뒤 파이프라인이 시작된다
     * (AnalysisPipelineLauncher). 회원 행을 잠가 같은 회원의 동시 요청이 한도를 함께 통과하지 못하게 한다. 저장소 URL은 여기서 외부 요청을 하지
     * 않고 FETCHING 단계에서 받는다 (F-05b AC1).
     */
    @Transactional
    public CreateAnalysisResponse request(long userId, CreateAnalysisRequest request) {
        User user = userService.lockActiveMember(userId);
        LocalDateTime now = now();
        checkDailyLimit(userId, now);

        Level level = request.level() != null ? request.level() : user.getLevel();
        Analysis analysis =
                request.repository() != null
                        ? Analysis.newUrl(userId, request.repository(), level, now)
                        : Analysis.newZip(
                                userId,
                                projectName(request.file().getOriginalFilename()),
                                level,
                                now);
        analysisMapper.insert(analysis);
        if (request.file() != null) {
            tempFiles.save(analysis.getId(), request.file());
        }
        events.publishEvent(new AnalysisRequestedEvent(analysis.getId()));
        return new CreateAnalysisResponse(analysis.getId(), analysis.getStatus());
    }

    /**
     * 실패한 내 URL 분석을 같은 설정으로 다시 실행한다 (L-1a AC1~AC3). 원본 soft delete와 새 분석 INSERT를 한 트랜잭션으로 하고, 커밋된 뒤
     * 새 분석의 파이프라인이 시작된다. 회원 행을 잠가 같은 분석의 동시 재실행은 차례로 판단되고, 뒤의 요청은 원본이 이미 삭제되어 404가 된다. 한도를 넘으면 원본
     * 삭제까지 롤백된다.
     */
    @Transactional
    public CreateAnalysisResponse rerun(long userId, long analysisId) {
        userService.lockActiveMember(userId);
        LocalDateTime now = now();
        if (analysisMapper.softDeleteFailedUrl(analysisId, userId, now) != 1) {
            analysisMapper
                    .findOwned(analysisId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_404));
            throw new BusinessException(ErrorCode.ANALYSIS_008);
        }
        checkDailyLimit(userId, now);
        Analysis analysis = Analysis.rerunOf(findForPipeline(analysisId), now);
        analysisMapper.insert(analysis);
        deleteChildren(analysisId);
        events.publishEvent(new AnalysisRequestedEvent(analysis.getId()));
        return new CreateAnalysisResponse(analysis.getId(), analysis.getStatus());
    }

    /** 오늘(Asia/Seoul) FAILED가 아닌 분석이 한도만큼이면 ANALYSIS_429. 호출자가 회원 행을 잠근 뒤 부른다. */
    private void checkDailyLimit(long userId, LocalDateTime now) {
        int limit = properties.dailyLimit();
        if (analysisMapper.countRequestedSince(userId, now.toLocalDate().atStartOfDay()) >= limit) {
            throw new BusinessException(
                    ErrorCode.ANALYSIS_429,
                    ErrorCode.ANALYSIS_429.message().replace("{limit}", String.valueOf(limit)),
                    null);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AnalysisSummaryResponse> listMine(long userId, PageQuery query) {
        var items =
                analysisMapper.findByUser(userId, query.size(), query.offset()).stream()
                        .map(AnalysisSummaryResponse::from)
                        .toList();
        return PageResponse.of(items, query, analysisMapper.countByUser(userId));
    }

    /** 볼 수 있는 분석의 상태 (F-19). 볼 수 없으면 ANALYSIS_404. */
    public AnalysisStatusResponse getStatus(long userId, long analysisId) {
        return AnalysisStatusResponse.from(checkViewable(userId, analysisId));
    }

    /**
     * 볼 수 있는 분석(미삭제이고 내 것이거나 공개·DONE이고 주인이 ACTIVE)을 준다 (F-19). 없거나 볼 수 없으면 존재를 드러내지 않도록
     * ANALYSIS_404.
     */
    public Analysis checkViewable(long userId, long analysisId) {
        return analysisMapper
                .findViewable(analysisId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_404));
    }

    /** 내 미삭제 분석. 없거나 남의 것이거나 삭제됐으면 empty (L-1a 재생성 거절 사유). */
    public Optional<Analysis> findOwned(long userId, long analysisId) {
        return analysisMapper.findOwned(analysisId, userId);
    }

    /** 내 미삭제 분석인지 확인한다. 없거나 남의 것이거나 삭제됐으면 ANALYSIS_404 (F-14 퀴즈 조회). */
    public void checkOwned(long userId, long analysisId) {
        if (analysisMapper.findOwned(analysisId, userId).isEmpty()) {
            throw new BusinessException(ErrorCode.ANALYSIS_404);
        }
    }

    /** 진행 중인 분석을 FAILED로 바꾼다. 커밋 이후 콜백(AFTER_COMMIT)에서도 불리므로 끝난 트랜잭션에 참여하지 않게 새 트랜잭션으로 쓴다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(long analysisId, ErrorCode code, String message) {
        analysisMapper.markFailed(analysisId, code.name(), message, now());
    }

    /** 파이프라인이 다음 단계로 넘어간다. 분석이 없거나 이미 끝났으면(DONE·FAILED, 재시작 복구 등) false. */
    @Transactional
    public boolean advance(long analysisId, AnalysisStatus status, int progress) {
        return analysisMapper.updateStage(analysisId, status, progress, now()) == 1;
    }

    /**
     * 분석이 아직 진행 중이고 삭제되지 않았으면 행을 잠그고 true. 파이프라인이 자식 행을 넣기 직전에 호출자 트랜잭션 안에서 부른다. 탈퇴(#91)가 분석을
     * FAILED·삭제로 바꾸는 중이면 그 커밋을 기다린 뒤 false가 되어, 삭제된 분석에 코드·연결이 다시 남지 않는다.
     */
    @Transactional
    public boolean lockInProgress(long analysisId) {
        return analysisMapper.lockInProgress(analysisId).isPresent();
    }

    /**
     * 마스킹이 끝난 인덱스 전체와 인덱스 수치를 한 트랜잭션으로 저장한다 (SELECTING, F-26a 5.5절). 그사이 중단·삭제됐으면 저장하지 않는다. MySQL
     * {@code max_allowed_packet}을 넘지 않게 {@link #chunks}로 나눠 넣는다.
     *
     * @param files 저장 순서 (선별 파일 먼저)
     * @param droppedFiles 상한으로 버린 파일 수
     */
    @Transactional
    public void saveFiles(long analysisId, List<SelectedFile> files, int droppedFiles) {
        if (!lockInProgress(analysisId)) {
            return;
        }
        for (List<SelectedFile> chunk : chunks(files)) {
            analysisMapper.insertFiles(
                    chunk.stream().map(f -> AnalysisFile.of(analysisId, f)).toList());
        }
        analysisMapper.updateIndexCounts(analysisId, files.size(), droppedFiles);
    }

    /**
     * 순서를 지켜 {@value #INSERT_CHUNK}행 또는 content {@value #INSERT_CHUNK_CHARS}자 중 먼저 닿는 쪽에서 끊는다. 상한보다
     * 큰 파일 하나는 혼자 묶음이 된다.
     */
    static List<List<SelectedFile>> chunks(List<SelectedFile> files) {
        List<List<SelectedFile>> chunks = new ArrayList<>();
        int from = 0;
        long chars = 0;
        for (int i = 0; i < files.size(); i++) {
            int length = files.get(i).content().length();
            if (i > from && (i - from == INSERT_CHUNK || chars + length > INSERT_CHUNK_CHARS)) {
                chunks.add(files.subList(from, i));
                from = i;
                chars = 0;
            }
            chars += length;
        }
        if (from < files.size()) {
            chunks.add(files.subList(from, files.size()));
        }
        return chunks;
    }

    /** 파이프라인이 처리할 분석 (삭제 여부와 상관없음). 없으면 예기치 못한 오류다. */
    public Analysis findForPipeline(long analysisId) {
        return analysisMapper
                .findById(analysisId)
                .orElseThrow(() -> new IllegalStateException("분석 없음: " + analysisId));
    }

    /** 저장된 선별 파일 전체 (content 포함, id 오름차순). EXPLAINING 이후 단계의 입력이다. */
    public List<AnalysisFile> findFiles(long analysisId) {
        return analysisMapper.findFiles(analysisId);
    }

    /** 저장된 인덱스 전체 (id 오름차순, content는 루트 README만). 개요 입력의 README·파일 목록에만 쓴다 (F-26a 5.5절). */
    public List<AnalysisFile> findIndexFiles(long analysisId) {
        return analysisMapper.findIndexFiles(analysisId);
    }

    /** 개요 저장 (EXPLAINING, F-06). LLM 호출과 분리한 짧은 트랜잭션. */
    @Transactional
    public void saveOverview(long analysisId, AnalysisOverview overview) {
        analysisMapper.updateOverview(analysisId, overview);
    }

    /** 파일 묶음 하나의 역할·설명 저장 (EXPLAINING, F-06). */
    @Transactional
    public void saveFileExplanations(long analysisId, List<FileExplanation> explanations) {
        for (FileExplanation e : explanations) {
            analysisMapper.updateFileExplanation(analysisId, e.fileId(), e.role(), e.summary());
        }
    }

    /** 볼 수 있는 분석의 상세 (F-06 AC9, F-19 AC9). 진행 중이면 overview가 null일 수 있다. */
    @Transactional(readOnly = true)
    public AnalysisDetailResponse getDetail(long userId, long analysisId) {
        return analysisMapper
                .findViewableDetail(analysisId, userId)
                .map(d -> AnalysisDetailResponse.from(d, userId, quizProperties.maxRegenerations()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_404));
    }

    /** 볼 수 있는 분석의 상태·개요 (F-21a 구조 그래프). 진행 중이면 개요가 null일 수 있다. 볼 수 없으면 ANALYSIS_404. */
    @Transactional(readOnly = true)
    public AnalysisDetail checkViewableDetail(long userId, long analysisId) {
        return analysisMapper
                .findViewableDetail(analysisId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_404));
    }

    /** 볼 수 있는 분석의 파일 목록 (content 제외, F-06 AC10). */
    @Transactional(readOnly = true)
    public List<AnalysisFileSummaryResponse> listFiles(long userId, long analysisId) {
        checkViewable(userId, analysisId);
        return analysisMapper.findFileSummaries(analysisId).stream()
                .map(AnalysisFileSummaryResponse::from)
                .toList();
    }

    /** 볼 수 있는 분석의 파일 하나 (F-06 AC11). 분석을 볼 수 없으면 ANALYSIS_404, 파일이 그 분석 것이 아니면 ANALYSIS_001. */
    @Transactional(readOnly = true)
    public AnalysisFileResponse getFile(long userId, long analysisId, long fileId) {
        checkViewable(userId, analysisId);
        return analysisMapper
                .findFile(analysisId, fileId)
                .map(AnalysisFileResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_001));
    }

    /** 내 분석의 제목·메모를 바꾸고 바뀐 상세를 준다 (F-19 AC1). 진행 중이어도 되고 파이프라인에는 영향이 없다. */
    @Transactional
    public AnalysisDetailResponse update(
            long userId, long analysisId, UpdateAnalysisRequest request) {
        if (analysisMapper.updateInfo(analysisId, userId, request.title(), request.memo(), now())
                != 1) {
            throw new BusinessException(ErrorCode.ANALYSIS_404);
        }
        return getDetail(userId, analysisId);
    }

    /**
     * 공개 여부를 바꾼다 (F-19 AC4·AC5). 피드 활동은 새로 만들지 않는다: 완료 때 남긴 ANALYSIS 활동이 공개 여부에 따라 보이고 빠진다 (F-04
     * AC7).
     */
    @Transactional
    public VisibilityResponse updateVisibility(long userId, long analysisId, boolean isPublic) {
        if (analysisMapper.updateVisibility(analysisId, userId, isPublic, now()) != 1) {
            analysisMapper
                    .findOwned(analysisId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_404));
            throw new BusinessException(ErrorCode.ANALYSIS_006);
        }
        return new VisibilityResponse(analysisId, isPublic);
    }

    /**
     * 끝난(DONE·FAILED) 내 분석을 soft delete하고 파일·흐름·개념 연결을 지운다 (F-19 AC6~AC8). 퀴즈와 풀이 기록은 남긴다. 판단은
     * UPDATE 한 문장이라 파이프라인 상태 갱신과 경쟁하지 않고, 0행이면 소유를 다시 보고 404와 409를 고른다.
     */
    @Transactional
    public void delete(long userId, long analysisId) {
        if (analysisMapper.softDelete(analysisId, userId, now()) != 1) {
            analysisMapper
                    .findOwned(analysisId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_404));
            throw new BusinessException(ErrorCode.ANALYSIS_005);
        }
        deleteChildren(analysisId);
    }

    /**
     * 회원 탈퇴 때 그 회원의 분석을 모두 지운다 (#91, 09-데이터셋 6절 "삭제"). 진행 중인 분석은 먼저 FAILED로 바꿔 파이프라인이 다음 {@link
     * #advance}에서 멈추게 한 뒤, F-19 삭제와 같이 soft delete하고 파일·흐름·개념 연결을 지운다. 탈퇴 트랜잭션에 참여한다.
     */
    @Transactional
    public void deleteAllOf(long userId) {
        LocalDateTime now = now();
        for (long id : analysisMapper.findUndeletedIds(userId)) {
            analysisMapper.markFailed(id, ErrorCode.ANALYSIS_500.name(), WITHDRAWN_MESSAGE, now);
            analysisMapper.softDelete(id, userId, now);
            deleteChildren(id);
        }
    }

    private void deleteChildren(long analysisId) {
        // analysis_concepts.file_id가 analysis_files를 참조하므로 개념 연결을 먼저 지운다
        analysisMapper.deleteConceptLinks(analysisId);
        analysisMapper.deleteFlows(analysisId);
        analysisMapper.deleteFiles(analysisId);
        events.publishEvent(new AnalysisDeletedEvent(analysisId));
    }

    /**
     * DONE으로 바꾸고 피드 활동(ANALYSIS)을 남긴다. 그 사이에 이미 끝난 분석이면 아무것도 하지 않는다. 분석 챌린지 달성도는 커밋 뒤 {@link
     * AnalysisCompletedEvent}로 별도 트랜잭션에서 센다 (F-25 AC7). 집계 실패가 분석을 롤백하지 않게 하려는 것이다 (#105).
     *
     * <p>회원 행을 먼저 잠가 탈퇴(users → analyses)와 잠금 순서를 맞춘다. 반대 순서면 activities INSERT의 users FK 잠금과 엇갈려
     * 데드락이 난다 (#91). 그사이 탈퇴했으면 분석도 중단·삭제되었으므로 끝낸다.
     */
    @Transactional
    public void complete(long analysisId) {
        Analysis analysis =
                analysisMapper
                        .findById(analysisId)
                        .orElseThrow(() -> new IllegalStateException("분석 없음: " + analysisId));
        try {
            userService.lockActiveMember(analysis.getUserId());
        } catch (BusinessException withdrawn) {
            return;
        }
        if (analysisMapper.updateStage(analysisId, AnalysisStatus.DONE, DONE_PROGRESS, now())
                != 1) {
            return;
        }
        activityService.record(analysis.getUserId(), ActivityType.ANALYSIS, analysisId);
        events.publishEvent(
                new AnalysisCompletedEvent(
                        analysisId, analysis.getUserId(), analysis.getCreatedAt().toLocalDate()));
    }

    /** QUIZ 단계의 퀴즈 상태를 기록한다 (L-1a AC4). 퀴즈 저장 트랜잭션에 참여한다. */
    @Transactional
    public void updateQuizStatus(long analysisId, QuizStatus status) {
        analysisMapper.updateQuizStatus(analysisId, status);
    }

    /**
     * 퀴즈 재생성을 시작한다 (L-1a AC5·AC7). 내 미삭제 DONE 분석의 퀴즈가 FAILED이고 횟수가 max 미만이면 GENERATING으로 바꾸고 횟수를 1
     * 늘린 뒤 true. 판단과 갱신이 UPDATE 한 문장이라 동시 요청 중 하나만 true다.
     */
    @Transactional
    public boolean startQuizRegeneration(long userId, long analysisId, int max) {
        return analysisMapper.startQuizRegeneration(analysisId, userId, max) == 1;
    }

    /**
     * 재생성 결과를 기록한다 (L-1a AC5·AC6). 아직 GENERATING이고 삭제되지 않았을 때만 바꾸고 true. 그사이 삭제·탈퇴했으면 false (AC8).
     * 퀴즈 저장 트랜잭션에 참여한다.
     */
    @Transactional
    public boolean finishQuizRegeneration(long analysisId, QuizStatus status) {
        return analysisMapper.finishQuizRegeneration(analysisId, status) == 1;
    }

    /**
     * 재생성을 FAILED로 끝낸다 (L-1a AC6). 실행기 거절처럼 커밋 이후 콜백(AFTER_COMMIT)에서도 불리므로 {@link #markFailed}와 같이
     * 끝난 트랜잭션에 참여하지 않게 새 트랜잭션으로 쓴다. 아직 GENERATING이고 삭제되지 않았을 때만 바꾼다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failQuizRegeneration(long analysisId) {
        analysisMapper.finishQuizRegeneration(analysisId, QuizStatus.FAILED);
    }

    /** 재생성 중이던 퀴즈를 모두 FAILED로 바꾼다 (서버 재시작, L-1a AC8). 바꾼 수를 돌려준다. */
    @Transactional
    public int failInterruptedQuizRegenerations() {
        return analysisMapper.failInterruptedQuizRegenerations();
    }

    /** 진행 중이던 미삭제 분석을 모두 FAILED·ANALYSIS_500으로 바꾼다 (서버 재시작, AC11). 바꾼 수를 돌려준다. */
    @Transactional
    public int failInterrupted(String message) {
        return analysisMapper.failInterrupted(ErrorCode.ANALYSIS_500.name(), message, now());
    }

    /**
     * 서비스 시계(Asia/Seoul)의 현재 시각. created_at·updated_at을 DB 기본값(세션 시간대) 대신 이 값으로 써서, 응답에 붙이는 +09:00과
     * 어긋나지 않게 한다.
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    /** 업로드 파일명에서 경로와 .zip을 뗀 이름. 비면 "project". */
    static String projectName(String originalFilename) {
        String name = originalFilename == null ? "" : originalFilename;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        if (name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            name = name.substring(0, name.length() - ".zip".length());
        }
        name = name.strip();
        if (name.isEmpty()) {
            return DEFAULT_PROJECT_NAME;
        }
        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
    }
}
