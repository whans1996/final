package com.vibenote.analysis.concept;

import com.vibenote.analysis.domain.Analysis;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.ExplainPrompts;
import com.vibenote.analysis.explain.LlmJsonCall;
import com.vibenote.analysis.explain.PromptTemplates;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.concept.domain.Concept;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmStage;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 분석의 개념을 LLM 1회로 뽑는다 (F-08 AC2·AC6). 형식 오류는 1회 재시도하고, 두 번 다 틀리거나 LLM이 응답하지 못하면 연결 없이 계속하도록 빈 목록을
 * 돌려준다 (분석은 DONE).
 *
 * <p>{@code @Transactional} 안에서 부르지 않는다 ({@link LlmClient} 주석).
 */
@Component
public class ConceptExtractor {

    private static final Logger log = LoggerFactory.getLogger(ConceptExtractor.class);

    private final LlmClient llm;
    private final PromptTemplates templates;
    private final ConceptExtractProperties properties;

    public ConceptExtractor(
            LlmClient llm, PromptTemplates templates, ConceptExtractProperties properties) {
        this.llm = llm;
        this.templates = templates;
        this.properties = properties;
    }

    /**
     * @param concepts 기존 카드 전체 (상태 무관, 이름 비교에 HIDDEN도 쓴다)
     * @param files 선별 파일 전체 (id 오름차순)
     * @return 저장할 항목. 실패하면 빈 목록
     */
    public List<ExtractedConcept> extract(
            Analysis analysis, List<Concept> concepts, List<AnalysisFile> files) {
        ConceptPrompts.Input input = ConceptPrompts.input(concepts, files, properties);
        String prompt =
                templates.render(
                        LlmStage.CONCEPT,
                        analysis.getLevel(),
                        Map.of(
                                "projectName",
                                        ExplainPrompts.escapeDelimiters(analysis.getProjectName()),
                                "catalog", input.catalog(),
                                "fileList", input.fileList(),
                                "files", input.files()));
        LlmRequest request =
                new LlmRequest(
                        LlmStage.CONCEPT,
                        analysis.getLevel(),
                        prompt,
                        analysis.getUserId(),
                        analysis.getId());
        try {
            return LlmJsonCall.call(
                            llm,
                            request,
                            root ->
                                    ConceptResponseParser.parse(
                                            root,
                                            input,
                                            concepts,
                                            analysis.getLevel(),
                                            properties.maxLinks(),
                                            properties.maxDrafts(),
                                            analysis.getId()))
                    .orElseGet(
                            () -> {
                                log.warn("분석 {} 개념 추출: 형식 오류로 연결 없이 계속", analysis.getId());
                                return List.of();
                            });
        } catch (BusinessException e) {
            log.warn("분석 {} 개념 추출: LLM 실패로 연결 없이 계속 ({})", analysis.getId(), e.getErrorCode());
            return List.of();
        }
    }
}
