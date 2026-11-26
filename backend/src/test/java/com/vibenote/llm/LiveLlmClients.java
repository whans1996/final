package com.vibenote.llm;

import java.util.Locale;
import java.util.Optional;
import org.springframework.web.client.RestClient;

/** 다른 패키지의 수동 실행 하네스(예: {@code prompteval.PromptEvalRunner})가 실제 클라이언트를 앱과 같은 타임아웃으로 만들 때 쓴다. */
public final class LiveLlmClients {

    private LiveLlmClients() {}

    /**
     * {@code llm.provider}의 공급자 <b>하나만</b> 만든다. 폴백·사용량 기록 없이 부르므로 V-2 기준선(GMS v1·Ollama v1)을 같은 러너로
     * 만들 수 있다 (F-27 AC13).
     *
     * @return 키가 필요한 공급자인데 키가 비었으면 empty (러너가 건너뛴다)
     * @throws IllegalArgumentException {@code gms|gemini|ollama}가 아니면
     */
    public static Optional<LlmClient> single(LlmProperties props) {
        String provider = props.provider().trim().toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "gms" ->
                    blank(props.gms().apiKey())
                            ? Optional.empty()
                            : Optional.of(
                                    new GmsLlmClient(
                                            LlmConfig.withTimeout(
                                                    RestClient.builder(), props.gms().timeout()),
                                            props.gms(),
                                            LlmSchemas.load()));
            case "gemini" ->
                    blank(props.gemini().apiKey())
                            ? Optional.empty()
                            : Optional.of(gemini(props.gemini()));
            case "ollama" ->
                    Optional.of(
                            new OllamaLlmClient(
                                    LlmConfig.withTimeout(
                                            RestClient.builder(), props.ollama().timeout()),
                                    props.ollama(),
                                    LlmSchemas.load()));
            default ->
                    throw new IllegalArgumentException(
                            "평가할 공급자(LLM_PROVIDER)는 gms, gemini, ollama 중 하나여야 합니다: " + provider);
        };
    }

    public static LlmClient gemini(LlmProperties.Gemini props) {
        return new GeminiLlmClient(
                LlmConfig.withTimeout(RestClient.builder(), props.timeout()), props);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
