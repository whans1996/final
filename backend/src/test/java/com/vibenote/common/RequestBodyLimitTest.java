package com.vibenote.common;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * JSON 요청 본문 전역 상한 (#55). {@code spring.jackson.factory.constraints.read.max-document-length}(1MB)가
 * 역직렬화 도중에 읽기를 멈추고 413 {@code COMMON_413}으로 응답하는지 확인한다. 공개 API인 로그인으로 확인하지만 모든 JSON API에 같은 Spring
 * 매퍼가 쓰인다.
 */
class RequestBodyLimitTest extends IntegrationTestBase {

    private static final int LIMIT = 1024 * 1024;

    @Test
    void JSON_본문이_1MB를_넘으면_413_COMMON_413() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody(LIMIT + 1024)))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.code").value("COMMON_413"));
    }

    @Test
    void 한도_안의_큰_본문은_기존처럼_처리된다() throws Exception {
        // 본문을 끝까지 읽어 서비스에 닿으면 없는 이메일이라 401 AUTH_001이 난다
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody(LIMIT - 1024)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    @Test
    void charset을_밝힌_UTF16_본문도_바이트_기준_1MB를_넘으면_413() throws Exception {
        // 문자 수는 한도의 절반 남짓이라 Jackson 문자 파서 한도에는 걸리지 않는다 (#73)
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType("application/json;charset=UTF-16")
                                .content(loginBody(LIMIT / 2 + 1024).getBytes(UTF_16)))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.code").value("COMMON_413"));
    }

    @Test
    void charset_없이_보낸_UTF16_본문도_바이트_기준_1MB를_넘으면_413() throws Exception {
        // charset이 없으면 Jackson이 바이트를 보고 UTF-16을 감지해 문자 파서로 읽는다
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody(LIMIT / 2 + 1024).getBytes(UTF_16BE)))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.code").value("COMMON_413"));
    }

    @Test
    void plus_json_타입_본문도_1MB를_넘으면_413() throws Exception {
        // Spring Jackson 변환기는 application/*+json도 읽으므로 필터도 같은 한도를 건다
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType("application/merge-patch+json")
                                .content(loginBody(LIMIT + 1024)))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.code").value("COMMON_413"));
    }

    /** 전체 길이가 대략 {@code length} 바이트인 로그인 본문 (비밀번호를 늘린다). */
    private static String loginBody(int length) {
        return "{\"email\":\"limit@vibenote.dev\",\"password\":\"" + "a".repeat(length) + "\"}";
    }
}
