package com.vibenote.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.analysis.AnalysisService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** F-25 AC5~AC8 활동이 생길 때 달성도 다시 세기. 실제 복습·퀴즈·분석 경로로 활동을 만든다. */
class ChallengeProgressTest extends ChallengeTestSupport {

    @Autowired AnalysisService analysisService;

    void changePlan(long userId, long planId, String status) throws Exception {
        mockMvc.perform(
                        patch("/api/review-plans/{id}", planId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\": \"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    void deletePlan(long userId, long planId) throws Exception {
        mockMvc.perform(
                        delete("/api/review-plans/{id}", planId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().is2xxSuccessful());
    }

    void submitQuiz(long userId, long quizId) throws Exception {
        mockMvc.perform(
                        post("/api/quizzes/{id}/attempts", quizId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"answers\": [{\"questionId\": %d, \"selectedIndex\": 0}]}"
                                                .formatted(questionOf(quizId))))
                .andExpect(status().isCreated());
    }

    long todoPlan(long userId, String name) {
        return insertPlan(userId, insertConcept("f25-test-" + name), "2026-11-24", "TODO", null);
    }

    // ── AC5 복습 ──

    @Test
    void AC5_복습을_끝내면_오르고_되돌리거나_지우면_줄며_세_번째에_달성한다() throws Exception {
        long u = insertUser("f25u");
        long c = insertChallenge("C", "2026-11-20", "2026-11-30", "REVIEW_COUNT", 3);
        insertParticipant(c, u);
        insertPlan(
                u, insertConcept("f25-test-before"), "2026-11-19", "DONE", "2026-11-19 23:59:59");
        todoPlan(u, "todo"); // 세지 않는다
        long p1 = todoPlan(u, "p1");
        long p2 = todoPlan(u, "p2");
        long p3 = todoPlan(u, "p3");
        long p4 = todoPlan(u, "p4");

        changePlan(u, p1, "DONE");
        assertThat(progressOf(c, u)).isEqualTo(1);
        changePlan(u, p1, "TODO");
        assertThat(progressOf(c, u)).isZero();
        changePlan(u, p1, "DONE");
        changePlan(u, p2, "DONE");
        assertThat(progressOf(c, u)).isEqualTo(2);
        deletePlan(u, p2);
        assertThat(progressOf(c, u)).isEqualTo(1);
        changePlan(u, p3, "DONE");
        assertThat(progressOf(c, u)).isEqualTo(2);
        assertThat(participant(c, u).get("completed_at")).isNull();
        assertThat(activityCount(u, "CHALLENGE_COMPLETE", c)).isZero();

        changePlan(u, p4, "DONE");
        assertThat(progressOf(c, u)).isEqualTo(3);
        assertThat(participant(c, u).get("completed_at")).isNotNull();
        assertThat(activityCount(u, "CHALLENGE_COMPLETE", c)).isEqualTo(1);
    }

    // ── AC6 퀴즈 ──

    @Test
    void AC6_퀴즈는_퀴즈와_날짜_조합마다_한_번_세고_삭제한_분석의_풀이도_센다() throws Exception {
        long u = insertUser("f25u");
        long c = insertChallenge("C", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 10);
        insertParticipant(c, u);
        long q1 = insertQuiz(insertAnalysis(u, "DONE", "2026-11-18 09:00:00", false));
        long q2 = insertQuiz(insertAnalysis(u, "DONE", "2026-11-18 10:00:00", false));
        long deletedQuiz = insertQuiz(insertAnalysis(u, "DONE", "2026-11-18 11:00:00", true));
        insertAttempt(q1, u, "2026-11-19 22:00:00"); // 시작 전
        insertAttempt(q1, u, "2026-11-21 10:00:00");
        insertAttempt(deletedQuiz, u, "2026-11-22 10:00:00");

        submitQuiz(u, q1);
        assertThat(progressOf(c, u)).isEqualTo(3); // Q1 11-21, 삭제 분석 11-22, Q1 오늘
        submitQuiz(u, q1);
        submitQuiz(u, q1);
        assertThat(progressOf(c, u)).isEqualTo(3);
        submitQuiz(u, q2);
        assertThat(progressOf(c, u)).isEqualTo(4);
    }

    // ── AC7 분석 ──

    @Test
    void AC7_기간_안에_요청해_DONE이_된_분석만_세고_삭제한_분석도_센다() throws Exception {
        long u = insertUser("f25u");
        long c = insertChallenge("C", "2026-11-20", "2026-11-30", "ANALYSIS_COUNT", 5);
        insertParticipant(c, u);
        insertAnalysis(u, "DONE", "2026-11-19 23:59:59", false); // 시작 전
        insertAnalysis(u, "DONE", "2026-11-21 10:00:00", false);
        insertAnalysis(u, "DONE", "2026-11-22 10:00:00", true); // 삭제했어도 센다
        insertAnalysis(u, "FAILED", "2026-11-22 11:00:00", false);
        insertAnalysis(u, "EXPLAINING", "2026-11-23 10:00:00", false);
        long today = insertAnalysis(u, "EXPLAINING", "2026-11-24 09:00:00", false);
        long lastMinute = insertAnalysis(u, "QUIZ", "2026-11-30 23:59:00", false);

        analysisService.complete(today);
        assertThat(progressOf(c, u)).isEqualTo(3);
        analysisService.complete(lastMinute); // 요청 날짜(11-30) 기준
        assertThat(progressOf(c, u)).isEqualTo(4);
    }

    // ── AC8 유형·기간이 맞는 미달성 참여만, 달성 뒤에는 그대로 ──

    @Test
    void AC8_맞는_유형만_다시_세고_달성한_뒤에는_줄어도_그대로이며_COMPLETE가_늘지_않는다() throws Exception {
        long u = insertUser("f25u");
        long review = insertChallenge("R", "2026-11-20", "2026-11-30", "REVIEW_COUNT", 1);
        long quiz = insertChallenge("Q", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 5);
        long upcoming = insertChallenge("F", "2026-12-01", "2026-12-31", "REVIEW_COUNT", 1);
        insertParticipant(review, u);
        insertParticipant(quiz, u);
        insertParticipant(upcoming, u);
        long plan = todoPlan(u, "plan");

        changePlan(u, plan, "DONE");
        Map<String, Object> done = participant(review, u);
        assertThat(done.get("progress")).isEqualTo(1);
        assertThat(done.get("completed_at")).isNotNull();
        assertThat(activityCount(u, "CHALLENGE_COMPLETE", review)).isEqualTo(1);
        assertThat(progressOf(quiz, u)).isZero();
        assertThat(progressOf(upcoming, u)).isZero();

        changePlan(u, plan, "TODO");
        assertThat(participant(review, u)).isEqualTo(done);
        changePlan(u, plan, "DONE");
        assertThat(participant(review, u)).isEqualTo(done);
        assertThat(activityCount(u, "CHALLENGE_COMPLETE", review)).isEqualTo(1);
        assertThat(activityCount(u, "CHALLENGE_COMPLETE", upcoming)).isZero();
    }
}
