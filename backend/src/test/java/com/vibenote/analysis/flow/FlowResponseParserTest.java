package com.vibenote.analysis.flow;

import static com.vibenote.analysis.flow.FlowContextBuilderTest.file;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.LlmJsonCall.InvalidResponseException;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** F-07 AC4(저장 형태 정리)·AC5(필수 필드 검증). */
class FlowResponseParserTest {

    static final JsonMapper JSON = JsonMapper.builder().build();

    static final List<AnalysisFile> FILES =
            List.of(
                    file(101, "src/a/TodoController.java", "controller", "c"),
                    file(102, "src/a/TodoService.java", "service", "s"));

    static FlowResult parse(String json) {
        JsonNode root = JSON.readTree(json);
        return FlowResponseParser.parse(root, FILES, 12);
    }

    static String step(String path, String title) {
        return """
        {"path":"%s","symbol":"A.b","title":"%s","description":"설명"}\
        """
                .formatted(path, title);
    }

    @Test
    void AC4_분석_파일_경로면_fileId와_layer를_붙이고_아니면_null() {
        FlowResult result =
                parse(
                        """
                        {"summary":"요약","steps":[%s,%s,%s]}\
                        """
                                .formatted(
                                        step("src/a/TodoController.java", "받기"),
                                        step("src/a/Unknown.java", "모름"),
                                        step("src/a/TodoService.java", "처리")));

        assertThat(result.summary()).isEqualTo("요약");
        assertThat(result.steps())
                .containsExactly(
                        new FlowStep(
                                1,
                                "controller",
                                101L,
                                "src/a/TodoController.java",
                                "A.b",
                                "받기",
                                "설명"),
                        new FlowStep(2, null, null, "src/a/Unknown.java", "A.b", "모름", "설명"),
                        new FlowStep(
                                3, "service", 102L, "src/a/TodoService.java", "A.b", "처리", "설명"));
    }

    @Test
    void AC4_12개까지만_남기고_번호를_다시_매긴다() {
        StringBuilder steps = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            steps.append(i == 0 ? "" : ",").append(step("p" + i, "t" + i));
        }
        // 13번째 이후 항목의 형식 오류는 버리므로 재시도 사유가 아니다
        steps.append(",{\"path\":1}");

        FlowResult result = parse("{\"summary\":\"s\",\"steps\":[" + steps + "]}");

        assertThat(result.steps()).hasSize(12);
        assertThat(result.steps()).extracting(FlowStep::no).startsWith(1, 2).endsWith(12);
        assertThat(result.steps().get(11).title()).isEqualTo("t11");
    }

    @Test
    void AC4_긴_필드는_잘라_저장하고_symbol은_없으면_null() {
        String json =
                """
                {"summary":"%s","steps":[{"path":"p","title":"%s","description":"%s","symbol":"%s"},
                {"path":"p","title":"t","description":"d"}]}\
                """
                        .formatted(
                                "가".repeat(600), "나".repeat(70), "다".repeat(700), "S".repeat(150));

        FlowResult result = parse(json);

        assertThat(result.summary()).hasSize(500);
        FlowStep first = result.steps().get(0);
        assertThat(first.title()).hasSize(60);
        assertThat(first.description()).hasSize(600);
        assertThat(first.symbol()).hasSize(100);
        assertThat(result.steps().get(1).symbol()).isNull();
    }

    @Test
    void AC5_필수_필드나_타입이_틀리면_형식_오류() {
        List<String> invalid =
                List.of(
                        "[]",
                        "{\"steps\":[" + step("p", "t") + "]}",
                        "{\"summary\":\"  \",\"steps\":[" + step("p", "t") + "]}",
                        "{\"summary\":\"s\",\"steps\":[]}",
                        "{\"summary\":\"s\",\"steps\":{}}",
                        "{\"summary\":\"s\",\"steps\":[\"x\"]}",
                        "{\"summary\":\"s\",\"steps\":[{\"path\":\"p\",\"title\":\"t\"}]}",
                        "{\"summary\":\"s\",\"steps\":[{\"path\":1,\"title\":\"t\",\"description\":\"d\"}]}",
                        "{\"summary\":\"s\",\"steps\":[{\"path\":\"p\",\"title\":\"t\",\"description\":\"d\",\"symbol\":3}]}");

        for (String json : invalid) {
            assertThatThrownBy(() -> parse(json))
                    .as(json)
                    .isInstanceOf(InvalidResponseException.class);
        }
    }
}
