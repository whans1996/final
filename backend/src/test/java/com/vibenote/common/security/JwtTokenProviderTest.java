package com.vibenote.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-jwt-secret-0123456789abcdef";
    private static final Instant NOW = Instant.parse("2026-11-18T05:00:00Z");

    private JwtTokenProvider providerAt(Instant instant) {
        return new JwtTokenProvider(
                new JwtProperties(SECRET, Duration.ofMinutes(30)),
                Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void 발급한_토큰에서_사용자_ID와_역할을_꺼낸다() {
        JwtTokenProvider provider = providerAt(NOW);
        String token = provider.createAccessToken(42L, "ADMIN");

        assertThat(provider.parse(token)).contains(new AuthUser(42L, "ADMIN"));
    }

    @Test
    void 만료된_토큰은_빈_값() {
        String token = providerAt(NOW).createAccessToken(1L, "USER");

        assertThat(providerAt(NOW.plus(Duration.ofMinutes(31))).parse(token)).isEmpty();
    }

    @Test
    void 다른_키로_서명한_토큰은_빈_값() {
        String token =
                new JwtTokenProvider(
                                new JwtProperties(
                                        "another-secret-0123456789abcdef-xyz",
                                        Duration.ofMinutes(30)),
                                Clock.fixed(NOW, ZoneOffset.UTC))
                        .createAccessToken(1L, "USER");

        assertThat(providerAt(NOW).parse(token)).isEmpty();
    }

    @Test
    void 형식이_잘못된_토큰은_빈_값() {
        assertThat(providerAt(NOW).parse("not-a-jwt")).isEmpty();
        assertThat(providerAt(NOW).parse("")).isEmpty();
    }

    @Test
    void 키가_32바이트보다_짧으면_시작할_수_없다() {
        assertThatThrownBy(
                        () ->
                                new JwtTokenProvider(
                                        new JwtProperties("short", Duration.ofMinutes(30))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }
}
