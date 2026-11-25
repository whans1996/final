package com.vibenote.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.vibenote.common.Level;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

@ExtendWith(OutputCaptureExtension.class)
class FallbackLlmClientTest {

    static final String KEY = "test-gemini-key-SECRET";
    static final String PROMPT = "PROMPT-BODY-SECRET 이 코드를 설명하라";
    static final String GEMINI_URL =
            "https://gemini.test/v1beta/models/gemini-test:generateContent";

    final RestClient.Builder builder = RestClient.builder();
    final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    final GeminiLlmClient gemini =
            new GeminiLlmClient(
                    builder,
                    new LlmProperties.Gemini(
                            KEY, "gemini-test", "https://gemini.test", Duration.ofSeconds(5)));

    final LlmRequest request = new LlmRequest(LlmStage.CONCEPT, Level.SENIOR, PROMPT);
    static final org.springframework.http.MediaType JSON =
            org.springframework.http.MediaType.APPLICATION_JSON;

    final LlmResponse ollamaResponse = new LlmResponse("{\"ok\":true}", "qwen-test", 3, 4);

    static Stream<Arguments> geminiFailures() {
        return Stream.of(
                Arguments.of("429", withStatus(HttpStatus.TOO_MANY_REQUESTS)),
                Arguments.of("500", withStatus(HttpStatus.INTERNAL_SERVER_ERROR)),
                Arguments.of("타임아웃", withException(new SocketTimeoutException("Read timed out"))),
                Arguments.of("연결 실패", withException(new ConnectException("refused"))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("geminiFailures")
    void AC4_Gemini_429_500_타임아웃이면_Ollama로_전환한다(String name, ResponseCreator failure) {
        server.expect(once(), requestTo(GEMINI_URL)).andRespond(failure);
        AtomicInteger ollamaCalls = new AtomicInteger();
        LlmClient ollama =
                req -> {
                    ollamaCalls.incrementAndGet();
                    return ollamaResponse;
                };

        LlmResponse response = new FallbackLlmClient(List.of(gemini, ollama)).complete(request);

        server.verify(); // Gemini는 정확히 한 번만 불렸다 (재시도 없음)
        assertThat(ollamaCalls).hasValue(1);
        assertThat(response).isEqualTo(ollamaResponse);
        assertThat(response.model()).isEqualTo("qwen-test");
    }

    @Test
    void AC4_Gemini가_성공하면_Ollama를_부르지_않는다() {
        LlmResponse geminiResponse = new LlmResponse("{}", "gemini-test", 1, 1);
        LlmClient ollama =
                req -> {
                    throw new AssertionError("Ollama를 부르면 안 된다");
                };

        assertThat(new FallbackLlmClient(List.of(req -> geminiResponse, ollama)).complete(request))
                .isEqualTo(geminiResponse);
    }

    @Test
    void AC5_둘_다_실패하면_LLM_503이고_키와_프롬프트가_없다(CapturedOutput output) {
        server.expect(once(), requestTo(GEMINI_URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        LlmClient ollama =
                req -> {
                    throw new LlmUnavailableException("Ollama ConnectException");
                };

        assertThatThrownBy(() -> new FallbackLlmClient(List.of(gemini, ollama)).complete(request))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> {
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LLM_503);
                            assertThat(e).hasMessageNotContaining(KEY);
                            assertThat(e).hasMessageNotContaining("PROMPT-BODY-SECRET");
                        });
        assertThat(output).contains("Gemini 503").contains("Ollama ConnectException");
        assertThat(output).doesNotContain(KEY).doesNotContain("PROMPT-BODY-SECRET");
    }

    @Test
    void AC5_provider가_ollama이고_Ollama가_실패하면_LLM_503이다() {
        LlmClient ollama =
                req -> {
                    throw new LlmUnavailableException("Ollama 500");
                };

        assertThatThrownBy(() -> new FallbackLlmClient(List.of(ollama)).complete(request))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LLM_503));
    }

    static final String GMS_URL = "https://gms.test/v1/chat/completions";

    static Stream<Arguments> gmsFailures() {
        String refusal =
                "{\"choices\":[{\"message\":{\"content\":null,\"refusal\":\"no\"},\"finish_reason\":\"stop\"}]}";
        String truncated =
                "{\"choices\":[{\"message\":{\"content\":\"{\"},\"finish_reason\":\"length\"}]}";
        return Stream.of(
                Arguments.of("401", withStatus(HttpStatus.UNAUTHORIZED)),
                Arguments.of("429", withStatus(HttpStatus.TOO_MANY_REQUESTS)),
                Arguments.of("503", withStatus(HttpStatus.SERVICE_UNAVAILABLE)),
                Arguments.of("타임아웃", withException(new SocketTimeoutException("Read timed out"))),
                Arguments.of("choices 없음", withSuccess("{\"choices\":[]}", JSON)),
                Arguments.of("거부", withSuccess(refusal, JSON)),
                Arguments.of("잘림", withSuccess(truncated, JSON)));
    }

    GmsLlmClient gms() {
        return new GmsLlmClient(
                builder,
                new LlmProperties.Gms(
                        "test-gms-key-SECRET",
                        "https://gms.test/v1",
                        "gpt-test",
                        java.util.Map.of(),
                        Duration.ofSeconds(5)),
                LlmSchemas.load());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("gmsFailures")
    void AC5_GMS가_실패하면_재시도_없이_Ollama가_답한다(String name, ResponseCreator failure) {
        server.expect(once(), requestTo(GMS_URL)).andRespond(failure);
        AtomicInteger ollamaCalls = new AtomicInteger();
        LlmClient ollama =
                req -> {
                    ollamaCalls.incrementAndGet();
                    return ollamaResponse;
                };

        LlmResponse response = new FallbackLlmClient(List.of(gms(), ollama)).complete(request);

        server.verify(); // GMS는 정확히 한 번만 불렸다 (재시도 없음)
        assertThat(ollamaCalls).hasValue(1);
        assertThat(response).isEqualTo(ollamaResponse);
    }

    @Test
    void AC5_GMS와_Ollama가_모두_실패하면_LLM_503이고_키와_프롬프트가_없다(CapturedOutput output) {
        server.expect(once(), requestTo(GMS_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        LlmClient ollama =
                req -> {
                    throw new LlmUnavailableException("Ollama ConnectException");
                };

        assertThatThrownBy(() -> new FallbackLlmClient(List.of(gms(), ollama)).complete(request))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LLM_503));
        assertThat(output).contains("GMS 401").contains("Ollama ConnectException");
        assertThat(output)
                .doesNotContain("test-gms-key-SECRET")
                .doesNotContain("PROMPT-BODY-SECRET");
    }

    @Test
    void 공급자_실패가_아닌_예외는_폴백하지_않고_그대로_던진다() {
        LlmClient broken =
                req -> {
                    throw new IllegalStateException("버그");
                };
        LlmClient ollama =
                req -> {
                    throw new AssertionError("Ollama를 부르면 안 된다");
                };

        assertThatThrownBy(() -> new FallbackLlmClient(List.of(broken, ollama)).complete(request))
                .isInstanceOf(IllegalStateException.class);
    }
}
