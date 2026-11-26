package com.vibenote.analysis.flow;

import com.vibenote.analysis.AnalysisService;
import com.vibenote.analysis.domain.AnalysisDetail;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.ApiEndpoint;
import com.vibenote.analysis.explain.ExplainPrompts;
import com.vibenote.analysis.explain.ExplainProperties;
import com.vibenote.analysis.explain.LlmJsonCall;
import com.vibenote.analysis.explain.PromptTemplates;
import com.vibenote.common.Level;
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
 * PENDING 흐름 하나를 만든다: 입력 파일 추적 → 프롬프트 → LLM 1회(형식 오류 시 1회 재시도) → 검증·저장 (F-07 AC1·AC5). 실패하면 그 흐름만
 * FAILED로 둔다.
 *
 * <p>{@code @Transactional} 안에서 부르지 않는다 ({@link LlmClient} 주석). 흐름 실행기({@code flowExecutor}) 스레드에서
 * 돈다.
 */
@Component
public class FlowGenerator {

    private static final Logger log = LoggerFactory.getLogger(FlowGenerator.class);

    private final FlowService flowService;
    private final AnalysisService analysisService;
    private final LlmClient llm;
    private final PromptTemplates templates;
    private final FlowProperties properties;
    private final ExplainProperties explainProperties;

    public FlowGenerator(
            FlowService flowService,
            AnalysisService analysisService,
            LlmClient llm,
            PromptTemplates templates,
            FlowProperties properties,
            ExplainProperties explainProperties) {
        this.flowService = flowService;
        this.analysisService = analysisService;
        this.llm = llm;
        this.templates = templates;
        this.properties = properties;
        this.explainProperties = explainProperties;
    }

    public void generate(long flowId) {
        try {
            Optional<AnalysisFlow> pending = flowService.findPending(flowId);
            if (pending.isEmpty()) {
                return;
            }
            Optional<FlowResult> result = call(pending.get());
            if (result.isPresent()) {
                flowService.complete(flowId, result.get());
            } else {
                log.warn("흐름 {}: 형식 오류가 두 번 나 FAILED", flowId);
                markFailed(flowId, ErrorCode.LLM_502);
            }
        } catch (BusinessException e) {
            log.warn("흐름 {}: LLM 실패로 FAILED ({})", flowId, e.getErrorCode());
            markFailed(flowId, e.getErrorCode());
        } catch (RuntimeException e) {
            log.error("흐름 {}: 예기치 못한 오류로 FAILED", flowId, e);
            markFailed(flowId, ErrorCode.ANALYSIS_500);
        }
    }

    /** PENDING일 때만 바꾸므로 흐름을 읽기 전에 실패해도 부를 수 있다. 기록 실패는 재시작 복구(AC11)가 정리한다. */
    private void markFailed(long flowId, ErrorCode code) {
        try {
            flowService.markFailed(flowId, code);
        } catch (RuntimeException e) {
            log.error("흐름 {}: FAILED 기록 실패 ({})", flowId, code, e);
        }
    }

    private Optional<FlowResult> call(AnalysisFlow flow) {
        AnalysisDetail analysis =
                flowService
                        .findAnalysis(flow.getAnalysisId())
                        .orElseThrow(() -> new IllegalStateException("분석 없음"));
        if (analysis.getOverview() == null) {
            throw new IllegalStateException("개요 없음: 분석 " + analysis.getId());
        }
        ApiEndpoint api =
                analysis.getOverview().apis().stream()
                        .filter(a -> FlowService.key(a).equals(flow.getEndpoint()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("개요에 없는 API"));
        return create(
                new FlowSource(
                        analysis.getProjectName(),
                        analysis.getLevel(),
                        analysis.getOverview().summary(),
                        analysis.getUserId(),
                        analysis.getId()),
                api,
                analysisService.findFiles(analysis.getId()));
    }

    /**
     * 흐름 하나를 DB 없이 만든다: 입력 파일 추적 → 프롬프트 → LLM 1회(형식 오류 시 1회 재시도) → 검증. 평가 러너도 앱과 같은 경로로 부른다 (E-1
     * AC1).
     *
     * @param files 분석의 선별 파일 (id 오름차순)
     * @return 형식 오류가 두 번 나면 empty
     */
    public Optional<FlowResult> create(
            FlowSource source, ApiEndpoint api, List<AnalysisFile> files) {
        List<AnalysisFile> input =
                FlowContextBuilder.inputFiles(files, api.fileId(), properties.maxFiles());
        if (input.isEmpty()) {
            throw new IllegalStateException("controller 파일 없음: " + api.fileId());
        }
        String prompt =
                templates.render(
                        LlmStage.FLOW,
                        source.level(),
                        Map.of(
                                "projectName",
                                        ExplainPrompts.escapeDelimiters(source.projectName()),
                                "endpoint", ExplainPrompts.escapeDelimiters(FlowService.key(api)),
                                "overviewSummary",
                                        ExplainPrompts.escapeDelimiters(
                                                Objects.requireNonNullElse(
                                                        source.overviewSummary(), "")),
                                "files",
                                        FlowContextBuilder.render(
                                                input,
                                                explainProperties.maxFileChars(),
                                                properties.maxChars())));
        LlmRequest request =
                new LlmRequest(
                        LlmStage.FLOW,
                        source.level(),
                        prompt,
                        source.userId(),
                        source.analysisId());
        return LlmJsonCall.call(
                llm, request, root -> FlowResponseParser.parse(root, files, properties.maxSteps()));
    }

    /** 흐름 프롬프트에 들어가는 분석 정보. 평가 러너는 {@code userId}·{@code analysisId}를 null로 둔다. */
    public record FlowSource(
            String projectName,
            Level level,
            String overviewSummary,
            Long userId,
            Long analysisId) {}
}
