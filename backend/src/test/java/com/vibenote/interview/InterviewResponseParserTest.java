package com.vibenote.interview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vibenote.analysis.explain.LlmJsonCall.InvalidResponseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** I-1a AC3(검증·정리)·AC4(형식 오류) — 5.3절. */
class InterviewResponseParserTest {

    static final JsonMapper JSON = JsonMapper.builder().build();
    static final InterviewProperties PROPS = InterviewPromptsTest.PROPS;

    /** 파일 1·2는 코드를 넣었고(30줄·10줄), 파일 3은 목록에만 있다. */
    static final InterviewPrompts.Input INPUT =
            new InterviewPrompts.Input(
                    "",
                    "",
                    "",
                    "",
                    List.of(
                            InterviewPromptsTest.file(101, "A.java", "controller", 1, "a"),
                            InterviewPromptsTest.file(102, "B.java", "service", 1, "b"),
                            InterviewPromptsTest.file(103, "C.java", "mapper", 1, "c")),
                    Map.of(1, 30, 2, 10),
                    Set.of(12L, 27L, 44L, 51L));

    static Map<String, Object> q(String category, String question) {
        return q(category, question, List.of("포인트"), List.of(ev(1, "L1-L3")), List.of());
    }

    static Map<String, Object> q(
            String category,
            String question,
            List<?> keyPoints,
            List<?> evidence,
            List<?> conceptIds) {
        return Map.of(
                "category", category,
                "question", question,
                "keyPoints", keyPoints,
                "evidence", evidence,
                "conceptIds", conceptIds);
    }

    static Map<String, Object> ev(Object fileNo, String lines) {
        return Map.of("fileNo", fileNo, "lines", lines);
    }

    /** 앞에 주어진 질문 + 5개를 채우는 정상 질문. */
    static List<GeneratedQuestion> parse(List<Map<String, Object>> questions) {
        List<Object> all = new ArrayList<>(questions);
        for (int i = 0; i < 5; i++) {
            all.add(q("FLOW", "채움" + i));
        }
        return InterviewResponseParser.parse(tree(Map.of("questions", all)), INPUT, PROPS);
    }

    static JsonNode tree(Object value) {
        return JSON.valueToTree(value);
    }

    @Test
    void AC3_범주는_대소문자를_무시하고_밖의_값이면_질문을_버린다() {
        List<GeneratedQuestion> result =
                parse(List.of(q("structure", "구조"), q("Tech_Choice", "기술"), q("NOT_MINE", "버림")));

        assertThat(result).hasSize(7);
        assertThat(result.get(0).category()).isEqualTo(InterviewCategory.STRUCTURE);
        assertThat(result.get(1).category()).isEqualTo(InterviewCategory.TECH_CHOICE);
        assertThat(result).extracting(GeneratedQuestion::question).doesNotContain("버림");
    }

    @Test
    void AC3_근거는_코드를_넣은_파일과_줄_범위_안만_남기고_앞에서_3개를_fileId와_path로_바꾼다() {
        List<Object> evidence =
                List.of(
                        ev(3, "L1"), // 코드 없는 파일
                        ev(9, "L1"), // 없는 번호
                        ev("1", "L1"), // 정수가 아님
                        ev(1, "L5-L3"), // 역순
                        ev(1, "L0"), // 0줄
                        ev(1, "L31"), // 넣은 줄 수 넘음
                        ev(1, "12-30"), // 형식
                        ev(1, "L12-L30"),
                        ev(2, "L10"),
                        ev(1, "L1"),
                        ev(2, "L2-L3"));

        GeneratedQuestion result =
                parse(List.of(q("FLOW", "흐름", List.of("p"), evidence, List.of()))).get(0);

        assertThat(result.evidence())
                .containsExactly(
                        new InterviewEvidence(101, "A.java", 12, 30),
                        new InterviewEvidence(102, "B.java", 10, 10),
                        new InterviewEvidence(101, "A.java", 1, 1));
    }

