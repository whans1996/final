package com.vibenote.reviewplan;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.user.dto.SignupRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** F-14 AC7 오답 개념, F-15 AC8 찜한 카드를 복습 계획에 넣는 규칙. 오늘 = 2026-11-20 (ReviewPlanTestSupport). */
class ReviewPlanServiceTest extends ReviewPlanTestSupport {

    private static final LocalDate TOMORROW = LocalDate.of(2026, 11, 20);

    @Autowired ReviewPlanService service;
    @Autowired PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM users WHERE email LIKE '%@f14rp.test'");
        jdbc.update("DELETE FROM concepts WHERE name LIKE 'f14rp-test-%'");
    }

    private long user(String name) {
        return userService.signup(
                new SignupRequest(name + "@f14rp.test", "vibe1234", name, "JUNIOR"));
    }

    private long concept(String name) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source)"
                        + " VALUES (?, 'spring', 'JUNIOR', '요약', 'PUBLISHED', 'SEED')",
                name);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void plan(long userId, long conceptId, String date, String status) {
        jdbc.update(
                "INSERT INTO review_plans (user_id, concept_id, plan_date, status, reason)"
                        + " VALUES (?, ?, ?, ?, 'MANUAL')",
                userId,
                conceptId,
                date,
                status);
    }

    @Test
    void AC7_TODO가_날짜와_상관없이_있거나_같은_날_DONE이_있으면_건너뛰고_새로_넣은_것만_돌려준다() {
        long userId = user("f14rp1");
        long fresh = concept("f14rp-test-새");
        long todoLater = concept("f14rp-test-나중 TODO");
        long todoPast = concept("f14rp-test-지난 TODO");
        long doneSameDay = concept("f14rp-test-같은 날 DONE");
        long doneOtherDay = concept("f14rp-test-다른 날 DONE");
        plan(userId, todoLater, "2026-12-01", "TODO");
        plan(userId, todoPast, "2026-11-01", "TODO");
        plan(userId, doneSameDay, "2026-11-20", "DONE");
        plan(userId, doneOtherDay, "2026-11-18", "DONE");

        List<ReviewPlanService.Added> added =
                service.addWrongAnswers(
                        userId,
                        List.of(fresh, todoLater, todoPast, doneSameDay, doneOtherDay),
                        TOMORROW);

        assertThat(added)
                .containsExactly(
                        new ReviewPlanService.Added(fresh, TOMORROW),
                        new ReviewPlanService.Added(doneOtherDay, TOMORROW));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM review_plans WHERE user_id = ? AND reason ="
                                    + " 'WRONG' AND status = 'TODO' AND plan_date = '2026-11-20'",
                                Integer.class,
                                userId))
                .isEqualTo(2);
    }

    @Test
    void AC7_다른_회원의_계획은_보지_않는다() {
        long me = user("f14rp2");
        long other = user("f14rp3");
        long c = concept("f14rp-test-공유");
        plan(other, c, "2026-11-20", "TODO");

        assertThat(service.addWrongAnswers(me, List.of(c), TOMORROW))
                .containsExactly(new ReviewPlanService.Added(c, TOMORROW));
    }

    // ── F-15 AC8 찜 자동 추가 ──

    private static final LocalDate F15_TOMORROW = LocalDate.of(2026, 11, 21);

    @Test
    void AC8_찜한_카드를_내일_WISH_TODO로_넣는다() {
        long me = createUser("f15ac8");
        long a = insertConcept("f15-test-A", "PUBLISHED");

        Optional<ReviewPlanService.Added> added = service.addWish(me, a);

        assertThat(added).contains(new ReviewPlanService.Added(a, F15_TOMORROW));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM review_plans WHERE user_id = ? AND concept_id"
                                    + " = ? AND plan_date = '2026-11-21' AND status = 'TODO' AND"
                                    + " reason = 'WISH'",
                                Integer.class,
                                me,
                                a))
                .isEqualTo(1);
    }

    @Test
    void AC8_TODO가_날짜와_상관없이_있거나_내일_계획이_있으면_넣지_않는다() {
        long me = createUser("f15ac8s");
        long todoLater = insertConcept("f15-test-나중 TODO", "PUBLISHED");
        long todoPast = insertConcept("f15-test-지난 TODO", "PUBLISHED");
        long doneTomorrow = insertConcept("f15-test-내일 DONE", "PUBLISHED");
        insertTodo(me, todoLater, "2026-12-01");
        insertTodo(me, todoPast, "2026-11-01");
        insertPlan(me, doneTomorrow, "2026-11-21", "DONE", "MANUAL", "2026-11-20 09:00:00");

        assertThat(service.addWish(me, todoLater)).isEmpty();
        assertThat(service.addWish(me, todoPast)).isEmpty();
        assertThat(service.addWish(me, doneTomorrow)).isEmpty();
        assertThat(countPlans(me)).isEqualTo(3);
    }

    @Test
    void AC8_호출자가_롤백하면_함께_롤백된다() {
        long me = createUser("f15ac8r");
        long a = insertConcept("f15-test-A", "PUBLISHED");

        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status -> {
                            assertThat(service.addWish(me, a)).isPresent();
                            status.setRollbackOnly();
                        });

        assertThat(countPlans(me)).isZero();
    }
}
