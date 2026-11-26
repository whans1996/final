package com.vibenote.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vibenote.analysis.explain.LlmJsonCall.InvalidResponseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** F-14 AC3(문항 검증·셔플)·AC4(형식 오류) — 6.3절. */
class QuizResponseParserTest {

    private static final Set<Long> IDS = Set.of(3L, 8L);
    private static final long ANALYSIS = 42L;

    /** 정답이 항상 첫 번째인 정상 문항. 선택지는 {@code q<n>-a..d}. */
    static String item(int n, Object conceptId) {
        return """
        {"conceptId": %s, "question": "질문 %d", "choices": ["q%d-a", "q%d-b", "q%d-c", "q%d-d"],
         "answerIndex": 0, "explanation": "해설 %d"}\
        """
                .formatted(conceptId, n, n, n, n, n, n);
    }

    static String questions(List<String> items) {
        return "{\"questions\": [" + String.join(",", items) + "]}";
    }

    static List<String> valid(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(n -> item(n, "null")).toList();
    }

    private static JsonNode json(String text) {
        return JsonMapper.builder().build().readTree(text);
    }

    private static List<GeneratedQuestion> parse(String text) {
        return QuizResponseParser.parse(json(text), IDS, ANALYSIS, 5, 10);
    }

    private static String correct(GeneratedQuestion q) {
        return q.choices().get(q.answerIndex());
    }

    @Test
    void AC3_정상_문항은_순서대로_남고_정답_선택지를_따라간다() {
        List<GeneratedQuestion> result = parse(questions(valid(6)));

        assertThat(result).hasSize(6);
        assertThat(result).extracting(GeneratedQuestion::question).startsWith("질문 1", "질문 2");
        for (int i = 0; i < 6; i++) {
            GeneratedQuestion q = result.get(i);
            assertThat(q.choices())
                    .containsExactlyInAnyOrder(
                            "q" + (i + 1) + "-a",
                            "q" + (i + 1) + "-b",
                            "q" + (i + 1) + "-c",
                            "q" + (i + 1) + "-d");
            assertThat(correct(q)).isEqualTo("q" + (i + 1) + "-a");
            assertThat(q.explanation()).isEqualTo("해설 " + (i + 1));
        }
    }

    @Test
    void AC3_선택지는_문항_순번_k마다_analysisId_31_k_seed로_섞는다() {
        List<GeneratedQuestion> result = parse(questions(valid(6)));

        for (int k = 1; k <= 6; k++) {
            List<String> expected =
                    new ArrayList<>(
                            List.of(
                                    "q" + k + "-a",
                                    "q" + k + "-b",
                                    "q" + k + "-c",
                                    "q" + k + "-d"));
            Collections.shuffle(expected, new Random(ANALYSIS * 31 + k));
            assertThat(result.get(k - 1).choices()).containsExactlyElementsOf(expected);
            assertThat(result.get(k - 1).answerIndex()).isEqualTo(expected.indexOf("q" + k + "-a"));
        }
        // 같은 입력이면 같은 결과, 정답 위치가 한쪽으로 쏠리지 않는다
        assertThat(parse(questions(valid(6)))).isEqualTo(result);
        assertThat(result.stream().map(GeneratedQuestion::answerIndex).collect(Collectors.toSet()))
                .hasSizeGreaterThan(1);
    }

