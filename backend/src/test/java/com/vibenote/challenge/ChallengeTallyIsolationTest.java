package com.vibenote.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.analysis.AnalysisService;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * F-25 집계의 격리·결합 (#104, #105). 테스트 트랜잭션 없이 실제로 커밋하고 끝나면 지운다.
 *
 * <ul>
 *   <li>#105: 분석 완료의 집계가 실패해도 분석은 DONE으로 남는다. 집계는 완료가 커밋된 뒤 별도 트랜잭션에서 돈다.
 *   <li>#104: 집계가 참여 행 잠금을 기다리는 동안 같은 회원의 다른 활동이 커밋되면, 잠금을 얻은 뒤 그 활동까지 센다. 바깥 트랜잭션의 REPEATABLE READ
 *       스냅샷이 잠금 전에 잡히면 1건이 빠진다.
 * </ul>
 */
class ChallengeTallyIsolationTest extends ChallengeTestSupport {

    @Autowired PlatformTransactionManager txManager;
    @Autowired AnalysisService analysisService;

    @Test
    void 분석_완료의_집계가_실패해도_분석은_DONE이고_활동이_남는다() {
        long u = insertUser("f25u");
        long c = insertChallenge("C", "2026-11-20", "2026-11-30", "ANALYSIS_COUNT", 5);
        insertParticipant(c, u);
        long a = insertAnalysis(u, "QUIZ", "2026-11-24 09:00:00", false);
        doThrow(new QueryTimeoutException("잠금 대기 시간 초과"))
                .when(progressService)
                .refresh(anyLong(), eq(ChallengeGoalType.ANALYSIS_COUNT), any());

        assertThatCode(() -> analysisService.complete(a)).doesNotThrowAnyException();

        verify(progressService)
                .refresh(u, ChallengeGoalType.ANALYSIS_COUNT, LocalDate.of(2026, 11, 24));

        assertThat(count("analyses", "id = ? AND status = 'DONE'", a)).isEqualTo(1);
        assertThat(count("activities", "user_id = ? AND type = 'ANALYSIS'", u)).isEqualTo(1);
        assertThat(progressOf(c, u)).isZero();
    }

    @Test
    void 분석_완료_집계는_잠금을_기다리는_동안_커밋된_다른_분석까지_센다() throws Exception {
        long u = insertUser("f25u");
        long c = insertChallenge("C", "2026-11-20", "2026-11-30", "ANALYSIS_COUNT", 5);
        insertParticipant(c, u);
        long other = insertAnalysis(u, "QUIZ", "2026-11-24 08:00:00", false);
        long mine = insertAnalysis(u, "QUIZ", "2026-11-24 09:00:00", false);

        // T1: 다른 분석의 완료처럼 참여 행을 잠그고 그 분석을 DONE으로 바꾼 채 커밋을 미룬다
        racing(
                () ->
                        jdbc.update(
                                "UPDATE analyses SET status = 'DONE', progress = 100 WHERE id = ?",
                                other),
                c,
                u,
                () -> analysisService.complete(mine));

        assertThat(progressOf(c, u)).isEqualTo(2);
    }

    @Test
    void 퀴즈_제출_집계는_잠금을_기다리는_동안_커밋된_다른_풀이까지_센다() throws Exception {
        long u = insertUser("f25u");
        long c = insertChallenge("C", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 5);
        insertParticipant(c, u);
        long otherQuiz = insertQuiz(insertAnalysis(u, "DONE", "2026-11-21 10:00:00", false));
        long myQuiz = insertQuiz(insertAnalysis(u, "DONE", "2026-11-22 10:00:00", false));

        // T1: 다른 탭의 퀴즈 제출처럼 참여 행을 잠그고 오늘 풀이를 넣은 채 커밋을 미룬다
        racing(
                () -> insertAttempt(otherQuiz, u, "2026-11-24 09:00:00"),
                c,
                u,
                () ->
                        perform(
                                post("/api/quizzes/{id}/attempts", myQuiz)
                                        .header(HttpHeaders.AUTHORIZATION, bearer(u))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"answers\": [{\"questionId\": %d,"
                                                                .formatted(questionOf(myQuiz))
                                                        + " \"selectedIndex\": 0}]}"),
                                201));

        assertThat(progressOf(c, u)).isEqualTo(2);
    }

    @Test
    void 복습_완료_집계는_잠금을_기다리는_동안_커밋된_다른_복습까지_센다() throws Exception {
        long u = insertUser("f25u");
        long c = insertChallenge("C", "2026-11-20", "2026-11-30", "REVIEW_COUNT", 5);
        insertParticipant(c, u);
        long otherPlan = insertPlan(u, insertConcept("f25-test-other"), "2026-11-24", "TODO", null);
        long myPlan = insertPlan(u, insertConcept("f25-test-mine"), "2026-11-24", "TODO", null);

        // T1: 다른 탭의 복습 완료처럼 참여 행을 잠그고 다른 계획을 DONE으로 바꾼 채 커밋을 미룬다
        racing(
                () ->
                        jdbc.update(
                                "UPDATE review_plans SET status = 'DONE', done_at = '2026-11-24"
                                        + " 09:00:00' WHERE id = ?",
                                otherPlan),
                c,
                u,
                () ->
                        perform(
                                patch("/api/review-plans/{id}", myPlan)
                                        .header(HttpHeaders.AUTHORIZATION, bearer(u))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"status\": \"DONE\"}"),
                                200));

        assertThat(progressOf(c, u)).isEqualTo(2);
    }

    /**
     * T1이 참여 행을 잠그고 {@code otherActivity}를 넣은 채, T2({@code mine})가 잠금을 기다리는 것이 DB에 보일 때까지 커밋을 미룬다.
     * T2의 읽기는 모두 T1 커밋 전에 일어난다. 고정 대기(500ms)로는 컨텍스트의 첫 요청이 잠금에 닿기 전에 T1이 커밋해 red가 보이지 않았다.
     */
    private void racing(Runnable otherActivity, long challengeId, long userId, Runnable mine)
            throws Exception {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        CountDownLatch t1Locked = new CountDownLatch(1);
        CountDownLatch t2Started = new CountDownLatch(1);
        CompletableFuture<Void> t1 =
                CompletableFuture.runAsync(
                        () ->
                                tx.executeWithoutResult(
                                        s -> {
                                            jdbc.queryForList(
                                                    "SELECT progress FROM challenge_participants"
                                                            + " WHERE challenge_id = ? AND user_id"
                                                            + " = ? FOR UPDATE",
                                                    challengeId,
                                                    userId);
                                            otherActivity.run();
                                            t1Locked.countDown();
                                            await(t2Started);
                                            awaitLockWait();
                                        }));
        CompletableFuture<Void> t2 =
                CompletableFuture.runAsync(
                        () -> {
                            await(t1Locked);
                            t2Started.countDown();
                            mine.run();
                        });

        try {
            t1.get(30, TimeUnit.SECONDS);
        } finally {
            // T1이 실패해도 T2가 끝난 뒤에 정리(@AfterEach)가 돌게 한다
            t2.get(30, TimeUnit.SECONDS);
        }
    }

    /**
     * 다른 트랜잭션이 행 잠금을 기다리기 시작할 때까지 기다린다 (최대 10초). 앱 계정은 performance_schema를 읽을 수 없어 테스트 컨테이너의 root로
     * 본다 (Testcontainers는 root 비밀번호를 앱 계정과 같게 둔다).
     */
    private void awaitLockWait() {
        JdbcTemplate root =
                new JdbcTemplate(
                        new DriverManagerDataSource(
                                MYSQL.getJdbcUrl(), "root", MYSQL.getPassword()));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (root.queryForObject(
                        "SELECT COUNT(*) FROM performance_schema.data_lock_waits", Integer.class)
                == 0) {
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("T2가 잠금을 기다리지 않음");
            }
            sleep(50);
        }
    }

    private void perform(
            org.springframework.test.web.servlet.RequestBuilder request, int expectedStatus) {
        try {
            mockMvc.perform(request).andExpect(status().is(expectedStatus));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
