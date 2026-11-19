package com.vibenote.analysis.concept;

import static com.vibenote.analysis.concept.ConceptPromptsTest.concept;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vibenote.analysis.concept.ExtractedConcept.NewConcept;
import com.vibenote.analysis.explain.LlmJsonCall.InvalidResponseException;
import com.vibenote.common.Level;
import com.vibenote.concept.ConceptStatus;
import com.vibenote.concept.domain.Concept;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** F-08 AC4(기존 카드 연결)·AC5(새 개념 검증·이름 비교)·AC6(형식 오류) — 5.3절·5.4절. */
class ConceptResponseParserTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** 카드 목록 1·2·3, 파일 3개(id 101~103) 중 1·2번 코드만 프롬프트에 들어감. */
    private static final ConceptPrompts.Input INPUT =
            new ConceptPrompts.Input(
                    "", "", "", Set.of(1L, 2L, 3L), List.of(101L, 102L, 103L), Set.of(1, 2));

    private static final List<Concept> EXISTING =
            List.of(
                    concept(1, "의존성 주입 (DI)", ConceptStatus.PUBLISHED),
                    concept(2, "트랜잭션", ConceptStatus.PUBLISHED),
                    concept(3, "AI 초안", ConceptStatus.DRAFT),
                    concept(9, "숨긴 개념 (Hidden)", ConceptStatus.HIDDEN));

    private static List<ExtractedConcept> parse(String json) {
        return parse(json, 15, 5);
    }

    private static List<ExtractedConcept> parse(String json, int maxLinks, int maxDrafts) {
        JsonNode root = JSON.readTree(json);
        return ConceptResponseParser.parse(
                root, INPUT, EXISTING, Level.JUNIOR, maxLinks, maxDrafts);
    }

    private static String newItem(String name) {
        return """
        {"new": {"name": "%s", "category": "spring", "difficulty": "SENIOR",
         "summary": "요약", "body": "## 핵심\\n본문"}, "fileNo": 1, "lineHint": "L3"}
        """
                .formatted(name);
    }

    @Test
    void AC6_concepts가_배열이_아니면_형식_오류() {
        assertThatThrownBy(() -> parse("{\"concepts\": {}}"))
                .isInstanceOf(InvalidResponseException.class);
        assertThatThrownBy(() -> parse("{}")).isInstanceOf(InvalidResponseException.class);
        assertThatThrownBy(() -> parse("[]")).isInstanceOf(InvalidResponseException.class);
    }

    @Test
    void AC6_빈_배열은_정상이다() {
        assertThat(parse("{\"concepts\": []}")).isEmpty();
    }

    @Test
    void AC4_기존_카드_연결은_파일_id_위치_맥락을_정리해_담는다() {
        List<ExtractedConcept> result =
                parse(
                        """
                        {"concepts": [
                          {"conceptId": 1, "fileNo": 2, "lineHint": " L8-L15 ",
                           "contextNote": "  생성자로 받습니다.  "}
                        ]}
                        """);

        assertThat(result)
                .containsExactly(new ExtractedConcept(1L, null, 102L, "L8-L15", "생성자로 받습니다."));
    }

    @Test
    void AC4_카드_목록_밖의_id와_정수가_아닌_id는_버린다() {
        List<ExtractedConcept> result =
                parse(
                        """
                        {"concepts": [
                          {"conceptId": 9}, {"conceptId": 77}, {"conceptId": "1"},
                          {"conceptId": 2.5}, "문자열", {"conceptId": 2}
                        ]}
                        """);

        assertThat(result).extracting(ExtractedConcept::conceptId).containsExactly(2L);
    }

    @Test
    void AC4_fileNo가_범위_밖이면_파일과_위치를_비운다() {
        List<ExtractedConcept> result =
                parse(
                        """
                        {"concepts": [
                          {"conceptId": 1, "fileNo": 0, "lineHint": "L1"},
                          {"conceptId": 2, "fileNo": 4, "lineHint": "L1"},
                          {"conceptId": 3, "fileNo": "1", "lineHint": "L1"}
                        ]}
                        """);

        assertThat(result)
                .allSatisfy(
                        c -> {
                            assertThat(c.fileId()).isNull();
                            assertThat(c.lineHint()).isNull();
                        });
    }

    @Test
    void AC4_lineHint는_형식이_맞고_코드가_들어간_파일일_때만_저장한다() {
        List<ExtractedConcept> result =
                parse(
                        """
                        {"concepts": [
                          {"conceptId": 1, "fileNo": 1, "lineHint": "L12"},
                          {"conceptId": 2, "fileNo": 1, "lineHint": "12-30"},
                          {"conceptId": 3, "fileNo": 3, "lineHint": "L5"}
                        ]}
                        """);

        assertThat(result)
                .extracting(c -> c.fileId() + " " + c.lineHint())
                .containsExactly("101 L12", "101 null", "103 null");
    }

    @Test
    void AC4_contextNote는_500자에서_자르고_비면_NULL이다() {
        String longNote = "가".repeat(600);
        List<ExtractedConcept> result =
                parse(
                        """
                        {"concepts": [
                          {"conceptId": 1, "contextNote": "%s"},
                          {"conceptId": 2, "contextNote": "   "},
                          {"conceptId": 3, "contextNote": 5}
                        ]}
                        """
                                .formatted(longNote));

        assertThat(result.get(0).contextNote()).hasSize(500);
        assertThat(result.get(1).contextNote()).isNull();
        assertThat(result.get(2).contextNote()).isNull();
    }

    @Test
    void AC4_같은_카드는_첫_항목만_쓰고_연결은_15개까지다() {
        assertThat(
                        parse(
                                """
                                {"concepts": [
                                  {"conceptId": 1, "contextNote": "첫"},
                                  {"conceptId": 1, "contextNote": "둘"}
                                ]}
                                """))
                .extracting(ExtractedConcept::contextNote)
                .containsExactly("첫");

        StringBuilder items = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            items.append(i == 0 ? "" : ",").append(newItem("새 개념 " + i));
        }
        List<ExtractedConcept> capped = parse("{\"concepts\": [" + items + "]}", 3, 10);
        assertThat(capped).hasSize(3);
    }

    @Test
    void AC5_새_개념은_정리해_초안으로_담는다() {
        List<ExtractedConcept> result = parse("{\"concepts\": [" + newItem("  빈 등록  ") + "]}");

        assertThat(result)
                .containsExactly(
                        new ExtractedConcept(
                                null,
                                new NewConcept("빈 등록", "spring", Level.SENIOR, "요약", "## 핵심\n본문"),
                                101L,
                                "L3",
                                null));
    }

    @Test
    void AC5_이름_요약_카테고리가_틀리면_버린다() {
        List<ExtractedConcept> result =
                parse(
                        """
                        {"concepts": [
                          {"new": {"name": "  ", "category": "spring", "summary": "s"}},
                          {"new": {"name": "%s", "category": "spring", "summary": "s"}},
                          {"new": {"name": "요약 없음", "category": "spring"}},
                          {"new": {"name": "요약 빈 값", "category": "spring", "summary": " "}},
                          {"new": {"name": "카테고리 밖", "category": "python", "summary": "s"}},
                          {"new": {"name": "카테고리 없음", "summary": "s"}},
                          {"new": "문자열"},
                          {"fileNo": 1}
                        ]}
                        """
                                .formatted("가".repeat(101)));

        assertThat(result).isEmpty();
    }

    @Test
    void AC5_difficulty가_틀리면_분석_수준이고_길이를_자르고_빈_body는_NULL이다() {
        List<ExtractedConcept> result =
                parse(
                        """
                        {"concepts": [
                          {"new": {"name": "%s", "category": "vue", "difficulty": "EXPERT",
                                   "summary": "%s", "body": "%s"}},
                          {"new": {"name": "본문 없음", "category": "web", "summary": "s",
                                   "body": "  "}}
                        ]}
                        """
                                .formatted("가".repeat(100), "나".repeat(501), "다".repeat(2001)));

        NewConcept first = result.get(0).draft();
        assertThat(first.name()).hasSize(100);
        assertThat(first.difficulty()).isEqualTo(Level.JUNIOR);
        assertThat(first.summary()).hasSize(500);
        assertThat(first.body()).hasSize(2000);
        assertThat(result.get(1).draft().body()).isNull();
        assertThat(result.get(1).draft().difficulty()).isEqualTo(Level.JUNIOR);
    }

    @Test
    void AC5_이름이_기존_카드와_같으면_새로_만들지_않고_그_카드에_연결한다() {
        List<ExtractedConcept> result =
                parse(
                        "{\"concepts\": ["
                                + String.join(
                                        ",",
                                        newItem("의존성  주입"),
                                        newItem("트랜잭션 (Transaction)"),
                                        newItem("숨긴개념"),
                                        newItem("DI"))
                                + "]}");

        assertThat(result)
                .extracting(c -> c.conceptId() + " " + (c.draft() == null ? "-" : c.draft().name()))
                .containsExactly("1 -", "2 -", "9 -", "null DI");
        assertThat(result.get(0).fileId()).isEqualTo(101L);
        assertThat(result.get(0).lineHint()).isEqualTo("L3");
    }

    @Test
    void AC5_이름_비교로_찾은_카드도_중복을_빼고_상한을_적용한다() {
        List<ExtractedConcept> result =
                parse(
                        """
                        {"concepts": [{"conceptId": 1, "contextNote": "첫"}, %s]}
                        """
                                .formatted(newItem("의존성 주입")));

        assertThat(result).extracting(ExtractedConcept::contextNote).containsExactly("첫");
    }

    @Test
    void AC5_새_초안은_5개까지이고_같은_이름은_한_번만_만든다() {
        List<String> items = new ArrayList<>();
        items.add(newItem("빈 등록"));
        items.add(newItem("빈등록 (Bean)"));
        for (int i = 1; i <= 6; i++) {
            items.add(newItem("개념 " + i));
        }

        List<ExtractedConcept> result = parse("{\"concepts\": [" + String.join(",", items) + "]}");

        assertThat(result)
                .extracting(c -> c.draft().name())
                .containsExactly("빈 등록", "개념 1", "개념 2", "개념 3", "개념 4");
    }

    @Test
    void AC5_conceptId와_new가_함께_있으면_conceptId를_쓴다() {
        List<ExtractedConcept> result =
                parse("{\"concepts\": [{\"conceptId\": 2, \"new\": " + "{\"name\": \"x\"}}]}");

        assertThat(result).extracting(ExtractedConcept::conceptId).containsExactly(2L);
        assertThat(result.get(0).draft()).isNull();
    }
}
