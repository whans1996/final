package com.vibenote.common.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT·Refresh 토큰 설정 ({@code app.jwt.*}). 키는 환경변수 {@code JWT_SECRET}으로만 받는다.
 *
 * @param secret HMAC-SHA 서명 키. UTF-8 기준 32바이트 이상
 * @param accessTokenValidity Access 토큰 유효 시간
 * @param refreshTokenValidity Refresh 토큰 유효 시간 (쿠키 Max-Age, DB expires_at)
 * @param refreshCookieSecure Refresh 쿠키의 Secure 속성. http로 띄우는 local 프로필만 false
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        String secret,
        Duration accessTokenValidity,
        Duration refreshTokenValidity,
        boolean refreshCookieSecure) {}
