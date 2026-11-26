package com.vibenote.llm;

import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 공급자 하나를 감싸 호출 시도마다 {@code llm_usage_logs}에 1행을 남긴다 (F-00 AC6·AC7, NF-07).
 *
 * <p>기록 실패는 경고만 남기고 삼킨다 (기록 때문에 분석이 멈추면 안 된다). 위임 예외는 기록한 뒤 그대로 다시 던진다. 오류 메시지는 {@link
 * LlmUnavailableException}이면 메시지(키·프롬프트 없음), 그 밖의 예외는 종류만 남긴다.
 */
public class UsageLoggingLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(UsageLoggingLlmClient.class);
    private static final int ERROR_MESSAGE_MAX = 500;

    private final LlmClient delegate;
    private final String provider;
    private final Function<LlmStage, String> model;
    private final LlmUsageLogMapper mapper;

    /**
     * @param provider {@code llm_usage_logs.provider}에 적을 공급자 ({@code gms|ollama|gemini|mock}, F-27
     *     AC9)
     * @param model 실패 행에 적을 설정 모델명 (성공 행은 응답의 모델명)
     */
    public UsageLoggingLlmClient(
            LlmClient delegate, String provider, String model, LlmUsageLogMapper mapper) {
        this(delegate, provider, stage -> model, mapper);
    }

    /**
     * @param model 단계별 설정 모델명 (GMS {@code stage-models}, F-27 AC4). 실패 행에 적는다
     */
    public UsageLoggingLlmClient(
            LlmClient delegate,
            String provider,
            Function<LlmStage, String> model,
            LlmUsageLogMapper mapper) {
        this.delegate = delegate;
        this.provider = provider;
        this.model = model;
        this.mapper = mapper;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        long start = System.nanoTime();
        LlmResponse response;
        try {
            response = delegate.complete(request);
        } catch (RuntimeException e) {
            record(request, model.apply(request.stage()), 0, 0, start, false, errorMessage(e));
            throw e;
        }
        record(
                request,
                response.model(),
                response.inputTokens(),
                response.outputTokens(),
                start,
                true,
                null);
        return response;
    }

    private void record(
            LlmRequest request,
            String model,
            int inputTokens,
            int outputTokens,
            long start,
            boolean success,
            String errorMessage) {
        int latencyMs = (int) ((System.nanoTime() - start) / 1_000_000);
        try {
            mapper.insert(
                    new LlmUsageLog(
                            request.userId(),
                            request.analysisId(),
                            request.stage().key(),
                            provider,
                            model,
                            inputTokens,
                            outputTokens,
                            latencyMs,
                            success,
                            errorMessage));
        } catch (RuntimeException e) {
            log.warn(
                    "llm_usage_logs 기록 실패 (stage={}, model={}): {}",
                    request.stage().key(),
                    model,
                    e.getClass().getSimpleName());
        }
    }

    private static String errorMessage(RuntimeException e) {
        String message =
                e instanceof LlmUnavailableException
                        ? e.getMessage()
                        : e.getClass().getSimpleName();
        return message.length() > ERROR_MESSAGE_MAX
                ? message.substring(0, ERROR_MESSAGE_MAX)
                : message;
    }

    LlmClient delegate() {
        return delegate;
    }
}
