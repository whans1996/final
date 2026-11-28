package com.vibenote.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

/** L-1b AC5~AC9: 오답 노트 — 마지막 답이 틀린 문항을 개념별로 묶는다 (docs/specs/L-1b-wrong-notes.md). */
class WrongNoteTest extends QuizTestSupport {

    private static final LocalDateTime T = LocalDateTime.of(2026, 11, 18, 9, 0, 0);

    private ResultActions wrongNotes(long userId) throws Exception {
        return mockMvc.perform(
                get("/api/users/me/wrong-notes").header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }

    @Test
    void AC5_마지막_답이_틀린_문항만_마지막_답과_틀린_횟수와_함께_보인다() throws Exception {
        long u = createUser("l1b5u");
        long quiz = insertQuiz(insertAnalysis(u, false));
        List<Long> q = insertQuestions(quiz, new int[] {0, 0, 0});
        insertAttempt(quiz, u, T, q, 2, 1, 0);
        insertAttempt(quiz, u, T.plusHours(1), q, 3, 0, 0);

        wrongNotes(u)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.groups", hasSize(1)))
                .andExpect(jsonPath("$.groups[0].concept").value(nullValue()))
                .andExpect(jsonPath("$.groups[0].items", hasSize(1)))
                .andExpect(jsonPath("$.groups[0].items[0].questionId").value(q.get(0)))
                .andExpect(jsonPath("$.groups[0].items[0].quizId").value(quiz))
                .andExpect(jsonPath("$.groups[0].items[0].question").value("질문 1"))
                .andExpect(jsonPath("$.groups[0].items[0].choices[3]").value("q1-d"))
                .andExpect(jsonPath("$.groups[0].items[0].selectedIndex").value(3))
                .andExpect(jsonPath("$.groups[0].items[0].answerIndex").value(0))
                .andExpect(jsonPath("$.groups[0].items[0].explanation").value("해설 1"))
                .andExpect(jsonPath("$.groups[0].items[0].wrongCount").value(2))
                .andExpect(
                        jsonPath("$.groups[0].items[0].lastAnsweredAt")
                                .value("2026-11-18T10:00:00+09:00"));
    }

    @Test
    void AC6_개념별로_묶어_문항_수_최근_시각_순으로_정렬하고_개념_없는_묶음은_맨_뒤다() throws Exception {
        long u = createUser("l1b6u");
        long a = insertConcept("f14-test-l1b6-a", "PUBLISHED");
        long b = insertConcept("f14-test-l1b6-b", "PUBLISHED");
        long c = insertConcept("f14-test-l1b6-c", "PUBLISHED");
        long draft = insertConcept("f14-test-l1b6-draft", "DRAFT");

        long an1 = insertAnalysis(u, "JWT 기반 인증", "proj1");
        long quiz1 = insertQuiz(an1);
        List<Long> q1 = insertQuestions(quiz1, new int[] {0, 0, 0}, a, a, b);
        insertAttempt(quiz1, u, T, q1, 1, 1, 1);

        long an2 = insertAnalysis(u, null, "my-proj");
        long quiz2 = insertQuiz(an2);
        List<Long> q2 = insertQuestions(quiz2, new int[] {0, 0, 0}, c, null, draft);
        insertAttempt(quiz2, u, T.plusHours(1), q2, 1, 1, 1);

        long an3 = insertAnalysis(u, "  ", "");
        long quiz3 = insertQuiz(an3);
        List<Long> q3 = insertQuestions(quiz3, new int[] {0});
        insertAttempt(quiz3, u, T.plusHours(2), q3, 1);

        wrongNotes(u)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(7))
                .andExpect(jsonPath("$.groups", hasSize(4)))
                // A: 2문항 → 맨 앞, 같은 시각이면 questionId 오름차순
                .andExpect(jsonPath("$.groups[0].concept.id").value(a))
                .andExpect(jsonPath("$.groups[0].concept.name").value("f14-test-l1b6-a"))
                .andExpect(jsonPath("$.groups[0].items", hasSize(2)))
                .andExpect(jsonPath("$.groups[0].items[0].questionId").value(q1.get(0)))
                .andExpect(jsonPath("$.groups[0].items[1].questionId").value(q1.get(1)))
                .andExpect(jsonPath("$.groups[0].items[0].analysisId").value(an1))
                .andExpect(jsonPath("$.groups[0].items[0].analysisTitle").value("JWT 기반 인증"))
                // 1문항 묶음: 최근 시각(C) → 오래된 것(B)
                .andExpect(jsonPath("$.groups[1].concept.id").value(c))
                .andExpect(jsonPath("$.groups[1].items[0].analysisTitle").value("my-proj"))
                .andExpect(jsonPath("$.groups[2].concept.id").value(b))
                // 개념 없음·비공개 개념은 한 묶음으로 맨 뒤, lastAnsweredAt 내림차순 → questionId 오름차순
                .andExpect(jsonPath("$.groups[3].concept").value(nullValue()))
                .andExpect(jsonPath("$.groups[3].items", hasSize(3)))
                .andExpect(jsonPath("$.groups[3].items[0].questionId").value(q3.get(0)))
                .andExpect(jsonPath("$.groups[3].items[0].analysisTitle").value("제목 없는 분석"))
                .andExpect(jsonPath("$.groups[3].items[1].questionId").value(q2.get(1)))
                .andExpect(jsonPath("$.groups[3].items[2].questionId").value(q2.get(2)));
    }

