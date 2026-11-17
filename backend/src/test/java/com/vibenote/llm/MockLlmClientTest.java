package com.vibenote.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibenote.common.Level;
import org.junit.jupiter.api.Test;

class MockLlmClientTest {

    private final MockLlmClient client = new MockLlmClient();

    @Test
    void 단계별_fixture를_그대로_돌려준다() throws Exception {
        // src/test/resources/llm-fixtures/overview.json (테스트 전용 fixture)
        LlmResponse response =
                client.complete(new LlmRequest(LlmStage.OVERVIEW, Level.BEGINNER, "prompt"));

        JsonNode json = new ObjectMapper().readTree(response.content());
        assertThat(json.path("summary").asText()).isNotBlank();
        assertThat(response.model()).isEqualTo("mock");
    }

    @Test
    void 같은_요청에는_항상_같은_응답() {
        LlmRequest request = new LlmRequest(LlmStage.OVERVIEW, Level.SENIOR, "prompt");

        assertThat(client.complete(request)).isEqualTo(client.complete(request));
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
