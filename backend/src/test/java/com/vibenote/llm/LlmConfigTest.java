package com.vibenote.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class LlmConfigTest {

    final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withUserConfiguration(LlmConfig.class)
                    .withBean(RestClient.Builder.class, RestClient::builder)
                    .withBean(LlmUsageLogMapper.class, () -> log -> {})
                    .withPropertyValues(
                            "llm.gemini.model=gemini-test", "llm.ollama.model=qwen-test");

    static Stream<Arguments> providers() {
        return Stream.of(
                Arguments.of(null, "Logging(Mock)"),
                Arguments.of("mock", "Logging(Mock)"),
                Arguments.of("ollama", "Fallback(Logging(Ollama))"),
                Arguments.of("gemini", "Fallback(Logging(Gemini), Logging(Ollama))"));
    }

    @ParameterizedTest(name = "provider={0}")
    @MethodSource("providers")
    void AC1_provider별로_LlmClient가_하나만_조립된다(String provider, String expected) {
        ApplicationContextRunner configured =
                provider == null
                        ? runner
                        : runner.withPropertyValues(
                                "llm.provider=" + provider, "llm.gemini.api-key=test-key");

        configured.run(
                context -> {
                    assertThat(context).hasSingleBean(LlmClient.class);
                    assertThat(shape(context.getBean(LlmClient.class))).isEqualTo(expected);
                });
    }

    @Test
    void AC8_gemini인데_키가_없으면_기동_실패() {
        runner.withPropertyValues("llm.provider=gemini", "llm.gemini.api-key=")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(context.getStartupFailure())
                                    .rootCause()
                                    .hasMessageContaining("GEMINI_API_KEY");
                        });
    }

    @Test
    void 알_수_없는_provider면_기동_실패() {
        runner.withPropertyValues("llm.provider=openai")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(context.getStartupFailure())
                                    .rootCause()
                                    .hasMessageContaining("openai")
                                    .hasMessageContaining("LLM_PROVIDER");
                        });
    }

    /** 조립 구조를 {@code Fallback(Logging(Gemini), Logging(Ollama))} 형태 문자열로 만든다. */
    static String shape(LlmClient client) {
        if (client instanceof UsageLoggingLlmClient logging) {
            return "Logging(" + shape(logging.delegate()) + ")";
        }
        if (client instanceof FallbackLlmClient fallback) {
            return fallback.chain().stream()
                    .map(LlmConfigTest::shape)
                    .collect(Collectors.joining(", ", "Fallback(", ")"));
        }
        return client.getClass().getSimpleName().replace("LlmClient", "");
    }
}
