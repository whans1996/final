package com.vibenote.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 인증 규칙: 공개 주소, 토큰 없음·잘못된 토큰 → 401, 관리자 주소 → 403, 응답은 공통 에러 형식. */
class SecurityConfigTest extends IntegrationTestBase {

    // 실제 API가 생겨도 깨지지 않도록 앞으로도 만들지 않을 주소로 규칙만 확인한다
    private static final String PROTECTED_PROBE = "/api/__probe__";
    private static final String ADMIN_PROBE = "/api/admin/__probe__";
    private static final String PUBLIC_AUTH_PROBE = "/api/auth/__probe__";

    @Autowired JwtTokenProvider tokenProvider;

    @Test
    void Swagger_문서는_토큰_없이_열린다() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void 인증_API_주소는_토큰_없이_통과한다() throws Exception {
        // 공개 주소라 401이 아니라 (없는 주소이므로) 404가 된다
        mockMvc.perform(get(PUBLIC_AUTH_PROBE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_404"));
    }

    @Test
    void 토큰_없이_보호된_주소에_접근하면_401_AUTH_401() throws Exception {
        mockMvc.perform(get(PROTECTED_PROBE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void 잘못된_토큰이면_401_AUTH_401() throws Exception {
        mockMvc.perform(get(PROTECTED_PROBE).header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void 유효한_토큰이면_인증을_통과한다() throws Exception {
        String token = tokenProvider.createAccessToken(1L, "USER");
        // 존재하지 않는 주소이므로 인증을 통과하면 404가 된다
        mockMvc.perform(get(PROTECTED_PROBE).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_404"));
    }

    @Test
    void 일반_사용자가_관리자_주소에_접근하면_403_AUTH_403() throws Exception {
        String token = tokenProvider.createAccessToken(1L, "USER");
        mockMvc.perform(get(ADMIN_PROBE).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));
    }

    @Test
    void 관리자는_관리자_주소의_인증을_통과한다() throws Exception {
        String token = tokenProvider.createAccessToken(1L, "ADMIN");
        mockMvc.perform(get(ADMIN_PROBE).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
