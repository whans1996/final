package com.vibenote.follow;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtTokenProvider;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** F-04 팔로우 상태·목록 조회 (docs/specs/F-04-follow-feed.md AC4·AC5). 공개 API라 비로그인도 본다. */
@Transactional
class FollowQueryApiTest extends IntegrationTestBase {

    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenProvider tokenProvider;

    private static final String FOLLOW = "/api/users/{id}/follow";
    private static final String FOLLOWERS = "/api/users/{id}/followers";
    private static final String FOLLOWINGS = "/api/users/{id}/followings";
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 9, 0);

    private long insertUser(String name, String level) {
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname, level) VALUES (?, 'hash', ?,"
                        + " ?)",
                name + "@f04q.test",
                name,
                level);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long user() {
        return insertUser("f04q" + System.nanoTime() % 1_000_000_000L, "BEGINNER");
    }

    private void insertFollow(long follower, long following, LocalDateTime at) {
        jdbc.update(
                "INSERT INTO follows (follower_id, following_id, created_at) VALUES (?, ?, ?)",
                follower,
                following,
                at);
    }

    private void withdraw(long userId) {
        jdbc.update(
                "UPDATE users SET status = 'WITHDRAWN', withdrawn_at = NOW() WHERE id = ?", userId);
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

    // ── AC4 ──

    @Test
    void AC4_팔로우_중인_회원이_상태를_보면_following_true와_수를_반환한다() throws Exception {
        long me = user();
        long target = user();
        long other = user();
        insertFollow(me, target, BASE);
        insertFollow(other, target, BASE);
        insertFollow(target, other, BASE);

        perform(get(FOLLOW, target), bearer(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(true))
                .andExpect(jsonPath("$.followerCount").value(2))
                .andExpect(jsonPath("$.followingCount").value(1));
    }

    @Test
    void AC4_비로그인_팔로우하지_않음_내_프로필이면_following_false다() throws Exception {
        long me = user();
        long target = user();
        insertFollow(target, me, BASE);

        perform(get(FOLLOW, target), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false))
                .andExpect(jsonPath("$.followerCount").value(0))
                .andExpect(jsonPath("$.followingCount").value(1));
        perform(get(FOLLOW, target), bearer(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false));
        perform(get(FOLLOW, me), bearer(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false))
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followingCount").value(0));
    }

    @Test
    void AC4_없거나_탈퇴한_회원이면_404_USER_404() throws Exception {
        long gone = user();
        withdraw(gone);

        for (long target : new long[] {gone, gone + 100_000}) {
            perform(get(FOLLOW, target), null)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("USER_404"));
        }
    }

    // ── AC5 ──

    @Test
    void AC5_팔로워_목록은_팔로우한_시각_최신순이고_같으면_회원_id_내림차순이다() throws Exception {
        long target = user();
        long a = insertUser("f04qa" + System.nanoTime() % 100000, "JUNIOR");
        long b = user();
        long c = user();
        insertFollow(a, target, BASE.plusHours(2));
        insertFollow(b, target, BASE);
        insertFollow(c, target, BASE);
        insertFollow(target, a, BASE); // 팔로잉은 팔로워 목록에 섞이지 않는다

        perform(get(FOLLOWERS, target), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains((int) a, (int) c, (int) b)))
                .andExpect(jsonPath("$.items[0].nickname").exists())
                .andExpect(jsonPath("$.items[0].level").value("JUNIOR"))
                .andExpect(jsonPath("$.items[0].followedAt").value("2026-01-01T11:00:00+09:00"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void AC5_팔로잉_목록은_내가_팔로우한_사람을_페이지로_준다() throws Exception {
        long target = user();
        long a = user();
        long b = user();
        long c = user();
        insertFollow(target, a, BASE);
        insertFollow(target, b, BASE.plusMinutes(1));
        insertFollow(target, c, BASE.plusMinutes(2));
        insertFollow(a, target, BASE); // 팔로워는 팔로잉 목록에 섞이지 않는다

        perform(get(FOLLOWINGS, target).param("page", "2").param("size", "2"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains((int) a)))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void AC5_목록이_없으면_빈_페이지다() throws Exception {
        long target = user();

        perform(get(FOLLOWERS, target), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(empty()))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void AC5_없거나_탈퇴한_회원이면_404_USER_404() throws Exception {
        long gone = user();
        withdraw(gone);

        for (String path : new String[] {FOLLOWERS, FOLLOWINGS}) {
            perform(get(path, gone), null)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("USER_404"));
            perform(get(path, gone + 100_000), null)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("USER_404"));
        }
    }

    @Test
    void AC5_page_size가_범위_밖이면_400_COMMON_400() throws Exception {
        long target = user();

        perform(get(FOLLOWERS, target).param("page", "0"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        perform(get(FOLLOWINGS, target).param("size", "101"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }
}
