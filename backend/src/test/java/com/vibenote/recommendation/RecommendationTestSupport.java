package com.vibenote.recommendation;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtTokenProvider;
import com.vibenote.user.UserService;
import com.vibenote.user.dto.SignupRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * F-16 통합 테스트 공통. 시각은 2026-11-20 10:00(KST)으로 고정한다 ("오늘" = 11-20). 후보를 통제하려고 테스트 동안 시드 등 다른 공개 카드를
 * HIDDEN으로 숨겼다가 되돌린다. {@code f16-test-} 카드를 직접 넣고, {@code @f16.test} 회원을 지우면 분석·퀴즈·풀이·찜·복습 계획·추천
 * 이유가 CASCADE로 함께 지워진다.
 */
abstract class RecommendationTestSupport extends IntegrationTestBase {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 11, 20, 10, 0, 0);

    @TestBean Clock clock;

    static Clock clock() {
        return Clock.fixed(FIXED_NOW.atZone(KST).toInstant(), KST);
    }

    @Autowired UserService userService;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired JdbcTemplate jdbc;

    private List<Long> hiddenForTest = List.of();

    @BeforeEach
    void hideOtherPublishedConcepts() {
        hiddenForTest =
                jdbc.queryForList(
                        "SELECT id FROM concepts WHERE status = 'PUBLISHED'"
                                + " AND name NOT LIKE 'f16-test-%'",
                        Long.class);
        jdbc.update(
                "UPDATE concepts SET status = 'HIDDEN' WHERE status = 'PUBLISHED'"
                        + " AND name NOT LIKE 'f16-test-%'");
    }

    @AfterEach
    void cleanUpF16() {
        jdbc.update("DELETE FROM users WHERE email LIKE '%@f16.test'");
        jdbc.update("DELETE FROM concepts WHERE name LIKE 'f16-test-%'");
        for (Long id : hiddenForTest) {
            jdbc.update("UPDATE concepts SET status = 'PUBLISHED' WHERE id = ?", id);
        }
    }

    long createUser(String name, String level) {
        return userService.signup(new SignupRequest(name + "@f16.test", "vibe1234", name, level));
    }

    String bearer(long userId) {
        return "Bearer " + tokenProvider.createAccessToken(userId, "USER");
    }

    /** 이름 앞에 {@code f16-test-}를 붙인다. */
    long insertConcept(
            String name,
            String category,
            String difficulty,
            String status,
            int wishCount,
            double avgRating) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source,"
                        + " wish_count, avg_rating) VALUES (?, ?, ?, ?, ?, 'SEED', ?, ?)",
                "f16-test-" + name,
                category,
                difficulty,
                name + " 요약",
                status,
                wishCount,
                avgRating);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    long insertConcept(String name, String category, String difficulty) {
        return insertConcept(name, category, difficulty, "PUBLISHED", 0, 0.0);
    }

    long insertAnalysis(long userId, boolean deleted) {
        jdbc.update(
                "INSERT INTO analyses (user_id, source_type, project_name, title, level, status,"
                        + " progress, created_at, deleted_at) VALUES (?, 'ZIP', 'p', 'p', 'JUNIOR',"
                        + " 'DONE', 100, ?, IF(?, NOW(), NULL))",
                userId,
                FIXED_NOW.minusDays(1),
                deleted);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    void linkConcept(long analysisId, long conceptId) {
        jdbc.update(
                "INSERT INTO analysis_concepts (analysis_id, concept_id) VALUES (?, ?)",
                analysisId,
                conceptId);
    }

    long insertQuiz(long analysisId) {
        jdbc.update("INSERT INTO quizzes (analysis_id) VALUES (?)", analysisId);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    long insertQuestion(long quizId, Long conceptId) {
        jdbc.update(
                "INSERT INTO quiz_questions (quiz_id, concept_id, question, choices,"
                        + " answer_index, explanation) VALUES (?, ?, '질문', '[\"a\", \"b\", \"c\","
                        + " \"d\"]', 0, '해설')",
                quizId,
                conceptId);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /** 한 번의 풀이. {@code wrongQuestionIds}는 틀리고 {@code rightQuestionIds}는 맞힌다. */
    void insertAttempt(long quizId, long userId, List<Long> wrongQuestionIds, List<Long> rightIds) {
        jdbc.update(
                "INSERT INTO quiz_attempts (quiz_id, user_id, score, total) VALUES (?, ?, ?, ?)",
                quizId,
                userId,
                rightIds.size(),
                wrongQuestionIds.size() + rightIds.size());
        long attemptId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        for (Long q : wrongQuestionIds) {
            jdbc.update(
                    "INSERT INTO quiz_answers (attempt_id, question_id, selected_index, is_correct)"
                            + " VALUES (?, ?, 1, FALSE)",
                    attemptId,
                    q);
        }
        for (Long q : rightIds) {
            jdbc.update(
                    "INSERT INTO quiz_answers (attempt_id, question_id, selected_index, is_correct)"
                            + " VALUES (?, ?, 0, TRUE)",
                    attemptId,
                    q);
        }
    }

    void insertWish(long userId, long conceptId) {
        jdbc.update(
                "INSERT INTO concept_wishes (user_id, concept_id) VALUES (?, ?)",
                userId,
                conceptId);
    }

    void insertPlan(long userId, long conceptId, String planDate, String status) {
        jdbc.update(
                "INSERT INTO review_plans (user_id, concept_id, plan_date, status, reason, done_at)"
                        + " VALUES (?, ?, ?, ?, 'WRONG', IF(? = 'DONE', NOW(), NULL))",
                userId,
                conceptId,
                planDate,
                status,
                status);
    }

    int count(String table, String where, Object... args) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + where, Integer.class, args);
    }
}
