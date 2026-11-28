package com.vibenote.quiz;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

/** L-1b AC1~AC4·AC9: 내 풀이 목록과 풀이 상세 (docs/specs/L-1b-wrong-notes.md). */
class QuizAttemptHistoryTest extends QuizTestSupport {

    private static final LocalDateTime T = LocalDateTime.of(2026, 11, 18, 9, 0, 0);

    private ResultActions attempts(long userId, long quizId) throws Exception {
        return mockMvc.perform(
                get("/api/quizzes/{quizId}/attempts", quizId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }

    private ResultActions attempt(long userId, long quizId, long attemptId) throws Exception {
        return mockMvc.perform(
                get("/api/quizzes/{quizId}/attempts/{attemptId}", quizId, attemptId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }

    @Test
    void AC1_내_풀이만_최신순으로_받고_같은_시각이면_id_내림차순이다() throws Exception {
        long u = createUser("l1b1u");
        long w = createUser("l1b1w");
        long team = insertTeam(u);
        addMember(team, w, "MEMBER");
        long analysis = insertAnalysis(u, false);
        long quiz = insertQuiz(analysis);
        List<Long> q = insertQuestions(quiz, new int[] {0, 1});
        share(team, analysis);

        long first = insertAttempt(quiz, u, T, q, 0, 0);
        long sameTimeA = insertAttempt(quiz, u, T.plusHours(1), q, 0, 1);
        long sameTimeB = insertAttempt(quiz, u, T.plusHours(1), q, 1, 1);
        insertAttempt(quiz, w, T.plusHours(2), q, 1, 0);

        attempts(u, quiz)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].attemptId").value(sameTimeB))
                .andExpect(jsonPath("$[0].score").value(1))
                .andExpect(jsonPath("$[0].total").value(2))
                .andExpect(jsonPath("$[0].createdAt").value("2026-11-18T10:00:00+09:00"))
                .andExpect(jsonPath("$[1].attemptId").value(sameTimeA))
                .andExpect(jsonPath("$[1].score").value(2))
                .andExpect(jsonPath("$[2].attemptId").value(first));

        // 공유받은 팀원은 자기 풀이만 본다 (AC2 뒷부분)
        attempts(w, quiz)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].score").value(0));
    }

    @Test
    void AC1_풀이가_없으면_빈_목록이고_21번_넘게_풀었으면_최근_20개만_받는다() throws Exception {
        long u = createUser("l1b1b");
        long quiz = insertQuiz(insertAnalysis(u, false));
        List<Long> q = insertQuestions(quiz, new int[] {0});

        attempts(u, quiz).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

        long oldest = insertAttempt(quiz, u, T, q, 1);
        long newest = 0;
        for (int i = 1; i <= 21; i++) {
            newest = insertAttempt(quiz, u, T.plusMinutes(i), q, 0);
        }
        attempts(u, quiz)
                .andExpect(jsonPath("$", hasSize(20)))
                .andExpect(jsonPath("$[0].attemptId").value(newest))
                .andExpect(jsonPath("$[?(@.attemptId == %d)]".formatted(oldest)).isEmpty());
    }

    @Test
    void AC2_열_수_없는_퀴즈는_목록_상세_모두_QUIZ_404다() throws Exception {
        long u = createUser("l1b2u");
        long w = createUser("l1b2w");
        long x = createUser("l1b2x");
        long team = insertTeam(u);
        addMember(team, w, "MEMBER");
        long analysis = insertAnalysis(u, false);
        long quiz = insertQuiz(analysis);
        List<Long> q = insertQuestions(quiz, new int[] {0});
        share(team, analysis);
        long wAttempt = insertAttempt(quiz, w, T, q, 0);
        long xAttempt = insertAttempt(quiz, x, T, q, 0);
        long uAttempt = insertAttempt(quiz, u, T, q, 0);

        // 팀원은 자기 풀이 상세를 본다
        attempt(w, quiz, wAttempt).andExpect(status().isOk());

        // 남의 비공유 분석 (팀 밖 회원)
        attempts(x, quiz)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUIZ_404"));
        attempt(x, quiz, xAttempt).andExpect(jsonPath("$.code").value("QUIZ_404"));

        // 공유가 끊긴 팀 분석
        jdbc.update("DELETE FROM team_analyses WHERE team_id = ?", team);
        attempts(w, quiz).andExpect(jsonPath("$.code").value("QUIZ_404"));
        attempt(w, quiz, wAttempt).andExpect(jsonPath("$.code").value("QUIZ_404"));

        // 삭제한 분석 (주인도)
        jdbc.update("UPDATE analyses SET deleted_at = NOW() WHERE id = ?", analysis);
        attempts(u, quiz).andExpect(jsonPath("$.code").value("QUIZ_404"));
        attempt(u, quiz, uAttempt)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUIZ_404"));

        // 없는 quizId
        attempts(u, 999_999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUIZ_404"));
        attempt(u, 999_999_999L, wAttempt).andExpect(jsonPath("$.code").value("QUIZ_404"));
    }

    @Test
    void AC3_풀이_상세는_저장된_순서로_문항별_내_답_정답_해설_공개_개념을_준다() throws Exception {
        long u = createUser("l1b3u");
        long quiz = insertQuiz(insertAnalysis(u, false));
        long published = insertConcept("f14-test-l1b3-pub", "PUBLISHED");
        long draft = insertConcept("f14-test-l1b3-draft", "DRAFT");
        List<Long> q = insertQuestions(quiz, new int[] {1, 2, 0}, published, draft);
        long attemptId = insertAttempt(quiz, u, T, q, 3, 2, 0);

        attempt(u, quiz, attemptId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(attemptId))
                .andExpect(jsonPath("$.score").value(2))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.createdAt").value("2026-11-18T09:00:00+09:00"))
                .andExpect(jsonPath("$.results", hasSize(3)))
                .andExpect(jsonPath("$.results[0].questionId").value(q.get(0)))
                .andExpect(jsonPath("$.results[0].no").value(1))
                .andExpect(jsonPath("$.results[0].question").value("질문 1"))
                .andExpect(jsonPath("$.results[0].choices[3]").value("q1-d"))
                .andExpect(jsonPath("$.results[0].selectedIndex").value(3))
                .andExpect(jsonPath("$.results[0].answerIndex").value(1))
                .andExpect(jsonPath("$.results[0].correct").value(false))
                .andExpect(jsonPath("$.results[0].explanation").value("해설 1"))
                .andExpect(jsonPath("$.results[0].concept.id").value(published))
                .andExpect(jsonPath("$.results[0].concept.name").value("f14-test-l1b3-pub"))
                .andExpect(jsonPath("$.results[0].reviewAdded").doesNotExist())
                .andExpect(jsonPath("$.results[1].no").value(2))
                .andExpect(jsonPath("$.results[1].correct").value(true))
                .andExpect(jsonPath("$.results[1].concept").value(nullValue()))
                .andExpect(jsonPath("$.results[2].no").value(3))
                .andExpect(jsonPath("$.results[2].concept").value(nullValue()))
                .andExpect(jsonPath("$.reviewAdded").doesNotExist());
    }

    @Test
    void AC4_남의_풀이_다른_퀴즈의_풀이_없는_풀이는_QUIZ_001다() throws Exception {
        long u = createUser("l1b4u");
        long w = createUser("l1b4w");
        long team = insertTeam(u);
        addMember(team, w, "MEMBER");
        long analysis = insertAnalysis(u, false);
        long quiz = insertQuiz(analysis);
        List<Long> q = insertQuestions(quiz, new int[] {0});
        share(team, analysis);
        long otherQuiz = insertQuiz(insertAnalysis(u, false));
        List<Long> oq = insertQuestions(otherQuiz, new int[] {0});

        long wAttempt = insertAttempt(quiz, w, T, q, 0);
        long otherQuizAttempt = insertAttempt(otherQuiz, u, T, oq, 0);

        attempt(u, quiz, wAttempt)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUIZ_001"));
        attempt(u, quiz, otherQuizAttempt).andExpect(jsonPath("$.code").value("QUIZ_001"));
        attempt(u, quiz, 999_999_999L).andExpect(jsonPath("$.code").value("QUIZ_001"));
    }

    @Test
    void AC9_비로그인은_풀이_목록_상세가_401이다() throws Exception {
        mockMvc.perform(get("/api/quizzes/{quizId}/attempts", 1))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        mockMvc.perform(get("/api/quizzes/{quizId}/attempts/{attemptId}", 1, 1))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }
}
