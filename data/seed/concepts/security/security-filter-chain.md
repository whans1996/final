---
name: SecurityFilterChain
category: security
difficulty: JUNIOR
reference: https://docs.spring.io/spring-security/reference/servlet/architecture.html
---

## summary

스프링 시큐리티는 요청이 컨트롤러에 닿기 전에 여러 보안 필터를 순서대로 통과시킨다. SecurityFilterChain 빈으로 어떤 필터를 어떤 규칙으로 쓸지 설정한다.

## body

## 핵심
- `HttpSecurity`로 CSRF, 세션 정책, URL별 권한, 커스텀 필터 위치를 정한다.
- JWT 방식 API 서버는 보통 세션을 쓰지 않고(STATELESS), CSRF를 끄고, JWT 검증 필터를 `UsernamePasswordAuthenticationFilter` 앞에 둔다.
- 401·403 응답 형식은 `AuthenticationEntryPoint`, `AccessDeniedHandler`로 바꾼다.

## Security 6(Boot 3)에서 바뀐 점
- `WebSecurityConfigurerAdapter`를 상속해 `configure(HttpSecurity)`를 오버라이드하는 방식은 **제거되었다**. `SecurityFilterChain`을 `@Bean`으로 등록한다.
- `authorizeRequests` → `authorizeHttpRequests`, `antMatchers`·`mvcMatchers` → `requestMatchers`.
- `.and()`로 잇는 체인 대신 람다 DSL(`csrf(csrf -> csrf.disable())`)을 쓴다. 예전 방식은 사용 중단되었다.
- AI가 Boot 2 시절 예제를 내면 위 세 가지가 섞여 컴파일 오류가 난다.

## 주의
- `permitAll` 경로 목록이 넓으면 보호해야 할 API가 열린다. 기본은 `authenticated()`로 두고 공개 경로만 연다.
- 규칙은 위에서부터 처음 맞는 것이 적용된다. `anyRequest()`는 마지막에 둔다.
- 세션 쿠키 기반 인증이라면 CSRF를 끄면 안 된다.

## example

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter)
        throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```