    @Test
    void AC3_핵심_포인트는_빈_문자열을_빼고_앞에서_5개이며_200자에서_자른다() {
        List<Object> points = List.of("", "  ", "하나", 3, "둘", "셋", "넷", "x".repeat(250), "여섯");

        GeneratedQuestion result =
                parse(List.of(q("FLOW", "흐름", points, List.of(ev(1, "L1")), List.of()))).get(0);

        assertThat(result.keyPoints()).containsExactly("하나", "둘", "셋", "넷", "x".repeat(200));
    }

    @Test
    void AC3_개념_id는_입력_집합_안의_것만_중복_없이_앞에서_3개다() {
        List<Object> ids = List.of(99, 12, 12, "27", 27, 44, 51);

        GeneratedQuestion result =
                parse(List.of(q("FLOW", "흐름", List.of("p"), List.of(ev(1, "L1")), ids))).get(0);

        assertThat(result.conceptIds()).containsExactly(12L, 27L, 44L);
    }

    @Test
    void AC3_conceptIds가_없거나_null이면_빈_목록이다() {
        Map<String, Object> noIds =
                Map.of(
                        "category",
                        "FLOW",
                        "question",
                        "흐름",
                        "keyPoints",
                        List.of("p"),
                        "evidence",
                        List.of(ev(1, "L1")));

        GeneratedQuestion result = parse(List.of(noIds)).get(0);

        assertThat(result.conceptIds()).isEmpty();
    }

    @Test
    void AC3_근거나_핵심_포인트가_0개인_질문은_버린다() {
        List<GeneratedQuestion> result =
                parse(
                        List.of(
                                q("FLOW", "근거 없음", List.of("p"), List.of(ev(3, "L1")), List.of()),
                                q(
                                        "FLOW",
                                        "포인트 없음",
                                        List.of(" "),
                                        List.of(ev(1, "L1")),
                                        List.of())));

        assertThat(result)
                .hasSize(5)
                .extracting(GeneratedQuestion::question)
                .doesNotContain("근거 없음", "포인트 없음");
    }

    @Test
    void AC3_질문은_앞에서_12개이고_no를_1부터_다시_매기며_질문은_300자에서_자른다() {
        List<Map<String, Object>> many = new ArrayList<>();
        many.add(q("BAD", "버림"));
        many.add(q("LIMITATION", "y".repeat(350)));
        for (int i = 0; i < 14; i++) {
            many.add(q("STRUCTURE", "질문" + i));
        }

        List<GeneratedQuestion> result = parse(many);

        assertThat(result).hasSize(12);
        assertThat(result)
                .extracting(GeneratedQuestion::no)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        assertThat(result.get(0).question()).isEqualTo("y".repeat(300));
        assertThat(result.get(11).question()).isEqualTo("질문10");
    }

    @Test
    void AC4_questions_배열이_없거나_객체가_아니면_형식_오류다() {
        assertThatThrownBy(() -> InterviewResponseParser.parse(tree(Map.of("x", 1)), INPUT, PROPS))
                .isInstanceOf(InvalidResponseException.class);
        assertThatThrownBy(
                        () ->
                                InterviewResponseParser.parse(
                                        tree(Map.of("questions", "a")), INPUT, PROPS))
                .isInstanceOf(InvalidResponseException.class);
        assertThatThrownBy(() -> InterviewResponseParser.parse(tree(List.of()), INPUT, PROPS))
                .isInstanceOf(InvalidResponseException.class);
    }

    @Test
    void AC4_검증을_거친_질문이_5개_미만이면_형식_오류다() {
        List<Object> four = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            four.add(q("FLOW", "질문" + i));
        }
        four.add(q("FLOW", "근거 없음", List.of("p"), List.of(), List.of()));

        assertThatThrownBy(
                        () ->
                                InterviewResponseParser.parse(
                                        tree(Map.of("questions", four)), INPUT, PROPS))
                .isInstanceOf(InvalidResponseException.class);
    }
}
