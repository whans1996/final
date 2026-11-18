package com.vibenote.analysis;

import com.vibenote.analysis.domain.Analysis;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.dto.AnalysisStatusResponse;
import com.vibenote.analysis.dto.AnalysisSummaryResponse;
import com.vibenote.analysis.dto.CreateAnalysisRequest;
import com.vibenote.analysis.dto.CreateAnalysisResponse;
import com.vibenote.analysis.pipeline.SelectedFile;
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

/** 분석 요청 접수, 내 분석 목록, 진행 상태 (F-05a). 파이프라인 실행은 AnalysisPipeline이 한다. */
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
    private final Clock clock;

    public AnalysisService(
            AnalysisMapper analysisMapper,
            UserService userService,
            AnalysisTempFiles tempFiles,
            AnalysisProperties properties,
            ApplicationEventPublisher events,
            ActivityService activityService,
            Clock clock) {
        this.analysisMapper = analysisMapper;
        this.userService = userService;
        this.tempFiles = tempFiles;
        this.properties = properties;
        this.events = events;
        this.activityService = activityService;
        this.clock = clock;
    }

    /**
     * 한도 확인 → 행 INSERT → 임시 파일 저장을 한 트랜잭션으로 하고, 커밋된 뒤 파이프라인이 시작된다 (AnalysisPipelineLauncher). 회원 행을
     * 잠가 같은 회원의 동시 요청이 한도를 함께 통과하지 못하게 한다.
     */
    @Transactional
    public CreateAnalysisResponse requestZip(long userId, CreateAnalysisRequest request) {
        User user = userService.lockActiveMember(userId);
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        int limit = properties.dailyLimit();
        if (analysisMapper.countRequestedSince(userId, now.toLocalDate().atStartOfDay()) >= limit) {
            throw new BusinessException(
                    ErrorCode.ANALYSIS_429,
                    ErrorCode.ANALYSIS_429.message().replace("{limit}", String.valueOf(limit)),
                    null);
        }

        Level level = request.level() != null ? request.level() : user.getLevel();
        Analysis analysis =
                Analysis.newZip(
                        userId, projectName(request.file().getOriginalFilename()), level, now);
        analysisMapper.insert(analysis);
        tempFiles.save(analysis.getId(), request.file());
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

    /** 내 미삭제 분석의 상태. 없거나 남의 것이거나 삭제됐으면 ANALYSIS_404. */
    public AnalysisStatusResponse getStatus(long userId, long analysisId) {
        return analysisMapper
                .findOwned(analysisId, userId)
                .map(AnalysisStatusResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_404));
    }

    /** 진행 중인 분석을 FAILED로 바꾼다. 커밋 이후 콜백(AFTER_COMMIT)에서도 불리므로 끝난 트랜잭션에 참여하지 않게 새 트랜잭션으로 쓴다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(long analysisId, ErrorCode code, String message) {
        analysisMapper.markFailed(analysisId, code.name(), message);
    }

    /** 파이프라인이 다음 단계로 넘어간다. 분석이 없거나 이미 끝났으면(DONE·FAILED, 재시작 복구 등) false. */
    @Transactional
    public boolean advance(long analysisId, AnalysisStatus status, int progress) {
        return analysisMapper.updateStage(analysisId, status, progress) == 1;
    }

    /** 선별·마스킹이 끝난 파일을 저장한다 (SELECTING). */
    @Transactional
    public void saveFiles(long analysisId, List<SelectedFile> files) {
        if (!files.isEmpty()) {
            analysisMapper.insertFiles(
                    files.stream().map(f -> AnalysisFile.of(analysisId, f)).toList());
        }
    }

    /** DONE으로 바꾸고 피드 활동(ANALYSIS)을 남긴다. 그 사이에 이미 끝난 분석이면 아무것도 하지 않는다. */
    @Transactional
    public void complete(long analysisId) {
        if (analysisMapper.updateStage(analysisId, AnalysisStatus.DONE, DONE_PROGRESS) != 1) {
            return;
        }
        long userId =
                analysisMapper
                        .findById(analysisId)
                        .map(Analysis::getUserId)
                        .orElseThrow(() -> new IllegalStateException("분석 없음: " + analysisId));
        activityService.record(userId, ActivityType.ANALYSIS, analysisId);
    }

    /** 진행 중이던 미삭제 분석을 모두 FAILED·ANALYSIS_500으로 바꾼다 (서버 재시작, AC11). 바꾼 수를 돌려준다. */
    @Transactional
    public int failInterrupted(String message) {
        return analysisMapper.failInterrupted(ErrorCode.ANALYSIS_500.name(), message);
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
