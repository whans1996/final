package com.vibenote.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** F-14 퀴즈 조회·풀이·채점 API (docs/specs/F-14-code-quiz.md AC5~AC8). 퀴즈·문항은 직접 넣는다. */
class QuizApiTest extends QuizTestSupport {

    private ResultActions getQuiz(long userId, long analysisId) throws Exception {
        return mockMvc.perform(
                get("/api/analyses/{id}/quiz", analysisId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }

    private ResultActions submit(long userId, long quizId, String body) throws Exception {
        return mockMvc.perform(
                post("/api/quizzes/{quizId}/attempts", quizId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    /** questionId → selectedIndex 순서대로 요청 본문을 만든다. */
    private static String answers(long... pairs) {
        StringBuilder out = new StringBuilder("{\"answers\": [");
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0) {
                out.append(',');
            }
            out.append(
                    "{\"questionId\": %d, \"selectedIndex\": %d}"
                            .formatted(pairs[i], pairs[i + 1]));
        }
        return out.append("]}").toString();
    }

    private void insertAttempt(long quizId, long userId, int score, int total, String createdAt) {
        jdbc.update(
                "INSERT INTO quiz_attempts (quiz_id, user_id, score, total, created_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                quizId,
                userId,
                score,
                total,
                createdAt);
    }

    private List<Map<String, Object>> plans(long userId) {
        return jdbc.queryForList(
                "SELECT concept_id, plan_date, status, reason FROM review_plans WHERE user_id = ?"
                        + " ORDER BY concept_id, plan_date",
                userId);
    }

    // ── AC5 조회 ──

    @Test
    void AC5_문항을_no_순서로_주고_정답_해설_개념은_담지_않는다() throws Exception {
        long userId = createUser("f14ac5");
        long analysisId = insertAnalysis(userId, false);
        long quizId = insertQuiz(analysisId);
        long concept = insertConcept("f14-test-JWT", "PUBLISHED");
        List<Long> q = insertQuestions(quizId, new int[] {1, 2}, concept);

        getQuiz(userId, analysisId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizId").value(quizId))
                .andExpect(jsonPath("$.analysisId").value(analysisId))
                .andExpect(jsonPath("$.questions", hasSize(2)))
                .andExpect(jsonPath("$.questions[0].id").value(q.get(0)))
                .andExpect(jsonPath("$.questions[0].no").value(1))
                .andExpect(jsonPath("$.questions[0].question").value("질문 1"))
                .andExpect(jsonPath("$.questions[0].choices[1]").value("q1-b"))
                .andExpect(jsonPath("$.questions[1].no").value(2))
                .andExpect(jsonPath("$.questions[0].answerIndex").doesNotExist())
                .andExpect(jsonPath("$.questions[0].explanation").doesNotExist())
                .andExpect(jsonPath("$.questions[0].concept").doesNotExist())
                .andExpect(jsonPath("$.questions[0].conceptId").doesNotExist())
                .andExpect(jsonPath("$.lastAttempt").value(nullValue()));
    }

    @Test
    void AC5_lastAttempt는_내_가장_최근_풀이이고_시각이_같으면_id가_큰_것이다() throws Exception {
        long userId = createUser("f14ac5l");
        long other = createUser("f14ac5o");
        long analysisId = insertAnalysis(userId, false);
        long quizId = insertQuiz(analysisId);
        insertQuestions(quizId, new int[] {0, 0});
        insertAttempt(quizId, userId, 0, 2, "2026-11-19 10:00:00");
        insertAttempt(quizId, userId, 1, 2, "2026-11-19 12:00:00");
        insertAttempt(quizId, userId, 2, 2, "2026-11-19 12:00:00");
        long latest = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        insertAttempt(quizId, other, 0, 2, "2026-11-19 13:00:00");

        getQuiz(userId, analysisId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastAttempt.attemptId").value(latest))
                .andExpect(jsonPath("$.lastAttempt.score").value(2))
                .andExpect(jsonPath("$.lastAttempt.total").value(2))
                .andExpect(jsonPath("$.lastAttempt.createdAt").value("2026-11-19T12:00:00+09:00"));
    }

    @Test
    void AC5_없는_다른_회원_삭제된_분석은_ANALYSIS_404_퀴즈가_없으면_QUIZ_404() throws Exception {
        long userId = createUser("f14ac5n");
        long other = createUser("f14ac5x");
        long othersAnalysis = insertAnalysis(other, false);
        insertQuiz(othersAnalysis);
        long deleted = insertAnalysis(userId, true);
        insertQuiz(deleted);
        long noQuiz = insertAnalysis(userId, false);

        getQuiz(userId, othersAnalysis)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        getQuiz(userId, deleted)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        getQuiz(userId, 999_999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        getQuiz(userId, noQuiz)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUIZ_404"));
        mockMvc.perform(get("/api/analyses/{id}/quiz", noQuiz))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    // ── AC6 채점 ──

    @Test
    void AC6_채점해_풀이와_답을_저장하고_결과를_no_순서로_준다() throws Exception {
        long userId = createUser("f14ac6");
        long analysisId = insertAnalysis(userId, false);
        long quizId = insertQuiz(analysisId);
        long published = insertConcept("f14-test-공개", "PUBLISHED");
        long draft = insertConcept("f14-test-초안", "DRAFT");
        long hidden = insertConcept("f14-test-숨김", "HIDDEN");
        List<Long> q =
                insertQuestions(quizId, new int[] {0, 1, 2, 3}, null, published, draft, hidden);

        // 순서를 섞어 보내도 결과는 no 순서다
        submit(userId, quizId, answers(q.get(3), 3, q.get(1), 0, q.get(0), 0, q.get(2), 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attemptId").isNumber())
                .andExpect(jsonPath("$.score").value(3))
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.createdAt").value("2026-11-19T17:30:00+09:00"))
                .andExpect(jsonPath("$.results", hasSize(4)))
                .andExpect(jsonPath("$.results[0].questionId").value(q.get(0)))
                .andExpect(jsonPath("$.results[0].no").value(1))
                .andExpect(jsonPath("$.results[0].question").value("질문 1"))
                .andExpect(jsonPath("$.results[0].choices", hasSize(4)))
                .andExpect(jsonPath("$.results[0].selectedIndex").value(0))
                .andExpect(jsonPath("$.results[0].answerIndex").value(0))
                .andExpect(jsonPath("$.results[0].correct").value(true))
                .andExpect(jsonPath("$.results[0].explanation").value("해설 1"))
                .andExpect(jsonPath("$.results[0].concept").value(nullValue()))
                .andExpect(jsonPath("$.results[1].correct").value(false))
                .andExpect(jsonPath("$.results[1].selectedIndex").value(0))
                .andExpect(jsonPath("$.results[1].answerIndex").value(1))
                .andExpect(jsonPath("$.results[1].concept.id").value(published))
                .andExpect(jsonPath("$.results[1].concept.name").value("f14-test-공개"))
                .andExpect(jsonPath("$.results[1].concept.category").value("security"))
                .andExpect(jsonPath("$.results[1].concept.difficulty").value("JUNIOR"))
                .andExpect(jsonPath("$.results[1].concept.avgRating").value(4.0))
                .andExpect(jsonPath("$.results[1].concept.wishCount").value(2))
                .andExpect(jsonPath("$.results[2].concept").value(nullValue()))
                .andExpect(jsonPath("$.results[3].concept").value(nullValue()));

        Map<String, Object> attempt =
                jdbc.queryForMap("SELECT * FROM quiz_attempts WHERE quiz_id = ?", quizId);
        assertThat(attempt.get("user_id")).isEqualTo(userId);
        assertThat(attempt.get("score")).isEqualTo(3);
        assertThat(attempt.get("total")).isEqualTo(4);
        assertThat(
                        jdbc.queryForList(
                                "SELECT question_id, selected_index, is_correct FROM quiz_answers"
                                        + " WHERE attempt_id = ? ORDER BY question_id",
                                attempt.get("id")))
                .extracting(
                        r ->
                                r.get("question_id")
                                        + " "
                                        + r.get("selected_index")
                                        + " "
                                        + r.get("is_correct"))
                .containsExactly(
                        q.get(0) + " 0 true",
                        q.get(1) + " 0 false",
                        q.get(2) + " 2 true",
                        q.get(3) + " 3 true");
    }

    @Test
    void AC6_다시_제출하면_새_풀이로_쌓이고_lastAttempt가_바뀐다() throws Exception {
        long userId = createUser("f14ac6r");
        long analysisId = insertAnalysis(userId, false);
        long quizId = insertQuiz(analysisId);
        List<Long> q = insertQuestions(quizId, new int[] {0, 0});

        submit(userId, quizId, answers(q.get(0), 1, q.get(1), 1)).andExpect(status().isCreated());
        submit(userId, quizId, answers(q.get(0), 0, q.get(1), 0))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(2));

        assertThat(count("quiz_attempts", "quiz_id = ?", quizId)).isEqualTo(2);
        getQuiz(userId, analysisId)
                .andExpect(jsonPath("$.lastAttempt.score").value(2))
                .andExpect(jsonPath("$.lastAttempt.createdAt").value("2026-11-19T17:30:00+09:00"));
    }

    // ── AC7 오답 복습 ──

    @Test
    void AC7_틀린_공개_개념을_중복_없이_내일_복습으로_넣고_새로_넣은_것만_돌려준다() throws Exception {
        long userId = createUser("f14ac7");
        long analysisId = insertAnalysis(userId, false);
        long quizId = insertQuiz(analysisId);
        long a = insertConcept("f14-test-A", "PUBLISHED");
        long b = insertConcept("f14-test-B", "PUBLISHED");
        long todo = insertConcept("f14-test-TODO 있음", "PUBLISHED");
        long doneTomorrow = insertConcept("f14-test-내일 DONE", "PUBLISHED");
        long draft = insertConcept("f14-test-초안", "DRAFT");
        long right = insertConcept("f14-test-맞힘", "PUBLISHED");
        // 다른 날짜의 TODO가 있으면 넣지 않는다, 내일 날짜에 DONE이 있으면 UNIQUE가 겹쳐 건너뛴다
        jdbc.update(
                "INSERT INTO review_plans (user_id, concept_id, plan_date, status, reason)"
                        + " VALUES (?, ?, '2026-11-25', 'TODO', 'WISH'),"
                        + " (?, ?, '2026-11-20', 'DONE', 'MANUAL')",
                userId,
                todo,
                userId,
                doneTomorrow);
        List<Long> q =
                insertQuestions(
                        quizId,
                        new int[] {0, 0, 0, 0, 0, 0, 0, 0},
                        b,
                        a,
                        b,
                        todo,
                        doneTomorrow,
                        draft,
                        null,
                        right);

        submit(
                        userId,
                        quizId,
                        answers(
                                q.get(0), 1, q.get(1), 1, q.get(2), 1, q.get(3), 1, q.get(4), 1,
                                q.get(5), 1, q.get(6), 1, q.get(7), 0))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.reviewAdded", hasSize(2)))
                .andExpect(jsonPath("$.reviewAdded[0].conceptId").value(Math.min(a, b)))
                .andExpect(jsonPath("$.reviewAdded[0].planDate").value("2026-11-20"))
                .andExpect(jsonPath("$.reviewAdded[1].conceptId").value(Math.max(a, b)));

        assertThat(plans(userId))
                .extracting(
                        r ->
                                r.get("concept_id")
                                        + " "
                                        + r.get("plan_date")
                                        + " "
                                        + r.get("status")
                                        + " "
                                        + r.get("reason"))
                .containsExactlyInAnyOrder(
                        a + " 2026-11-20 TODO WRONG",
                        b + " 2026-11-20 TODO WRONG",
                        todo + " 2026-11-25 TODO WISH",
                        doneTomorrow + " 2026-11-20 DONE MANUAL");
    }

    @Test
    void AC7_모두_맞히면_reviewAdded는_빈_배열이다() throws Exception {
        long userId = createUser("f14ac7a");
        long analysisId = insertAnalysis(userId, false);
        long quizId = insertQuiz(analysisId);
        long a = insertConcept("f14-test-A2", "PUBLISHED");
        List<Long> q = insertQuestions(quizId, new int[] {2}, a);

        submit(userId, quizId, answers(q.get(0), 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewAdded", hasSize(0)));
        assertThat(plans(userId)).isEmpty();
    }

    // ── AC8 잘못된 제출 ──

    @Test
    void AC8_answers_검증_실패는_COMMON_400() throws Exception {
        long userId = createUser("f14ac8v");
        long quizId = insertQuiz(insertAnalysis(userId, false));
        long q = insertQuestions(quizId, new int[] {0}).get(0);

        submit(userId, quizId, "{\"answers\": []}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.answers").exists());
        submit(userId, quizId, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        submit(userId, quizId, "{\"answers\": [{\"questionId\": null, \"selectedIndex\": 0}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details['answers[0].questionId']").exists());
        submit(userId, quizId, "{\"answers\": [{\"questionId\": %d}]}".formatted(q))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details['answers[0].selectedIndex']").exists());
        submit(userId, quizId, answers(q, 4))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details['answers[0].selectedIndex']").exists());
        submit(userId, quizId, answers(q, -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        assertThat(count("quiz_attempts", "quiz_id = ?", quizId)).isZero();
    }

    @Test
    void AC8_문항마다_한_번씩_답하지_않으면_QUIZ_400이고_아무것도_저장하지_않는다() throws Exception {
        long userId = createUser("f14ac8q");
        long quizId = insertQuiz(insertAnalysis(userId, false));
        long concept = insertConcept("f14-test-오답", "PUBLISHED");
        List<Long> q = insertQuestions(quizId, new int[] {0, 0}, concept, concept);
        long otherQuiz = insertQuiz(insertAnalysis(userId, false));
        long foreign = insertQuestions(otherQuiz, new int[] {0}).get(0);

        for (String body :
                List.of(
                        answers(q.get(0), 1),
                        answers(q.get(0), 1, q.get(0), 1),
                        answers(q.get(0), 1, q.get(1), 1, q.get(0), 1),
                        answers(q.get(0), 1, foreign, 1),
                        answers(q.get(0), 1, q.get(1), 1, foreign, 1))) {
            submit(userId, quizId, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("QUIZ_400"))
                    .andExpect(jsonPath("$.message").value("모든 문항에 한 번씩 답해 주세요."));
        }
        assertThat(count("quiz_attempts", "user_id = ?", userId)).isZero();
        assertThat(plans(userId)).isEmpty();
    }

    @Test
    void AC8_없는_퀴즈_남의_분석_삭제된_분석의_퀴즈는_QUIZ_404_토큰이_없으면_401() throws Exception {
        long userId = createUser("f14ac8n");
        long other = createUser("f14ac8x");
        long othersQuiz = insertQuiz(insertAnalysis(other, false));
        long q1 = insertQuestions(othersQuiz, new int[] {0}).get(0);
        long deletedQuiz = insertQuiz(insertAnalysis(userId, true));
        long q2 = insertQuestions(deletedQuiz, new int[] {0}).get(0);

        submit(userId, othersQuiz, answers(q1, 0))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUIZ_404"));
        submit(userId, deletedQuiz, answers(q2, 0))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUIZ_404"));
        submit(userId, 999_999_999L, answers(q2, 0))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUIZ_404"));
        mockMvc.perform(
                        post("/api/quizzes/{quizId}/attempts", othersQuiz)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(answers(q1, 0)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        assertThat(count("quiz_attempts", "quiz_id IN (?, ?)", othersQuiz, deletedQuiz)).isZero();
    }
}
