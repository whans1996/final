package com.vibenote.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;

/** NT-1 알림 API (AC1~AC5). 행을 직접 넣고 롤백한다. */
@Transactional
class NotificationControllerTest extends NotificationTestSupport {

    private static final String LIST = "/api/notifications";
    private static final String COUNT = "/api/notifications/unread-count";
    private static final String READ = "/api/notifications/{id}/read";
    private static final String READ_ALL = "/api/notifications/read-all";

    // ── AC1 ──

    @Test
    void AC1_내_알림만_최신순으로_돌려준다() throws Exception {
        long a = insertUser("nta");
        long b = insertUser("ntb");
        long w = insertUser("ntw");
        LocalDateTime t = FIXED_NOW.minusHours(1);
        long older = insertNotification(a, "FOLLOW", w, "회원님을 팔로우했어요", "/users/" + w, t, t);
        long sameTimeLow =
                insertNotification(
                        a,
                        "COMMENT",
                        w,
                        "회원님의 글에 댓글을 남겼어요 · 글",
                        "/community/3",
                        t.plusMinutes(5),
                        null);
        long sameTimeHigh =
                insertNotification(
                        a,
                        "CHALLENGE_COMPLETE",
                        null,
                        "챌린지를 달성했어요 · C",
                        "/challenges/2",
                        t.plusMinutes(5),
                        null);
        insertNotification(b, t.plusMinutes(10), null);

        mockMvc.perform(get(LIST).header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].id").value(sameTimeHigh))
                .andExpect(jsonPath("$.items[0].type").value("CHALLENGE_COMPLETE"))
                .andExpect(jsonPath("$.items[0].actor").isEmpty())
                .andExpect(jsonPath("$.items[0].read").value(false))
                .andExpect(jsonPath("$.items[0].link").value("/challenges/2"))
                .andExpect(jsonPath("$.items[1].id").value(sameTimeLow))
                .andExpect(jsonPath("$.items[1].actor.id").value(w))
                .andExpect(jsonPath("$.items[1].actor.nickname").value(nickname(w)))
                .andExpect(jsonPath("$.items[1].message").value("회원님의 글에 댓글을 남겼어요 · 글"))
                .andExpect(jsonPath("$.items[1].createdAt").value("2026-11-29T11:05:00+09:00"))
                .andExpect(jsonPath("$.items[2].id").value(older))
                .andExpect(jsonPath("$.items[2].read").value(true));
    }

    @Test
    void AC1_actor가_탈퇴했으면_마스킹_닉네임을_보인다() throws Exception {
        long a = insertUser("nta");
        long w = insertUser("ntw");
        insertNotification(a, "FOLLOW", w, "회원님을 팔로우했어요", "/users/" + w, FIXED_NOW, null);
        withdrawRow(w);

        mockMvc.perform(get(LIST).header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].actor.id").value(w))
                .andExpect(jsonPath("$.items[0].actor.nickname").value("#탈퇴" + w));
    }

    @Test
    void AC1_페이지와_범위_밖_값() throws Exception {
        long a = insertUser("nta");
        for (int i = 0; i < 3; i++) {
            insertNotification(a, FIXED_NOW.minusMinutes(i), null);
        }

        mockMvc.perform(
                        get(LIST)
                                .param("page", "2")
                                .param("size", "2")
                                .header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items.length()").value(1));
        mockMvc.perform(get(LIST).param("size", "101").header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        mockMvc.perform(get(LIST).param("page", "0").header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    // ── AC2 ──

    @Test
    void AC2_안_읽은_알림_수를_돌려준다() throws Exception {
        long a = insertUser("nta");
        long b = insertUser("ntb");
        // 0 확인은 다른 회원으로 한다. 같은 트랜잭션에서 같은 조회를 다시 하면 MyBatis 1차 캐시가 JdbcTemplate으로 넣은 행을 못 본다
        long empty = insertUser("nte");
        mockMvc.perform(get(COUNT).header(HttpHeaders.AUTHORIZATION, bearer(empty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        insertNotification(a, FIXED_NOW, null);
        insertNotification(a, FIXED_NOW, null);
        insertNotification(a, FIXED_NOW, FIXED_NOW);
        insertNotification(b, FIXED_NOW, null);

        mockMvc.perform(get(COUNT).header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    // ── AC3 ──

    @Test
    void AC3_읽음_처리는_멱등이고_처음_읽은_시각을_지킨다() throws Exception {
        long a = insertUser("nta");
        long n = insertNotification(a, FIXED_NOW.minusDays(1), null);
        long read = insertNotification(a, FIXED_NOW.minusDays(1), FIXED_NOW.minusHours(3));

        mockMvc.perform(patch(READ, n).header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isNoContent());
        assertThat(readAt(n)).isEqualTo(FIXED_NOW);
        mockMvc.perform(get(COUNT).header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(jsonPath("$.count").value(0));

        mockMvc.perform(patch(READ, read).header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isNoContent());
        assertThat(readAt(read)).isEqualTo(FIXED_NOW.minusHours(3));
    }

    // ── AC4 ──

    @Test
    void AC4_모두_읽음은_내_알림만_바꾼다() throws Exception {
        long a = insertUser("nta");
        long b = insertUser("ntb");
        for (int i = 0; i < 3; i++) {
            insertNotification(a, FIXED_NOW.minusMinutes(i), null);
        }
        long others = insertNotification(b, FIXED_NOW, null);

        mockMvc.perform(patch(READ_ALL).header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(COUNT).header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(jsonPath("$.count").value(0));
        assertThat(readAt(others)).isNull();

        mockMvc.perform(patch(READ_ALL).header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isNoContent());
    }

    // ── AC5 ──

    @Test
    void AC5_남의_알림과_없는_알림은_404이고_바뀌지_않는다() throws Exception {
        long a = insertUser("nta");
        long b = insertUser("ntb");
        long others = insertNotification(b, FIXED_NOW, null);

        mockMvc.perform(patch(READ, others).header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_404"));
        mockMvc.perform(patch(READ, others + 100_000).header(HttpHeaders.AUTHORIZATION, bearer(a)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_404"));
        assertThat(readAt(others)).isNull();
    }

    @Test
    void AC5_토큰이_없으면_401() throws Exception {
        long a = insertUser("nta");
        long n = insertNotification(a, FIXED_NOW, null);

        mockMvc.perform(get(LIST))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        mockMvc.perform(get(COUNT)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(READ, n)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(READ_ALL)).andExpect(status().isUnauthorized());
        assertThat(readAt(n)).isNull();
    }

    @Test
    void AC5_탈퇴한_회원의_남은_토큰은_401이고_바뀌지_않는다() throws Exception {
        long a = insertUser("nta");
        long n = insertNotification(a, FIXED_NOW, null);
        String token = bearer(a);
        withdrawRow(a);

        mockMvc.perform(patch(READ, n).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        mockMvc.perform(patch(READ_ALL).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        assertThat(readAt(n)).isNull();
    }
}
