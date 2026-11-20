package com.vibenote.concept.wish;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.concept.wish.dto.ConceptWishResponse;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * F-12 AC2·AC6 동시 찜. 카드 행 잠금으로 같은 카드의 찜이 직렬화되어 찜 수가 실제 행 수와 같고, 같은 회원이 동시에 찜해도 둘 다 성공한다. 테스트 트랜잭션
 * 없이 실제로 커밋하고 끝나면 지운다.
 */
class ConceptWishConcurrencyTest extends IntegrationTestBase {

    @Autowired ConceptWishService service;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM concepts WHERE category = 'f12-conc'"); // 찜·복습 계획은 CASCADE
        jdbc.update("DELETE FROM users WHERE email LIKE '%@f12conc.test'"); // 활동은 CASCADE
    }

    private long user(String name) {
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                name + "@f12conc.test",
                name);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long concept() {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source)"
                        + " VALUES (?, 'f12-conc', 'JUNIOR', '요약', 'PUBLISHED', 'AI')",
                "f12-conc 카드 " + System.nanoTime());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private List<ConceptWishResponse> runTogether(List<Callable<ConceptWishResponse>> tasks)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<ConceptWishResponse>> futures =
                    tasks.stream()
                            .map(
                                    task ->
                                            pool.submit(
                                                    () -> {
                                                        start.await(10, TimeUnit.SECONDS);
                                                        return task.call();
                                                    }))
                            .toList();
            start.countDown();
            return futures.stream()
                    .map(
                            f -> {
                                try {
                                    return f.get(30, TimeUnit.SECONDS);
                                } catch (Exception e) {
                                    throw new IllegalStateException(e);
                                }
                            })
                    .toList();
        } finally {
            pool.shutdownNow();
        }
    }

    private int wishCount(long conceptId) {
        return jdbc.queryForObject(
                "SELECT wish_count FROM concepts WHERE id = ?", Integer.class, conceptId);
    }

    private int rows(long conceptId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM concept_wishes WHERE concept_id = ?",
                Integer.class,
                conceptId);
    }

    @Test
    void AC2_같은_회원이_동시에_찜해도_둘_다_성공하고_찜은_하나다() throws Exception {
        long u = user("f12conc-same");
        long c = concept();

        List<ConceptWishResponse> results =
                runTogether(List.of(() -> service.wish(c, u), () -> service.wish(c, u)));

        assertThat(results).allSatisfy(r -> assertThat(r.wished()).isTrue());
        assertThat(results).extracting(ConceptWishResponse::wishCount).containsOnly(1);
        assertThat(rows(c)).isEqualTo(1);
        assertThat(wishCount(c)).isEqualTo(1);
        int activities =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM activities WHERE user_id = ? AND type = 'WISH'",
                        Integer.class,
                        u);
        assertThat(activities).isEqualTo(1);
    }

    @Test
    void AC6_서로_다른_회원이_동시에_찜해도_찜_수가_행_수와_같다() throws Exception {
        long u1 = user("f12conc-a");
        long u2 = user("f12conc-b");
        long u3 = user("f12conc-c");
        long c = concept();

        List<ConceptWishResponse> results =
                runTogether(
                        List.of(
                                () -> service.wish(c, u1),
                                () -> service.wish(c, u2),
                                () -> service.wish(c, u3)));

        assertThat(results)
                .extracting(ConceptWishResponse::wishCount)
                .containsExactlyInAnyOrder(1, 2, 3);
        assertThat(rows(c)).isEqualTo(3);
        assertThat(wishCount(c)).isEqualTo(3);
    }
}
