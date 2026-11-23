package com.vibenote.common;

import static org.assertj.core.api.Assertions.assertThat;
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
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody(LIMIT - 1024)))
                .andExpect(
                        result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(413));
    }

    /** 전체 길이가 대략 {@code length} 바이트인 로그인 본문 (비밀번호를 늘린다). */
    private static String loginBody(int length) {
        return "{\"email\":\"limit@vibenote.dev\",\"password\":\"" + "a".repeat(length) + "\"}";
    }
}
