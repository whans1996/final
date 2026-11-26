package com.vibenote.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.vibenote.common.Level;
import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * 실제 GMS·Gemini·Ollama 연결 확인 (F-00 AC9, F-27 9절). <b>사람이 수동으로</b> {@code ./gradlew liveTest}로만
 * 실행한다. {@code test}(CI·git hook·verify)에서는 제외된다.
 *
 * <p>설정은 {@code application.yml}과 저장소 루트 {@code .env}에서 읽는다. 키가 비었거나 Ollama 서버가 없으면 실패 대신 건너뛴다. 출력된
 * 모델·지연·토큰을 {@code docs/산출물/08-외부API-생성형AI-활용.md} §1.5·§2에 적는다. GMS 크레딧·Gemini 무료 한도를 쓴다.
 */
@Tag("live")
class LlmLiveTest {

    static final String PROMPT =
            """
            다음 Java 메서드가 하는 일을 초보자에게 한 문장으로 설명하라.
            {"summary": "<설명>"} 형식의 JSON으로만 답하라.

            int add(int a, int b) { return a + b; }
            """;

    final LlmProperties properties = loadProperties();
    final LlmRequest request = new LlmRequest(LlmStage.OVERVIEW, Level.BEGINNER, PROMPT);

    @Test
    void AC9_Gemini_실제_호출() throws Exception {
        LlmProperties.Gemini gemini = properties.gemini();
        assumeTrue(gemini.apiKey() != null && !gemini.apiKey().isBlank(), "GEMINI_API_KEY가 없어 건너뜀");

        call(
                "Gemini",
                new GeminiLlmClient(
                        LlmConfig.withTimeout(RestClient.builder(), gemini.timeout()), gemini));
    }

    @Test
    void AC9_Ollama_실제_호출() throws Exception {
        LlmProperties.Ollama ollama = properties.ollama();
        assumeTrue(reachable(ollama.baseUrl()), "Ollama 서버(" + ollama.baseUrl() + ")가 없어 건너뜀");

        call(
                "Ollama",
                new OllamaLlmClient(
                        LlmConfig.withTimeout(RestClient.builder(), ollama.timeout()),
                        ollama,
                        LlmSchemas.load()));
    }

    private void call(String provider, LlmClient client) throws Exception {
        long start = System.nanoTime();
        LlmResponse response = client.complete(request);
        long latencyMs = (System.nanoTime() - start) / 1_000_000;

        System.out.printf(
                "[live] %s model=%s latency=%dms input=%d output=%d%n[live] content=%s%n",
                provider,
                response.model(),
                latencyMs,
                response.inputTokens(),
                response.outputTokens(),
                response.content());
        assertThat(new ObjectMapper().readTree(response.content()).path("summary").asString())
                .isNotBlank();
        assertThat(response.inputTokens()).isPositive();
        assertThat(response.outputTokens()).isPositive();
    }

    /**
     * F-27 9절: 모델 목록 1회 + 구조화 출력 1회. 앞뒤로 {@code key-info}(크레딧)를 찍어 호출 1회의 크레딧 소모를 본다. 키 값은 출력하지
     * 않는다. 모델은 {@code GMS_MODEL}로 바꿔 돌린다.
     */
    @Test
    void gms_구조화_출력() throws Exception {
        LlmProperties.Gms gms = properties.gms();
        assumeTrue(gms.apiKey() != null && !gms.apiKey().isBlank(), "GMS_API_KEY가 없어 건너뜀");
        RestClient raw =
                LlmConfig.withTimeout(RestClient.builder(), gms.timeout())
                        .defaultHeader("Authorization", "Bearer " + gms.apiKey())
                        .build();
        String keyInfo = gms.baseUrl().replaceFirst("/gmsapi/.*$", "/gmsapi/key-info");

        System.out.printf("[live] GMS key-info(전)=%s%n", get(raw, keyInfo));
        System.out.printf("[live] GMS models=%s%n", get(raw, gms.baseUrl() + "/models"));
        call(
                "GMS",
                new GmsLlmClient(
                        LlmConfig.withTimeout(RestClient.builder(), gms.timeout()),
                        gms,
                        LlmSchemas.load()));
        System.out.printf("[live] GMS key-info(후)=%s%n", get(raw, keyInfo));
    }

    /** 실패해도 테스트를 멈추지 않고 상태만 돌려준다 (모델 목록을 열지 않은 게이트웨이일 수 있다). */
    private static String get(RestClient client, String url) {
        try {
            return client.get().uri(url).retrieve().body(String.class);
        } catch (RuntimeException e) {
            return "조회 실패 " + e.getClass().getSimpleName();
        }
    }

    private static boolean reachable(String baseUrl) {
        try {
            LlmConfig.withTimeout(RestClient.builder(), Duration.ofSeconds(2))
                    .build()
                    .get()
                    .uri(baseUrl + "/api/tags")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** 앱과 같은 방식(application.yml + ../.env)으로 {@code llm.*}을 읽는다. */
    private static LlmProperties loadProperties() {
        StandardEnvironment environment = new StandardEnvironment();
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        return Binder.get(environment).bindOrCreate("llm", LlmProperties.class);
    }
}
