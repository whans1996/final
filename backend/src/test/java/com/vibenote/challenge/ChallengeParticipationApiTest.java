package com.vibenote.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

/** F-25 AC3·AC4·AC9 참여·취소. */
class ChallengeParticipationApiTest extends ChallengeTestSupport {

    static final String PARTICIPATION = "/api/challenges/{id}/participation";

    ResultActions join(long userId, long challengeId) throws Exception {
        return mockMvc.perform(
                post(PARTICIPATION, challengeId).header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }

    ResultActions cancel(long userId, long challengeId) throws Exception {
        return mockMvc.perform(
                delete(PARTICIPATION, challengeId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }

    int participantRows(long challengeId) {
        return count("challenge_participants", "challenge_id = ?", challengeId);
    }

    // ── AC3 참여 ──

    @Test
    void AC3_진행_중_챌린지에_참여하면_참여_행과_JOIN_활동이_생기고_다시_보내도_늘지_않는다() throws Exception {
        long u = insertUser("f25u");
        long c = insertChallenge("C", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 10);

        join(u, c)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joined").value(true))
                .andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.participation.progress").value(0))
                .andExpect(jsonPath("$.participation.goalValue").value(10))
                .andExpect(jsonPath("$.participation.completedAt").value(nullValue()))
                .andExpect(jsonPath("$.participation.joinedAt").value(NOW_TEXT));
        assertThat(participantRows(c)).isEqualTo(1);
        assertThat(activityCount(u, "CHALLENGE_JOIN", c)).isEqualTo(1);

        join(u, c)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joined").value(true))
                .andExpect(jsonPath("$.participantCount").value(1));
        assertThat(participantRows(c)).isEqualTo(1);
        assertThat(activityCount(u, "CHALLENGE_JOIN", c)).isEqualTo(1);
    }

    @Test
    void AC3_예정_챌린지도_참여하고_종료_챌린지는_409다() throws Exception {
        long u = insertUser("f25u");
        long upcoming = insertChallenge("F", "2026-12-01", "2026-12-31", "REVIEW_COUNT", 3);
        long ended = insertChallenge("E", "2026-11-01", "2026-11-23", "REVIEW_COUNT", 3);

        join(u, upcoming)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participation.progress").value(0));

        join(u, ended)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHALLENGE_002"));
        assertThat(participantRows(ended)).isZero();
        assertThat(activityCount(u, "CHALLENGE_JOIN", ended)).isZero();
    }

    @Test
    void AC3_없는_챌린지는_404이고_비로그인은_401이다() throws Exception {
        long u = insertUser("f25u");
        long c = insertChallenge("C", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 10);

        join(u, 999_999)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHALLENGE_404"));
        mockMvc.perform(post(PARTICIPATION, c))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        mockMvc.perform(delete(PARTICIPATION, c)).andExpect(status().isUnauthorized());
        assertThat(participantRows(c)).isZero();
    }

    // ── AC4 참여 직후 집계 ──

    @Test
    void AC4_기간_안에_이미_푼_퀴즈로_참여하자마자_달성한다() throws Exception {
        long u = insertUser("f25u");
        long quizX = insertQuiz(insertAnalysis(u, "DONE", "2026-11-18 09:00:00", false));
        long quizY = insertQuiz(insertAnalysis(u, "DONE", "2026-11-18 10:00:00", false));
        insertAttempt(quizX, u, "2026-11-19 23:59:59"); // 시작 전
        insertAttempt(quizX, u, "2026-11-21 10:00:00");
        insertAttempt(quizY, u, "2026-11-22 10:00:00");
        long c = insertChallenge("C", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 2);
        long wide = insertChallenge("W", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 5);

        join(u, c)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participation.progress").value(2))
                .andExpect(jsonPath("$.participation.completedAt").value(NOW_TEXT));
        assertThat(activityCount(u, "CHALLENGE_JOIN", c)).isEqualTo(1);
        assertThat(activityCount(u, "CHALLENGE_COMPLETE", c)).isEqualTo(1);

        join(u, wide)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participation.progress").value(2))
                .andExpect(jsonPath("$.participation.completedAt").value(nullValue()));
        assertThat(activityCount(u, "CHALLENGE_COMPLETE", wide)).isZero();
    }

    // ── AC9 취소 ──

    @Test
    void AC9_취소하면_참여_행과_그_챌린지의_내_JOIN_활동만_지워진다() throws Exception {
        long u = insertUser("f25u");
        long other = insertUser("f25other");
        long c = insertChallenge("C", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 10);
        long d = insertChallenge("D", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 10);
        join(u, c).andExpect(status().isOk());
        join(u, d).andExpect(status().isOk());
        join(other, c).andExpect(status().isOk());

        cancel(u, c)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joined").value(false))
                .andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.participation").value(nullValue()));
        assertThat(count("challenge_participants", "challenge_id = ? AND user_id = ?", c, u))
                .isZero();
        assertThat(activityCount(u, "CHALLENGE_JOIN", c)).isZero();
        assertThat(activityCount(u, "CHALLENGE_JOIN", d)).isEqualTo(1);
        assertThat(activityCount(other, "CHALLENGE_JOIN", c)).isEqualTo(1);

        cancel(u, c)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joined").value(false))
                .andExpect(jsonPath("$.participantCount").value(1));
    }

    @Test
    void AC9_달성한_챌린지는_409_003이고_종료_챌린지는_409_002이며_아무것도_지워지지_않는다() throws Exception {
        long u = insertUser("f25u");
        long done = insertChallenge("Done", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 1);
        long ended = insertChallenge("E", "2026-11-01", "2026-11-23", "QUIZ_COUNT", 1);
        insertParticipant(done, u, 1, "2026-11-23 12:00:00", "2026-11-22 09:00:00");
        insertActivity(u, "CHALLENGE_JOIN", done);
        insertParticipant(ended, u);
        insertActivity(u, "CHALLENGE_JOIN", ended);

        cancel(u, done)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHALLENGE_003"));
        cancel(u, ended)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHALLENGE_002"));

        assertThat(participantRows(done)).isEqualTo(1);
        assertThat(participantRows(ended)).isEqualTo(1);
        assertThat(activityCount(u, "CHALLENGE_JOIN", done)).isEqualTo(1);
        assertThat(activityCount(u, "CHALLENGE_JOIN", ended)).isEqualTo(1);
    }

    @Test
    void AC9_취소_뒤_다시_참여하면_시작일부터_다시_세고_JOIN을_새로_남긴다() throws Exception {
        long u = insertUser("f25u");
        long quiz = insertQuiz(insertAnalysis(u, "DONE", "2026-11-18 09:00:00", false));
        long c = insertChallenge("C", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 10);
        join(u, c).andExpect(jsonPath("$.participation.progress").value(0));
        cancel(u, c).andExpect(status().isOk());
        insertAttempt(quiz, u, "2026-11-21 10:00:00");

        join(u, c)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participation.progress").value(1));
        assertThat(activityCount(u, "CHALLENGE_JOIN", c)).isEqualTo(1);
    }

    @Test
    void AC9_없는_챌린지_취소는_404다() throws Exception {
        cancel(insertUser("f25u"), 999_999)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHALLENGE_404"));
    }
}
