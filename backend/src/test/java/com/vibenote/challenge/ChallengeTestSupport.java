package com.vibenote.challenge;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtTokenProvider;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * F-25 통합 테스트 공통 (docs/specs/F-25-challenge-participation.md). 오늘은 2026-11-24 10:00(KST)이다.
 * 회원·챌린지·참여·활동 행을 테스트마다 직접 넣고, 챌린지 전체와 {@code @f25.test} 회원({@code f25-test-} 카드)을 지워 정리한다. 회원을 지우면
 * 분석·퀴즈·풀이·복습 계획·활동이 CASCADE로 함께 지워진다.
 */
abstract class ChallengeTestSupport extends IntegrationTestBase {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 11, 24, 10, 0, 0);
    static final String NOW_TEXT = "2026-11-24T10:00:00+09:00";

    @TestBean Clock clock;

    static Clock clock() {
        return Clock.fixed(FIXED_NOW.atZone(KST).toInstant(), KST);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenProvider tokenProvider;

    long adminId;

    @BeforeEach
    void setUpF25() {
        cleanUpF25();
        adminId = insertUser("f25admin");
    }

    @AfterEach
    void cleanUpF25() {
        jdbc.update("DELETE FROM challenges");
        jdbc.update("DELETE FROM users WHERE email LIKE '%@f25.test'");
        jdbc.update("DELETE FROM concepts WHERE name LIKE 'f25-test-%'");
    }

    // ── 회원·챌린지·참여 ──

    long insertUser(String name) {
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                name + "@f25.test",
                name);
        return lastId();
    }

    String bearer(long userId) {
        return "Bearer " + tokenProvider.createAccessToken(userId, "USER");
    }

    long insertChallenge(String title, String start, String end, String goalType, int goal) {
        jdbc.update(
                "INSERT INTO challenges (title, description, start_date, end_date, goal_type,"
                        + " goal_value, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)",
                title,
                title + " 설명",
                start,
                end,
                goalType,
                goal,
                adminId);
        return lastId();
    }

    /** 참여 행을 직접 넣는다. {@code completedAt}은 {@code yyyy-MM-dd HH:mm:ss} 또는 null. */
    void insertParticipant(
            long challengeId, long userId, int progress, String completedAt, String joinedAt) {
        jdbc.update(
                "INSERT INTO challenge_participants (challenge_id, user_id, progress, completed_at,"
                        + " joined_at) VALUES (?, ?, ?, ?, ?)",
                challengeId,
                userId,
                progress,
                completedAt,
                joinedAt);
    }

    void insertParticipant(long challengeId, long userId) {
        insertParticipant(challengeId, userId, 0, null, "2026-11-23 09:00:00");
    }

    Map<String, Object> participant(long challengeId, long userId) {
        return jdbc.queryForMap(
                "SELECT progress, completed_at FROM challenge_participants"
                        + " WHERE challenge_id = ? AND user_id = ?",
                challengeId,
                userId);
    }

    int progressOf(long challengeId, long userId) {
        return ((Number) participant(challengeId, userId).get("progress")).intValue();
    }

    void insertActivity(long userId, String type, long targetId) {
        jdbc.update(
                "INSERT INTO activities (user_id, type, target_id) VALUES (?, ?, ?)",
                userId,
                type,
                targetId);
    }

    int activityCount(long userId, String type, long targetId) {
        return count(
                "activities", "user_id = ? AND type = ? AND target_id = ?", userId, type, targetId);
    }

    // ── 집계 대상 활동 ──

    /** 이름은 {@code f25-test-}로 시작해야 정리된다. */
    long insertConcept(String name) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source,"
                        + " wish_count, avg_rating) VALUES (?, 'security', 'JUNIOR', '요약',"
                        + " 'PUBLISHED', 'SEED', 0, 0)",
                name);
        return lastId();
    }

    /** {@code doneAt}은 {@code yyyy-MM-dd HH:mm:ss} 또는 null. */
    long insertPlan(long userId, long conceptId, String planDate, String status, String doneAt) {
        jdbc.update(
                "INSERT INTO review_plans (user_id, concept_id, plan_date, status, reason, done_at)"
                        + " VALUES (?, ?, ?, ?, 'MANUAL', ?)",
                userId,
                conceptId,
                planDate,
                status,
                doneAt);
        return lastId();
    }

    /** {@code createdAt}은 {@code yyyy-MM-dd HH:mm:ss}. */
    long insertAnalysis(long userId, String status, String createdAt, boolean deleted) {
        jdbc.update(
                "INSERT INTO analyses (user_id, source_type, project_name, title, level, status,"
                        + " progress, created_at, deleted_at) VALUES (?, 'ZIP', 'p', 'p', 'JUNIOR',"
                        + " ?, 0, ?, IF(?, NOW(), NULL))",
                userId,
                status,
                createdAt,
                deleted);
        return lastId();
    }

    /** 문항 하나(정답 0번, 개념 없음)짜리 퀴즈. 분석 하나에 퀴즈 하나다. */
    long insertQuiz(long analysisId) {
        jdbc.update("INSERT INTO quizzes (analysis_id) VALUES (?)", analysisId);
        long quizId = lastId();
        jdbc.update(
                "INSERT INTO quiz_questions (quiz_id, concept_id, question, choices, answer_index,"
                        + " explanation) VALUES (?, NULL, '질문', '[\"a\", \"b\", \"c\", \"d\"]', 0,"
                        + " '해설')",
                quizId);
        return quizId;
    }

    long questionOf(long quizId) {
        return jdbc.queryForObject(
                "SELECT id FROM quiz_questions WHERE quiz_id = ?", Long.class, quizId);
    }

    void insertAttempt(long quizId, long userId, String createdAt) {
        jdbc.update(
                "INSERT INTO quiz_attempts (quiz_id, user_id, score, total, created_at)"
                        + " VALUES (?, ?, 1, 1, ?)",
                quizId,
                userId,
                createdAt);
    }

    // ── 공통 ──

    int count(String table, String where, Object... args) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + where, Integer.class, args);
    }

    long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
