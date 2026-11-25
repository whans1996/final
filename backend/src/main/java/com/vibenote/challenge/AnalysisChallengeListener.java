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
 * 그 회원의 다음 분석 완료 때 다시 센다.
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

    /** 커밋 뒤 콜백의 예외는 완료를 부른 쪽(파이프라인)으로 올라가 분석을 FAILED로 만들므로 여기서 삼킨다. */
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
            log.warn("분석 {} 완료 뒤 챌린지 집계 실패 (다음 분석 완료 때 다시 센다)", event.analysisId(), e);
        }
    }
}
