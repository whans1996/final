package com.vibenote.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vibenote.common.Level;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class UsageLoggingLlmClientTest {

    final List<LlmUsageLog> rows = new ArrayList<>();
    final LlmUsageLogMapper mapper = rows::add;

    final LlmRequest request =
            new LlmRequest(LlmStage.QUIZ, Level.JUNIOR, "PROMPT-BODY-SECRET", 7L, 99L);

    @Test
    void AC6_성공하면_응답_모델과_토큰과_요청의_사용자_분석_id를_기록한다() {
        LlmResponse ok = new LlmResponse("{}", "qwen-test", 11, 22);
        LlmClient client = new UsageLoggingLlmClient(req -> ok, "qwen-test", mapper);

        assertThat(client.complete(request)).isEqualTo(ok);

        assertThat(rows).hasSize(1);
        LlmUsageLog row = rows.get(0);
        assertThat(row.userId()).isEqualTo(7L);
        assertThat(row.analysisId()).isEqualTo(99L);
        assertThat(row.stage()).isEqualTo("quiz");
        assertThat(row.model()).isEqualTo("qwen-test");
        assertThat(row.inputTokens()).isEqualTo(11);
        assertThat(row.outputTokens()).isEqualTo(22);
        assertThat(row.latencyMs()).isGreaterThanOrEqualTo(0);
        assertThat(row.success()).isTrue();
        assertThat(row.errorMessage()).isNull();
    }

    @Test
    void AC6_실패하면_설정_모델과_오류를_기록하고_예외를_다시_던진다() {
        LlmUnavailableException failure = new LlmUnavailableException("Gemini 429");
        LlmClient client =
                new UsageLoggingLlmClient(
                        req -> {
                            throw failure;
                        },
                        "gemini-test",
                        mapper);

        assertThatThrownBy(() -> client.complete(request)).isSameAs(failure);

        assertThat(rows).hasSize(1);
        LlmUsageLog row = rows.get(0);
        assertThat(row.model()).isEqualTo("gemini-test");
        assertThat(row.inputTokens()).isZero();
        assertThat(row.outputTokens()).isZero();
        assertThat(row.success()).isFalse();
        assertThat(row.errorMessage()).isEqualTo("Gemini 429");
    }

    @Test
    void AC6_공급자_실패가_아닌_예외는_메시지_대신_종류만_기록한다() {
        LlmClient client =
                new UsageLoggingLlmClient(
                        req -> {
                            throw new IllegalStateException("PROMPT-BODY-SECRET 포함 메시지");
                        },
                        "mock",
                        mapper);

        assertThatThrownBy(() -> client.complete(request))
                .isInstanceOf(IllegalStateException.class);

        assertThat(rows.get(0).errorMessage()).isEqualTo("IllegalStateException");
    }

    @Test
    void AC6_오류_메시지는_500자로_자른다() {
        LlmClient client =
                new UsageLoggingLlmClient(
                        req -> {
                            throw new LlmUnavailableException("x".repeat(600));
                        },
                        "gemini-test",
                        mapper);

        assertThatThrownBy(() -> client.complete(request));

        assertThat(rows.get(0).errorMessage()).hasSize(500);
    }

    @Test
    void AC6_Gemini가_실패하고_Ollama로_폴백하면_두_행이_쌓인다() {
        LlmClient gemini =
                new UsageLoggingLlmClient(
                        req -> {
                            throw new LlmUnavailableException("Gemini 500");
                        },
                        "gemini-test",
                        mapper);
        LlmClient ollama =
                new UsageLoggingLlmClient(
                        req -> new LlmResponse("{}", "qwen-test", 1, 2), "qwen-test", mapper);

        new FallbackLlmClient(List.of(gemini, ollama)).complete(request);

        assertThat(rows)
                .extracting(LlmUsageLog::model, LlmUsageLog::success)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("gemini-test", false),
                        org.assertj.core.groups.Tuple.tuple("qwen-test", true));
    }

    @Test
    void AC7_기록이_실패해도_응답을_반환한다(CapturedOutput output) {
        LlmResponse ok = new LlmResponse("{}", "mock", 0, 0);
        LlmUsageLogMapper broken =
                log -> {
                    throw new IllegalStateException("DB down");
                };
        LlmClient client = new UsageLoggingLlmClient(req -> ok, "mock", broken);

        assertThat(client.complete(request)).isEqualTo(ok);
        assertThat(output).contains("WARN").contains("llm_usage_logs");
    }

    @Test
    void AC7_기록이_실패해도_원래_예외를_던진다() {
        LlmUnavailableException failure = new LlmUnavailableException("Ollama 500");
        LlmUsageLogMapper broken =
                log -> {
                    throw new IllegalStateException("DB down");
                };
        LlmClient client =
                new UsageLoggingLlmClient(
                        req -> {
                            throw failure;
                        },
                        "qwen-test",
                        broken);

        assertThatThrownBy(() -> client.complete(request)).isSameAs(failure);
    }
}
