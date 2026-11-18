package com.vibenote.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.security.AuthUser;
import com.vibenote.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    /** 없거나 이미 회전된 토큰: 다른 탭이 먼저 회전해 받은 새 쿠키를 지우지 않도록 삭제 헤더를 보내지 않는다. */
    private static void expectAuth002AndCookieKept(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void AC4_DB에_없는_토큰이면_401_AUTH_002이고_쿠키는_지우지_않는다() throws Exception {
        expectAuth002AndCookieKept(refresh("unknown-token-value"));
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
    void AC4_이미_회전된_옛_토큰이면_401_AUTH_002이고_쿠키는_지우지_않는다() throws Exception {
        createUser("user@example.com", "코딩초보");
        String oldToken = loginAndGetRefreshToken("user@example.com");
        refresh(oldToken).andExpect(status().isOk());

        expectAuth002AndCookieKept(refresh(oldToken));
    }

    /** 테스트 트랜잭션 밖에서 돌려, 실패 응답(AUTH_002) 뒤에도 행 삭제가 실제로 커밋되는지 본다. */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void AC4_만료_토큰_삭제는_실패_응답이어도_커밋된다() throws Exception {
        String email = "commit-check@example.com";
        long id = createUser(email, "커밋확인");
        try {
            String token = loginAndGetRefreshToken(email);
            jdbc.update(
                    "UPDATE refresh_tokens SET expires_at = NOW() - INTERVAL 1 MINUTE"
                            + " WHERE token_hash = ?",
                    sha256(token));

            expectAuth002AndCookieCleared(refresh(token));
            assertThat(refreshRows(token)).isZero();
        } finally {
            jdbc.update("DELETE FROM refresh_tokens WHERE user_id = ?", id);
            jdbc.update("DELETE FROM users WHERE id = ?", id);
        }
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
