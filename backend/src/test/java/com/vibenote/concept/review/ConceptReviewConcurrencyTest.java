package com.vibenote.concept.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.concept.review.dto.ConceptReviewRequest;
import com.vibenote.concept.review.dto.ConceptReviewSaveResponse;
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
 * F-11 AC3: 같은 카드에 동시에 리뷰를 쓰면, 카드 행 잠금을 기다린 쪽의 응답 {@code reviewCount}·{@code avgRating}이 먼저 커밋된 리뷰를
 * 포함한다. 잠금보다 먼저 일반 SELECT를 하면 REPEATABLE READ 스냅샷이 잠금 전에 잡혀 앞 트랜잭션의 커밋을 놓친다. 테스트 트랜잭션 없이 실제로 커밋하고
 * 끝나면 지운다.
 */
class ConceptReviewConcurrencyTest extends IntegrationTestBase {

    @Autowired ConceptReviewService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM concepts WHERE category = 'f11-conc'"); // 리뷰는 CASCADE
        jdbc.update("DELETE FROM users WHERE email LIKE '%@f11conc.test'"); // 활동은 CASCADE
    }

    private long user(String name) {
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                name + "@f11conc.test",
                name);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long concept() {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source)"
                        + " VALUES (?, 'f11-conc', 'JUNIOR', '요약', 'PUBLISHED', 'AI')",
                "f11-conc 카드 " + System.nanoTime());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    void AC3_잠금을_기다린_작성은_먼저_커밋된_리뷰를_포함해_평점과_리뷰_수를_돌려준다() throws Exception {
        long u1 = user("f11conc1");
        long u2 = user("f11conc2");
        long c = concept();
        TransactionTemplate tx = new TransactionTemplate(txManager);
        CountDownLatch t1Locked = new CountDownLatch(1);
        CountDownLatch t2Started = new CountDownLatch(1);

        // T1: 카드 행을 잠그고 리뷰(4점)를 넣은 채, T2가 시작해 잠금을 기다릴 때까지 커밋을 미룬다
        CompletableFuture<Void> t1 =
                CompletableFuture.runAsync(
                        () ->
                                tx.executeWithoutResult(
                                        s -> {
                                            jdbc.queryForObject(
                                                    "SELECT id FROM concepts WHERE id = ? FOR"
                                                            + " UPDATE",
                                                    Long.class,
                                                    c);
                                            jdbc.update(
                                                    "INSERT INTO concept_reviews (concept_id,"
                                                        + " user_id, rating, content) VALUES (?, ?,"
                                                        + " 4, '먼저')",
                                                    c,
                                                    u1);
                                            t1Locked.countDown();
                                            await(t2Started);
                                            sleep(500); // T2가 잠금을 기다리기 시작할 시간
                                        }));
        // T2: 서비스로 리뷰(2점)를 쓴다
        CompletableFuture<ConceptReviewSaveResponse> t2 =
                CompletableFuture.supplyAsync(
                        () -> {
                            await(t1Locked);
                            t2Started.countDown();
                            return service.create(c, u2, new ConceptReviewRequest(2, "나중"));
                        });

        t1.get(30, TimeUnit.SECONDS);
        ConceptReviewSaveResponse saved = t2.get(30, TimeUnit.SECONDS);
        assertThat(saved.reviewCount()).isEqualTo(2);
        assertThat(saved.avgRating()).isEqualTo(3.0);
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
