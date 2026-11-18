package com.vibenote.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.IntegrationTestBase;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** F-01 회원가입 API (docs/specs/F-01-signup.md AC1~AC5). */
@Transactional
class SignupApiTest extends IntegrationTestBase {

    private static final String URL = "/api/auth/signup";

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;

    private ResultActions signup(Map<String, Object> body) throws Exception {
        return mockMvc.perform(
                post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)));
    }

    private static Map<String, Object> body(
            String email, String password, String nickname, String level) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("nickname", nickname);
        body.put("level", level);
        return body;
    }

    private static Map<String, Object> valid() {
        return body("user@example.com", "vibe1234", "코딩초보", "BEGINNER");
    }

    @Test
    void AC1_유효한_값이면_201과_id를_받고_정규화한_이메일과_해시를_저장한다() throws Exception {
        String response =
                signup(body("  User@Example.COM ", "vibe1234", " 코딩초보 ", "JUNIOR"))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").isNumber())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM users WHERE id = ?", id);
        assertThat(row.get("email")).isEqualTo("user@example.com");
        assertThat(row.get("nickname")).isEqualTo("코딩초보");
        assertThat(row.get("level")).isEqualTo("JUNIOR");
        assertThat(row.get("role")).isEqualTo("USER");
        assertThat(row.get("status")).isEqualTo("ACTIVE");
        String hash = (String) row.get("password_hash");
        assertThat(hash).isNotEqualTo("vibe1234");
        assertThat(passwordEncoder.matches("vibe1234", hash)).isTrue();
    }

    static Stream<Arguments> invalidBodies() {
        return Stream.of(
                Arguments.of(body("not-an-email", "vibe1234", "코딩초보", "BEGINNER"), "email"),
                Arguments.of(body(null, "vibe1234", "코딩초보", "BEGINNER"), "email"),
                Arguments.of(body("user@example.com", null, "코딩초보", "BEGINNER"), "password"),
                Arguments.of(body("user@example.com", "vibe1234", null, "BEGINNER"), "nickname"),
                Arguments.of(body("user@example.com", "vibe1234", "코딩초보", null), "level"),
                Arguments.of(body("user@example.com", "vibe1234", "코딩초보", "EXPERT"), "level"));
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void AC2_검증에_실패하면_400_COMMON_400과_필드별_사유를_받는다(Map<String, Object> body, String field)
            throws Exception {
        signup(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details." + field).isString());
    }

    @Test
    void AC2_여러_필드가_틀리면_모두_알려준다() throws Exception {
        signup(body("bad", "short", "!", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").isString())
                .andExpect(jsonPath("$.details.password").isString())
                .andExpect(jsonPath("$.details.nickname").isString())
                .andExpect(jsonPath("$.details.level").isString());
    }

    @Test
    void AC3_대소문자와_공백만_다른_이메일은_409_USER_409() throws Exception {
        signup(valid()).andExpect(status().isCreated());

        signup(body(" User@Example.com ", "vibe1234", "다른닉네임", "BEGINNER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_409"))
                .andExpect(jsonPath("$.details.email").value("이미 사용 중인 이메일입니다."))
                .andExpect(jsonPath("$.details.nickname").doesNotExist());
    }

    @Test
    void AC4_같은_닉네임이면_409_USER_409() throws Exception {
        signup(valid()).andExpect(status().isCreated());

        signup(body("other@example.com", "vibe1234", "코딩초보", "BEGINNER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_409"))
                .andExpect(jsonPath("$.details.nickname").value("이미 사용 중인 닉네임입니다."))
                .andExpect(jsonPath("$.details.email").doesNotExist());
    }

    static Stream<Arguments> boundaries() {
        String p64 = "a1".repeat(32);
        String n20 = "가".repeat(20);
        return Stream.of(
                Arguments.of("abcdef12", "경계닉", true), // 비밀번호 8자
                Arguments.of(p64, "경계닉", true), // 비밀번호 64자
                Arguments.of("abcde12", "경계닉", false), // 7자
                Arguments.of(p64 + "a", "경계닉", false), // 65자
                Arguments.of("abcdefgh", "경계닉", false), // 영문만
                Arguments.of("12345678", "경계닉", false), // 숫자만
                Arguments.of("가".repeat(30) + "a1", "경계닉", false), // 한글: 32자지만 92바이트
                Arguments.of("vibe1234😀", "경계닉", false), // 이모지
                Arguments.of("vibe 1234!@#", "경계닉", true), // 공백·특수문자 허용
                Arguments.of("vibe1234", "ab", true), // 닉네임 2자
                Arguments.of("vibe1234", n20, true), // 닉네임 20자
                Arguments.of("vibe1234", "a", false), // 1자
                Arguments.of("vibe1234", n20 + "가", false), // 21자
                Arguments.of("vibe1234", "코딩 초보", false), // 공백 포함
                Arguments.of("vibe1234", "코딩!초보", false), // 특수문자
                Arguments.of("vibe1234", "vibe_coder9", true)); // 영문·숫자·_
    }

    @ParameterizedTest
    @MethodSource("boundaries")
    void AC5_경계값(String password, String nickname, boolean ok) throws Exception {
        ResultActions result = signup(body("edge@example.com", password, nickname, "SENIOR"));
        if (ok) {
            result.andExpect(status().isCreated());
        } else {
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_400"));
        }
    }
}
