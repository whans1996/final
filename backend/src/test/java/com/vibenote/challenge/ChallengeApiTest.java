package com.vibenote.challenge;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/** F-25 AC1·AC2 회원용 챌린지 목록·상세 (공개). */
class ChallengeApiTest extends ChallengeTestSupport {

    static final String LIST = "/api/challenges";
    static final String ONE = "/api/challenges/{id}";

    // ── AC1 목록 ──

    @Test
    void AC1_상태별_목록은_상태마다_정해진_순서이고_참여자_수와_참여_여부가_있다() throws Exception {
        long c1 = insertChallenge("C1", "2026-11-01", "2026-11-10", "QUIZ_COUNT", 10);
        long c2 = insertChallenge("C2", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 10);
        long c3 = insertChallenge("C3", "2026-11-10", "2026-11-25", "REVIEW_COUNT", 5);
        long c4 = insertChallenge("C4", "2026-12-01", "2026-12-31", "ANALYSIS_COUNT", 3);
        long c5 = insertChallenge("C5", "2026-12-05", "2026-12-20", "QUIZ_COUNT", 3);
        long u = insertUser("f25u");
        insertParticipant(c2, u);
        insertParticipant(c2, insertUser("f25other"));

        mockMvc.perform(get(LIST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", contains((int) c3, (int) c2)))
                .andExpect(jsonPath("$.items[1].title").value("C2"))
                .andExpect(jsonPath("$.items[1].imageUrl").value(nullValue()))
                .andExpect(jsonPath("$.items[1].startDate").value("2026-11-20"))
                .andExpect(jsonPath("$.items[1].endDate").value("2026-11-30"))
                .andExpect(jsonPath("$.items[1].goalType").value("QUIZ_COUNT"))
                .andExpect(jsonPath("$.items[1].goalValue").value(10))
                .andExpect(jsonPath("$.items[1].status").value("ONGOING"))
                .andExpect(jsonPath("$.items[1].participantCount").value(2))
                .andExpect(jsonPath("$.items[1].joined").value(false))
                .andExpect(jsonPath("$.items[0].participantCount").value(0))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.total").value(2));

        mockMvc.perform(get(LIST).param("status", "UPCOMING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", contains((int) c4, (int) c5)))
                .andExpect(jsonPath("$.items[0].status").value("UPCOMING"));

        mockMvc.perform(get(LIST).param("status", "ENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", contains((int) c1)))
                .andExpect(jsonPath("$.items[0].status").value("ENDED"));

        mockMvc.perform(get(LIST).header(HttpHeaders.AUTHORIZATION, bearer(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", contains((int) c3, (int) c2)))
                .andExpect(jsonPath("$.items[0].joined").value(false))
                .andExpect(jsonPath("$.items[1].joined").value(true));
    }

    @Test
    void AC1_종료일이_같으면_id_역순이고_size로_페이지를_나눈다() throws Exception {
        long a = insertChallenge("A", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 10);
        long b = insertChallenge("B", "2026-11-21", "2026-11-30", "QUIZ_COUNT", 10);
        long c = insertChallenge("C", "2026-11-22", "2026-11-30", "QUIZ_COUNT", 10);

        mockMvc.perform(get(LIST))
                .andExpect(jsonPath("$.items[*].id", contains((int) c, (int) b, (int) a)));
        mockMvc.perform(get(LIST).param("page", "2").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", contains((int) a)))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.total").value(3));
        mockMvc.perform(get(LIST).param("size", "50")).andExpect(status().isOk());
    }

    @Test
    void AC1_잘못된_상태_페이지_크기는_400이다() throws Exception {
        mockMvc.perform(get(LIST).param("status", "DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.status").exists());
        mockMvc.perform(get(LIST).param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.page").exists());
        mockMvc.perform(get(LIST).param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.size").exists());
    }

    // ── AC2 상세 ──

    @Test
    void AC2_상세는_설명을_포함하고_참여한_회원에게만_참여_정보가_있다() throws Exception {
        long c = insertChallenge("C", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 10);
        long joined = insertUser("f25joined");
        long stranger = insertUser("f25stranger");
        insertParticipant(c, joined, 3, null, "2026-11-22 21:05:00");

        mockMvc.perform(get(ONE, c))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(c))
                .andExpect(jsonPath("$.title").value("C"))
                .andExpect(jsonPath("$.description").value("C 설명"))
                .andExpect(jsonPath("$.imageUrl").value(nullValue()))
                .andExpect(jsonPath("$.startDate").value("2026-11-20"))
                .andExpect(jsonPath("$.endDate").value("2026-11-30"))
                .andExpect(jsonPath("$.goalType").value("QUIZ_COUNT"))
                .andExpect(jsonPath("$.goalValue").value(10))
                .andExpect(jsonPath("$.status").value("ONGOING"))
                .andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.participation").value(nullValue()));

        mockMvc.perform(get(ONE, c).header(HttpHeaders.AUTHORIZATION, bearer(stranger)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participation").value(nullValue()));

        mockMvc.perform(get(ONE, c).header(HttpHeaders.AUTHORIZATION, bearer(joined)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participation.progress").value(3))
                .andExpect(jsonPath("$.participation.goalValue").value(10))
                .andExpect(jsonPath("$.participation.completedAt").value(nullValue()))
                .andExpect(jsonPath("$.participation.joinedAt").value("2026-11-22T21:05:00+09:00"));
    }

    @Test
    void AC2_없는_챌린지는_404다() throws Exception {
        mockMvc.perform(get(ONE, 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHALLENGE_404"));
    }
}
