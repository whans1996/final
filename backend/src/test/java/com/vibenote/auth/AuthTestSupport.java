package com.vibenote.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.user.UserService;
import com.vibenote.user.dto.SignupRequest;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** F-02 통합 테스트 공통: 회원 만들기, 로그인, refresh 쿠키 꺼내기, DB의 refresh 행 확인. */
@Transactional
abstract class AuthTestSupport extends IntegrationTestBase {

    static final String COOKIE = "refresh_token";
    static final String PASSWORD = "vibe1234";

    @Autowired UserService userService;
    @Autowired JdbcTemplate jdbc;

    long createUser(String email, String nickname) {
        return userService.signup(new SignupRequest(email, PASSWORD, nickname, "BEGINNER"));
    }

    ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        Map.of("email", email, "password", password))));
    }

    /** 로그인하고 받은 refresh 토큰 원문. */
    String loginAndGetRefreshToken(String email) throws Exception {
        MvcResult result = login(email, PASSWORD).andReturn();
        return result.getResponse().getCookie(COOKIE).getValue();
    }

    ResultActions refresh(String token) throws Exception {
        var request = post("/api/auth/refresh");
        if (token != null) {
            request.cookie(new Cookie(COOKIE, token));
        }
        return mockMvc.perform(request);
    }

    ResultActions logout(String token) throws Exception {
        var request = post("/api/auth/logout");
        if (token != null) {
            request.cookie(new Cookie(COOKIE, token));
        }
        return mockMvc.perform(request);
    }

    int refreshRows(String token) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE token_hash = ?",
                Integer.class,
                sha256(token));
    }

    static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
