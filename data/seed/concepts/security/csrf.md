---
name: CSRF (사이트 간 요청 위조)
category: security
difficulty: JUNIOR
reference: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
---

## summary

CSRF는 사용자가 로그인한 상태에서 공격자 사이트가 사용자 브라우저를 이용해 우리 서버로 원치 않는 요청(송금, 탈퇴)을 보내게 하는 공격이다. 브라우저가 쿠키를 자동으로 붙이기 때문에 생긴다.

## body

## 핵심
- 공격 조건: 인증 정보가 **쿠키로 자동 전송**되고, 서버가 요청 출처를 확인하지 않을 때.
- 방어:
  - CSRF 토큰: 서버가 준 임의 값을 요청 헤더·폼에 담아 보내게 하고 검증한다. 스프링 시큐리티는 기본으로 켜져 있다.
  - `SameSite` 쿠키 속성(Lax·Strict): 다른 사이트에서 시작된 요청에 쿠키를 붙이지 않는다.
  - 상태를 바꾸는 작업은 GET으로 만들지 않는다.

## CSRF를 꺼도 되는 경우
- 인증을 `Authorization: Bearer` 헤더로만 하는 API(JWT를 메모리에 보관)는 브라우저가 자동으로 붙이지 않으므로 CSRF 위험이 낮아 보통 끈다.
- 단, Refresh 토큰처럼 **쿠키로 받는 엔드포인트가 있으면** 그 경로는 SameSite=Strict, 경로 제한(Path) 등으로 따로 지킨다.

## 주의
- AI 예제는 습관처럼 `csrf.disable()`을 넣는다. 세션·쿠키 인증을 쓰는 서버에서 끄면 바로 취약해진다.
- 시큐리티 6부터 CSRF 토큰은 요청마다 값을 가려서(XOR) 내보내고, 실제로 쓰일 때까지 만들지 않는다(지연 로딩). 그래서 Boot 2 시절 예제처럼 `CookieCsrfTokenRepository.withHttpOnlyFalse()` 한 줄만 넣으면 SPA에서 403이 난다. 공식 문서의 "Single-Page Applications" 절 구성을 따른다.

## example

```java
// JWT를 헤더로만 받는 API 서버
http.csrf(csrf -> csrf.disable())
    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
```

```java
// 세션·쿠키 인증 + SPA (시큐리티 6): 쿠키로 CSRF 토큰을 주고 헤더로 돌려받는다
http.csrf(csrf -> csrf
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        // 6의 기본 핸들러(XOR)는 쿠키 원문 값을 거부한다. 원문 값을 받는 핸들러로 바꾼다
        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
// 토큰은 지연 로딩이라 첫 요청에 쿠키가 없을 수 있다.
// 토큰을 한 번 읽어 주는 필터를 추가한다 (공식 문서 Single-Page Applications 절)
// 프론트: 상태를 바꾸는 요청마다 X-XSRF-TOKEN 헤더에 XSRF-TOKEN 쿠키 값을 담는다
```
