package com.vibenote.prompteval;

import static com.vibenote.prompteval.GradeFixtures.JSON;
import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

/** E-1 AC6: 라벨 금지 사실 문장을 수준마다 판정 모델이 출력과 대조한다. 판정 응답은 가짜 클라이언트가 준다. */
class ForbiddenJudgeTest {

    @TempDir Path dir;

    @Test
    void AC6_금지_사실은_수준마다_판정하고_형식_오류는_뺀다() {
        // 입문: 1번 주장. 주니어: 형식 오류 → 재질문에 주장 없음. 시니어: 범위 밖 번호 2회 → 뺀다
        List<String> prompts = new ArrayList<>();
        Map<String, Integer> asked = new LinkedHashMap<>();
        LlmClient judge =
                request -> {
                    String p = request.prompt();
                    prompts.add(p);
                    String level =
                            p.contains("입문 설명")
                                    ? "beginner"
                                    : p.contains("주니어 설명") ? "junior" : "senior";
                    int n = asked.merge(level, 1, Integer::sum);
                    String content =
                            switch (level) {
                                case "beginner" -> "{\"made\":[1]}";
                                case "junior" -> n == 1 ? "판정 결과입니다" : "{\"made\":[]}";
                                default -> "{\"made\":[3]}";
                            };
                    return new LlmResponse(content, "gemini-test", 1, 1);
                };
        Map<String, Object> details = new LinkedHashMap<>();

        JsonNode b =
                JSON.valueToTree(
                        new ForbiddenJudge(judge)
                                .judge(
                                        GradeFixtures.report(GradeFixtures.zip(dir)),
                                        GradeFixtures.label(),
                                        details));

        assertThat(prompts).hasSize(5);
        assertThat(b.path("forbiddenHits").asInt()).isEqualTo(1);
        assertThat(b.path("forbiddenChecked").asInt()).isEqualTo(4);
        assertThat(b.path("forbiddenExcluded").asInt()).isEqualTo(1);
        // 금지 문장은 번호를 달아 넣고, 출력 텍스트에는 경로를 넣지 않는다
        String first = prompts.get(0);
        assertThat(first)
                .contains("1. " + GradeFixtures.label().path("forbidden").get(0).asString())
                .contains("2. ")
                .doesNotContain("src/main/java/io/spring");
        assertThat(JSON.valueToTree(details).path("forbiddenHits")).hasSize(1);
    }
}
