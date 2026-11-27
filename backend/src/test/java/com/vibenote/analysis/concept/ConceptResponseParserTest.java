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

/**
 * F-08 AC4(기존 카드 연결)·AC5(새 개념 검증·이름 비교)·AC6(형식 오류) — 5.3절·5.4절. K-1 AC7(수준 요약)·AC8(카테고리 별칭)·AC9(괄호
 * 안 키)·AC10(숨긴 카드).
 */
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
                root, INPUT, EXISTING, Level.JUNIOR, maxLinks, maxDrafts, 42L);
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

    /** F-27 AC8: strict 스키마는 선택 필드를 null로 채운다. 생략했을 때와 같은 결과여야 한다. */
    @Test
    void F27_AC8_선택_필드가_null이면_생략과_같다() {
        String omitted =
                """
                {"concepts": [
                  {"conceptId": 1, "contextNote": "연결"},
                  {"new": {"name": "새 개념", "category": "spring", "difficulty": "SENIOR",
                   "summary": "요약", "body": "## 핵심\\n본문"}, "fileNo": 1, "contextNote": "초안"}
                ]}
                """;
        String nulls =
                """
                {"concepts": [
                  {"conceptId": 1, "new": null, "fileNo": null, "lineHint": null, "contextNote": "연결"},
                  {"conceptId": null, "new": {"name": "새 개념", "category": "spring", "difficulty": "SENIOR",
                   "summary": "요약", "body": "## 핵심\\n본문"}, "fileNo": 1, "lineHint": null, "contextNote": "초안"}
                ]}
                """;

        assertThat(parse(nulls)).isEqualTo(parse(omitted)).hasSize(2);
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
    void AC4_long_범위를_넘는_정수는_그_항목만_버린다() {
        List<ExtractedConcept> result =
                parse(
                        """
                        {"concepts": [
                          {"conceptId": 99999999999999999999},
                          {"conceptId": 1, "fileNo": 99999999999999999999},
                          {"conceptId": 2, "fileNo": 1}
                        ]}
                        """);

        assertThat(result)
                .containsExactly(
                        new ExtractedConcept(1L, null, null, null, null),
                        new ExtractedConcept(2L, null, 101L, null, null));
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

    /** K-1에서 바뀜: 숨긴 카드와 같은 이름은 연결하지 않고 버린다 (AC10). "DI"는 괄호 안 키로 카드 1에 연결된다 (AC9 테스트). */
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
                                        newItem("새 개념"))
                                + "]}");

        assertThat(result)
                .extracting(c -> c.conceptId() + " " + (c.draft() == null ? "-" : c.draft().name()))
                .containsExactly("1 -", "2 -", "null 새 개념");
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

    // ── K-1 ──

    private static String levelItem(String name, String levelSummaries) {
        return """
        {"new": {"name": "%s", "category": "spring", "difficulty": "JUNIOR",
         "summary": "요약", "body": null, "levelSummaries": %s}}
        """
                .formatted(name, levelSummaries);
    }

    @Test
    void AC7_levelSummaries_세_값을_정리해_초안에_담는다() {
        List<ExtractedConcept> result =
                parse(
                        "{\"concepts\": ["
                                + levelItem(
                                        "빈 등록",
                                        """
                                        {"beginner": "  비유  ", "junior": "%s", "senior": " "}
                                        """
                                                .formatted("가".repeat(301)))
                                + "]}");

        NewConcept draft = result.get(0).draft();
        assertThat(draft.summaryBeginner()).isEqualTo("비유");
        assertThat(draft.summaryJunior()).hasSize(300);
        assertThat(draft.summarySenior()).isNull();
    }

    @Test
    void AC7_levelSummaries가_없거나_객체가_아니면_초안은_남고_세_칸은_NULL() {
        List<ExtractedConcept> result =
                parse(
                        "{\"concepts\": ["
                                + String.join(
                                        ",",
                                        newItem("요약 없음"),
                                        levelItem("문자열", "\"요약\""),
                                        levelItem("널", "null"),
                                        levelItem("숫자 값", "{\"beginner\": 1, \"junior\": null}"))
                                + "]}");

        assertThat(result)
                .hasSize(4)
                .allSatisfy(
                        c -> {
                            assertThat(c.draft().summaryBeginner()).isNull();
                            assertThat(c.draft().summaryJunior()).isNull();
                            assertThat(c.draft().summarySenior()).isNull();
                        });
    }

    private static String categoryItem(String name, String category) {
        return """
        {"new": {"name": "%s", "category": "%s", "summary": "요약"}}
        """
                .formatted(name, category);
    }

    @Test
    void AC8_12종_밖_카테고리는_별칭으로_바꾸고_그래도_밖이면_그_초안만_버린다() {
        List<ExtractedConcept> result =
                parse(
                        "{\"concepts\": ["
                                + String.join(
                                        ",",
                                        categoryItem("화살표 함수", "JS"),
                                        categoryItem("자동 설정", "Spring Boot"),
                                        categoryItem("반응형 상태", "Vue.js"),
                                        categoryItem("인덱스", "MySQL"),
                                        categoryItem("컨테이너", "Docker"),
                                        categoryItem("코루틴", "kotlin"),
                                        categoryItem("제네릭 타입", "typescript"))
                                + "]}",
                        15,
                        10);

        assertThat(result)
                .extracting(c -> c.draft().name() + " " + c.draft().category())
                .containsExactly(
                        "화살표 함수 javascript",
                        "자동 설정 spring",
                        "반응형 상태 vue",
                        "인덱스 database",
                        "컨테이너 tooling",
                        "제네릭 타입 typescript");
    }

    /** AC9 표의 기존 카드 다섯 장 (모두 PUBLISHED). */
    private static final List<Concept> AC9_CARDS =
            List.of(
                    concept(21, "의존성 주입 (DI)", ConceptStatus.PUBLISHED),
                    concept(22, "CSRF (사이트 간 요청 위조)", ConceptStatus.PUBLISHED),
                    concept(
                            23,
                            "요청 파라미터 바인딩 (@PathVariable·@RequestParam·@RequestBody)",
                            ConceptStatus.PUBLISHED),
                    concept(24, "가상 스레드 (Java 21)", ConceptStatus.PUBLISHED),
                    concept(25, "패턴 매칭 switch와 record 패턴 (Java 21)", ConceptStatus.PUBLISHED));

    private static List<ExtractedConcept> parseWith(List<Concept> cards, String... names) {
        List<String> items = new ArrayList<>();
        for (String name : names) {
            items.add(newItem(name));
        }
        return ConceptResponseParser.parse(
                JSON.readTree("{\"concepts\": [" + String.join(",", items) + "]}"),
                INPUT,
                cards,
                Level.JUNIOR,
                15,
                5,
                42L);
    }

    private static List<String> linkOrDraft(List<ExtractedConcept> result) {
        return result.stream()
                .map(c -> c.conceptId() != null ? "id " + c.conceptId() : "new " + c.draft().name())
                .toList();
    }

    @Test
    void AC9_괄호_안_키로_약어와_한글_이름을_같은_카드에_연결한다() {
        assertThat(linkOrDraft(parseWith(AC9_CARDS, "DI"))).containsExactly("id 21");
        assertThat(linkOrDraft(parseWith(AC9_CARDS, "사이트 간 요청 위조"))).containsExactly("id 22");
        assertThat(linkOrDraft(parseWith(AC9_CARDS, "DI (의존성 주입)"))).containsExactly("id 21");
        assertThat(linkOrDraft(parseWith(AC9_CARDS, "@PathVariable")))
                .containsExactly("new @PathVariable");
        assertThat(linkOrDraft(parseWith(AC9_CARDS, "Java 21"))).containsExactly("new Java 21");
    }

    @Test
    void AC9_정확히_같은_전체_이름이_앞부분과_괄호_안보다_이긴다() {
        List<Concept> cards = new ArrayList<>(AC9_CARDS);
        cards.add(concept(30, "DI", ConceptStatus.PUBLISHED));
        assertThat(linkOrDraft(parseWith(cards, "DI"))).containsExactly("id 30");

        // 앞부분 키가 괄호 안 키보다 먼저다
        List<Concept> fronts =
                List.of(
                        concept(40, "스레드 (가상 스레드)", ConceptStatus.PUBLISHED),
                        concept(41, "가상 스레드 (Loom)", ConceptStatus.PUBLISHED));
        assertThat(linkOrDraft(parseWith(fronts, "가상 스레드"))).containsExactly("id 41");
    }

    @Test
    void AC9_괄호_안은_2자에서_30자까지만_키가_된다() {
        String longInner = "가".repeat(31);
        List<Concept> cards =
                List.of(
                        concept(50, "제네릭 (T)", ConceptStatus.PUBLISHED),
                        concept(51, "긴 괄호 (" + longInner + ")", ConceptStatus.PUBLISHED));

        assertThat(linkOrDraft(parseWith(cards, "T", longInner)))
                .containsExactly("new T", "new " + longInner);
    }

    @Test
    void AC10_숨긴_카드와_같은_이름은_버리고_검수_대기_카드에는_연결한다() {
        List<ExtractedConcept> result =
                parse(
                        "{\"concepts\": ["
                                + String.join(
                                        ",", newItem("숨긴 개념"), newItem("Hidden"), newItem("AI 초안"))
                                + "]}");

        assertThat(linkOrDraft(result)).containsExactly("id 3");
    }

    /** 리뷰 반영: 괄호 안 키끼리는 비교하지 않는다 (괄호 안만 같은 서로 다른 개념이 기존 카드에 붙지 않게). */
    @Test
    void AC9_괄호_안만_같은_초안은_그_카드에_연결하지_않는다() {
        List<Concept> cards =
                List.of(
                        concept(60, "파일 업로드 (MultipartFile)", ConceptStatus.PUBLISHED),
                        concept(61, "OpenAPI 문서화 (springdoc)", ConceptStatus.PUBLISHED));

        assertThat(
                        linkOrDraft(
                                parseWith(
                                        cards,
                                        "파일 크기 제한 (MultipartFile)",
                                        "Swagger UI 설정 (springdoc)",
                                        "MultipartFile")))
                .containsExactly(
                        "new 파일 크기 제한 (MultipartFile)", "new Swagger UI 설정 (springdoc)", "id 60");
    }

    /** 리뷰 반영: 숨긴 카드가 더 높은 키로 맞아도 숨기지 않은 카드가 맞으면 그 카드에 연결한다. */
    @Test
    void AC10_숨긴_카드보다_숨기지_않은_카드에_먼저_연결한다() {
        List<Concept> cards =
                List.of(
                        concept(70, "DI", ConceptStatus.HIDDEN),
                        concept(71, "의존성 주입 (DI)", ConceptStatus.PUBLISHED),
                        concept(72, "빈 (Bean)", ConceptStatus.HIDDEN),
                        concept(73, "빈 (스프링 빈)", ConceptStatus.DRAFT));

        assertThat(linkOrDraft(parseWith(cards, "DI", "빈"))).containsExactly("id 71", "id 73");
    }

    @Test
    void AC8_버린_카테고리는_분석_id_이름_원래_값을_WARN으로_남기고_본문은_남기지_않는다() {
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger)
                        org.slf4j.LoggerFactory.getLogger(ConceptResponseParser.class);
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender =
                new ch.qos.logback.core.read.ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            parse(
                    """
                    {"concepts": [{"new": {"name": "코루틴", "category": "kotlin",
                     "summary": "요약", "body": "비밀 본문"}}]}
                    """);
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).hasSize(1);
        ch.qos.logback.classic.spi.ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.WARN);
        assertThat(event.getFormattedMessage())
                .contains("42", "코루틴", "kotlin")
                .doesNotContain("비밀 본문");
    }
}
