package com.vibenote.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.vibenote.common.Level;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GmsLlmClientTest {

    static final String KEY = "test-gms-key-SECRET";
    static final String PROMPT = "PROMPT-BODY-SECRET 이 코드를 설명하고 JSON으로 답하라";
    static final String URL = "https://gms.test/v1/chat/completions";
    static final LlmSchemas SCHEMAS = LlmSchemas.load();

    final RestClient.Builder builder = RestClient.builder();
    final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    GmsLlmClient client(Map<String, String> stageModels) {
        return new GmsLlmClient(
                builder,
                new LlmProperties.Gms(
                        KEY, "https://gms.test/v1", "model-a", stageModels, Duration.ofSeconds(5)),
                SCHEMAS);
    }

    static String ok(String model, String content) {
        String modelField = model == null ? "" : "\"model\":\"" + model + "\",";
        return """
        {%s"choices":[{"index":0,"message":{"role":"assistant","content":%s,"refusal":null},"finish_reason":"stop"}],
         "usage":{"prompt_tokens":12,"completion_tokens":34,"total_tokens":46}}
        """
                .formatted(modelField, quote(content));
    }

    static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Test
    void AC3_GMS_요청은_단계_스키마를_strict로_보내고_응답을_LlmResponse로_바꾼다() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + KEY))
                .andExpect(jsonPath("$.model").value("model-a"))
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content").value(PROMPT))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andExpect(jsonPath("$.response_format.json_schema.name").value("overview"))
                .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
                .andExpect(
                        jsonPath("$.response_format.json_schema.schema.required")
                                .value(
                                        Matchers.contains(
                                                "summary", "techStack", "layers", "readingOrder")))
                .andExpect(
                        jsonPath("$.response_format.json_schema.schema.additionalProperties")
                                .value(false))
                .andExpect(jsonPath("$.temperature").doesNotExist())
                .andExpect(jsonPath("$.max_tokens").doesNotExist())
                .andRespond(
                        withSuccess(
                                ok("model-a-2026-01-01", "{\"summary\":\"요약\"}"),
                                MediaType.APPLICATION_JSON));

        LlmResponse response =
                client(Map.of())
                        .complete(new LlmRequest(LlmStage.OVERVIEW, Level.BEGINNER, PROMPT));

        server.verify();
        assertThat(response)
                .isEqualTo(new LlmResponse("{\"summary\":\"요약\"}", "model-a-2026-01-01", 12, 34));
    }

    @Test
    void AC3_응답에_model이_없으면_설정_모델을_쓴다() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess(ok(null, "{}"), MediaType.APPLICATION_JSON));

        LlmResponse response =
                client(Map.of()).complete(new LlmRequest(LlmStage.FILE, Level.JUNIOR, PROMPT));

        assertThat(response.model()).isEqualTo("model-a");
    }

    @Test
    void AC4_단계별_모델이_있으면_그_모델로_보내고_없으면_기본_모델이다() {
        GmsLlmClient client = client(Map.of("quiz", "model-b"));
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.model").value("model-b"))
                .andExpect(jsonPath("$.response_format.json_schema.name").value("quiz"))
                .andRespond(withSuccess(ok(null, "{}"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.model").value("model-a"))
                .andRespond(withSuccess(ok(null, "{}"), MediaType.APPLICATION_JSON));

        LlmResponse quiz = client.complete(new LlmRequest(LlmStage.QUIZ, Level.JUNIOR, PROMPT));
        LlmResponse file = client.complete(new LlmRequest(LlmStage.FILE, Level.JUNIOR, PROMPT));

        server.verify();
        assertThat(quiz.model()).isEqualTo("model-b");
        assertThat(file.model()).isEqualTo("model-a");
        assertThat(client.modelFor(LlmStage.QUIZ)).isEqualTo("model-b");
        assertThat(client.modelFor(LlmStage.FILE)).isEqualTo("model-a");
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 429, 500, 503})
    void AC5_오류_상태는_LlmUnavailableException이고_키와_프롬프트가_없다(int status) {
        server.expect(requestTo(URL))
                .andRespond(
                        withStatus(HttpStatus.valueOf(status))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body("{\"error\":{\"message\":\"echo " + PROMPT + KEY + "\"}}"));

        assertThatThrownBy(
                        () ->
                                client(Map.of())
                                        .complete(
                                                new LlmRequest(
                                                        LlmStage.CONCEPT, Level.SENIOR, PROMPT)))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessage("GMS " + status)
                .hasNoCause();
    }

    @Test
    void AC5_타임아웃은_LlmUnavailableException이다() {
        server.expect(requestTo(URL))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(
                        () ->
                                client(Map.of())
                                        .complete(
                                                new LlmRequest(
                                                        LlmStage.CONCEPT, Level.SENIOR, PROMPT)))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessage("GMS SocketTimeoutException");
    }

    static Stream<Arguments> unusableBodies() {
        return Stream.of(
                Arguments.of("choices 없음", "{\"choices\":[]}", "GMS 응답 형식 오류"),
                Arguments.of(
                        "거부",
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,\"refusal\":\"I"
                            + " can't help with "
                                + PROMPT
                                + "\"},\"finish_reason\":\"stop\"}]}",
                        "GMS 응답 거부"),
                Arguments.of(
                        "잘림",
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"summ\"},\"finish_reason\":\"length\"}]}",
                        "GMS 응답 잘림"),
                Arguments.of(
                        "content 없음",
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\"},\"finish_reason\":\"stop\"}]}",
                        "GMS 응답 형식 오류"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unusableBodies")
    void AC5_쓸_수_없는_응답은_LlmUnavailableException이고_프롬프트가_없다(
            String name, String body, String message) {
        server.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThatThrownBy(
                        () ->
                                client(Map.of())
                                        .complete(
                                                new LlmRequest(
                                                        LlmStage.OVERVIEW, Level.SENIOR, PROMPT)))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessage(message)
                .hasNoCause();
    }
}
