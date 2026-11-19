package com.vibenote.quiz;

import com.vibenote.analysis.concept.LinkedConcept;
import com.vibenote.analysis.domain.Analysis;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.ExplainPrompts;
import com.vibenote.analysis.explain.LlmJsonCall;
import com.vibenote.analysis.explain.PromptTemplates;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmStage;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 분석의 퀴즈를 LLM 1회로 만든다 (F-14 AC2·AC4). 형식 오류는 1회 재시도하고, 두 번 다 틀리거나 LLM이 응답하지 못하면 퀴즈 없이 계속하도록 빈 목록을
 * 돌려준다 (분석은 DONE).
 *
 * <p>{@code @Transactional} 안에서 부르지 않는다 ({@link LlmClient} 주석).
 */
@Component
public class QuizGenerator {

    private static final Logger log = LoggerFactory.getLogger(QuizGenerator.class);

    private final LlmClient llm;
    private final PromptTemplates templates;
    private final QuizProperties properties;

    public QuizGenerator(LlmClient llm, PromptTemplates templates, QuizProperties properties) {
        this.llm = llm;
        this.templates = templates;
        this.properties = properties;
    }

    /**
     * @param concepts 분석의 연결 개념 (HIDDEN 제외, {@code concept_id} 오름차순)
     * @param files 선별 파일 전체 (id 오름차순)
     * @return 저장할 문항 (순서 = 문항 번호). 실패하면 빈 목록
     */
    public List<GeneratedQuestion> generate(
            Analysis analysis, List<LinkedConcept> concepts, List<AnalysisFile> files) {
        QuizPrompts.Input input = QuizPrompts.input(concepts, files, properties);
        String prompt =
                templates.render(
                        LlmStage.QUIZ,
                        analysis.getLevel(),
                        Map.of(
                                "projectName",
                                        ExplainPrompts.escapeDelimiters(analysis.getProjectName()),
                                "concepts", input.concepts(),
                                "files", input.files()));
        LlmRequest request =
                new LlmRequest(
                        LlmStage.QUIZ,
                        analysis.getLevel(),
                        prompt,
                        analysis.getUserId(),
                        analysis.getId());
        try {
            return LlmJsonCall.call(
                            llm,
                            request,
                            root ->
                                    QuizResponseParser.parse(
                                            root,
                                            input.conceptIds(),
                                            analysis.getId(),
                                            properties.minQuestions(),
                                            properties.maxQuestions()))
                    .orElseGet(
                            () -> {
                                log.warn("분석 {} 퀴즈 생성: 형식 오류로 퀴즈 없이 계속", analysis.getId());
                                return List.of();
                            });
        } catch (BusinessException e) {
            log.warn("분석 {} 퀴즈 생성: LLM 실패로 퀴즈 없이 계속 ({})", analysis.getId(), e.getErrorCode());
            return List.of();
        }
    }
}
