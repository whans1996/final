package com.vibenote.llm;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * {@code llm.provider}에 따라 {@link LlmClient} 빈을 <b>하나만</b> 조립한다 (F-00 AC1).
 *
 * <ul>
 *   <li>{@code mock}(기본): {@code Logging(Mock)}
 *   <li>{@code ollama}: {@code Fallback(Logging(Ollama))} — 실패를 {@code LLM_503}으로 바꾸려고 감싼다
 *   <li>{@code gemini}: {@code Fallback(Logging(Gemini), Logging(Ollama))} — Gemini 실패 시 재시도 없이
 *       Ollama
 * </ul>
 *
 * <p>호출 시도마다 사용량을 남기도록 공급자 하나하나를 {@link UsageLoggingLlmClient}로 감싼다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    LlmClient llmClient(
            LlmProperties properties,
            RestClient.Builder restClientBuilder,
            LlmUsageLogMapper usageLogMapper) {
        String provider = properties.provider().trim().toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "mock" ->
                    new UsageLoggingLlmClient(
                            new MockLlmClient(), MockLlmClient.MODEL, usageLogMapper);
            case "ollama" ->
                    new FallbackLlmClient(
                            List.of(
                                    ollama(
                                            properties.ollama(),
                                            restClientBuilder,
                                            usageLogMapper)));
            case "gemini" ->
                    new FallbackLlmClient(
                            List.of(
                                    gemini(properties.gemini(), restClientBuilder, usageLogMapper),
                                    ollama(
                                            properties.ollama(),
                                            restClientBuilder,
                                            usageLogMapper)));
            default ->
                    throw new IllegalStateException(
                            "llm.provider(LLM_PROVIDER)는 mock, gemini, ollama 중 하나여야 합니다: "
                                    + provider);
        };
    }

    private static LlmClient gemini(
            LlmProperties.Gemini props, RestClient.Builder builder, LlmUsageLogMapper mapper) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "llm.provider=gemini인데 GEMINI_API_KEY가 비어 있습니다. .env에 키를 넣거나 LLM_PROVIDER를"
                            + " mock으로 바꾸세요.");
        }
        return new UsageLoggingLlmClient(
                new GeminiLlmClient(withTimeout(builder, props.timeout()), props),
                props.model(),
                mapper);
    }

    private static LlmClient ollama(
            LlmProperties.Ollama props, RestClient.Builder builder, LlmUsageLogMapper mapper) {
        return new UsageLoggingLlmClient(
                new OllamaLlmClient(withTimeout(builder, props.timeout()), props),
                props.model(),
                mapper);
    }

    /** 공급자마다 빌더를 복제해 타임아웃이 서로 섞이지 않게 한다. */
    static RestClient.Builder withTimeout(RestClient.Builder builder, Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(timeout);
        return builder.clone().requestFactory(factory);
    }
}
