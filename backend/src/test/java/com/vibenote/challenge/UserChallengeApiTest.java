package com.vibenote.challenge;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

/** F-25 AC10·AC11 내·회원의 참여 챌린지 목록. */
class UserChallengeApiTest extends ChallengeTestSupport {

    static final String MINE = "/api/users/me/challenges";
    static final String OF_USER = "/api/users/{id}/challenges";

    long u;
    long ongoingDone;
    long ongoing;
    long upcoming;
    long ended;

    /** 진행 중 2개(하나 달성), 예정 1개, 종료 1개. 참여 시각은 ended → ongoingDone → upcoming → ongoing 순이다. */
    void joinFour() {
        u = insertUser("f25u");
        ongoingDone = insertChallenge("달성", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 2);
        ongoing = insertChallenge("진행", "2026-11-10", "2026-11-25", "REVIEW_COUNT", 5);
        upcoming = insertChallenge("예정", "2026-12-01", "2026-12-31", "ANALYSIS_COUNT", 3);
        ended = insertChallenge("종료", "2026-11-01", "2026-11-10", "QUIZ_COUNT", 10);
        insertParticipant(ended, u, 4, null, "2026-11-02 09:00:00");
        insertParticipant(ongoingDone, u, 2, "2026-11-22 12:30:00", "2026-11-20 09:00:00");
        insertParticipant(upcoming, u, 0, null, "2026-11-21 09:00:00");
        insertParticipant(ongoing, u, 1, null, "2026-11-23 09:00:00");
    }

    ResultActions mine(String... params) throws Exception {
        var request = get(MINE).header(HttpHeaders.AUTHORIZATION, bearer(u));
        for (int i = 0; i < params.length; i += 2) request.param(params[i], params[i + 1]);
        return mockMvc.perform(request);
    }

    // ── AC10 내 챌린지 ──

    @Test
    void AC10_내_챌린지는_참여_시각_최신순이고_상태로_거르며_달성도가_있다() throws Exception {
        joinFour();

        mine().andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.items[*].id",
                                contains(
                                        (int) ongoing,
                                        (int) upcoming,
                                        (int) ongoingDone,
                                        (int) ended)))
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.items[2].title").value("달성"))
                .andExpect(jsonPath("$.items[2].imageUrl").value(nullValue()))
                .andExpect(jsonPath("$.items[2].startDate").value("2026-11-20"))
                .andExpect(jsonPath("$.items[2].endDate").value("2026-11-30"))
                .andExpect(jsonPath("$.items[2].goalType").value("QUIZ_COUNT"))
                .andExpect(jsonPath("$.items[2].goalValue").value(2))
                .andExpect(jsonPath("$.items[2].status").value("ONGOING"))
                .andExpect(jsonPath("$.items[2].progress").value(2))
                .andExpect(jsonPath("$.items[2].completedAt").value("2026-11-22T12:30:00+09:00"))
                .andExpect(jsonPath("$.items[2].joinedAt").value("2026-11-20T09:00:00+09:00"))
                .andExpect(jsonPath("$.items[0].completedAt").value(nullValue()))
                .andExpect(jsonPath("$.items[3].status").value("ENDED"));

        mine("status", "ONGOING")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", contains((int) ongoing, (int) ongoingDone)))
                .andExpect(jsonPath("$.total").value(2));
        mine("status", "UPCOMING").andExpect(jsonPath("$.items[*].id", contains((int) upcoming)));
        mine("status", "ENDED").andExpect(jsonPath("$.items[*].id", contains((int) ended)));
        mine("page", "2", "size", "3")
                .andExpect(jsonPath("$.items[*].id", contains((int) ended)))
                .andExpect(jsonPath("$.total").value(4));
    }

    @Test
    void AC10_비로그인은_401이고_잘못된_조건은_400이다() throws Exception {
        u = insertUser("f25u");
        mockMvc.perform(get(MINE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        mine("status", "MY")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.status").exists());
        mine("page", "0").andExpect(status().isBadRequest());
        mine("size", "51").andExpect(status().isBadRequest());
    }

    // ── AC11 회원의 챌린지 (공개) ──

    @Test
    void AC11_비로그인도_회원의_참여_챌린지를_본다() throws Exception {
        joinFour();
        insertParticipant(ongoing, insertUser("f25other")); // 다른 회원의 참여는 섞이지 않는다

        mockMvc.perform(get(OF_USER, u))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.items[*].id",
                                contains(
                                        (int) ongoing,
                                        (int) upcoming,
                                        (int) ongoingDone,
                                        (int) ended)))
                .andExpect(jsonPath("$.items[0].progress").value(1))
                .andExpect(jsonPath("$.items[2].completedAt").value("2026-11-22T12:30:00+09:00"));
        mockMvc.perform(get(OF_USER, u).param("status", "ENDED"))
                .andExpect(jsonPath("$.items[*].id", contains((int) ended)));
    }

    @Test
    void AC11_없는_회원과_탈퇴한_회원은_404다() throws Exception {
        long gone = insertUser("f25gone");
        jdbc.update("UPDATE users SET status = 'WITHDRAWN' WHERE id = ?", gone);

        mockMvc.perform(get(OF_USER, 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_404"));
        mockMvc.perform(get(OF_USER, gone))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_404"));
        mockMvc.perform(get(OF_USER, 999_999).param("status", "BAD"))
                .andExpect(status().isNotFound());
    }
}
