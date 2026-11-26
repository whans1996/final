package com.vibenote.prompteval;

import com.vibenote.common.Level;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 호출마다 단계·시도·토큰·지연·원문을 기록하고, 호출 사이에 {@code pauseMs}만큼 쉰다. {@link #beginUnit()} 뒤의 호출은 단위(LLM 호출 묶음)
 * 하나로 센다 (E-1 AC4).
 */
final class RecordingLlmClient implements LlmClient {

    /** 일시적인 서버 오류만 다시 보낸다. 429(한도 초과)는 기다려도 그날은 풀리지 않으므로 다시 보내지 않는다. */
    private static final List<String> TRANSIENT =
            List.of(" 500", " 502", " 503", " 504", "Timeout");

    private static final long[] BACKOFF_MS = {20_000, 40_000};

    private final LlmClient delegate;
    private final long pauseMs;
    private final Map<Level, List<Map<String, Object>>> calls = new LinkedHashMap<>();
    private final Set<String> models = new LinkedHashSet<>();
    private Level current;
    private boolean first = true;
    private List<Map<String, Object>> unit = new ArrayList<>();

    RecordingLlmClient(LlmClient delegate, long pauseMs) {
        this.delegate = delegate;
        this.pauseMs = pauseMs;
    }

    void startLevel(Level level) {
        current = level;
        calls.put(level, new ArrayList<>());
    }

    List<Map<String, Object>> callsOf(Level level) {
        return calls.getOrDefault(level, List.of());
    }

    /** 응답한 모델 (처음 응답한 순서). */
    Set<String> models() {
        return models;
    }

    void beginUnit() {
        unit = new ArrayList<>();
    }

    /** {@link #beginUnit()} 뒤의 호출 기록. */
    UnitCalls unitCalls() {
        int responses = (int) unit.stream().filter(r -> !r.containsKey("error")).count();
        Map<String, Object> last = unit.isEmpty() ? null : unit.get(unit.size() - 1);
        return new UnitCalls(
                unit.size(),
                responses,
                last != null && last.containsKey("error"),
                last == null ? null : (String) last.get("content"));
    }

    /**
     * @param calls 시도 수 (일시 오류 재전송 포함)
     * @param responses 응답을 받은 시도 수 (형식 재시도 판단에 쓴다)
     * @param lastFailed 마지막 시도가 응답 없이 실패했다
     * @param lastContent 마지막 응답 원문
     */
    record UnitCalls(int calls, int responses, boolean lastFailed, String lastContent) {}

    Map<String, Object> totals() {
        List<Map<String, Object>> all = calls.values().stream().flatMap(List::stream).toList();
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("calls", all.size());
        totals.put("inputTokens", sum(all, "inputTokens"));
        totals.put("outputTokens", sum(all, "outputTokens"));
        totals.put("latencyMs", sum(all, "latencyMs"));
        return totals;
    }

    private static long sum(List<Map<String, Object>> rows, String key) {
        return rows.stream()
                .map(r -> r.get(key))
                .filter(Number.class::isInstance)
                .mapToLong(v -> ((Number) v).longValue())
                .sum();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        for (int retry = 0; ; retry++) {
            try {
                return attempt(request, retry == 0 ? pauseMs : BACKOFF_MS[retry - 1]);
            } catch (RuntimeException e) {
                String message = String.valueOf(e.getMessage());
                boolean transientError = TRANSIENT.stream().anyMatch(message::contains);
                if (!transientError || retry >= BACKOFF_MS.length) {
                    throw e;
                }
                System.out.printf(
                        "[prompt-eval] %s → %d초 뒤 다시 시도%n", message, BACKOFF_MS[retry] / 1000);
            }
        }
    }

    private LlmResponse attempt(LlmRequest request, long waitMs) {
        if (!first && waitMs > 0) {
            sleep(waitMs);
        }
        first = false;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("stage", request.stage().name());
        row.put("promptChars", request.prompt().length());
        long start = System.nanoTime();
        try {
            LlmResponse response = delegate.complete(request);
            row.put("model", response.model());
            row.put("inputTokens", response.inputTokens());
            row.put("outputTokens", response.outputTokens());
            row.put("latencyMs", (System.nanoTime() - start) / 1_000_000);
            row.put("content", response.content());
            models.add(Objects.requireNonNullElse(response.model(), "unknown"));
            return response;
        } catch (RuntimeException e) {
            row.put("latencyMs", (System.nanoTime() - start) / 1_000_000);
            row.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            throw e;
        } finally {
            calls.get(current).add(row);
            unit.add(row);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
