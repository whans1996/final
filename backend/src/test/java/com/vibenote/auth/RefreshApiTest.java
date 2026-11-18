package com.vibenote.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.security.AuthUser;
import com.vibenote.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** F-02 토큰 재발급 (AC3, AC4, AC5). */
class RefreshApiTest extends AuthTestSupport {

    @Autowired JwtTokenProvider tokenProvider;

    private static void expectAuth002AndCookieCleared(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"))
                .andExpect(jsonPath("$.message").value("로그인이 만료되었습니다. 다시 로그인하세요."))
                .andExpect(cookie().maxAge(COOKIE, 0))
                .andExpect(cookie().path(COOKIE, "/api/auth"));
    }

    @Test
    void AC3_유효한_쿠키로_재발급하면_새_accessToken과_새_쿠키를_받고_옛_토큰은_지워진다() throws Exception {
        long id = createUser("user@example.com", "코딩초보");
        String oldToken = loginAndGetRefreshToken("user@example.com");

        MvcResult result =
                refresh(oldToken)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken").isString())
                        .andExpect(jsonPath("$.expiresIn").value(1800))
                        .andExpect(jsonPath("$.user.id").value(id))
                        .andExpect(cookie().httpOnly(COOKIE, true))
                        .andExpect(cookie().maxAge(COOKIE, 1209600))
                        .andReturn();

        String newToken = result.getResponse().getCookie(COOKIE).getValue();
        assertThat(newToken).isNotEqualTo(oldToken);
        assertThat(refreshRows(oldToken)).isZero();
        assertThat(refreshRows(newToken)).isEqualTo(1);
    }

    @Test
    void AC4_쿠키가_없으면_401_AUTH_002() throws Exception {
        expectAuth002AndCookieCleared(refresh(null));
    }

    @Test
    void AC4_DB에_없는_토큰이면_401_AUTH_002() throws Exception {
        expectAuth002AndCookieCleared(refresh("unknown-token-value"));
    }

    @Test
    void AC4_만료된_토큰이면_401_AUTH_002이고_행을_지운다() throws Exception {
        createUser("user@example.com", "코딩초보");
        String token = loginAndGetRefreshToken("user@example.com");
        jdbc.update(
                "UPDATE refresh_tokens SET expires_at = NOW() - INTERVAL 1 MINUTE"
                        + " WHERE token_hash = ?",
                sha256(token));

        expectAuth002AndCookieCleared(refresh(token));
        assertThat(refreshRows(token)).isZero();
    }

    @Test
    void AC4_이미_회전된_옛_토큰이면_401_AUTH_002() throws Exception {
        createUser("user@example.com", "코딩초보");
        String oldToken = loginAndGetRefreshToken("user@example.com");
        refresh(oldToken).andExpect(status().isOk());

        expectAuth002AndCookieCleared(refresh(oldToken));
    }

    @Test
    void AC5_탈퇴한_회원이면_401_AUTH_002이고_행을_지운다() throws Exception {
        long id = createUser("user@example.com", "코딩초보");
        String token = loginAndGetRefreshToken("user@example.com");
        jdbc.update("UPDATE users SET status = 'WITHDRAWN' WHERE id = ?", id);

        expectAuth002AndCookieCleared(refresh(token));
        assertThat(refreshRows(token)).isZero();
    }

    @Test
    void AC5_role이_바뀌면_새_accessToken에_DB의_role이_들어간다() throws Exception {
        long id = createUser("user@example.com", "코딩초보");
        String token = loginAndGetRefreshToken("user@example.com");
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", id);

        MvcResult result =
                refresh(token)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.user.role").value("ADMIN"))
                        .andReturn();

        String accessToken =
                objectMapper
                        .readTree(result.getResponse().getContentAsString())
                        .get("accessToken")
                        .asText();
        assertThat(tokenProvider.parse(accessToken)).contains(new AuthUser(id, "ADMIN"));
    }
}
