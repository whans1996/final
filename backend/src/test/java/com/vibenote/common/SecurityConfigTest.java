package com.vibenote.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 인증 규칙: 공개 주소, 토큰 없음·잘못된 토큰 → 401, 관리자 주소 → 403, 응답은 공통 에러 형식. */
class SecurityConfigTest extends IntegrationTestBase {

    @Autowired JwtTokenProvider tokenProvider;

    @Test
    void Swagger_문서는_토큰_없이_열린다() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void 토큰_없이_보호된_주소에_접근하면_401_AUTH_401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void 잘못된_토큰이면_401_AUTH_401() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void 유효한_토큰이면_인증을_통과한다() throws Exception {
        String token = tokenProvider.createAccessToken(1L, "USER");
        // 아직 해당 API가 없으므로 인증 통과 후 404가 된다
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_404"));
    }

    @Test
    void 일반_사용자가_관리자_주소에_접근하면_403_AUTH_403() throws Exception {
        String token = tokenProvider.createAccessToken(1L, "USER");
        mockMvc.perform(get("/api/admin/concepts").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));
    }

    @Test
    void 관리자는_관리자_주소의_인증을_통과한다() throws Exception {
        String token = tokenProvider.createAccessToken(1L, "ADMIN");
        mockMvc.perform(get("/api/admin/concepts").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
