package com.vibenote.llm;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * {@code llm.provider}에 따라 {@link LlmClient} 빈을 <b>하나만</b> 조립한다 (F-00 AC1, F-27 AC1).
 *
 * <ul>
 *   <li>{@code mock}(기본): {@code Logging(Mock)}
 *   <li>{@code gms}: {@code Fallback(Logging(Gms), Logging(Ollama))} — GMS 실패 시 재시도 없이 Ollama(간이
 *       모드)
 *   <li>{@code ollama}: {@code Fallback(Logging(Ollama))} — 실패를 {@code LLM_503}으로 바꾸려고 감싼다
 *   <li>{@code gemini}: {@code Fallback(Logging(Gemini))} — 평가 비교용. 폴백 없음
 * </ul>
 *
 * <p>호출 시도마다 사용량을 남기도록 공급자 하나하나를 {@link UsageLoggingLlmClient}로 감싼다. GMS·Ollama는 단계 출력 스키마({@link
 * LlmSchemas})로 형식을 강제한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final String PROVIDERS = "mock, gms, ollama, gemini";

    @Bean
    LlmClient llmClient(
            LlmProperties properties,
            RestClient.Builder restClientBuilder,
            LlmUsageLogMapper usageLogMapper) {
        String provider = properties.provider().trim().toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "mock" ->
                    new UsageLoggingLlmClient(
                            new MockLlmClient(), "mock", MockLlmClient.MODEL, usageLogMapper);
            case "gms" -> {
                LlmSchemas schemas = LlmSchemas.load();
                yield new FallbackLlmClient(
                        List.of(
                                gms(properties.gms(), schemas, restClientBuilder, usageLogMapper),
                                ollama(
                                        properties.ollama(),
                                        schemas,
                                        restClientBuilder,
                                        usageLogMapper)));
            }
            case "ollama" ->
                    new FallbackLlmClient(
                            List.of(
                                    ollama(
                                            properties.ollama(),
                                            LlmSchemas.load(),
                                            restClientBuilder,
                                            usageLogMapper)));
            case "gemini" ->
                    new FallbackLlmClient(
                            List.of(
                                    gemini(
                                            properties.gemini(),
                                            restClientBuilder,
                                            usageLogMapper)));
            default ->
                    throw new IllegalStateException(
                            "llm.provider(LLM_PROVIDER)는 "
                                    + PROVIDERS
                                    + " 중 하나여야 합니다: "
                                    + provider);
        };
    }

    /**
     * 키 없음·빈 모델·없는 단계 키·빈 단계 모델은 기동 실패 (AC2·AC4). 메시지에 키 값을 넣지 않는다.
     *
     * <p>{@code .env}의 {@code GMS_MODEL=}은 기본값이 아니라 빈 문자열이 된다. 그대로 두면 GMS가 매번 400으로 거절해 모든 분석이 조용히
     * 간이 모드(Ollama)로 넘어가므로 기동 때 막는다.
     */
    static LlmClient gms(
            LlmProperties.Gms props,
            LlmSchemas schemas,
            RestClient.Builder builder,
            LlmUsageLogMapper mapper) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "llm.provider=gms인데 GMS_API_KEY가 비어 있습니다. .env에 GMS 키를 넣거나 LLM_PROVIDER를"
                            + " mock으로 바꾸세요.");
        }
        if (props.model() == null || props.model().isBlank()) {
            throw new IllegalStateException(
                    "llm.gms.model이 비어 있습니다. .env의 GMS_MODEL= 줄을 지우거나 모델 이름을 넣으세요.");
        }
        Set<String> stages =
                Arrays.stream(LlmStage.values()).map(LlmStage::key).collect(Collectors.toSet());
        Set<String> unknown = new TreeSet<>(props.stageModels().keySet());
        unknown.removeAll(stages);
        if (!unknown.isEmpty()) {
            throw new IllegalStateException(
                    "llm.gms.stage-models에 없는 단계가 있습니다: "
                            + unknown
                            + ". 쓸 수 있는 단계: "
                            + new TreeSet<>(stages));
        }
        Set<String> blank =
                props.stageModels().entrySet().stream()
                        .filter(e -> e.getValue() == null || e.getValue().isBlank())
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toCollection(TreeSet::new));
        if (!blank.isEmpty()) {
            throw new IllegalStateException(
                    "llm.gms.stage-models에 모델 이름이 빈 단계가 있습니다: "
                            + blank
                            + ". 값을 넣거나 그 줄을 지우세요(기본 모델을 씁니다).");
        }
        GmsLlmClient client =
                new GmsLlmClient(withTimeout(builder, props.timeout()), props, schemas);
        return new UsageLoggingLlmClient(client, "gms", client::modelFor, mapper);
    }

    static LlmClient gemini(
            LlmProperties.Gemini props, RestClient.Builder builder, LlmUsageLogMapper mapper) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "llm.provider=gemini인데 GEMINI_API_KEY가 비어 있습니다. .env에 키를 넣거나 LLM_PROVIDER를"
                            + " mock으로 바꾸세요.");
        }
        return new UsageLoggingLlmClient(
                new GeminiLlmClient(withTimeout(builder, props.timeout()), props),
                "gemini",
                props.model(),
                mapper);
    }

    static LlmClient ollama(
            LlmProperties.Ollama props,
            LlmSchemas schemas,
            RestClient.Builder builder,
            LlmUsageLogMapper mapper) {
        return new UsageLoggingLlmClient(
                new OllamaLlmClient(withTimeout(builder, props.timeout()), props, schemas),
                "ollama",
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
