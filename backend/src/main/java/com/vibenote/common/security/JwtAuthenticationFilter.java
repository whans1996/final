package com.vibenote.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code Authorization: Bearer <token>}을 검증해 SecurityContext에 {@link AuthUser}를 넣는다.
 *
 * <p>토큰이 없거나 잘못되었으면 아무것도 하지 않는다. 인증이 필요한 주소라면 {@link JsonAuthenticationEntryPoint}가 401(AUTH_401)을
 * 응답한다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            tokenProvider
                    .parse(header.substring(BEARER.length()).trim())
                    .ifPresent(
                            user -> {
                                var authorities =
                                        List.of(new SimpleGrantedAuthority("ROLE_" + user.role()));
                                var auth =
                                        new UsernamePasswordAuthenticationToken(
                                                user, null, authorities);
                                SecurityContextHolder.getContext().setAuthentication(auth);
                            });
        }
        chain.doFilter(request, response);
    }
}
