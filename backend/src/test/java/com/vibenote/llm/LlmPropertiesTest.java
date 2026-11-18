package com.vibenote.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class LlmPropertiesTest {

    @Test
    void 설정을_문자열로_찍어도_Gemini_키가_나오지_않는다() {
        LlmProperties props =
                new LlmProperties(
                        "gemini",
                        new LlmProperties.Gemini(
                                "secret-key-123",
                                "gemini-3.5-flash-lite",
                                "https://generativelanguage.googleapis.com",
                                Duration.ofSeconds(60)),
                        new LlmProperties.Ollama(
                                "http://localhost:11434",
                                "qwen2.5-coder:7b",
                                Duration.ofSeconds(120)));

        assertThat(props.toString())
                .doesNotContain("secret-key-123")
                .contains("apiKey=***")
                .contains("gemini-3.5-flash-lite");
    }
}
