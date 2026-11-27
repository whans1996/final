---
name: 메서드 보안 (@PreAuthorize)
category: security
difficulty: JUNIOR
reference: https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html
---

## summary

메서드 보안은 URL 규칙 대신 서비스·컨트롤러 메서드에 @PreAuthorize를 붙여 호출 직전에 권한을 검사한다. SpEL 식으로 역할뿐 아니라 파라미터 값까지 조건으로 쓸 수 있다.

## body

## 핵심
- `@EnableMethodSecurity`로 켠다. (예전 `@EnableGlobalMethodSecurity`는 사용 중단되었다.)
- `@PreAuthorize("hasRole('ADMIN')")`: 관리자만 호출할 수 있다.
- `@PreAuthorize("#userId == authentication.principal.id")`: 파라미터와 로그인 사용자를 비교한다.
- 실패하면 `AccessDeniedException` → 403.

## URL 규칙과 비교
- URL 규칙(`requestMatchers("/api/admin/**").hasRole("ADMIN")`)은 한눈에 보이고 경로 단위로 막는다.
- 메서드 보안은 같은 서비스가 여러 경로에서 불려도 한곳에서 지킨다.
- 소유자 확인(내 글만 수정)은 DB를 조회해야 하므로 Service 안의 코드로 하는 편이 더 명확할 때가 많다.

## 주의
- 프록시 방식이라 같은 클래스 내부 호출에는 적용되지 않는다.
- SpEL 문자열 오타는 실행 시점에야 드러난다. 테스트로 확인한다.

## example

```java
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig { }
```

```java
@Service
public class AdminConceptService {

    @PreAuthorize("hasRole('ADMIN')")
    public void publish(long conceptId) {
        conceptMapper.updateStatus(conceptId, Status.PUBLISHED);
    }
}
```

```java
@PreAuthorize("#userId == authentication.principal.id")
public ProfileResponse getMyProfile(long userId) { ... }
```
