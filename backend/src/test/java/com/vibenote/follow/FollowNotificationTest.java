package com.vibenote.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtTokenProvider;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** NT-1 AC11 팔로우 알림. */
@Transactional
class FollowNotificationTest extends IntegrationTestBase {

    private static final String FOLLOW = "/api/users/{id}/follow";

    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenProvider tokenProvider;

    private long user() {
        String name = "nt1f" + System.nanoTime() % 1_000_000_000L;
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                name + "@nt1.test",
                name);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private List<Map<String, Object>> notifications(long userId) {
        return jdbc.queryForList(
                "SELECT id, type, actor_id, message, link FROM notifications WHERE recipient_id ="
                        + " ?",
                userId);
    }

    private void follow(long target, long me) throws Exception {
        mockMvc.perform(
                        post(FOLLOW, target)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenProvider.createAccessToken(me, "USER")))
                .andExpect(status().isOk());
    }

    private void unfollow(long target, long me) throws Exception {
        mockMvc.perform(
                        delete(FOLLOW, target)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenProvider.createAccessToken(me, "USER")))
                .andExpect(status().isOk());
    }

    @Test
    void AC11_처음_팔로우하면_대상에게_알리고_이미_팔로우_중이면_알리지_않는다() throws Exception {
        long a = user();
        long f = user();

        follow(a, f);
        follow(a, f);

        assertThat(notifications(a))
                .singleElement()
                .satisfies(
                        n -> {
                            assertThat(n.get("type")).isEqualTo("FOLLOW");
                            assertThat(((Number) n.get("actor_id")).longValue()).isEqualTo(f);
                            assertThat(n.get("message")).isEqualTo("회원님을 팔로우했어요");
                            assertThat(n.get("link")).isEqualTo("/users/" + f);
                        });
    }

    @Test
    void AC11_언팔로우_뒤_다시_팔로우해도_안_읽은_알림이_있으면_쌓이지_않는다() throws Exception {
        long a = user();
        long f = user();

        follow(a, f);
        unfollow(a, f);
        follow(a, f);
        assertThat(notifications(a)).hasSize(1);

        jdbc.update("UPDATE notifications SET read_at = NOW() WHERE recipient_id = ?", a);
        unfollow(a, f);
        follow(a, f);
        assertThat(notifications(a)).hasSize(2);
    }
}
