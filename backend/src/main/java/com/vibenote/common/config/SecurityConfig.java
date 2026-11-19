package com.vibenote.common.config;

import com.vibenote.common.security.JsonAuthenticationEntryPoint;
import com.vibenote.common.security.JwtAuthenticationFilter;
import com.vibenote.common.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * JWT 기반 무상태 인증. 공개 주소는 {@link #PUBLIC_PATHS}에 모으고, 나머지는 인증이 필요하다.
 *
 * <p>공개 GET API(예: 개념 카드 목록)를 추가할 때는 해당 기능 spec에 적고 아래 규칙에 더한다.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** 인증 API는 와일드카드 대신 공개할 주소만 나열한다 (F-02 AC7). */
    static final String[] PUBLIC_PATHS = {
        "/api/auth/signup",
        "/api/auth/login",
        "/api/auth/refresh",
        "/api/auth/logout",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/uploads/**",
        "/error"
    };

    /**
     * 로그인 없이 볼 수 있는 GET API. 각 기능은 <b>자기 도메인 블록 안에만</b> 추가하고 모든 항목 끝에 쉼표를 단다 (docs/conventions.md
     * 9절).
     */
    static final String[] PUBLIC_GET_PATHS = {
        // ── USER (공통 F-03). 숫자 id만 맞춰 /api/users/me는 계속 인증이 필요하다 ──
        "/api/users/{id:\\d+}",
        "/api/users/{id:\\d+}/stats",

        // ── ANALYSIS (A F-19 공개 분석) ──

        // ── CONCEPT (B F-09~11) ──
        "/api/concepts",
        "/api/concepts/{id:\\d+}",

        // ── POST (B F-17·F-18) ──

        // ── CHALLENGE (A F-25) ──

    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenProvider tokenProvider,
            JsonAuthenticationEntryPoint entryPoint)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(PUBLIC_PATHS)
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS)
                                        .permitAll()
                                        .requestMatchers("/api/admin/**")
                                        .hasRole("ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(entryPoint))
                .addFilterBefore(
                        new JwtAuthenticationFilter(tokenProvider),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
