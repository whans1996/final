package com.vibenote.interview;

import com.vibenote.analysis.AnalysisService;
import com.vibenote.analysis.AnalysisStatus;
import com.vibenote.analysis.concept.AnalysisConceptService;
import com.vibenote.analysis.concept.LinkedConcept;
import com.vibenote.analysis.domain.AnalysisDetail;
import com.vibenote.common.config.ClockConfig;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 면접 질문 요청 접수·조회 (I-1a AC5~AC9). LLM 호출은 {@link InterviewGenerator}가 트랜잭션 밖에서 한다. */
@Service
public class InterviewService {

    /** 서버 재시작·큐 거부·예기치 못한 오류를 구분해 저장하지 않으므로 공통 문구를 쓴다 (AC10, F-07 AC11과 같음). */
    static final String INTERRUPTED_MESSAGE = "면접 질문 생성이 중단되었습니다. 다시 요청해 주세요.";

    private final InterviewMapper mapper;
    private final AnalysisService analysisService;
    private final AnalysisConceptService conceptService;
    private final InterviewProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public InterviewService(
            InterviewMapper mapper,
            AnalysisService analysisService,
            AnalysisConceptService conceptService,
            InterviewProperties properties,
            ApplicationEventPublisher events,
            Clock clock) {
        this.mapper = mapper;
        this.analysisService = analysisService;
        this.conceptService = conceptService;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
    }

    /**
     * 처음 만들기(행 없음) / 다시 시도(FAILED, 횟수 안 씀) / 다시 만들기(DONE, 횟수 1 차감)를 접수한다. 분석 행을 잠근 채 판단하고, 커밋된 뒤
     * 생성이 시작된다 ({@link InterviewLauncher}).
     */
    @Transactional
    public InterviewResponse request(long userId, long analysisId) {
        AnalysisDetail analysis =
                mapper.lockOwnedAnalysis(analysisId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_404));
        if (analysis.getStatus() != AnalysisStatus.DONE) {
            throw new BusinessException(ErrorCode.INTERVIEW_409);
        }
        LocalDateTime now = now();
        Optional<AnalysisInterview> existing = mapper.findByAnalysis(analysisId);
        long interviewId;
        if (existing.isEmpty()) {
            AnalysisInterview created = AnalysisInterview.pending(analysisId, now);
            mapper.insert(created);
            interviewId = created.getId();
        } else {
            AnalysisInterview interview = existing.get();
            interviewId = interview.getId();
            switch (interview.getStatus()) {
                case FAILED -> mapper.resetFailed(interviewId, now);
                case DONE -> {
                    if (mapper.startRegeneration(interviewId, properties.maxRegenerations(), now)
                            != 1) {
                        int limit = properties.maxRegenerations();
                        throw new BusinessException(
                                ErrorCode.INTERVIEW_429,
                                ErrorCode.INTERVIEW_429
                                        .message()
                                        .replace("{limit}", String.valueOf(limit)),
                                null);
                    }
                }
                default -> throw new BusinessException(ErrorCode.INTERVIEW_409);
            }
        }
        events.publishEvent(new InterviewRequestedEvent(interviewId));
        return response(analysisId);
    }

    /** 내 미삭제 분석의 면접 질문 (AC9). 분석이 진행 중이어도 조회는 된다. */
    @Transactional(readOnly = true)
    public InterviewResponse get(long userId, long analysisId) {
        analysisService.checkOwned(userId, analysisId);
        return response(analysisId);
    }

    private InterviewResponse response(long analysisId) {
        Optional<AnalysisInterview> found = mapper.findByAnalysis(analysisId);
        if (found.isEmpty()) {
            return new InterviewResponse(
                    analysisId,
                    InterviewStatus.NONE,
                    null,
                    null,
                    properties.maxRegenerations(),
                    null,
                    null);
        }
        AnalysisInterview interview = found.get();
        boolean done = interview.getStatus() == InterviewStatus.DONE;
        boolean failed = interview.getStatus() == InterviewStatus.FAILED;
        String errorCode = failed ? interview.getErrorCode() : null;
        return new InterviewResponse(
                analysisId,
                interview.getStatus(),
                errorCode,
                failed ? errorMessage(errorCode) : null,
                Math.max(0, properties.maxRegenerations() - interview.getRegenerations()),
                done && interview.getGeneratedAt() != null
                        ? interview.getGeneratedAt().atZone(ClockConfig.ZONE).toOffsetDateTime()
                        : null,
                done ? questions(interview) : null);
    }

    private List<InterviewResponse.Question> questions(AnalysisInterview interview) {
        Map<Long, String> visible =
                conceptService.findForQuiz(interview.getAnalysisId()).stream()
                        .collect(
                                Collectors.toMap(
                                        LinkedConcept::conceptId,
                                        LinkedConcept::name,
                                        (a, b) -> a));
        return mapper.findQuestions(interview.getId()).stream()
                .map(
                        q ->
                                new InterviewResponse.Question(
                                        q.getId(),
                                        q.getNo(),
                                        q.getCategory(),
                                        q.getQuestion(),
                                        List.of(q.getKeyPoints()),
                                        List.of(q.getEvidence()),
                                        Arrays.stream(q.getConceptIds())
                                                .filter(Objects::nonNull)
                                                .filter(visible::containsKey)
                                                .map(
                                                        id ->
                                                                new InterviewResponse.ConceptRef(
                                                                        id, visible.get(id)))
                                                .toList()))
                .toList();
    }

    static String errorMessage(String errorCode) {
        if (errorCode == null) {
            return null;
        }
        if (errorCode.equals(ErrorCode.ANALYSIS_500.name())) {
            return INTERRUPTED_MESSAGE;
        }
        try {
            return ErrorCode.valueOf(errorCode).message();
        } catch (IllegalArgumentException e) {
            return INTERRUPTED_MESSAGE;
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
