package com.vibenote.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** F-27 AC13: 평가 러너는 {@code llm.provider}의 공급자 하나만 폴백·기록 없이 부른다. 키가 없으면 건너뛴다(empty). */
class LiveLlmClientsTest {

    static LlmProperties props(String provider, String gmsKey, String geminiKey) {
        return new LlmProperties(
                provider,
                new LlmProperties.Gms(
                        gmsKey, "https://gms.test/v1", "gpt-test", Map.of(), Duration.ofSeconds(5)),
                new LlmProperties.Gemini(
                        geminiKey, "gemini-test", "https://gemini.test", Duration.ofSeconds(5)),
                new LlmProperties.Ollama(
                        "http://ollama.test:11434", "qwen-test", Duration.ofSeconds(5), 16384));
    }

    @Test
    void AC13_provider_하나만_폴백_없이_만든다() {
        assertThat(LiveLlmClients.single(props("gms", "k", "")))
                .get()
                .isInstanceOf(GmsLlmClient.class);
        assertThat(LiveLlmClients.single(props("GEMINI", "", "k")))
                .get()
                .isInstanceOf(GeminiLlmClient.class);
        assertThat(LiveLlmClients.single(props("ollama", "", "")))
                .get()
                .isInstanceOf(OllamaLlmClient.class);
    }

    @Test
    void AC13_키가_없으면_건너뛰도록_empty다() {
        assertThat(LiveLlmClients.single(props("gms", "", "k"))).isEqualTo(Optional.empty());
        assertThat(LiveLlmClients.single(props("gemini", "k", " "))).isEqualTo(Optional.empty());
    }

    @Test
    void AC13_mock이나_모르는_값은_평가할_수_없다() {
        assertThatThrownBy(() -> LiveLlmClients.single(props("mock", "k", "k")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gms, gemini, ollama");
        assertThatThrownBy(() -> LiveLlmClients.single(props("openai", "k", "k")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
