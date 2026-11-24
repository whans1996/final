package com.vibenote.analysis.structure;

import com.vibenote.analysis.AnalysisDeletedEvent;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 구조 계산 결과 캐시와 동시 계산 상한 (F-21a 5.3절 4). 분석 id 단위로 메모리에만 둔다 (DB 저장 없음).
 *
 * <p>DONE 분석의 선별 파일·역할·개요는 바뀌지 않으므로(파일은 SELECTING, 역할·개요는 EXPLAINING 때 쓰고 그 뒤에는 삭제만 있다) 결과를 그대로 다시
 * 쓴다. 같은 분석을 동시에 요청하면 계산은 하나만 하고 나머지는 그 결과를 기다린다. 계산은 전체에서 {@code maxConcurrent}개까지만 하고, {@code
 * queueTimeout} 안에 자리가 나지 않으면 ANALYSIS_007이다. 열람 권한은 캐시와 상관없이 호출자가 매 요청 확인한다.
 */
@Component
public class StructureCache {

    /** 계산 결과. cacheable이 거짓(작업 예산을 다 씀 등)이면 응답만 하고 캐시에 두지 않는다. */
    public record Computed(AnalysisStructureResponse response, boolean cacheable) {}

    private final Map<Long, AnalysisStructureResponse> results;
    private final Map<Long, CompletableFuture<AnalysisStructureResponse>> running =
            new ConcurrentHashMap<>();
    private final Semaphore permits;
    private final Duration queueTimeout;

    public StructureCache(StructureProperties properties) {
        int size = Math.max(0, properties.cacheSize());
        this.results =
                new LinkedHashMap<>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(
                            Map.Entry<Long, AnalysisStructureResponse> eldest) {
                        return size() > size;
                    }
                };
        this.permits = new Semaphore(Math.max(1, properties.maxConcurrent()), true);
        this.queueTimeout = properties.queueTimeout();
    }

    /**
     * DONE 분석의 결과. 캐시에 있으면 그것을, 같은 분석을 계산 중이면 그 결과를 기다려 주고, 아니면 동시 계산 상한 안에서 계산해 cacheable이면 넣는다.
     * 계산이 예외로 끝나면 기다리던 요청도 같은 예외다.
     */
    public AnalysisStructureResponse get(long analysisId, Supplier<Computed> compute) {
        AnalysisStructureResponse cached = cached(analysisId);
        if (cached != null) {
            return cached;
        }
        CompletableFuture<AnalysisStructureResponse> mine = new CompletableFuture<>();
        CompletableFuture<AnalysisStructureResponse> other = running.putIfAbsent(analysisId, mine);
        if (other != null) {
            return await(other);
        }
        try {
            // 첫 확인과 등록 사이에 다른 요청이 계산을 마쳤을 수 있다
            AnalysisStructureResponse response = cached(analysisId);
            if (response == null) {
                Computed computed = limited(compute);
                response = computed.response();
                if (computed.cacheable()) {
                    synchronized (results) {
                        results.put(analysisId, response);
                    }
                }
            }
            mine.complete(response);
            return response;
        } catch (RuntimeException | Error e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            // 결과를 넣은 뒤에 지운다: 새 요청은 계산 중 표시나 캐시 중 하나를 본다
            running.remove(analysisId, mine);
        }
    }

    /** 캐시 없이 동시 계산 상한 안에서만 계산한다 (진행 중·실패한 분석). */
    public AnalysisStructureResponse compute(Supplier<Computed> compute) {
        return limited(compute).response();
    }

    /** 삭제가 커밋되면 비운다. 그 전에 시작한 계산이 뒤늦게 넣어도 열람 확인이 404로 막고, 크기 상한이 결국 밀어낸다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void evict(AnalysisDeletedEvent event) {
        synchronized (results) {
            results.remove(event.analysisId());
        }
    }

    boolean contains(long analysisId) {
        synchronized (results) {
            return results.containsKey(analysisId);
        }
    }

    private AnalysisStructureResponse cached(long analysisId) {
        synchronized (results) {
            return results.get(analysisId);
        }
    }

    private Computed limited(Supplier<Computed> compute) {
        boolean acquired;
        try {
            acquired = permits.tryAcquire(queueTimeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.ANALYSIS_007);
        }
        if (!acquired) {
            throw new BusinessException(ErrorCode.ANALYSIS_007);
        }
        try {
            return compute.get();
        } finally {
            permits.release();
        }
    }

    private static AnalysisStructureResponse await(
            CompletableFuture<AnalysisStructureResponse> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }
}
