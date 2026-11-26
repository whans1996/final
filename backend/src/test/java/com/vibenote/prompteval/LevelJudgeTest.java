package com.vibenote.prompteval;

import static com.vibenote.prompteval.GradeFixtures.JSON;
import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

/** E-1 AC7: 같은 key의 세 수준 출력을 섞어 판정 LLM에 수준을 묻는다. 판정 응답은 가짜 클라이언트가 준다. */
class LevelJudgeTest {

    @TempDir Path dir;

    /** 섞인 설명 하나 ({@code ### a} 다음 줄부터 다음 항목 또는 출력 형식 안내 전까지, 여러 줄). */
    private static final Pattern ITEM =
            Pattern.compile("### ([a-c])\\n(.*?)(?=\\n### |\\n\\n출력 형식)", Pattern.DOTALL);

    /** 합성 보고서의 수준 표시("입문 설명" 등)를 보고 맞게 답한다. */
    static String correctAnswer(String prompt) {
        Matcher m = ITEM.matcher(prompt.replace("\r", ""));
        List<String> items = new ArrayList<>();
        while (m.find()) {
            String text = m.group(2);
            String level =
                    text.contains("입문 설명")
                            ? "beginner"
                            : text.contains("주니어 설명") ? "junior" : "senior";
            items.add("{\"id\":\"" + m.group(1) + "\",\"level\":\"" + level + "\"}");
        }
        return "{\"items\":[" + String.join(",", items) + "]}";
    }

    static LlmClient recording(List<String> prompts) {
        return request -> {
            prompts.add(request.prompt());
            return new LlmResponse(correctAnswer(request.prompt()), "gemini-test", 1, 1);
        };
    }

    @Test
    void AC7_같은_seed면_섞는_순서가_같다() {
        JsonNode report = GradeFixtures.report(GradeFixtures.zip(dir));
        List<String> first = new ArrayList<>();
        List<String> second = new ArrayList<>();

        Map<String, Object> result = new LevelJudge(recording(first), 42).judge(report);
        new LevelJudge(recording(second), 42).judge(report);

        // 개요 1 + 파일 2 + 흐름 1 = 4묶음 × 3수준
        assertThat(first).hasSize(4).isEqualTo(second);
        assertThat(JSON.valueToTree(result).path("judged").asInt()).isEqualTo(12);
        assertThat(JSON.valueToTree(result).path("accuracy").asDouble()).isEqualTo(1.0);
        assertThat(JSON.valueToTree(result).path("judge").asString()).isEqualTo("gemini-test");
        // 세 출력이 섞여 들어가고 수준 이름은 프롬프트의 설명 부분에 드러나지 않는다
        assertThat(first.get(0)).contains("### a", "### b", "### c");

        boolean anyDiffers = false;
        for (long seed = 1; seed <= 10 && !anyDiffers; seed++) {
            List<String> other = new ArrayList<>();
            new LevelJudge(recording(other), seed).judge(report);
            anyDiffers = !other.equals(first);
        }
        assertThat(anyDiffers).as("seed가 다르면 섞는 순서가 달라진다").isTrue();
    }

    @Test
    void AC7_판정_형식_오류는_한_번_더_묻고_빼고_센다() {
        JsonNode report = GradeFixtures.report(GradeFixtures.zip(dir));
        // 묶음 1: 형식 오류 → 재질문 성공. 묶음 2: 수준 중복 → 재질문도 id 누락 → 뺀다. 묶음 3·4: 바로 성공
        String duplicate =
                "{\"items\":[{\"id\":\"a\",\"level\":\"junior\"},{\"id\":\"b\",\"level\":\"junior\"},"
                    + "{\"id\":\"c\",\"level\":\"senior\"}]}";
        String missing = "{\"items\":[{\"id\":\"a\",\"level\":\"junior\"}]}";
        int[] call = {0};
        LlmClient judge =
                request -> {
                    call[0]++;
                    String content =
                            switch (call[0]) {
                                case 1 -> "수준을 판정하면 다음과 같습니다";
                                case 3 -> duplicate;
                                case 4 -> missing;
                                default -> correctAnswer(request.prompt());
                            };
                    return new LlmResponse(content, "gemini-test", 1, 1);
                };

        JsonNode result = JSON.valueToTree(new LevelJudge(judge, 42).judge(report));

        assertThat(call[0]).isEqualTo(6);
        assertThat(result.path("excluded").asInt()).isEqualTo(1);
        assertThat(result.path("judged").asInt()).isEqualTo(9);
        assertThat(result.path("accuracy").asDouble()).isEqualTo(1.0);
    }
}
