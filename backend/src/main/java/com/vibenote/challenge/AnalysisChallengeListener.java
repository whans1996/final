package com.vibenote.challenge;

import com.vibenote.analysis.AnalysisCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 분석 완료가 커밋된 뒤 분석 챌린지 달성도를 별도 트랜잭션에서 다시 센다 (F-25 AC7, #105). 집계가 실패해도 분석은 DONE으로 남고 로그만 남긴다. 놓친 집계는
 * 같은 챌린지 기간 안의 다음 분석 완료(또는 재참여) 때 바로잡히고, 기간이 끝나면 남는다 (#146).
 *
 * <p>READ_COMMITTED라 참여 행 잠금을 기다리는 동안 커밋된 다른 분석까지 센다 (#104).
 */
@Component
class AnalysisChallengeListener {

    private static final Logger log = LoggerFactory.getLogger(AnalysisChallengeListener.class);

    private final ChallengeProgressService progressService;
    private final TransactionTemplate newTransaction;

    AnalysisChallengeListener(
            ChallengeProgressService progressService, PlatformTransactionManager txManager) {
        this.progressService = progressService;
        this.newTransaction = new TransactionTemplate(txManager);
        newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        newTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    /**
     * Spring도 AFTER_COMMIT 리스너의 예외를 afterCompletion에서 ERROR 로그로 삼키지만, 분석 id를 담은 warn으로 남기고 Spring
     * 내부 동작에 기대지 않으려고 직접 잡는다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void refresh(AnalysisCompletedEvent event) {
        try {
            newTransaction.executeWithoutResult(
                    status ->
                            progressService.refresh(
                                    event.userId(),
                                    ChallengeGoalType.ANALYSIS_COUNT,
                                    event.requestDate()));
        } catch (RuntimeException e) {
            log.warn("분석 {} 완료 뒤 챌린지 집계 실패 (같은 기간의 다음 분석 완료 때 다시 센다)", event.analysisId(), e);
        }
    }
}
