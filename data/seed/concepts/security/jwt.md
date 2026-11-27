---
name: JWT 기반 인증
category: security
difficulty: SENIOR
reference: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
---

## summary

JWT는 사용자 정보와 만료 시간을 담고 서명한 토큰이다. 서버는 세션을 저장하지 않고, 요청마다 Authorization 헤더의 토큰 서명과 만료를 검증해 사용자를 식별한다.

## body

## 핵심
- 구조: header.payload.signature (Base64URL). payload는 암호화가 아니라 인코딩이라 누구나 읽을 수 있다.
- Access 토큰은 짧게(예: 30분), Refresh 토큰은 길게 두고 Refresh로 Access를 재발급한다.
- 서버는 서명 키로 위조 여부를, `exp`로 만료를 확인한다.

## 주의
- payload에 비밀번호나 개인정보를 넣지 않는다.
- 서명 키는 충분히 길고 환경변수로 관리한다.
- 발급한 JWT는 만료 전에 서버가 취소하기 어렵다. 로그아웃·탈취 대응은 Refresh 토큰 저장·폐기로 보완한다.
- 서명 알고리즘을 고정하고 `alg: none` 토큰을 받지 않는다.

## example

```http
// 요청
GET /api/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZSI6IlVTRVIiLCJleHAiOjE3OTk5OTk5OTl9.서명
```

```java
// 필터에서 검증
String token = resolveBearer(request);
if (token != null && jwtTokenProvider.isValid(token)) {
    long userId = jwtTokenProvider.getUserId(token);
    Authentication auth = new UsernamePasswordAuthenticationToken(
            new AuthUser(userId), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);
}
```
