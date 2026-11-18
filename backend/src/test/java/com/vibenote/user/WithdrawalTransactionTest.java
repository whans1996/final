package com.vibenote.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtTokenProvider;
import com.vibenote.follow.FollowService;
import com.vibenote.user.dto.SignupRequest;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * F-03 AC6 "한 트랜잭션": 마지막 단계(팔로우 삭제)가 실패하면 마스킹과 토큰 폐기도 함께 롤백된다.
 *
 * <p>클래스에 {@code @Transactional}을 붙이지 않는다. 붙이면 요청이 테스트 트랜잭션에 합류해, 서비스의 {@code @Transactional}을 지워도
 * 통과해 버린다. 대신 만든 행을 {@link #cleanUp()}에서 지운다.
 */
class WithdrawalTransactionTest extends IntegrationTestBase {

    private static final String EMAIL = "tx-withdraw@example.com";
    private static final String PASSWORD = "vibe1234";

    @Autowired UserService userService;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean FollowService followService;

    private Long createdId;

    @AfterEach
    void cleanUp() {
        if (createdId != null) {
            jdbc.update("DELETE FROM refresh_tokens WHERE user_id = ?", createdId);
            jdbc.update("DELETE FROM users WHERE id = ?", createdId);
        }
    }

    @Test
    void AC6_마지막_단계가_실패하면_마스킹과_토큰_폐기도_롤백된다() throws Exception {
        long id = userService.signup(new SignupRequest(EMAIL, PASSWORD, "트랜잭션회원", "BEGINNER"));
        createdId = id;
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of("email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk());
        willThrow(new IllegalStateException("팔로우 삭제 실패(테스트)"))
                .given(followService)
                .removeAllOf(anyLong());

        mockMvc.perform(
                        delete("/api/users/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenProvider.createAccessToken(id, "USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isInternalServerError());

        Map<String, Object> row =
                jdbc.queryForMap(
                        "SELECT status, email, nickname, withdrawn_at FROM users WHERE id = ?", id);
        assertThat(row.get("status")).isEqualTo("ACTIVE");
        assertThat(row.get("email")).isEqualTo(EMAIL);
        assertThat(row.get("nickname")).isEqualTo("트랜잭션회원");
        assertThat(row.get("withdrawn_at")).isNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                                Integer.class,
                                id))
                .isEqualTo(1);
    }
}