    @Test
    void AC6_문항_수와_시각이_같으면_개념_id_오름차순이다() throws Exception {
        long u = createUser("l1b6b");
        long first = insertConcept("f14-test-l1b6b-1", "PUBLISHED");
        long second = insertConcept("f14-test-l1b6b-2", "PUBLISHED");
        long quiz = insertQuiz(insertAnalysis(u, false));
        List<Long> q = insertQuestions(quiz, new int[] {0, 0}, second, first);
        insertAttempt(quiz, u, T, q, 1, 1);

        wrongNotes(u)
                .andExpect(jsonPath("$.groups[0].concept.id").value(first))
                .andExpect(jsonPath("$.groups[1].concept.id").value(second));
    }

    @Test
    void AC7_지금_열_수_있는_분석의_문항만_보이고_다른_사람의_풀이는_영향이_없다() throws Exception {
        long u = createUser("l1b7u");
        long w = createUser("l1b7w");
        long team = insertTeam(w);
        addMember(team, u, "MEMBER");

        // 내 분석: 내가 틀림. 팀원 W가 나중에 맞혀도 내 노트에 남는다
        long mine = insertAnalysis(u, false);
        long mineQuiz = insertQuiz(mine);
        List<Long> mq = insertQuestions(mineQuiz, new int[] {0, 0});
        insertAttempt(mineQuiz, u, T, mq, 1, 0);
        // 공유받은 W의 분석
        long shared = insertAnalysis(w, false);
        long sharedQuiz = insertQuiz(shared);
        List<Long> sq = insertQuestions(sharedQuiz, new int[] {0});
        share(team, shared);
        insertAttempt(sharedQuiz, u, T, sq, 2);
        // 공유가 끊긴 W의 분석
        long unshared = insertAnalysis(w, false);
        long unsharedQuiz = insertQuiz(unshared);
        List<Long> uq = insertQuestions(unsharedQuiz, new int[] {0});
        share(team, unshared);
        insertAttempt(unsharedQuiz, u, T, uq, 3);
        jdbc.update("DELETE FROM team_analyses WHERE analysis_id = ?", unshared);
        // 삭제한 내 분석
        long deleted = insertAnalysis(u, false);
        long deletedQuiz = insertQuiz(deleted);
        List<Long> dq = insertQuestions(deletedQuiz, new int[] {0});
        insertAttempt(deletedQuiz, u, T, dq, 1);
        jdbc.update("UPDATE analyses SET deleted_at = NOW() WHERE id = ?", deleted);

        // 다른 사람의 풀이: W는 내 두 번째 문항을 틀리고 첫 문항을 나중에 맞힌다
        share(team, mine);
        insertAttempt(mineQuiz, w, T.plusHours(1), mq, 0, 2);

        wrongNotes(u)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.groups[0].items", hasSize(2)))
                .andExpect(jsonPath("$.groups[0].items[0].questionId").value(mq.get(0)))
                .andExpect(jsonPath("$.groups[0].items[0].selectedIndex").value(1))
                .andExpect(jsonPath("$.groups[0].items[0].wrongCount").value(1))
                .andExpect(jsonPath("$.groups[0].items[1].questionId").value(sq.get(0)));

        // W의 노트에는 W가 틀린 문항만 있다
        wrongNotes(w)
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.groups[0].items[0].questionId").value(mq.get(1)));

        // 빠진 문항의 풀이 기록은 그대로 남는다 (F-19)
        assertThat(count("quiz_attempts", "quiz_id IN (?, ?)", unsharedQuiz, deletedQuiz))
                .isEqualTo(2);
        assertThat(count("quiz_answers", "question_id IN (?, ?)", uq.get(0), dq.get(0)))
                .isEqualTo(2);
    }

    @Test
    void AC8_오답이_없으면_빈_노트다() throws Exception {
        long u = createUser("l1b8a");
        long quiz = insertQuiz(insertAnalysis(u, false));
        List<Long> q = insertQuestions(quiz, new int[] {0});
        insertAttempt(quiz, u, T, q, 0);

        wrongNotes(u)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.groups", hasSize(0)));
    }

    @Test
    void AC8_오답이_201개_이상이면_최근_200개만_묶고_totalCount는_전체_수다() throws Exception {
        long u = createUser("l1b8b");
        long oldQuiz = insertQuiz(insertAnalysis(u, false));
        List<Long> old = insertQuestions(oldQuiz, new int[] {0});
        insertAttempt(oldQuiz, u, T.minusDays(1), old, 1);

        long quiz = insertQuiz(insertAnalysis(u, false));
        List<Long> q = insertQuestions(quiz, new int[200]);
        int[] wrong = new int[200];
        Arrays.fill(wrong, 1);
        insertAttempt(quiz, u, T, q, wrong);

        wrongNotes(u)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(201))
                .andExpect(jsonPath("$.groups", hasSize(1)))
                .andExpect(jsonPath("$.groups[0].items", hasSize(200)))
                .andExpect(jsonPath("$.groups[0].items[0].questionId").value(q.get(0)))
                .andExpect(
                        jsonPath("$.groups[0].items[?(@.questionId == %d)]".formatted(old.get(0)))
                                .isEmpty());
    }

    @Test
    void AC9_비로그인은_오답_노트가_401이다() throws Exception {
        mockMvc.perform(get("/api/users/me/wrong-notes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }
}
