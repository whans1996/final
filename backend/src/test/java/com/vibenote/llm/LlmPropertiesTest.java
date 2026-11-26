package com.vibenote.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LlmPropertiesTest {

    final LlmProperties props =
            new LlmProperties(
                    "gms",
                    new LlmProperties.Gms(
                            "gms-secret-key-456",
                            "https://gms.ssafy.io/gmsapi/api.openai.com/v1",
                            "gpt-4.1-mini",
                            Map.of("quiz", "gpt-4.1"),
                            Duration.ofSeconds(60)),
                    new LlmProperties.Gemini(
                            "secret-key-123",
                            "gemini-3.5-flash-lite",
                            "https://generativelanguage.googleapis.com",
                            Duration.ofSeconds(60)),
                    new LlmProperties.Ollama(
                            "http://localhost:11434",
                            "qwen2.5-coder:7b",
                            Duration.ofSeconds(120),
                            16384));

    @Test
    void 설정을_문자열로_찍어도_Gemini_키가_나오지_않는다() {
        assertThat(props.toString())
                .doesNotContain("secret-key-123")
                .contains("apiKey=***")
                .contains("gemini-3.5-flash-lite");
    }

    @Test
    void AC2_설정을_문자열로_찍어도_GMS_키가_나오지_않는다() {
        assertThat(props.gms().toString())
                .doesNotContain("gms-secret-key-456")
                .contains("apiKey=***")
                .contains("gpt-4.1-mini")
                .contains("quiz=gpt-4.1");
        assertThat(props.toString()).doesNotContain("gms-secret-key-456");
    }

    @Test
    void stage_models가_없으면_빈_맵이다() {
        LlmProperties.Gms gms = new LlmProperties.Gms("k", "u", "m", null, Duration.ofSeconds(1));

        assertThat(gms.stageModels()).isEmpty();
    }
}
