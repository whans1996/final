package com.vibenote.analysis;

import com.vibenote.analysis.domain.Analysis;
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
import com.vibenote.challenge.ChallengeGoalType;
import com.vibenote.challenge.ChallengeProgressService;
import com.vibenote.common.Level;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.feed.ActivityService;
import com.vibenote.feed.ActivityType;
import com.vibenote.user.UserService;
import com.vibenote.user.domain.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 분석 요청 접수, 내 분석 목록, 진행 상태 (F-05a), 결과 조회 (F-06), 수정·공개·삭제와 공개 분석 열람 (F-19). 파이프라인 실행은
 * AnalysisPipeline이 한다.
 */
@Service
public class AnalysisService {

    /** analyses.title 길이. project_name(200)보다 짧으므로 둘 다 이 길이로 자른다. */
    static final int MAX_NAME_LENGTH = 100;

    static final String DEFAULT_PROJECT_NAME = "project";

    static final int DONE_PROGRESS = 100;

    private final AnalysisMapper analysisMapper;
    private final UserService userService;
    private final AnalysisTempFiles tempFiles;
    private final AnalysisProperties properties;
    private final ApplicationEventPublisher events;
    private final ActivityService activityService;
    private final ChallengeProgressService challengeProgressService;
    private final Clock clock;

    public AnalysisService(
            AnalysisMapper analysisMapper,
            UserService userService,
            AnalysisTempFiles tempFiles,
            AnalysisProperties properties,
            ApplicationEventPublisher events,
            ActivityService activityService,
            ChallengeProgressService challengeProgressService,
            Clock clock) {
        this.analysisMapper = analysisMapper;
        this.userService = userService;
        this.tempFiles = tempFiles;
        this.properties = properties;
        this.events = events;
        this.activityService = activityService;
        this.challengeProgressService = challengeProgressService;
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
        int limit = properties.dailyLimit();
        if (analysisMapper.countRequestedSince(userId, now.toLocalDate().atStartOfDay()) >= limit) {
            throw new BusinessException(
                    ErrorCode.ANALYSIS_429,
                    ErrorCode.ANALYSIS_429.message().replace("{limit}", String.valueOf(limit)),
                    null);
        }

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

    /** 선별·마스킹이 끝난 파일을 저장한다 (SELECTING). */
    @Transactional
    public void saveFiles(long analysisId, List<SelectedFile> files) {
        if (!files.isEmpty()) {
            analysisMapper.insertFiles(
                    files.stream().map(f -> AnalysisFile.of(analysisId, f)).toList());
        }
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
                .map(d -> AnalysisDetailResponse.from(d, userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_404));
    }

    /** 볼 수 있는 분석의 개요 (F-21a 구조 그래프). 진행 중이면 null. 볼 수 없으면 ANALYSIS_404. */
    @Transactional(readOnly = true)
    public AnalysisOverview getViewableOverview(long userId, long analysisId) {
        return analysisMapper
                .findViewableDetail(analysisId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_404))
                .getOverview();
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
        // analysis_concepts.file_id가 analysis_files를 참조하므로 개념 연결을 먼저 지운다
        analysisMapper.deleteConceptLinks(analysisId);
        analysisMapper.deleteFlows(analysisId);
        analysisMapper.deleteFiles(analysisId);
    }

    /**
     * DONE으로 바꾸고 피드 활동(ANALYSIS)을 남긴 뒤, 요청 날짜의 분석 챌린지 달성도를 다시 센다 (F-25 AC7). 그 사이에 이미 끝난 분석이면 아무것도
     * 하지 않는다.
     */
    @Transactional
    public void complete(long analysisId) {
        if (analysisMapper.updateStage(analysisId, AnalysisStatus.DONE, DONE_PROGRESS, now())
                != 1) {
            return;
        }
        Analysis analysis =
                analysisMapper
                        .findById(analysisId)
                        .orElseThrow(() -> new IllegalStateException("분석 없음: " + analysisId));
        activityService.record(analysis.getUserId(), ActivityType.ANALYSIS, analysisId);
        challengeProgressService.refresh(
                analysis.getUserId(),
                ChallengeGoalType.ANALYSIS_COUNT,
                analysis.getCreatedAt().toLocalDate());
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
