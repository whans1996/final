---
name: Bean Validation (@Valid)
category: web
difficulty: JUNIOR
reference: https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html
---

## summary

Bean Validation은 DTO 필드에 @NotBlank·@Size·@Email 같은 제약 어노테이션을 달고, 컨트롤러 파라미터에 @Valid를 붙여 요청을 자동 검증하는 표준이다.

## body

## 핵심
- 검증에 실패하면 컨트롤러 메서드가 실행되지 않고 `MethodArgumentNotValidException`이 난다.
- 전역 예외 처리에서 필드별 오류 메시지를 모아 400 응답으로 바꾼다.
- `spring-boot-starter-validation` 의존성이 필요하다.

## Boot 3에서 바뀐 점
- 패키지가 `javax.validation`에서 **`jakarta.validation`**으로 바뀌었다(Jakarta EE 9+). AI가 `import javax.validation.constraints.NotBlank;`를 쓰면 Boot 3에서 컴파일되지 않는다.
- 같은 이유로 `javax.servlet` → `jakarta.servlet`, `javax.persistence` → `jakarta.persistence`.

## 주의
- `@Valid`를 빼먹으면 어노테이션이 있어도 검증되지 않는다. AI 코드에서 자주 빠진다.
- 중첩 객체·리스트 안의 요소를 검증하려면 그 필드에도 `@Valid`를 붙인다.
- 입력 형식 검증은 DTO에서, "이미 가입된 이메일" 같은 비즈니스 규칙 검증은 Service에서 한다.

## example

```java
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Size(max = 20) String nickname) { }
```

```java
@PostMapping("/api/auth/signup")
public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest req) { ... }
```
