package com.vibenote.reviewplan;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtTokenProvider;
import com.vibenote.user.UserService;
import com.vibenote.user.dto.SignupRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * F-15 통합 테스트 공통. 시각은 2026-11-20 10:00(KST, 금요일)으로 고정한다 ("오늘" = 11-20, "내일" = 11-21). {@code
 * f15-test-} 카드를 직접 넣고, {@code @f15.test} 회원을 지우면 복습 계획이 CASCADE로 함께 지워진다.
 */
abstract class ReviewPlanTestSupport extends IntegrationTestBase {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 11, 20, 10, 0, 0);

    @TestBean Clock clock;

    static Clock clock() {
        return Clock.fixed(FIXED_NOW.atZone(KST).toInstant(), KST);
    }

    @Autowired UserService userService;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanUpF15() {
        jdbc.update("DELETE FROM users WHERE email LIKE '%@f15.test'");
        jdbc.update("DELETE FROM concepts WHERE name LIKE 'f15-test-%'");
    }

    long createUser(String name) {
        return userService.signup(
                new SignupRequest(name + "@f15.test", "vibe1234", name, "JUNIOR"));
    }

    String bearer(long userId) {
        return "Bearer " + tokenProvider.createAccessToken(userId, "USER");
    }

    /** 이름은 {@code f15-test-}로 시작해야 정리된다. */
    long insertConcept(String name, String status) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source,"
                        + " wish_count, avg_rating) VALUES (?, 'security', 'JUNIOR', '요약', ?,"
                        + " 'SEED', 2, 4.5)",
                name,
                status);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /** {@code doneAt}은 {@code yyyy-MM-dd HH:mm:ss} 또는 null. */
    long insertPlan(
            long userId,
            long conceptId,
            String planDate,
            String status,
            String reason,
            String doneAt) {
        jdbc.update(
                "INSERT INTO review_plans (user_id, concept_id, plan_date, status, reason,"
                        + " done_at) VALUES (?, ?, ?, ?, ?, ?)",
                userId,
                conceptId,
                planDate,
                status,
                reason,
                doneAt);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    long insertTodo(long userId, long conceptId, String planDate) {
        return insertPlan(userId, conceptId, planDate, "TODO", "WRONG", null);
    }

    int countPlans(long userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM review_plans WHERE user_id = ?", Integer.class, userId);
    }
}
