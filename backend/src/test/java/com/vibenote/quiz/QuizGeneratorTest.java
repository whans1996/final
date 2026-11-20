package com.vibenote.quiz;

import static com.vibenote.quiz.QuizPromptsTest.PROPS;
import static com.vibenote.quiz.QuizPromptsTest.concept;
import static com.vibenote.quiz.QuizPromptsTest.file;
import static com.vibenote.quiz.QuizResponseParserTest.item;
import static com.vibenote.quiz.QuizResponseParserTest.questions;
import static com.vibenote.quiz.QuizResponseParserTest.valid;
import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.concept.LinkedConcept;
import com.vibenote.analysis.domain.Analysis;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.FakeLlmClient;
import com.vibenote.analysis.explain.PromptTemplates;
import com.vibenote.common.Level;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.llm.LlmStage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** F-14 AC2(요청 구성)·AC4(재시도와 실패 처리). */
class QuizGeneratorTest {

    private static final List<LinkedConcept> CONCEPTS =
            List.of(concept(3, "의존성 주입 (DI)", "생성자로 받습니다."));

    private static final List<AnalysisFile> FILES =
            List.of(file(10, "web/A.java", "controller", "class A {}"));

    private static Analysis analysis(Level level) {
        Analysis analysis = new Analysis();
        analysis.setId(42L);
        analysis.setUserId(7L);
        analysis.setProjectName("todo-app");
        analysis.setLevel(level);
        return analysis;
    }

    private static List<GeneratedQuestion> generate(FakeLlmClient llm, Level level) {
        return new QuizGenerator(llm, new PromptTemplates(), PROPS)
                .generate(analysis(level), CONCEPTS, FILES);
    }

    private static String ok() {
        List<String> items = new ArrayList<>(valid(4));
        items.add(item(5, "3"));
        return questions(items);
    }

    @Test
    void AC2_수준별_quiz_프롬프트로_1회_부르고_입력을_채운다() {
        for (Level level : Level.values()) {
            FakeLlmClient llm = new FakeLlmClient().answer(ok());

            List<GeneratedQuestion> result = generate(llm, level);

            assertThat(result).hasSize(5);
            assertThat(result.get(4).conceptId()).isEqualTo(3L);
            assertThat(llm.requests).hasSize(1);
            var request = llm.requests.get(0);
            assertThat(request.stage()).isEqualTo(LlmStage.QUIZ);
            assertThat(request.level()).isEqualTo(level);
            assertThat(request.userId()).isEqualTo(7L);
            assertThat(request.analysisId()).isEqualTo(42L);
            assertThat(request.prompt())
                    .contains("todo-app")
                    .contains("3 | 의존성 주입 (DI) | 생성자로 받습니다.")
                    .contains("<<<FILE no=1 path=web/A.java layer=controller>>>\nclass A {}")
                    .contains("\"questions\"")
                    .doesNotContain("{{");
        }
    }

    @Test
    void AC2_수준마다_다른_프롬프트를_쓴다() {
        List<String> prompts = new ArrayList<>();
        for (Level level : Level.values()) {
            FakeLlmClient llm = new FakeLlmClient().answer(ok());
            generate(llm, level);
            prompts.add(llm.requests.get(0).prompt());
        }

        assertThat(prompts).doesNotHaveDuplicates();
        assertThat(prompts.get(0)).contains("입문자");
        assertThat(prompts.get(1)).contains("주니어 개발자");
        assertThat(prompts.get(2)).contains("숙련 개발자");
    }

    @Test
    void AC4_형식_오류면_1회만_다시_부르고_두_번째가_맞으면_쓴다() {
        FakeLlmClient llm = new FakeLlmClient().answer("not json").answer(ok());

        assertThat(generate(llm, Level.JUNIOR)).hasSize(5);
        assertThat(llm.requests).hasSize(2);
        assertThat(llm.requests.get(1)).isEqualTo(llm.requests.get(0));
    }

    @Test
    void AC4_형식이_두_번_다_틀리면_빈_결과() {
        FakeLlmClient llm =
                new FakeLlmClient().answer(questions(valid(4))).answer("{\"questions\": 1}");

        assertThat(generate(llm, Level.JUNIOR)).isEmpty();
        assertThat(llm.requests).hasSize(2);
    }

    @Test
    void AC4_LLM_503이면_재시도_없이_빈_결과() {
        FakeLlmClient llm = new FakeLlmClient().fail(new BusinessException(ErrorCode.LLM_503));

        assertThat(generate(llm, Level.JUNIOR)).isEmpty();
        assertThat(llm.requests).hasSize(1);
    }
}
