package com.vibenote.challenge;

import com.vibenote.challenge.domain.ChallengeParticipant;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 끝난 챌린지의 미달성 참여를 하루 한 번 다시 센다 (#146). 분석 완료 집계는 커밋 뒤 별도 트랜잭션이라 실패하면 분석만 DONE으로 남고({@link
 * AnalysisChallengeListener}), 기간 안에 다음 분석이 없으면 progress가 모자란 채 남는다. 이 작업이 그 몫을 바로잡는다.
 *
 * <p>종료일이 최근 {@value #LOOKBACK_DAYS}일 안인 챌린지를 본다. 서버가 며칠 꺼져 있어도 놓치지 않게 하고, 다시 세기는 같은 결과를 내므로 여러 번
 * 세어도 된다. 늦게 달성한 참여의 completed_at은 이 작업이 센 시각이다.
 *
 * <p>참여마다 READ_COMMITTED 트랜잭션 하나로 센다 (#104). 하나가 실패해도 나머지는 센다.
 */
@Component
class ChallengeRecountJob {

    static final int LOOKBACK_DAYS = 7;

    private static final Logger log = LoggerFactory.getLogger(ChallengeRecountJob.class);

    private final ChallengeParticipantMapper participantMapper;
    private final ChallengeProgressService progressService;
    private final TransactionTemplate newTransaction;
    private final Clock clock;

    ChallengeRecountJob(
            ChallengeParticipantMapper participantMapper,
            ChallengeProgressService progressService,
            PlatformTransactionManager txManager,
            Clock clock) {
        this.participantMapper = participantMapper;
        this.progressService = progressService;
        this.newTransaction = new TransactionTemplate(txManager);
        newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        newTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.clock = clock;
    }

    /** 기본 매일 00:10(Asia/Seoul). {@code challenge.recount-cron}이 {@code -}이면 돌지 않는다 (통합 테스트). */
    @Scheduled(cron = "${challenge.recount-cron:0 10 0 * * *}", zone = "Asia/Seoul")
    void recountEnded() {
        LocalDate today = LocalDate.now(clock);
        for (ChallengeParticipant p :
                participantMapper.findOpenEnded(today.minusDays(LOOKBACK_DAYS), today)) {
            try {
                newTransaction.executeWithoutResult(
                        status -> progressService.recountOpen(p.getChallengeId(), p.getUserId()));
            } catch (RuntimeException e) {
                log.warn(
                        "끝난 챌린지 {} 회원 {} 다시 세기 실패 (종료 7일 안이면 다음 날 다시 센다)",
                        p.getChallengeId(),
                        p.getUserId(),
                        e);
            }
        }
    }
}
