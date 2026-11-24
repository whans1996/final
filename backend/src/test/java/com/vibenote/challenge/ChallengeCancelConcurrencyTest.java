package com.vibenote.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * F-25 AC9: 취소가 달성 여부를 읽은 뒤 같은 참여 행을 집계({@code ChallengeProgressService.refresh})가 달성시키고 커밋해도, 달성한
 * 참여는 지워지지 않고 409 {@code CHALLENGE_003}이다. 집계는 참여 행만 잠그므로 취소도 참여 행을 잠그고 읽어야 한다 (PR 리뷰 지적). 테스트 트랜잭션
 * 없이 실제로 커밋하고 끝나면 지운다.
 */
class ChallengeCancelConcurrencyTest extends ChallengeTestSupport {

    @Autowired PlatformTransactionManager txManager;

    @Test
    void AC9_집계가_참여_행을_잠그고_달성시키는_중에_취소하면_달성을_기다려_409이고_행은_남는다() throws Exception {
        long u = insertUser("f25u");
        long c = insertChallenge("C", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 2);
        insertParticipant(c, u, 1, null, "2026-11-21 10:00:00");
        TransactionTemplate tx = new TransactionTemplate(txManager);
        CountDownLatch t1Locked = new CountDownLatch(1);
        CountDownLatch t2Started = new CountDownLatch(1);

        // T1: 집계처럼 참여 행만 잠그고 달성시킨 채, T2가 시작해 잠금을 기다릴 때까지 커밋을 미룬다
        CompletableFuture<Void> t1 =
                CompletableFuture.runAsync(
                        () ->
                                tx.executeWithoutResult(
                                        s -> {
                                            jdbc.queryForList(
                                                    "SELECT progress FROM challenge_participants"
                                                            + " WHERE challenge_id = ? AND user_id"
                                                            + " = ? FOR UPDATE",
                                                    c,
                                                    u);
                                            jdbc.update(
                                                    "UPDATE challenge_participants SET progress ="
                                                        + " 2, completed_at = '2026-11-24 09:59:00'"
                                                        + " WHERE challenge_id = ? AND user_id = ?",
                                                    c,
                                                    u);
                                            t1Locked.countDown();
                                            await(t2Started);
                                            sleep(500); // T2가 잠금을 기다리기 시작할 시간
                                        }));
        // T2: 취소를 요청한다
        CompletableFuture<Integer> t2 =
                CompletableFuture.supplyAsync(
                        () -> {
                            await(t1Locked);
                            t2Started.countDown();
                            try {
                                return mockMvc.perform(
                                                delete("/api/challenges/{id}/participation", c)
                                                        .header(
                                                                HttpHeaders.AUTHORIZATION,
                                                                bearer(u)))
                                        .andExpect(jsonPath("$.code").value("CHALLENGE_003"))
                                        .andExpect(status().isConflict())
                                        .andReturn()
                                        .getResponse()
                                        .getStatus();
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }
                        });

        t1.get(30, TimeUnit.SECONDS);
        assertThat(t2.get(30, TimeUnit.SECONDS)).isEqualTo(409);
        assertThat(count("challenge_participants", "challenge_id = ? AND user_id = ?", c, u))
                .isEqualTo(1);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("래치 대기 시간 초과");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
