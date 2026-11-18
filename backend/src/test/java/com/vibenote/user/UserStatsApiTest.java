package com.vibenote.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/** F-03 학습 통계 (docs/specs/F-03-profile.md AC3, AC2의 /stats). */
class UserStatsApiTest extends UserTestSupport {

    private long concept(String name) {
        return insert(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source)"
                        + " VALUES (?, 'spring', 'BEGINNER', '요약', 'PUBLISHED', 'SEED')",
                name);
    }

    private long analysis(long userId, String status, boolean deleted) {
        return insert(
                "INSERT INTO analyses (user_id, source_type, level, status, deleted_at)"
                        + " VALUES (?, 'ZIP', 'BEGINNER', ?, IF(?, NOW(), NULL))",
                userId,
                status,
                deleted);
    }

    private long question(long quizId, Long conceptId) {
        return insert(
                "INSERT INTO quiz_questions (quiz_id, concept_id, question, choices, answer_index)"
                        + " VALUES (?, ?, '질문', '[\"a\",\"b\"]', 0)",
                quizId,
                conceptId);
    }

    private long attempt(long quizId, long userId, int score, int total) {
        return insert(
                "INSERT INTO quiz_attempts (quiz_id, user_id, score, total) VALUES (?, ?, ?, ?)",
                quizId,
                userId,
                score,
                total);
    }

    private void answer(long attemptId, long questionId, boolean correct) {
        jdbc.update(
                "INSERT INTO quiz_answers (attempt_id, question_id, selected_index, is_correct)"
                        + " VALUES (?, ?, 0, ?)",
                attemptId,
                questionId,
                correct);
    }

    private void reviewPlan(long userId, long conceptId, String status, int dayOffset) {
        jdbc.update(
                "INSERT INTO review_plans (user_id, concept_id, plan_date, status, reason)"
                        + " VALUES (?, ?, CURDATE() + INTERVAL ? DAY, ?, 'WRONG')",
                userId,
                conceptId,
                dayOffset,
                status);
    }

    @Test
    void AC3_완료_미삭제_분석_수_맞힌_카드와_복습_완료_카드의_합집합_정답률을_준다() throws Exception {
        long me = createUser("user@example.com", "코딩초보");
        long other = createUser("other@example.com", "다른사람");
        long jwt = concept("JWT");
        long tx = concept("트랜잭션");
        long di = concept("DI");
        long aop = concept("AOP");

        // 분석: DONE 1개만 센다 (삭제된 DONE, FAILED, 다른 회원의 DONE은 제외)
        long done = analysis(me, "DONE", false);
        analysis(me, "DONE", true);
        analysis(me, "FAILED", false);
        analysis(other, "DONE", false);

        // 퀴즈: 내 풀이 2/3, 1/1 → 합계 기준 3/4 = 75% (풀이별 평균이면 83%라 구분된다)
        // 맞힌 문항의 카드는 JWT (concept 없는 문항은 제외)
        long quiz = insert("INSERT INTO quizzes (analysis_id) VALUES (?)", done);
        long qJwt = question(quiz, jwt);
        long qTx = question(quiz, tx);
        long qNone = question(quiz, null);
        long mine = attempt(quiz, me, 2, 3);
        answer(mine, qJwt, true);
        answer(mine, qTx, false);
        answer(mine, qNone, true);
        long retry = attempt(quiz, me, 1, 1);
        answer(retry, qJwt, true);
        // 다른 회원이 트랜잭션을 맞힌 기록은 세지 않는다
        long theirs = attempt(quiz, other, 3, 3);
        answer(theirs, qTx, true);

        // 복습: DONE인 JWT(퀴즈와 중복)·DI만 센다. TODO인 AOP와 다른 회원의 DONE은 제외
        reviewPlan(me, jwt, "DONE", 0);
        reviewPlan(me, di, "DONE", 0);
        reviewPlan(me, di, "DONE", 1);
        reviewPlan(me, aop, "TODO", 0);
        reviewPlan(other, tx, "DONE", 0);

        mockMvc.perform(get("/api/users/{id}/stats", me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisCount").value(1))
                .andExpect(jsonPath("$.learnedConceptCount").value(2))
                .andExpect(jsonPath("$.quizAccuracy").value(75));
    }

    @Test
    void AC3_문항_수_합계가_0이면_정답률은_null() throws Exception {
        long me = createUser("user@example.com", "코딩초보");
        long quiz =
                insert("INSERT INTO quizzes (analysis_id) VALUES (?)", analysis(me, "DONE", false));
        attempt(quiz, me, 0, 0);

        mockMvc.perform(get("/api/users/{id}/stats", me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisCount").value(1))
                .andExpect(jsonPath("$.quizAccuracy").isEmpty());
    }

    @Test
    void AC3_기록이_없으면_0_0_null() throws Exception {
        long me = createUser("user@example.com", "코딩초보");

        mockMvc.perform(get("/api/users/{id}/stats", me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisCount").value(0))
                .andExpect(jsonPath("$.learnedConceptCount").value(0))
                .andExpect(jsonPath("$.quizAccuracy").isEmpty());
    }

    @Test
    void AC2_탈퇴_회원이나_없는_id의_통계는_404_USER_404() throws Exception {
        long id = createUser("user@example.com", "코딩초보");
        jdbc.update("UPDATE users SET status = 'WITHDRAWN' WHERE id = ?", id);

        for (long target : new long[] {id, id + 1000}) {
            mockMvc.perform(get("/api/users/{id}/stats", target))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("USER_404"));
        }
    }
}
