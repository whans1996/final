package com.vibenote.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.vibenote.auth.dto.LoginRequest;
import com.vibenote.common.Level;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.common.security.JwtProperties;
import com.vibenote.common.security.JwtTokenProvider;
import com.vibenote.user.UserService;
import com.vibenote.user.domain.User;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * F-02 AC4: 같은 refresh 토큰으로 동시에 재발급하면 먼저 지운 요청만 성공한다.
 *
 * <p>F-03 AC8: 회원을 읽은 뒤 탈퇴가 커밋되면 refresh 행을 만들지 못하므로(INSERT ... WHERE status=ACTIVE) 토큰을 주지 않는다.
 */
class AuthServiceTest {

    private static final JwtProperties PROPERTIES =
            new JwtProperties(
                    "test-secret-test-secret-test-secret-test-secret",
                    Duration.ofMinutes(30),
                    Duration.ofDays(14),
                    false);

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

    @Test
    void F03_AC8_로그인_중_탈퇴가_커밋되어_refresh_행을_못_만들면_토큰_없이_AUTH_001() {
        User user = User.newMember("user@example.com", "hashed", "코딩초보", Level.BEGINNER);
        user.setId(7L);
        UserService userService = mock(UserService.class);
        given(userService.findByEmail("user@example.com")).willReturn(Optional.of(user));
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        given(encoder.matches("vibe1234", "hashed")).willReturn(true);
        RefreshTokenMapper mapper = mock(RefreshTokenMapper.class);
        given(mapper.insert(anyLong(), anyString(), anyLong())).willReturn(0);
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        AuthService service =
                new AuthService(userService, mapper, tokenProvider, PROPERTIES, encoder);

        BusinessException e =
                catchThrowableOfType(
                        BusinessException.class,
                        () -> service.login(new LoginRequest("user@example.com", "vibe1234")));

        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_001);
        verify(tokenProvider, never()).createAccessToken(anyLong(), anyString());
    }

    @Test
    void F03_AC8_재발급_중_탈퇴가_커밋되어_refresh_행을_못_만들면_AUTH_002() {
        User user = User.newMember("user@example.com", "hashed", "코딩초보", Level.BEGINNER);
        user.setId(7L);
        UserService userService = mock(UserService.class);
        given(userService.findById(7L)).willReturn(Optional.of(user));
        RefreshTokenMapper mapper = mock(RefreshTokenMapper.class);
        given(mapper.findByHash(anyString()))
                .willReturn(Optional.of(new RefreshTokenMapper.StoredRefreshToken(1L, 7L, false)));
        given(mapper.deleteByHash(anyString())).willReturn(1);
        given(mapper.insert(anyLong(), anyString(), anyLong())).willReturn(0);
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        AuthService service =
                new AuthService(
                        userService,
                        mapper,
                        tokenProvider,
                        PROPERTIES,
                        mock(PasswordEncoder.class));

        BusinessException e =
                catchThrowableOfType(BusinessException.class, () -> service.refresh("token"));

        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_002);
        verify(tokenProvider, never()).createAccessToken(anyLong(), anyString());
    }
}
