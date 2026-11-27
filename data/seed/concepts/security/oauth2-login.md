---
name: OAuth2 소셜 로그인
category: security
difficulty: SENIOR
reference: https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html
---

## summary

OAuth2 로그인은 구글·깃허브·카카오 같은 외부 서비스가 사용자를 인증하고, 우리 서버는 그 결과(사용자 정보)를 받아 로그인 처리하는 방식이다. 비밀번호를 우리 서버가 저장하지 않는다.

## body

## 핵심
- 흐름(Authorization Code): 로그인 버튼 → 제공자 로그인·동의 화면 → 우리 서버 콜백으로 인가 코드 전달 → 서버가 코드를 토큰으로 교환 → 사용자 정보 조회 → 우리 회원과 연결.
- 스프링 시큐리티는 `spring-boot-starter-oauth2-client` + `spring.security.oauth2.client.registration.*` 설정 + `oauth2Login()`으로 이 흐름을 처리한다.
- 구글·깃허브 등은 제공자 정보가 내장되어 있다. 카카오·네이버는 `provider` 설정을 직접 적는다.
- 처음 로그인한 사용자는 `OAuth2UserService`를 구현해 우리 DB에 회원으로 저장(또는 기존 계정과 연결)한다.

## 주의
- client secret은 환경변수로 받는다. 저장소에 커밋하지 않는다.
- 제공자 콘솔에 등록한 Redirect URI와 서버 설정이 정확히 같아야 한다.
- SPA + JWT 구조라면 로그인 성공 핸들러에서 우리 토큰을 발급해 프론트로 넘기는 단계가 추가로 필요하다. 토큰을 URL 쿼리로 넘기면 기록에 남으므로 쿠키 등 다른 방법을 고려한다.
- 이메일이 같다는 이유만으로 기존 계정과 자동 연결하면 계정 탈취로 이어질 수 있다. 제공자의 이메일 검증 여부를 확인한다.

## example

```yaml
# application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: read:user, user:email
```

```java
@Bean
SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login/**").permitAll()
                .anyRequest().authenticated())
        .oauth2Login(oauth -> oauth
                .userInfoEndpoint(info -> info.userService(customOAuth2UserService))
                .successHandler(oauth2SuccessHandler));     // 여기서 우리 JWT 발급
    return http.build();
}
```
