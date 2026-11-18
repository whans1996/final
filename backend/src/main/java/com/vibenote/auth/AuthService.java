package com.vibenote.auth;

import com.vibenote.auth.dto.LoginRequest;
import com.vibenote.auth.dto.LoginResponse;
import com.vibenote.auth.dto.LoginResponse.LoginUser;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.common.security.JwtProperties;
import com.vibenote.common.security.JwtTokenProvider;
import com.vibenote.user.UserService;
import com.vibenote.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인·재발급·로그아웃 (F-02).
 *
 * <p>Refresh 토큰은 32바이트 랜덤 문자열이고 DB에는 SHA-256 해시만 둔다. 재발급할 때마다 기존 행을 지우고 새 토큰을 만든다(회전).
 */
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final UserService userService;
    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserService userService,
            RefreshTokenMapper refreshTokenMapper,
            JwtTokenProvider tokenProvider,
            JwtProperties jwtProperties,
            PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.refreshTokenMapper = refreshTokenMapper;
        this.tokenProvider = tokenProvider;
        this.jwtProperties = jwtProperties;
        this.passwordEncoder = passwordEncoder;
    }

    /** 발급 결과. body는 응답 본문, refreshToken은 쿠키로 보낼 원문. */
    public record IssuedTokens(LoginResponse body, String refreshToken) {}

    /** DB에 없거나 다른 요청이 먼저 회전한 토큰 (AUTH_002). */
    public static class UnknownRefreshTokenException extends BusinessException {
        UnknownRefreshTokenException() {
            super(ErrorCode.AUTH_002);
        }
    }

    /** 이메일 없음·비밀번호 불일치·탈퇴 회원을 구분하지 않고 AUTH_001. */
    @Transactional
    public IssuedTokens login(LoginRequest request) {
        User user =
                userService
                        .findByEmail(request.email())
                        .filter(User::isActive)
                        .filter(
                                u ->
                                        passwordEncoder.matches(
                                                request.password(), u.getPasswordHash()))
                        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_001));
        return issue(user);
    }

    /**
     * 쿠키의 Refresh 토큰을 새 토큰으로 바꾼다. 쓴 토큰은 결과와 상관없이 지운다(만료·탈퇴여도 행이 남지 않게). 실패하면 AUTH_002.
     *
     * <p>실패 시에도 삭제를 남기려고 BusinessException에는 롤백하지 않는다.
     *
     * <p>같은 토큰으로 동시에 들어오면 둘 다 조회에 성공할 수 있다. DELETE는 커밋된 최신 행을 보고 잠그므로, 지운 행이 0이면 다른 요청이 먼저 회전한 것이라
     * 실패시킨다(1회용 보장).
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public IssuedTokens refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }
        String hash = sha256(refreshToken);
        RefreshTokenMapper.StoredRefreshToken stored =
                refreshTokenMapper.findByHash(hash).orElseThrow(UnknownRefreshTokenException::new);
        if (refreshTokenMapper.deleteByHash(hash) == 0) {
            throw new UnknownRefreshTokenException();
        }
        if (stored.expired()) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }
        // role·status는 Access 토큰이 아니라 DB에서 다시 읽는다 (탈퇴·권한 변경 반영)
        User user =
                userService
                        .findById(stored.userId())
                        .filter(User::isActive)
                        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_002));
        return issue(user);
    }

    /** 그 토큰의 행만 지운다 (다른 기기 세션은 유지). 토큰이 없거나 모르는 값이어도 조용히 끝낸다. */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenMapper.deleteByHash(sha256(refreshToken));
        }
    }

    private IssuedTokens issue(User user) {
        String refreshToken = newRefreshToken();
        refreshTokenMapper.insert(
                user.getId(),
                sha256(refreshToken),
                jwtProperties.refreshTokenValidity().toSeconds());
        LoginResponse body =
                new LoginResponse(
                        tokenProvider.createAccessToken(user.getId(), user.getRole()),
                        jwtProperties.accessTokenValidity().toSeconds(),
                        new LoginUser(
                                user.getId(), user.getNickname(), user.getLevel(), user.getRole()));
        return new IssuedTokens(body, refreshToken);
    }

    private static String newRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 쓸 수 없습니다.", e);
        }
    }
}
