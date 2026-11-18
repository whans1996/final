package com.vibenote.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.common.security.JwtProperties;
import com.vibenote.common.security.JwtTokenProvider;
import com.vibenote.user.UserService;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/** F-02 AC4: 같은 refresh 토큰으로 동시에 재발급하면 먼저 지운 요청만 성공한다. */
class AuthServiceTest {

    @Test
    void AC4_조회한_토큰을_다른_요청이_먼저_지웠으면_새_토큰을_만들지_않고_AUTH_002() {
        RefreshTokenMapper mapper = mock(RefreshTokenMapper.class);
        given(mapper.findByHash(anyString()))
                .willReturn(Optional.of(new RefreshTokenMapper.StoredRefreshToken(1L, 7L, false)));
        given(mapper.deleteByHash(anyString())).willReturn(0);
        JwtProperties properties =
                new JwtProperties(
                        "test-secret-test-secret-test-secret-test-secret",
                        Duration.ofMinutes(30),
                        Duration.ofDays(14),
                        false);
        AuthService service =
                new AuthService(
                        mock(UserService.class),
                        mapper,
                        mock(JwtTokenProvider.class),
                        properties,
                        mock(PasswordEncoder.class));

        BusinessException e =
                catchThrowableOfType(BusinessException.class, () -> service.refresh("token"));

        assertThat(e).isInstanceOf(AuthService.UnknownRefreshTokenException.class);
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_002);
        verify(mapper, never()).insert(anyLong(), anyString(), anyLong());
    }
}
