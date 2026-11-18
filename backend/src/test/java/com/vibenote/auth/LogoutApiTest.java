package com.vibenote.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/** F-02 로그아웃 (AC6). */
class LogoutApiTest extends AuthTestSupport {

    @Test
    void AC6_로그아웃하면_204_행_삭제_쿠키_삭제_이후_refresh는_AUTH_002() throws Exception {
        createUser("user@example.com", "코딩초보");
        String token = loginAndGetRefreshToken("user@example.com");

        logout(token)
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(COOKIE, 0))
                .andExpect(cookie().path(COOKIE, "/api/auth"));

        assertThat(refreshRows(token)).isZero();
        refresh(token)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    void AC6_쿠키_없이_로그아웃해도_204() throws Exception {
        logout(null).andExpect(status().isNoContent());
        logout("unknown-token-value").andExpect(status().isNoContent());
    }

    @Test
    void AC6_다른_기기의_세션은_남는다() throws Exception {
        createUser("user@example.com", "코딩초보");
        String pc = loginAndGetRefreshToken("user@example.com");
        String phone = loginAndGetRefreshToken("user@example.com");

        logout(pc).andExpect(status().isNoContent());

        assertThat(refreshRows(phone)).isEqualTo(1);
    }
}
