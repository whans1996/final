package com.vibenote.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.MvcResult;

/** F-02 로그인 (docs/specs/F-02-login-jwt.md AC1, AC2). */
class LoginApiTest extends AuthTestSupport {

    @Test
    void AC1_로그인하면_accessToken과_회원정보_HttpOnly_refresh_쿠키를_받고_해시가_저장된다() throws Exception {
        long id = createUser("user@example.com", "코딩초보");

        MvcResult result =
                login(" User@Example.com ", PASSWORD)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken").isString())
                        .andExpect(jsonPath("$.expiresIn").value(1800))
                        .andExpect(jsonPath("$.user.id").value(id))
                        .andExpect(jsonPath("$.user.nickname").value("코딩초보"))
                        .andExpect(jsonPath("$.user.level").value("BEGINNER"))
                        .andExpect(jsonPath("$.user.role").value("USER"))
                        .andExpect(jsonPath("$.refreshToken").doesNotExist())
                        .andExpect(cookie().httpOnly(COOKIE, true))
                        .andExpect(cookie().path(COOKIE, "/api/auth"))
                        .andExpect(cookie().maxAge(COOKIE, 1209600))
                        .andExpect(cookie().sameSite(COOKIE, "Strict"))
                        .andReturn();

        String refreshToken = result.getResponse().getCookie(COOKIE).getValue();
        assertThat(refreshToken).hasSizeGreaterThanOrEqualTo(43);
        assertThat(refreshRows(refreshToken)).isEqualTo(1);
        Long days =
                jdbc.queryForObject(
                        "SELECT TIMESTAMPDIFF(HOUR, NOW(), expires_at) FROM refresh_tokens"
                                + " WHERE token_hash = ?",
                        Long.class,
                        sha256(refreshToken));
        assertThat(days).isBetween(14 * 24L - 1, 14 * 24L);

        // 받은 Access 토큰으로 보호된 주소의 인증을 통과한다 (없는 주소라 404)
        String accessToken =
                objectMapper
                        .readTree(result.getResponse().getContentAsString())
                        .get("accessToken")
                        .asText();
        mockMvc.perform(get("/api/__probe__").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    static Stream<Arguments> wrongCredentials() {
        return Stream.of(
                Arguments.of("nobody@example.com", PASSWORD, false), // 없는 이메일
                Arguments.of("user@example.com", "wrong1234", false), // 틀린 비밀번호
                Arguments.of("user@example.com", PASSWORD, true)); // 탈퇴 회원
    }

    @ParameterizedTest
    @MethodSource("wrongCredentials")
    void AC2_로그인_실패는_모두_401_AUTH_001_같은_메시지_쿠키없음(String email, String password, boolean withdrawn)
            throws Exception {
        long id = createUser("user@example.com", "코딩초보");
        if (withdrawn) {
            jdbc.update("UPDATE users SET status = 'WITHDRAWN' WHERE id = ?", id);
        }

        login(email, password)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."))
                .andExpect(cookie().doesNotExist(COOKIE));

        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                                Integer.class,
                                id))
                .isZero();
    }

    @Test
    void AC2_email이나_password가_비면_400_COMMON_400() throws Exception {
        login("", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.email").isString())
                .andExpect(jsonPath("$.details.password").isString());
    }
}
