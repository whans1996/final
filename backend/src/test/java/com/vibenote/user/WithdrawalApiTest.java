package com.vibenote.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.auth.RefreshTokenMapper;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** F-03 회원 탈퇴 (docs/specs/F-03-profile.md AC6, AC7, AC8). */
class WithdrawalApiTest extends UserTestSupport {

    private static final String COOKIE = "refresh_token";

    @Autowired RefreshTokenMapper refreshTokenMapper;

    private ResultActions withdraw(long userId, String json) throws Exception {
        return withdraw(bearer(userId), json);
    }

    private ResultActions withdraw(String authorization, String json) throws Exception {
        return mockMvc.perform(
                delete("/api/users/me")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        Map.of("email", email, "password", PASSWORD))));
    }

    /** 로그인해 refresh_tokens 행을 하나 만들고 쿠키 값을 돌려준다 (기기 하나). */
    private String loginOnDevice(String email) throws Exception {
        return login(email).andReturn().getResponse().getCookie(COOKIE).getValue();
    }

    private void follow(long follower, long following) {
        jdbc.update(
                "INSERT INTO follows (follower_id, following_id) VALUES (?, ?)",
                follower,
                following);
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @Test
    void AC6_맞는_비밀번호면_204_쿠키삭제_마스킹_토큰_팔로우_정리_작성물은_남는다() throws Exception {
        long me = createUser("user@example.com", "코딩초보");
        long b = createUser("b@example.com", "회원비");
        long c = createUser("c@example.com", "회원씨");
        jdbc.update("UPDATE users SET bio = '스프링 공부 중' WHERE id = ?", me);
        loginOnDevice("user@example.com");
        loginOnDevice("user@example.com");
        loginOnDevice("b@example.com");
        follow(me, b);
        follow(c, me);
        follow(b, c);
        jdbc.update(
                "INSERT INTO posts (user_id, board_type, title, content) VALUES (?, 'FREE', '제목',"
                        + " '본문')",
                me);

        withdraw(me, "{\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isNoContent())
                .andExpect(cookie().value(COOKIE, ""))
                .andExpect(cookie().maxAge(COOKIE, 0))
                .andExpect(cookie().path(COOKIE, "/api/auth"));

        assertThat(column("status", me)).isEqualTo("WITHDRAWN");
        assertThat(column("withdrawn_at", me)).isNotNull();
        assertThat(column("email", me)).isEqualTo("withdrawn:" + me);
        assertThat(column("nickname", me)).isEqualTo("#탈퇴" + me);
        assertThat(column("bio", me)).isNull();
        assertThat(column("password_hash", me)).doesNotStartWith("$2");

        // 내 refresh 토큰은 전부, 팔로우는 양방향 모두 지운다. 다른 회원의 것은 남는다
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", me)).isZero();
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", b)).isEqualTo(1);
        assertThat(
                        count(
                                "SELECT COUNT(*) FROM follows WHERE follower_id = ? OR following_id"
                                        + " = ?",
                                me,
                                me))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM follows WHERE follower_id = ?", b)).isEqualTo(1);
        // 작성물은 남는다 (users 행도 남는다)
        assertThat(count("SELECT COUNT(*) FROM posts WHERE user_id = ?", me)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ?", me)).isEqualTo(1);
    }

    @Test
    void AC7_비밀번호가_없으면_400_COMMON_400() throws Exception {
        long me = createUser("user@example.com", "코딩초보");

        withdraw(me, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.password").value("비밀번호를 입력하세요."));

        assertThat(column("status", me)).isEqualTo("ACTIVE");
    }

    @Test
    void AC7_비밀번호가_틀리면_400_USER_001이고_아무것도_바뀌지_않는다() throws Exception {
        long me = createUser("user@example.com", "코딩초보");
        long b = createUser("b@example.com", "회원비");
        loginOnDevice("user@example.com");
        follow(me, b);

        withdraw(me, "{\"password\":\"wrong1234\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.details.password").value("비밀번호가 올바르지 않습니다."))
                .andExpect(cookie().doesNotExist(COOKIE));

        assertThat(column("status", me)).isEqualTo("ACTIVE");
        assertThat(column("email", me)).isEqualTo("user@example.com");
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", me)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM follows WHERE follower_id = ?", me)).isEqualTo(1);
    }

    @Test
    void AC7_BCrypt_72바이트를_넘는_비밀번호도_500이_아니라_USER_001() throws Exception {
        long me = createUser("user@example.com", "코딩초보");

        withdraw(me, "{\"password\":\"" + "비밀번호".repeat(10) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_001"));
    }

    @Test
    void AC8_탈퇴_뒤_로그인_refresh_남은_Access_토큰은_거부되고_같은_정보로_재가입할_수_있다() throws Exception {
        long me = createUser("user@example.com", "코딩초보");
        String otherDevice = loginOnDevice("user@example.com");
        String accessToken = bearer(me);

        withdraw(accessToken, "{\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isNoContent());

        login("user@example.com")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, otherDevice)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"));

        mockMvc.perform(get("/api/users/me").header("Authorization", accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        mockMvc.perform(
                        patch("/api/users/me")
                                .header("Authorization", accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"level\":\"SENIOR\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        withdraw(accessToken, "{\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));

        mockMvc.perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "email", "user@example.com",
                                                        "password", PASSWORD,
                                                        "nickname", "코딩초보",
                                                        "level", "BEGINNER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not((int) me)));
    }

    @Test
    void AC8_탈퇴한_회원에게는_refresh_토큰_행을_만들지_않는다() {
        // 로그인이 회원을 읽은 뒤 탈퇴가 커밋되는 경합 방어: INSERT가 users.status를 잠금 읽기로 다시 확인한다
        long me = createUser("user@example.com", "코딩초보");
        long other = createUser("other@example.com", "다른사람");
        userService.markWithdrawn(me);

        assertThat(refreshTokenMapper.insert(me, "a".repeat(64), 60)).isZero();
        assertThat(refreshTokenMapper.insert(other, "b".repeat(64), 60)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", me)).isZero();
    }

    // ── F-20a AC10: 회원 탈퇴 시 팀 정리 ──

    private long team(long ownerId, String code) {
        long teamId =
                insert(
                        "INSERT INTO teams (name, owner_id, invite_code) VALUES ('팀', ?, ?)",
                        ownerId,
                        code);
        teamMember(teamId, ownerId, "OWNER");
        return teamId;
    }

    private void teamMember(long teamId, long userId, String role) {
        jdbc.update(
                "INSERT INTO team_members (team_id, user_id, role) VALUES (?, ?, ?)",
                teamId,
                userId,
                role);
    }

    @Test
    void AC10_F20a_탈퇴하면_팀장인_팀은_삭제되고_멤버_행이_지워진다() throws Exception {
        long me = createUser("user@example.com", "코딩초보");
        long v = createUser("v@example.com", "회원브이");
        long w = createUser("w@example.com", "회원더블유");
        long t1 = team(me, "WDRW0001");
        teamMember(t1, v, "MEMBER");
        long t2 = team(w, "WDRW0002");
        teamMember(t2, me, "MEMBER");
        teamMember(t2, v, "MEMBER");

        withdraw(me, "{\"password\":\"" + PASSWORD + "\"}").andExpect(status().isNoContent());

        assertThat(count("SELECT COUNT(*) FROM teams WHERE id = ?", t1)).isZero();
        assertThat(count("SELECT COUNT(*) FROM team_members WHERE user_id = ?", me)).isZero();
        assertThat(count("SELECT COUNT(*) FROM team_members WHERE team_id = ?", t2)).isEqualTo(2);
        mockMvc.perform(get("/api/users/me/teams").header("Authorization", bearer(v)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(t2))
                .andExpect(jsonPath("$[0].memberCount").value(2));
    }

    @Test
    void AC10_F20a_비밀번호가_틀려_탈퇴에_실패하면_팀과_멤버는_그대로다() throws Exception {
        long me = createUser("user@example.com", "코딩초보");
        long v = createUser("v@example.com", "회원브이");
        long t1 = team(me, "WDRW0001");
        teamMember(t1, v, "MEMBER");

        withdraw(me, "{\"password\":\"wrong1234\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_001"));

        assertThat(count("SELECT COUNT(*) FROM teams WHERE id = ?", t1)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM team_members WHERE team_id = ?", t1)).isEqualTo(2);
    }
}
