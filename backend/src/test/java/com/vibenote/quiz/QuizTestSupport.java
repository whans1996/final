package com.vibenote.quiz;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.TestProjects;
import com.vibenote.common.security.JwtTokenProvider;
import com.vibenote.user.UserService;
import com.vibenote.user.dto.SignupRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * F-14 통합 테스트 공통. 시각은 2026-11-19 17:30(KST)으로 고정한다 ("내일" = 11-20). 시드 카드에 기대지 않고 {@code f14-test-}
 * 카드를 직접 넣는다. {@code @f14.test} 회원을 지우면 분석·퀴즈·풀이·복습 계획이 CASCADE로 함께 지워진다.
 */
abstract class QuizTestSupport extends IntegrationTestBase {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 11, 19, 17, 30, 0);

    @TestBean Clock clock;

    static Clock clock() {
        return Clock.fixed(FIXED_NOW.atZone(KST).toInstant(), KST);
    }

    @Autowired UserService userService;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM users WHERE email LIKE '%@f14.test'");
        jdbc.update("DELETE FROM concepts WHERE name LIKE 'f14-test-%'");
    }

    long createUser(String name) {
        return userService.signup(
                new SignupRequest(name + "@f14.test", "vibe1234", name, "JUNIOR"));
    }

    String bearer(long userId) {
        return "Bearer " + tokenProvider.createAccessToken(userId, "USER");
    }

    long insertAnalysis(long userId, boolean deleted) {
        jdbc.update(
                "INSERT INTO analyses (user_id, project_id, source_type, project_name, title,"
                    + " level, status, progress, created_at, deleted_at) VALUES (?, ?, 'ZIP', 'p',"
                    + " 'p', 'JUNIOR', 'DONE', 100, ?, IF(?, NOW(), NULL))",
                userId,
                TestProjects.zip(jdbc, userId),
                FIXED_NOW.minusHours(1),
                deleted);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    long insertConcept(String name, String status) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source,"
                        + " wish_count, avg_rating) VALUES (?, 'security', 'JUNIOR', '요약', ?,"
                        + " 'SEED', 2, 4.0)",
                name,
                status);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    long insertQuiz(long analysisId) {
        jdbc.update(
                "INSERT INTO quizzes (analysis_id, created_at) VALUES (?, ?)",
                analysisId,
                FIXED_NOW.minusHours(1));
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /** 선택지 {@code q<n>-a..d}, 정답 {@code answerIndex}, 해설 {@code 해설 <n>}. 넣은 순서대로 id를 돌려준다. */
    List<Long> insertQuestions(long quizId, int[] answerIndexes, Long... conceptIds) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < answerIndexes.length; i++) {
            int n = i + 1;
            jdbc.update(
                    "INSERT INTO quiz_questions (quiz_id, concept_id, question, choices,"
                            + " answer_index, explanation) VALUES (?, ?, ?, ?, ?, ?)",
                    quizId,
                    i < conceptIds.length ? conceptIds[i] : null,
                    "질문 " + n,
                    "[\"q%d-a\", \"q%d-b\", \"q%d-c\", \"q%d-d\"]".formatted(n, n, n, n),
                    answerIndexes[i],
                    "해설 " + n);
            ids.add(jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
        }
        return ids;
    }

    int count(String table, String where, Object... args) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + where, Integer.class, args);
    }
}
