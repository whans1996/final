package com.vibenote.reviewplan;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.user.UserService;
import com.vibenote.user.dto.SignupRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * F-14 AC6·AC7: 다른 회원 둘이 동시에 제출해 오답 복습 계획을 같은 인덱스 gap에 넣어도 데드락으로 한쪽 풀이가 롤백되지 않는다. 두 트랜잭션의 문장 순서를
 * 래치로 고정한다 (T1 첫 행 → T2 한 행 → T1 두 번째 행).
 */
class ReviewPlanConcurrencyTest extends IntegrationTestBase {

    private static final LocalDate TOMORROW = LocalDate.of(2026, 11, 20);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 19, 10, 0);

    @Autowired ReviewPlanMapper mapper;
    @Autowired UserService userService;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM users WHERE email LIKE '%@f14rpc.test'");
        jdbc.update("DELETE FROM concepts WHERE name LIKE 'f14rpc-test-%'");
    }

    private long user(String name) {
        return userService.signup(
                new SignupRequest(name + "@f14rpc.test", "vibe1234", name, "JUNIOR"));
    }

    private long concept(String name) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source)"
                        + " VALUES (?, 'spring', 'JUNIOR', '요약', 'PUBLISHED', 'SEED')",
                name);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    void AC7_다른_회원이_동시에_오답_계획을_넣어도_둘_다_커밋된다() throws Exception {
        long u1 = user("f14rpc1");
        long u2 = user("f14rpc2");
        long c1 = concept("f14rpc-test-1");
        long c2 = concept("f14rpc-test-2");
        TransactionTemplate tx = new TransactionTemplate(txManager);
        CountDownLatch t1First = new CountDownLatch(1);
        CountDownLatch t2Started = new CountDownLatch(1);

        CompletableFuture<Integer> t1 =
                CompletableFuture.supplyAsync(
                        () ->
                                tx.execute(
                                        s -> {
                                            int n =
                                                    mapper.insertIgnoringDuplicate(
                                                            u1, c1, TOMORROW, "WRONG", NOW);
                                            t1First.countDown();
                                            await(t2Started);
                                            sleep(500); // T2의 INSERT가 락을 기다리기 시작할 시간
                                            return n
                                                    + mapper.insertIgnoringDuplicate(
                                                            u1, c2, TOMORROW, "WRONG", NOW);
                                        }));
        CompletableFuture<Integer> t2 =
                CompletableFuture.supplyAsync(
                        () ->
                                tx.execute(
                                        s -> {
                                            await(t1First);
                                            t2Started.countDown();
                                            return mapper.insertIgnoringDuplicate(
                                                    u2, c1, TOMORROW, "WRONG", NOW);
                                        }));

        assertThat(t1.get(30, TimeUnit.SECONDS)).isEqualTo(2);
        assertThat(t2.get(30, TimeUnit.SECONDS)).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM review_plans WHERE user_id IN (?, ?)",
                                Integer.class,
                                u1,
                                u2))
                .isEqualTo(3);
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
