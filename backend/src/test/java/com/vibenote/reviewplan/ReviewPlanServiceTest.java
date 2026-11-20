package com.vibenote.reviewplan;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.user.UserService;
import com.vibenote.user.dto.SignupRequest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** F-14 AC7 오답 개념을 복습 계획에 넣는 규칙. */
class ReviewPlanServiceTest extends IntegrationTestBase {

    private static final LocalDate TOMORROW = LocalDate.of(2026, 11, 20);

    @Autowired ReviewPlanService service;
    @Autowired UserService userService;
    @Autowired JdbcTemplate jdbc;

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
}
