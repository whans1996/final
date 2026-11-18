package com.vibenote.analysis.explain;

import static com.vibenote.analysis.explain.LlmJsonCall.cut;
import static com.vibenote.analysis.explain.LlmJsonCall.text;

import com.vibenote.analysis.domain.Analysis;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.LlmJsonCall.InvalidResponseException;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmStage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * 파일 묶음(최대 6개) 하나를 LLM 1회로 설명한다 (F-06 AC2~AC6). 묶음이 실패해도 분석은 계속되므로 예외 대신 빈 결과를 돌려준다.
 *
 * <p>{@code @Transactional} 안에서 부르지 않는다 ({@link LlmClient} 주석).
 */
@Component
public class FileExplainer {

    private static final Logger log = LoggerFactory.getLogger(FileExplainer.class);
    static final int MAX_ROLE = 100;
    static final int MAX_EXPLANATION = 1500;

    private final LlmClient llm;
    private final PromptTemplates templates;
    private final ExplainProperties properties;

    public FileExplainer(LlmClient llm, PromptTemplates templates, ExplainProperties properties) {
        this.llm = llm;
        this.templates = templates;
        this.properties = properties;
    }

    /**
     * @return 응답에 있던 파일의 역할·설명 (응답에 없는 파일은 빠진다). 두 번 다 형식이 틀리거나 LLM이 응답하지 못하면 빈 목록
     */
    public List<FileExplanation> explain(
            Analysis analysis, String overviewSummary, List<AnalysisFile> batch) {
        String prompt =
                templates.render(
                        LlmStage.FILE,
                        analysis.getLevel(),
                        Map.of(
                                "projectName", analysis.getProjectName(),
                                "overviewSummary", overviewSummary,
                                "files", ExplainPrompts.files(batch, properties.maxFileChars())));
        LlmRequest request =
                new LlmRequest(
                        LlmStage.FILE,
                        analysis.getLevel(),
                        prompt,
                        analysis.getUserId(),
                        analysis.getId());
        try {
            return LlmJsonCall.call(llm, request, root -> parse(root, batch))
                    .orElseGet(
                            () -> {
                                log.warn("분석 {} 파일 묶음: 형식 오류로 건너뜀", analysis.getId());
                                return List.of();
                            });
        } catch (BusinessException e) {
            log.warn("분석 {} 파일 묶음: LLM 실패로 건너뜀 ({})", analysis.getId(), e.getErrorCode());
            return List.of();
        }
    }

    private static List<FileExplanation> parse(JsonNode root, List<AnalysisFile> batch) {
        JsonNode files = root.get("files");
        if (!root.isObject() || files == null || !files.isArray()) {
            throw new InvalidResponseException("files가 배열이 아님");
        }
        List<FileExplanation> result = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (JsonNode item : files) {
            JsonNode no = item.get("no");
            if (!item.isObject() || no == null || !no.isIntegralNumber()) {
                throw new InvalidResponseException("files[].no가 정수가 아님");
            }
            int n = no.asInt();
            // 버릴 항목(범위 밖·중복 no)의 필드 오류로 묶음 전체를 다시 요청하지 않도록 먼저 거른다 (AC4)
            if (n < 1 || n > batch.size() || !seen.add(n)) {
                continue;
            }
            String role = text(item, "role");
            String explanation = text(item, "explanation");
            // 빈 설명을 저장하면 화면은 "설명을 만들지 못했습니다"인데 explainedFileCount에는 세어진다. NULL로 남긴다
            if (explanation.isBlank()) {
                continue;
            }
            result.add(
                    new FileExplanation(
                            batch.get(n - 1).getId(),
                            role.isBlank() ? null : cut(role, MAX_ROLE),
                            cut(explanation, MAX_EXPLANATION)));
        }
        return result;
    }
}
