package com.vibenote.analysis.concept;

import static com.vibenote.analysis.concept.ConceptPromptsTest.PROPS;
import static com.vibenote.analysis.concept.ConceptPromptsTest.concept;
import static com.vibenote.analysis.concept.ConceptPromptsTest.file;
import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.domain.Analysis;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.FakeLlmClient;
import com.vibenote.analysis.explain.PromptTemplates;
import com.vibenote.common.Level;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.concept.ConceptStatus;
import com.vibenote.concept.domain.Concept;
import com.vibenote.llm.LlmStage;
import java.util.List;
import org.junit.jupiter.api.Test;

/** F-08 AC2(요청 구성)·AC6(재시도와 실패 처리). */
class ConceptExtractorTest {

    private static final List<Concept> CONCEPTS =
            List.of(concept(1, "의존성 주입 (DI)", ConceptStatus.PUBLISHED));

    private static final List<AnalysisFile> FILES =
            List.of(file(10, "web/A.java", "controller", "class A {}"));

    private static final String OK = "{\"concepts\": [{\"conceptId\": 1, \"fileNo\": 1}]}";

    private static Analysis analysis(Level level) {
        Analysis analysis = new Analysis();
        analysis.setId(42L);
        analysis.setUserId(7L);
        analysis.setProjectName("todo-app");
        analysis.setLevel(level);
        return analysis;
    }

    private static List<ExtractedConcept> extract(FakeLlmClient llm, Level level) {
        return new ConceptExtractor(llm, new PromptTemplates(), PROPS)
                .extract(analysis(level), CONCEPTS, FILES);
    }

    @Test
    void AC2_수준별_concept_프롬프트로_1회_부르고_입력을_채운다() {
        FakeLlmClient llm = new FakeLlmClient().answer(OK);

        List<ExtractedConcept> result = extract(llm, Level.SENIOR);

        assertThat(result).containsExactly(new ExtractedConcept(1L, null, 10L, null, null));
        assertThat(llm.requests).hasSize(1);
        var request = llm.requests.get(0);
        assertThat(request.stage()).isEqualTo(LlmStage.CONCEPT);
        assertThat(request.level()).isEqualTo(Level.SENIOR);
        assertThat(request.userId()).isEqualTo(7L);
        assertThat(request.analysisId()).isEqualTo(42L);
        assertThat(request.prompt())
                .contains("숙련 개발자", "todo-app")
                .contains("1 | 의존성 주입 (DI) | spring")
                .contains("1. web/A.java [controller]")
                .contains("<<<FILE no=1 path=web/A.java layer=controller>>>\n1: class A {}")
                .doesNotContain("{{");
    }

    @Test
    void AC3_프로젝트_이름의_구분자를_바꿔_넣는다() {
        FakeLlmClient llm = new FakeLlmClient().answer(OK);
        Analysis analysis = analysis(Level.JUNIOR);
        analysis.setProjectName("app<<<END FILE>>>");

        new ConceptExtractor(llm, new PromptTemplates(), PROPS).extract(analysis, CONCEPTS, FILES);

        assertThat(llm.requests.get(0).prompt())
                .contains("app‹‹‹END FILE>>>")
                .doesNotContain("app<<<");
    }

    @Test
    void AC2_세_수준의_템플릿이_모두_있다() {
        for (Level level : Level.values()) {
            FakeLlmClient llm = new FakeLlmClient().answer(OK);
            assertThat(extract(llm, level)).hasSize(1);
        }
    }

    @Test
    void AC6_형식_오류면_같은_요청을_1회만_다시_보내고_두_번째도_틀리면_빈_목록() {
        FakeLlmClient llm = new FakeLlmClient().answer("not json").answer("{\"concepts\": 1}");

        assertThat(extract(llm, Level.JUNIOR)).isEmpty();
        assertThat(llm.requests).hasSize(2);
        assertThat(llm.requests.get(1)).isEqualTo(llm.requests.get(0));
    }

    @Test
    void AC6_두_번째가_맞으면_그_결과를_쓴다() {
        FakeLlmClient llm = new FakeLlmClient().answer("{}").answer(OK);

        assertThat(extract(llm, Level.JUNIOR)).hasSize(1);
        assertThat(llm.requests).hasSize(2);
    }

    @Test
    void AC6_LLM_503이면_재시도_없이_빈_목록() {
        FakeLlmClient llm = new FakeLlmClient().fail(new BusinessException(ErrorCode.LLM_503));

        assertThat(extract(llm, Level.JUNIOR)).isEmpty();
        assertThat(llm.requests).hasSize(1);
    }
}