    @Test
    void AC3_규칙을_어긴_문항만_버린다() {
        List<String> items = new ArrayList<>(valid(5));
        items.add("\"문자열\"");
        items.add(
                "{\"question\": \"  \", \"choices\": [\"a\",\"b\",\"c\",\"d\"], \"answerIndex\": 0,"
                        + " \"explanation\": \"e\"}");
        items.add(
                "{\"question\": \""
                        + "x".repeat(1001)
                        + "\", \"choices\": [\"a\",\"b\",\"c\",\"d\"], \"answerIndex\": 0,"
                        + " \"explanation\": \"e\"}");
        items.add(
                "{\"question\": \"q\", \"choices\": [\"a\",\"b\",\"c\"], \"answerIndex\": 0,"
                        + " \"explanation\": \"e\"}");
        items.add(
                "{\"question\": \"q\", \"choices\": [\"a\",\"b\",\"c\",\"d\",\"e\"],"
                        + " \"answerIndex\": 0, \"explanation\": \"e\"}");
        items.add(
                "{\"question\": \"q\", \"choices\": [\"a\",\"b\",\" a \",\"d\"], \"answerIndex\":"
                        + " 0, \"explanation\": \"e\"}");
        items.add(
                "{\"question\": \"q\", \"choices\": [\"a\",\"b\",\"\",\"d\"], \"answerIndex\": 0,"
                        + " \"explanation\": \"e\"}");
        items.add(
                "{\"question\": \"q\", \"choices\": [\"a\",\"b\",\""
                        + "c".repeat(301)
                        + "\",\"d\"], \"answerIndex\": 0, \"explanation\": \"e\"}");
        items.add(
                "{\"question\": \"q\", \"choices\": [\"a\",\"b\",1,\"d\"], \"answerIndex\": 0,"
                        + " \"explanation\": \"e\"}");
        items.add(
                "{\"question\": \"q\", \"choices\": [\"a\",\"b\",\"c\",\"d\"], \"answerIndex\": 4,"
                        + " \"explanation\": \"e\"}");
        items.add(
                "{\"question\": \"q\", \"choices\": [\"a\",\"b\",\"c\",\"d\"], \"answerIndex\":"
                        + " 1.5, \"explanation\": \"e\"}");
        items.add(
                "{\"question\": \"q\", \"choices\": [\"a\",\"b\",\"c\",\"d\"], \"answerIndex\":"
                        + " \"1\", \"explanation\": \"e\"}");
        items.add(
                "{\"question\": \"q\", \"choices\": [\"a\",\"b\",\"c\",\"d\"], \"answerIndex\":"
                        + " 0}");
        items.add(
                "{\"question\": \"q\", \"choices\": [\"a\",\"b\",\"c\",\"d\"], \"answerIndex\": 0,"
                        + " \"explanation\": \""
                        + "e".repeat(1001)
                        + "\"}");

        List<GeneratedQuestion> result = parse(questions(items));

        assertThat(result)
                .extracting(GeneratedQuestion::question)
                .containsExactly("질문 1", "질문 2", "질문 3", "질문 4", "질문 5");
    }

    @Test
    void AC3_앞뒤_공백을_떼고_저장한다() {
        List<String> items = new ArrayList<>(valid(4));
        items.add(
                "{\"question\": \"  q  \", \"choices\": [\" a\",\"b \",\"c\",\"d\"],"
                        + " \"answerIndex\": 1, \"explanation\": \" e \"}");

        GeneratedQuestion last = parse(questions(items)).get(4);

        assertThat(last.question()).isEqualTo("q");
        assertThat(last.choices()).containsExactlyInAnyOrder("a", "b", "c", "d");
        assertThat(correct(last)).isEqualTo("b");
        assertThat(last.explanation()).isEqualTo("e");
    }

    /** F-27 AC8: strict 스키마는 맞는 개념이 없을 때 conceptId를 null로 채운다. 생략했을 때와 같은 결과여야 한다. */
    @Test
    void F27_AC8_conceptId가_null이면_생략과_같다() {
        List<String> nulls = valid(5);
        List<String> omitted =
                nulls.stream().map(s -> s.replace("\"conceptId\": null, ", "")).toList();

        assertThat(omitted.get(0)).doesNotContain("conceptId");
        assertThat(parse(questions(nulls))).isEqualTo(parse(questions(omitted))).hasSize(5);
    }

    @Test
    void AC3_목록_밖이거나_정수가_아닌_conceptId는_NULL로_두고_문항은_유지한다() {
        List<GeneratedQuestion> result =
                parse(
                        questions(
                                List.of(
                                        item(1, "3"),
                                        item(2, "8"),
                                        item(3, "99"),
                                        item(4, "\"3\""),
                                        item(5, "3.5"),
                                        item(6, "99999999999999999999999"))));

        assertThat(result)
                .extracting(GeneratedQuestion::conceptId)
                .containsExactly(3L, 8L, null, null, null, null);
    }

    @Test
    void AC3_유효_문항은_앞에서부터_10개까지_쓴다() {
        List<GeneratedQuestion> result = parse(questions(valid(12)));

        assertThat(result).hasSize(10);
        assertThat(result.get(9).question()).isEqualTo("질문 10");
    }

    @Test
    void AC4_유효_문항이_5개_미만이면_형식_오류() {
        List<String> items = new ArrayList<>(valid(4));
        items.add("{\"question\": \"q\"}");

        assertThatThrownBy(() -> parse(questions(items)))
                .isInstanceOf(InvalidResponseException.class);
    }

    @Test
    void AC4_questions가_배열이_아니거나_객체가_아니면_형식_오류() {
        assertThatThrownBy(() -> parse("{\"questions\": \"x\"}"))
                .isInstanceOf(InvalidResponseException.class);
        assertThatThrownBy(() -> parse("{}")).isInstanceOf(InvalidResponseException.class);
        assertThatThrownBy(() -> parse("[1]")).isInstanceOf(InvalidResponseException.class);
    }
}
