package com.vibenote.analysis.explain;

import static com.vibenote.analysis.explain.LlmJsonCall.cut;
import static com.vibenote.analysis.explain.LlmJsonCall.text;

import com.vibenote.analysis.domain.Analysis;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.AnalysisOverview.LayerRole;
import com.vibenote.analysis.explain.AnalysisOverview.ReadingStep;
import com.vibenote.analysis.explain.LlmJsonCall.InvalidResponseException;
import com.vibenote.analysis.pipeline.AnalysisFailedException;
import com.vibenote.analysis.pipeline.Layer;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmStage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * 프로젝트 개요를 LLM 1회로 만든다 (F-06 AC2·AC5·AC6·AC8). 개요가 없으면 파일 설명의 맥락도 없으므로 실패하면 분석을 멈춘다.
 *
 * <p>{@code @Transactional} 안에서 부르지 않는다 ({@link LlmClient} 주석).
 */
@Component
public class OverviewExplainer {

    static final int MAX_SUMMARY = 800;
    static final int MAX_TECH = 10;
    static final int MAX_TECH_LENGTH = 40;
    static final int MAX_LAYER_ROLE = 300;
    static final int MAX_READING = 5;
    static final int MAX_REASON = 200;

    private static final Set<String> LAYERS =
            Arrays.stream(Layer.values()).map(Layer::value).collect(Collectors.toSet());

    private final LlmClient llm;
    private final PromptTemplates templates;
    private final ExplainProperties properties;

    public OverviewExplainer(
            LlmClient llm, PromptTemplates templates, ExplainProperties properties) {
        this.llm = llm;
        this.templates = templates;
        this.properties = properties;
    }

    /**
     * @return 검증·정리한 개요 + {@code apis}
     * @throws AnalysisFailedException LLM이 응답하지 못하면 LLM_503, 두 번 다 형식이 틀리면 LLM_502
     */
    public AnalysisOverview explain(
            Analysis analysis, List<AnalysisFile> files, List<ApiEndpoint> apis) {
        ExplainPrompts.OverviewInput input =
                ExplainPrompts.overviewInput(files, apis, properties.overviewMaxChars());
        String prompt =
                templates.render(
                        LlmStage.OVERVIEW,
                        analysis.getLevel(),
                        Map.of(
                                "projectName",
                                        ExplainPrompts.escapeDelimiters(analysis.getProjectName()),
                                "fileTree", input.fileTree(),
                                "apis", input.apis(),
                                "sources", input.sources()));
        LlmRequest request =
                new LlmRequest(
                        LlmStage.OVERVIEW,
                        analysis.getLevel(),
                        prompt,
                        analysis.getUserId(),
                        analysis.getId());
        Map<String, Long> fileIds = new HashMap<>();
        files.forEach(f -> fileIds.putIfAbsent(f.getPath(), f.getId()));
        try {
            return LlmJsonCall.call(llm, request, root -> parse(root, fileIds, apis))
                    .orElseThrow(() -> new AnalysisFailedException(ErrorCode.LLM_502));
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.LLM_503) {
                throw new AnalysisFailedException(ErrorCode.LLM_503);
            }
            throw e;
        }
    }

    private static AnalysisOverview parse(
            JsonNode root, Map<String, Long> fileIds, List<ApiEndpoint> apis) {
        if (!root.isObject()) {
            throw new InvalidResponseException("객체가 아님");
        }
        String summary = text(root, "summary");
        if (summary.isBlank()) {
            throw new InvalidResponseException("summary가 비어 있음");
        }
        JsonNode techStack = array(root, "techStack", true);
        JsonNode layers = array(root, "layers", true);
        JsonNode readingOrder = array(root, "readingOrder", false);

        List<String> tech = new ArrayList<>();
        for (JsonNode t : techStack) {
            if (!t.isString()) {
                throw new InvalidResponseException("techStack 항목이 문자열이 아님");
            }
            if (tech.size() < MAX_TECH && !t.asString().isBlank()) {
                tech.add(cut(t.asString().strip(), MAX_TECH_LENGTH));
            }
        }

        List<LayerRole> layerRoles = new ArrayList<>();
        for (JsonNode l : layers) {
            JsonNode layer = l.get("layer");
            JsonNode role = l.get("role");
            if (layer != null
                    && layer.isString()
                    && LAYERS.contains(layer.asString())
                    && role != null
                    && role.isString()) {
                layerRoles.add(
                        new LayerRole(layer.asString(), cut(role.asString(), MAX_LAYER_ROLE)));
            }
        }

        List<ReadingStep> steps = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (readingOrder != null) {
            for (JsonNode r : readingOrder) {
                JsonNode path = r.get("path");
                if (steps.size() >= MAX_READING
                        || path == null
                        || !path.isString()
                        || !fileIds.containsKey(path.asString())
                        || !seen.add(path.asString())) {
                    continue;
                }
                JsonNode reason = r.get("reason");
                steps.add(
                        new ReadingStep(
                                fileIds.get(path.asString()),
                                path.asString(),
                                reason != null && reason.isString()
                                        ? cut(reason.asString(), MAX_REASON)
                                        : ""));
            }
        }
        return new AnalysisOverview(
                cut(summary, MAX_SUMMARY), tech, layerRoles, steps, List.copyOf(apis));
    }

    /** 배열 필드. 필수가 아니면 없거나 null일 때 null을 돌려준다. */
    private static JsonNode array(JsonNode root, String field, boolean required) {
        JsonNode node = root.get(field);
        if (!required && (node == null || node.isNull())) {
            return null;
        }
        if (node == null || !node.isArray()) {
            throw new InvalidResponseException(field + "가 배열이 아님");
        }
        return node;
    }
}
