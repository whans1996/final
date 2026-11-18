package com.vibenote.analysis.explain;

import static com.vibenote.analysis.explain.ApiDetectorTest.file;
import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.domain.Analysis;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.common.Level;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmStage;
import java.util.List;
import org.junit.jupiter.api.Test;

/** F-06 AC2~AC6 파일 묶음 설명. */
class FileExplainerTest {

    static Analysis analysis(Level level) {
        Analysis analysis = new Analysis();
        analysis.setId(42L);
        analysis.setUserId(7L);
        analysis.setProjectName("todo-app");
        analysis.setLevel(level);
        return analysis;
    }

    static final ExplainProperties PROPS = new ExplainProperties(6, 6000, 24000);

    private final List<AnalysisFile> batch =
            List.of(
                    file(11, "web/A.java", "controller", "class A {}"),
                    file(12, "svc/B.java", "service", "class B {}"),
                    file(13, "map/C.java", "mapper", "interface C {}"));

    private static String item(int no, String role, String explanation) {
        return "{\"no\":%d,\"role\":\"%s\",\"explanation\":\"%s\"}"
                .formatted(no, role, explanation);
    }

    private static String files(String... items) {
        return "{\"files\":[" + String.join(",", items) + "]}";
    }

    @Test
    void AC2_분석의_수준과_소유자로_FILE_단계를_한_번_부른다() {
        FakeLlmClient llm =
                new FakeLlmClient()
                        .answer(
                                files(
                                        item(1, "r1", "e1"),
                                        item(2, "r2", "e2"),
                                        item(3, "r3", "e3")));
        FileExplainer explainer = new FileExplainer(llm, new PromptTemplates(), PROPS);

        List<FileExplanation> result = explainer.explain(analysis(Level.BEGINNER), "할 일 앱", batch);

        assertThat(result)
                .containsExactly(
                        new FileExplanation(11, "r1", "e1"),
                        new FileExplanation(12, "r2", "e2"),
                        new FileExplanation(13, "r3", "e3"));
        assertThat(llm.requests).hasSize(1);
        LlmRequest request = llm.requests.get(0);
        assertThat(request.stage()).isEqualTo(LlmStage.FILE);
        assertThat(request.level()).isEqualTo(Level.BEGINNER);
        assertThat(request.userId()).isEqualTo(7L);
        assertThat(request.analysisId()).isEqualTo(42L);
        assertThat(request.prompt())
                .contains("todo-app", "할 일 앱")
                .contains("<<<FILE no=3 path=map/C.java layer=mapper>>>");
    }

    @Test
    void AC3_긴_파일은_프롬프트에서만_자른다() {
        AnalysisFile big = file(11, "web/A.java", "controller", "z".repeat(7000));
        FakeLlmClient llm = new FakeLlmClient().answer(files(item(1, "r", "e")));

        new FileExplainer(llm, new PromptTemplates(), PROPS)
                .explain(analysis(Level.JUNIOR), "요약", List.of(big));

        assertThat(llm.requests.get(0).prompt())
                .contains("// ... (이하 1000자 생략)")
                .doesNotContain("z".repeat(6001));
        assertThat(big.getContent()).hasSize(7000);
    }

    @Test
    void AC4_범위_밖_중복_no는_버리고_긴_값은_잘라_저장한다() {
        FakeLlmClient llm =
                new FakeLlmClient()
                        .answer(
                                files(
                                        item(2, "r".repeat(150), "e".repeat(2000)),
                                        item(0, "zero", "x"),
                                        item(4, "four", "x"),
                                        item(1, "first", "one"),
                                        item(1, "dup", "dup")));

        List<FileExplanation> result =
                new FileExplainer(llm, new PromptTemplates(), PROPS)
                        .explain(analysis(Level.JUNIOR), "요약", batch);

        assertThat(result)
                .containsExactlyInAnyOrder(
                        new FileExplanation(12, "r".repeat(100), "e".repeat(1500)),
                        new FileExplanation(11, "first", "one"));
    }

    @Test
    void AC5_형식이_틀리면_같은_요청을_한_번만_다시_보내고_두_번째를_쓴다() {
        FakeLlmClient llm =
                new FakeLlmClient().answer("이건 JSON이 아님").answer(files(item(1, "r", "e")));

        List<FileExplanation> result =
                new FileExplainer(llm, new PromptTemplates(), PROPS)
                        .explain(analysis(Level.JUNIOR), "요약", batch);

        assertThat(result).containsExactly(new FileExplanation(11, "r", "e"));
        assertThat(llm.requests).hasSize(2);
        assertThat(llm.requests.get(1)).isEqualTo(llm.requests.get(0));
    }

    @Test
    void AC5_두_번_다_틀리면_빈_결과다() {
        FakeLlmClient llm =
                new FakeLlmClient()
                        .answer("{\"files\":\"not array\"}")
                        .answer(files("{\"no\":1,\"role\":\"r\"}"));

        List<FileExplanation> result =
                new FileExplainer(llm, new PromptTemplates(), PROPS)
                        .explain(analysis(Level.JUNIOR), "요약", batch);

        assertThat(result).isEmpty();
        assertThat(llm.requests).hasSize(2);
    }

    @Test
    void AC6_LLM_503이면_재시도_없이_빈_결과다() {
        FakeLlmClient llm = new FakeLlmClient().fail(new BusinessException(ErrorCode.LLM_503));

        List<FileExplanation> result =
                new FileExplainer(llm, new PromptTemplates(), PROPS)
                        .explain(analysis(Level.JUNIOR), "요약", batch);

        assertThat(result).isEmpty();
        assertThat(llm.requests).hasSize(1);
    }

    @Test
    void AC4_범위_밖_no는_필드가_틀려도_묶음을_다시_요청하지_않고_버린다() {
        FakeLlmClient llm =
                new FakeLlmClient()
                        .answer(files(item(1, "r1", "e1"), "{\"no\":9}", "{\"no\":1,\"role\":3}"));
        FileExplainer explainer = new FileExplainer(llm, new PromptTemplates(), PROPS);

        List<FileExplanation> result = explainer.explain(analysis(Level.JUNIOR), "s", batch);

        assertThat(result).containsExactly(new FileExplanation(11, "r1", "e1"));
        assertThat(llm.requests).hasSize(1);
    }

    @Test
    void AC4_설명이_비어_있으면_그_파일은_NULL로_남기고_빈_역할은_NULL로_저장한다() {
        FakeLlmClient llm =
                new FakeLlmClient()
                        .answer(
                                files(
                                        item(1, "r1", "  "),
                                        item(2, " ", "e2"),
                                        item(3, "r3", "e3")));
        FileExplainer explainer = new FileExplainer(llm, new PromptTemplates(), PROPS);

        List<FileExplanation> result = explainer.explain(analysis(Level.JUNIOR), "s", batch);

        assertThat(result)
                .containsExactly(
                        new FileExplanation(12, null, "e2"), new FileExplanation(13, "r3", "e3"));
    }
}
