package com.vibenote.auth;

import com.vibenote.common.security.JwtProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Refresh 토큰 쿠키 {@code refresh_token}: HttpOnly, SameSite=Strict, Path=/api/auth.
 *
 * <p>Path를 /api/auth로 좁혀 다른 API 요청에는 실리지 않게 하고, SameSite=Strict로 다른 사이트에서 보낸 요청(CSRF)에도 실리지 않게 한다.
 */
@Component
public class RefreshTokenCookie {

    public static final String NAME = "refresh_token";
    static final String PATH = "/api/auth";

    private final JwtProperties properties;

    public RefreshTokenCookie(JwtProperties properties) {
        this.properties = properties;
    }

    /** 새 토큰을 담은 Set-Cookie 값. */
    public String issue(String token) {
        return build(token, properties.refreshTokenValidity());
    }

    /** 브라우저의 쿠키를 지우는 Set-Cookie 값. */
    public String clear() {
        return build("", Duration.ZERO);
    }

    private String build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Strict")
                .path(PATH)
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
