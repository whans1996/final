package com.vibenote.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vibenote.common.Level;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class MockLlmClientTest {

    private final MockLlmClient client = new MockLlmClient();

    @Test
    void 단계별_fixture를_그대로_돌려준다() throws Exception {
        // src/test/resources/llm-fixtures/overview.json (테스트 전용 fixture)
        LlmResponse response =
                client.complete(new LlmRequest(LlmStage.OVERVIEW, Level.BEGINNER, "prompt"));

        JsonNode json = new ObjectMapper().readTree(response.content());
        assertThat(json.path("summary").asString()).isNotBlank();
        assertThat(response.model()).isEqualTo("mock");
    }

    @Test
    void 같은_요청에는_항상_같은_응답() {
        LlmRequest request = new LlmRequest(LlmStage.OVERVIEW, Level.SENIOR, "prompt");

        assertThat(client.complete(request)).isEqualTo(client.complete(request));
    }

    @Test
    void issue49_concept_fixture의_conceptName을_프롬프트_카드_목록의_id로_바꾼다() throws Exception {
        String fixture =
                """
                { "concepts": [
                  { "conceptName": "의존성 주입 (DI)", "fileNo": 1, "contextNote": "생성자 주입" },
                  { "conceptName": "목록에 없는 카드", "contextNote": "빠져야 한다" },
                  { "conceptId": 7, "contextNote": "id로 적은 항목은 그대로" },
                  { "new": { "name": "GraphQL" }, "contextNote": "새 개념은 그대로" }
                ] }
                """;
        String prompt =
                """
                ## 카드 목록 (id | 이름 | 카테고리)

                103 | @RestController와 요청 매핑 | web
                118 | 의존성 주입 (DI) | spring

                ## 코드 (줄 번호가 붙어 있다)

                1: class A { int a | b; }
                """;

        JsonNode concepts =
                new ObjectMapper()
                        .readTree(MockLlmClient.linkConceptNames(fixture, prompt))
                        .path("concepts");

        assertThat(concepts).hasSize(3);
        assertThat(concepts.get(0).path("conceptId").asLong()).isEqualTo(118L);
        assertThat(concepts.get(0).has("conceptName")).isFalse();
        assertThat(concepts.get(0).path("contextNote").asString()).isEqualTo("생성자 주입");
        assertThat(concepts.get(1).path("conceptId").asLong()).isEqualTo(7L);
        assertThat(concepts.get(2).path("new").path("name").asString()).isEqualTo("GraphQL");
    }

    @Test
    void issue49_이름에_구분자가_들어간_카드도_마지막_구분자_앞까지를_이름으로_읽는다() throws Exception {
        String fixture =
                """
                { "concepts": [ { "conceptName": "A | B 패턴", "contextNote": "구분자 포함" } ] }
                """;
        String prompt =
                """
                120 | A | B 패턴 | spring
                121 | A | web
                """;

        JsonNode concepts =
                new ObjectMapper()
                        .readTree(MockLlmClient.linkConceptNames(fixture, prompt))
                        .path("concepts");

        assertThat(concepts).hasSize(1);
        assertThat(concepts.get(0).path("conceptId").asLong()).isEqualTo(120L);
    }

    @Test
    void issue49_complete는_CONCEPT_단계에서만_conceptName을_id로_바꾼다() throws Exception {
        String fixture =
                """
                { "concepts": [ { "conceptName": "의존성 주입 (DI)", "contextNote": "생성자 주입" } ] }
                """;
        MockLlmClient fixed = new MockLlmClient(path -> fixture);
        String prompt = "118 | 의존성 주입 (DI) | spring\n";

        JsonNode concept =
                new ObjectMapper()
                        .readTree(
                                fixed.complete(
                                                new LlmRequest(
                                                        LlmStage.CONCEPT, Level.JUNIOR, prompt))
                                        .content())
                        .path("concepts")
                        .get(0);
        String overview =
                fixed.complete(new LlmRequest(LlmStage.OVERVIEW, Level.JUNIOR, prompt)).content();

        assertThat(concept.path("conceptId").asLong()).isEqualTo(118L);
        assertThat(concept.has("conceptName")).isFalse();
        assertThat(overview).isEqualTo(fixture);
    }

    @Test
    void AC11_quiz_fixture의_conceptName을_프롬프트_개념_목록의_id로_바꾸고_없으면_null() throws Exception {
        String fixture =
                """
                { "questions": [
                  { "conceptName": "의존성 주입 (DI)", "question": "q1" },
                  { "conceptName": "@RestController와 요청 매핑", "question": "q2" },
                  { "conceptName": "목록에 없는 카드", "question": "q3" },
                  { "conceptId": null, "question": "q4" }
                ] }
                """;
        String prompt =
                """
                ## 개념 목록 (id | 이름 | 이 코드에서 쓰인 방식)

                103 | @RestController와 요청 매핑
                118 | 의존성 주입 (DI) | 생성자로 서비스를 받는다

                ## 코드
                """;
        MockLlmClient fixed = new MockLlmClient(path -> fixture);

        JsonNode questions =
                new ObjectMapper()
                        .readTree(
                                fixed.complete(new LlmRequest(LlmStage.QUIZ, Level.JUNIOR, prompt))
                                        .content())
                        .path("questions");

        assertThat(questions).hasSize(4);
        assertThat(questions.get(0).path("conceptId").asLong()).isEqualTo(118L);
        assertThat(questions.get(0).has("conceptName")).isFalse();
        assertThat(questions.get(0).path("question").asString()).isEqualTo("q1");
        assertThat(questions.get(1).path("conceptId").asLong()).isEqualTo(103L);
        assertThat(questions.get(2).path("conceptId").isNull()).isTrue();
        assertThat(questions.get(2).has("conceptName")).isFalse();
        assertThat(questions.get(3).path("conceptId").isNull()).isTrue();
    }

    @Test
    void fixture가_없으면_명확한_오류() {
        MockLlmClient empty =
                new MockLlmClient(
                        path -> {
                            throw new IllegalStateException("Mock LLM fixture가 없습니다: " + path);
                        });

        assertThatThrownBy(
                        () -> empty.complete(new LlmRequest(LlmStage.FLOW, Level.JUNIOR, "prompt")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("llm-fixtures/flow.json");
    }

    @Test
    void 모든_단계에_main_fixture가_있다() {
        for (LlmStage stage : LlmStage.values()) {
            assertThat(new ClassPathResource("llm-fixtures/" + stage.key() + ".json").exists())
                    .as(stage.key())
                    .isTrue();
        }
    }

    @Test
    void linkRecommendIds_fixture_이유를_프롬프트_카드_순서대로_붙이고_카드가_더_많으면_처음부터_다시_쓴다() {
        String fixture =
                """
                { "reasons": [ { "reason": "첫째" }, { "reason": "둘째" } ] }
                """;
        String prompt =
                """
                ## 카드 목록 (id | 이름)

                31 | JWT 인증 | security | JUNIOR | 요약: 토큰 | 근거: 없음(수준 맞춤·인기)
                7 | 트랜잭션 | spring | JUNIOR | 요약: 묶음 | 근거: 내 분석 코드에 나온 개념이에요
                105 | 빈 | spring | BEGINNER | 요약: 객체 | 근거: 없음(수준 맞춤·인기)
                """;
        MockLlmClient fixed = new MockLlmClient(path -> fixture);

        JsonNode reasons =
                new ObjectMapper()
                        .readTree(
                                fixed.complete(
                                                new LlmRequest(
                                                        LlmStage.RECOMMEND, Level.JUNIOR, prompt))
                                        .content())
                        .path("reasons");

        assertThat(reasons).hasSize(3);
        assertThat(reasons.get(0).path("conceptId").asLong()).isEqualTo(31L);
        assertThat(reasons.get(0).path("reason").asString()).isEqualTo("첫째");
        assertThat(reasons.get(1).path("conceptId").asLong()).isEqualTo(7L);
        assertThat(reasons.get(1).path("reason").asString()).isEqualTo("둘째");
        assertThat(reasons.get(2).path("conceptId").asLong()).isEqualTo(105L);
        assertThat(reasons.get(2).path("reason").asString()).isEqualTo("첫째");
    }
}
