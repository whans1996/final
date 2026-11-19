package com.vibenote.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vibenote.common.Level;
import org.junit.jupiter.api.Test;
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
    void fixture가_없는_단계는_명확한_오류() {
        // 분석 파이프라인 spec에서 main fixture를 추가하면 이 테스트의 대상 단계를 바꾼다
        assertThatThrownBy(
                        () ->
                                client.complete(
                                        new LlmRequest(LlmStage.RECOMMEND, Level.JUNIOR, "prompt")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("llm-fixtures/recommend.json");
    }
}
