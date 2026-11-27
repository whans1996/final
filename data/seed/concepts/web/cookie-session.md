---
name: 쿠키와 세션
category: web
difficulty: BEGINNER
reference: https://developer.mozilla.org/ko/docs/Web/HTTP/Guides/Cookies
---

## summary

HTTP는 요청끼리 기억이 없으므로, 서버는 쿠키로 브라우저에 작은 값을 저장하게 하고 다음 요청마다 돌려받는다. 세션은 서버가 사용자 상태를 저장하고 그 식별자만 쿠키로 주고받는 방식이다.

## body

## 핵심
- 서버가 `Set-Cookie` 헤더로 쿠키를 주면, 브라우저는 조건이 맞는 요청마다 `Cookie` 헤더로 자동 전송한다.
- 보안 속성:
  - `HttpOnly`: 자바스크립트에서 읽을 수 없다. 토큰 탈취(XSS)를 줄인다.
  - `Secure`: HTTPS에서만 전송한다.
  - `SameSite=Strict|Lax|None`: 다른 사이트에서 시작된 요청에 쿠키를 보낼지 정한다. CSRF를 줄인다.
  - `Max-Age`·`Expires`: 수명. `Path`: 전송할 경로 범위.
- 세션 방식은 서버 메모리·저장소에 상태가 있고, JWT 방식은 토큰 자체에 정보가 있다. Refresh 토큰을 HttpOnly 쿠키에 두는 혼합 방식도 많이 쓴다.

## 주의
- 토큰을 `localStorage`에 두면 XSS 한 번에 탈취된다. 민감한 토큰은 HttpOnly 쿠키가 낫다.
- 쿠키로 인증하면 CSRF 대책(SameSite, CSRF 토큰)이 필요하다.
- 프론트와 백엔드 출처가 다르면 `credentials: "include"`와 CORS `allowCredentials`가 함께 필요하다.

## example

```java
// Spring: Refresh 토큰을 HttpOnly 쿠키로 내려주기
ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/api/auth")
        .maxAge(Duration.ofDays(14))
        .build();
return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new TokenResponse(accessToken));
```

```http
// 응답 헤더
// Set-Cookie: refresh_token=...; Path=/api/auth; Max-Age=1209600; Secure; HttpOnly; SameSite=Strict
```
