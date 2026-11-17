package com.vibenote.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Access 토큰 발급·검증. Refresh 토큰의 저장 위치와 재발급 흐름은 F-02 spec에서 정한다.
 *
 * <p>토큰 내용: {@code sub} = 사용자 ID, {@code role} = USER | ADMIN.
 */
@Component
public class JwtTokenProvider {

    static final int MIN_SECRET_BYTES = 32;
    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final JwtProperties properties;
    private final Clock clock;

    @Autowired
    public JwtTokenProvider(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtTokenProvider(JwtProperties properties, Clock clock) {
        String secret = properties.secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret(JWT_SECRET)은 "
                            + MIN_SECRET_BYTES
                            + "바이트 이상이어야 합니다. .env.example을 참고하세요.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.properties = properties;
        this.clock = clock;
    }

    public String createAccessToken(long userId, String role) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(Long.toString(userId))
                .claim(ROLE_CLAIM, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenValidity())))
                .signWith(key)
                .compact();
    }

    /** 서명·만료를 검증하고 사용자 정보를 꺼낸다. 잘못되었거나 만료된 토큰이면 빈 값. */
    public Optional<AuthUser> parse(String token) {
        try {
            Claims claims =
                    Jwts.parser()
                            .verifyWith(key)
                            .clock(() -> Date.from(clock.instant()))
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
            return Optional.of(
                    new AuthUser(
                            Long.parseLong(claims.getSubject()),
                            claims.get(ROLE_CLAIM, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
