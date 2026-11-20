package com.vibenote.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** F-04 팔로우·언팔로우 (docs/specs/F-04-follow-feed.md AC1~AC3). 회원을 직접 넣고 롤백한다. */
@Transactional
class FollowApiTest extends IntegrationTestBase {

    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenProvider tokenProvider;

    private static final String FOLLOW = "/api/users/{id}/follow";

    private long insertUser(String name) {
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                name + "@f04.test",
                name);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long user() {
        return insertUser("f04u" + System.nanoTime() % 1_000_000_000L);
    }

    private void insertFollow(long follower, long following) {
        jdbc.update(
                "INSERT INTO follows (follower_id, following_id) VALUES (?, ?)",
                follower,
                following);
    }

    private void withdraw(long userId) {
        jdbc.update(
                "UPDATE users SET status = 'WITHDRAWN', withdrawn_at = NOW() WHERE id = ?", userId);
    }

    private int followRows(long follower, long following) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM follows WHERE follower_id = ? AND following_id = ?",
                Integer.class,
                follower,
                following);
    }

    private int allFollowRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM follows", Integer.class);
    }

    private String bearer(long userId) {
        return "Bearer " + tokenProvider.createAccessToken(userId, "USER");
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String authorization)
            throws Exception {
        if (authorization != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return mockMvc.perform(request);
    }

    // ── AC1 ──

    @Test
    void AC1_다른_회원을_팔로우하면_200과_팔로우_상태를_반환한다() throws Exception {
        long me = user();
        long target = user();
        long other = user();
        insertFollow(other, target); // target의 팔로워 1명
        insertFollow(target, other); // target의 팔로잉 1명

        perform(post(FOLLOW, target), bearer(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(true))
                .andExpect(jsonPath("$.followerCount").value(2))
                .andExpect(jsonPath("$.followingCount").value(1));

        assertThat(followRows(me, target)).isEqualTo(1);
    }

    @Test
    void AC1_이미_팔로우_중이면_같은_200이고_행은_하나다() throws Exception {
        long me = user();
        long target = user();
        insertFollow(me, target);

        perform(post(FOLLOW, target), bearer(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(true))
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followingCount").value(0));

        assertThat(followRows(me, target)).isEqualTo(1);
    }

    // ── AC2 ──

    @Test
    void AC2_팔로우_중인_회원을_언팔로우하면_200과_해제_상태를_반환한다() throws Exception {
        long me = user();
        long target = user();
        long other = user();
        insertFollow(me, target);
        insertFollow(other, target);

        perform(delete(FOLLOW, target), bearer(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false))
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followingCount").value(0));

        assertThat(followRows(me, target)).isZero();
        assertThat(followRows(other, target)).isEqualTo(1);
    }

    @Test
    void AC2_팔로우하지_않은_회원을_언팔로우해도_같은_200이다() throws Exception {
        long me = user();
        long target = user();

        perform(delete(FOLLOW, target), bearer(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false))
                .andExpect(jsonPath("$.followerCount").value(0))
                .andExpect(jsonPath("$.followingCount").value(0));
    }

    // ── AC3 ──

    @Test
    void AC3_자기_자신을_팔로우하거나_언팔로우하면_400_USER_002() throws Exception {
        long me = user();
        int before = allFollowRows();

        perform(post(FOLLOW, me), bearer(me))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_002"))
                .andExpect(jsonPath("$.message").value("자기 자신은 팔로우할 수 없습니다."));
        perform(delete(FOLLOW, me), bearer(me))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_002"));

        assertThat(allFollowRows()).isEqualTo(before);
    }

    @Test
    void AC3_없거나_탈퇴한_회원이면_404_USER_404() throws Exception {
        long me = user();
        long gone = user();
        insertFollow(me, gone); // 탈퇴 정리 전에 남은 행이 있어도 바꾸지 않는다
        withdraw(gone);
        long missing = gone + 100_000;
        int before = allFollowRows();

        for (long target : new long[] {gone, missing}) {
            perform(post(FOLLOW, target), bearer(me))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("USER_404"));
            perform(delete(FOLLOW, target), bearer(me))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("USER_404"));
        }

        assertThat(allFollowRows()).isEqualTo(before);
    }

    @Test
    void AC3_토큰이_없으면_401_AUTH_401() throws Exception {
        long target = user();
        int before = allFollowRows();

        perform(post(FOLLOW, target), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        perform(delete(FOLLOW, target), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));

        assertThat(allFollowRows()).isEqualTo(before);
    }

    @Test
    void AC3_탈퇴한_회원의_남은_토큰이면_401_AUTH_401() throws Exception {
        long me = user();
        long target = user();
        long followed = user();
        insertFollow(me, followed);
        withdraw(me);
        String token = bearer(me);
        int before = allFollowRows();

        perform(post(FOLLOW, target), token)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        perform(delete(FOLLOW, followed), token)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));

        assertThat(allFollowRows()).isEqualTo(before);
        assertThat(followRows(me, target)).isZero();
    }
}
