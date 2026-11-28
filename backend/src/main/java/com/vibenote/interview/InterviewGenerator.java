package com.vibenote.interview;

import com.vibenote.analysis.concept.AnalysisConceptService;
import com.vibenote.analysis.domain.AnalysisDetail;
import com.vibenote.analysis.explain.ExplainPrompts;
import com.vibenote.analysis.explain.LlmJsonCall;
import com.vibenote.analysis.explain.PromptTemplates;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmStage;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * PENDING 면접 하나를 만든다: 입력 구성 → 프롬프트 → LLM 1회(형식 오류·유효 질문 부족 시 1회 재시도) → 검증·저장 (I-1a AC1·AC4). 실패하면
 * FAILED로 둔다.
 *
 * <p>{@code @Transactional} 안에서 부르지 않는다 ({@link LlmClient} 주석). 요청 시 생성 실행기({@code flowExecutor})
 * 스레드에서 돈다.
 */
@Component
public class InterviewGenerator {

    private static final Logger log = LoggerFactory.getLogger(InterviewGenerator.class);

    private final InterviewSaveService saveService;
    private final InterviewMapper mapper;
    private final AnalysisConceptService conceptService;
    private final LlmClient llm;
    private final PromptTemplates templates;
    private final InterviewProperties properties;

    public InterviewGenerator(
            InterviewSaveService saveService,
            InterviewMapper mapper,
            AnalysisConceptService conceptService,
            LlmClient llm,
            PromptTemplates templates,
            InterviewProperties properties) {
        this.saveService = saveService;
        this.mapper = mapper;
        this.conceptService = conceptService;
        this.llm = llm;
        this.templates = templates;
        this.properties = properties;
    }

    public void generate(long interviewId) {
        try {
            Optional<AnalysisInterview> pending = saveService.findPending(interviewId);
            if (pending.isEmpty()) {
                return;
            }
            Optional<List<GeneratedQuestion>> result = call(pending.get());
            if (result.isPresent()) {
                saveService.complete(interviewId, result.get());
            } else {
                log.warn("면접 {}: 형식 오류가 두 번 나 FAILED", interviewId);
                markFailed(interviewId, ErrorCode.LLM_502);
            }
        } catch (BusinessException e) {
            log.warn("면접 {}: LLM 실패로 FAILED ({})", interviewId, e.getErrorCode());
            markFailed(interviewId, e.getErrorCode());
        } catch (RuntimeException e) {
            log.error("면접 {}: 예기치 못한 오류로 FAILED", interviewId, e);
            markFailed(interviewId, ErrorCode.ANALYSIS_500);
        } catch (Error e) {
            // PENDING으로 남으면 재시작 전까지 다시 요청이 409라 FAILED로 바꾸고 스레드에는 그대로 알린다 (QuizRegenerationRunner와
            // 같음)
            log.error("면접 {}: 치명적 오류로 FAILED", interviewId, e);
            markFailed(interviewId, ErrorCode.ANALYSIS_500);
            throw e;
        }
    }

    /** PENDING일 때만 바꾸므로 면접을 읽기 전에 실패해도 부를 수 있다. 기록 실패는 재시작 복구(AC10)가 정리한다. */
    private void markFailed(long interviewId, ErrorCode code) {
        try {
            saveService.markFailed(interviewId, code);
        } catch (RuntimeException e) {
            log.error("면접 {}: FAILED 기록 실패 ({})", interviewId, code, e);
        }
    }

    private Optional<List<GeneratedQuestion>> call(AnalysisInterview interview) {
        AnalysisDetail analysis =
                mapper.findAnalysis(interview.getAnalysisId())
                        .orElseThrow(() -> new IllegalStateException("분석 없음"));
        InterviewPrompts.Input input =
                InterviewPrompts.input(
                        analysis.getOverview(),
                        mapper.findFiles(analysis.getId()),
                        conceptService.findForQuiz(analysis.getId()),
                        properties);
        String prompt =
                templates.render(
                        LlmStage.INTERVIEW,
                        analysis.getLevel(),
                        Map.of(
                                "projectName",
                                ExplainPrompts.escapeDelimiters(
                                        Objects.requireNonNullElse(analysis.getProjectName(), "")),
                                "overview",
                                input.overview(),
                                "fileList",
                                input.fileList(),
                                "concepts",
                                input.concepts(),
                                "files",
                                input.files().isEmpty() ? InterviewPrompts.NONE : input.files()));
        LlmRequest request =
                new LlmRequest(
                        LlmStage.INTERVIEW,
                        analysis.getLevel(),
                        prompt,
                        analysis.getUserId(),
                        analysis.getId());
        return LlmJsonCall.call(
                llm, request, root -> InterviewResponseParser.parse(root, input, properties));
    }
}
