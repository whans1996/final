package com.vibenote.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/** NT-1 AC12 챌린지 달성 알림. 오늘은 2026-11-24다 (ChallengeTestSupport). */
class ChallengeNotificationTest extends ChallengeTestSupport {

    private List<Map<String, Object>> notifications(long userId) {
        return jdbc.queryForList(
                "SELECT type, actor_id, message, link FROM notifications WHERE recipient_id = ?",
                userId);
    }

    @Test
    void AC12_목표를_처음_달성하면_챌린지_달성_알림_하나를_남긴다() {
        long u = insertUser("f25u");
        long c = insertChallenge("11월 복습 10회", "2026-11-20", "2026-11-30", "ANALYSIS_COUNT", 1);
        insertParticipant(c, u);
        insertAnalysis(u, "DONE", "2026-11-23 09:00:00", false);

        progressService.recountOpen(c, u);
        progressService.recountOpen(c, u);

        assertThat(notifications(u))
                .singleElement()
                .satisfies(
                        n -> {
                            assertThat(n.get("type")).isEqualTo("CHALLENGE_COMPLETE");
                            assertThat(n.get("actor_id")).isNull();
                            assertThat(n.get("message")).isEqualTo("챌린지를 달성했어요 · 11월 복습 10회");
                            assertThat(n.get("link")).isEqualTo("/challenges/" + c);
                        });
    }

    @Test
    void AC12_참여하자마자_달성해도_알림을_남긴다() throws Exception {
        long u = insertUser("f25u");
        long c = insertChallenge("바로 달성", "2026-11-20", "2026-11-30", "ANALYSIS_COUNT", 1);
        insertAnalysis(u, "DONE", "2026-11-23 09:00:00", false);

        mockMvc.perform(
                        post("/api/challenges/{id}/participation", c)
                                .header(HttpHeaders.AUTHORIZATION, bearer(u)))
                .andExpect(status().is2xxSuccessful());

        assertThat(notifications(u))
                .singleElement()
                .extracting(n -> n.get("message"))
                .isEqualTo("챌린지를 달성했어요 · 바로 달성");
    }

    @Test
    void AC12_목표에_못_미치면_알림이_없다() {
        long u = insertUser("f25u");
        long c = insertChallenge("C", "2026-11-20", "2026-11-30", "ANALYSIS_COUNT", 2);
        insertParticipant(c, u);
        insertAnalysis(u, "DONE", "2026-11-23 09:00:00", false);

        progressService.recountOpen(c, u);

        assertThat(notifications(u)).isEmpty();
    }
}
