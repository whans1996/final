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
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiLlmClientTest {

    static final String KEY = "test-gemini-key-SECRET";
    static final String PROMPT = "PROMPT-BODY-SECRET 이 코드를 설명하고 JSON으로 답하라";

    final RestClient.Builder builder = RestClient.builder();
    final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    final GeminiLlmClient client =
            new GeminiLlmClient(
                    builder,
                    new LlmProperties.Gemini(
                            KEY, "gemini-test", "https://gemini.test", Duration.ofSeconds(5)));

    final LlmRequest request = new LlmRequest(LlmStage.OVERVIEW, Level.BEGINNER, PROMPT);

    @Test
    void AC2_헤더_키와_JSON_응답형식으로_호출하고_본문과_토큰을_돌려준다() {
        server.expect(requestTo("https://gemini.test/v1beta/models/gemini-test:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", KEY))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value(PROMPT))
                .andExpect(
                        jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andRespond(
                        withSuccess(
                                """
                                {"candidates":[{"content":{"parts":[{"text":"{\\"summary\\":\\"요약\\"}"}],"role":"model"}}],
                                 "usageMetadata":{"promptTokenCount":12,"candidatesTokenCount":34,"totalTokenCount":46}}
                                """,
                                MediaType.APPLICATION_JSON));

        LlmResponse response = client.complete(request);

        server.verify();
        assertThat(response)
                .isEqualTo(new LlmResponse("{\"summary\":\"요약\"}", "gemini-test", 12, 34));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 429, 500, 503})
    void AC4_오류_상태는_LlmUnavailableException이고_키와_프롬프트가_없다(int status) {
        server.expect(requestTo("https://gemini.test/v1beta/models/gemini-test:generateContent"))
                .andRespond(
                        withStatus(HttpStatus.valueOf(status))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body("{\"error\":{\"message\":\"echo " + PROMPT + "\"}}"));

        assertThatThrownBy(() -> client.complete(request))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessage("Gemini " + status)
                .hasNoCause();
    }

    @Test
    void AC4_타임아웃과_연결_실패는_LlmUnavailableException이다() {
        server.expect(requestTo("https://gemini.test/v1beta/models/gemini-test:generateContent"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.complete(request))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessage("Gemini SocketTimeoutException")
                .hasNoCause();

        server.reset();
        server.expect(requestTo("https://gemini.test/v1beta/models/gemini-test:generateContent"))
                .andRespond(withException(new ConnectException("Connection refused")));

        assertThatThrownBy(() -> client.complete(request))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessage("Gemini ConnectException");
    }

    @Test
    void AC4_응답_형식이_이상하면_LlmUnavailableException이다() {
        server.expect(requestTo("https://gemini.test/v1beta/models/gemini-test:generateContent"))
                .andRespond(
                        withSuccess(
                                "{\"candidates\":[],\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}",
                                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete(request))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessage("Gemini 응답 형식 오류");
    }
}
