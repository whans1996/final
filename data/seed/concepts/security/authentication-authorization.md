---
name: 인증과 인가
category: security
difficulty: BEGINNER
reference: https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html
---

## summary

인증(Authentication)은 "누구인가"를 확인하는 것이고, 인가(Authorization)는 "그 사람이 이 일을 해도 되는가"를 판단하는 것이다.

## body

## 핵심
- 인증 실패는 401 Unauthorized, 인증은 됐지만 권한이 없으면 403 Forbidden이다.
- 스프링 시큐리티는 인증 결과를 `SecurityContext`에 담고, URL 규칙이나 `@PreAuthorize`로 인가를 판단한다.

## 주의
- "로그인했는가"만 확인하고 "내 글인가"를 확인하지 않으면 남의 글을 수정·삭제할 수 있다. 소유자 확인은 Service에서 한다.
- 관리자 API는 URL 규칙으로 역할을 강제한다.

## example

```java
http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()      // 인증 불필요
        .requestMatchers("/api/admin/**").hasRole("ADMIN") // 인가: 관리자만
        .anyRequest().authenticated());                    // 나머지는 인증 필요
```

```java
// 소유자 확인 (인가)
if (post.userId() != loginUserId) {
    throw new BusinessException(ErrorCode.FORBIDDEN);
}
```
