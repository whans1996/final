---
name: 전역 예외 처리 (@RestControllerAdvice)
category: web
difficulty: JUNIOR
reference: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html
---

## summary

@RestControllerAdvice 클래스의 @ExceptionHandler 메서드는 모든 컨트롤러에서 던진 예외를 한곳에서 잡아 일관된 에러 응답으로 바꾼다.

## body

## 핵심
- 컨트롤러마다 try-catch를 쓰지 않아도 된다.
- 비즈니스 예외는 에러 코드·HTTP 상태·메시지로, 검증 예외는 필드 오류 목록으로, 예상 못한 예외는 500으로 변환한다.
- 더 구체적인 예외 타입의 핸들러가 우선한다.

## 응답 형식: ProblemDetail과 자체 형식
- **ProblemDetail**(Spring 6): HTTP API 에러 표준 RFC 9457 형식(`type`, `title`, `status`, `detail`, `instance`)을 만드는 클래스다. 필요한 필드는 `setProperty`로 더한다. 스프링이 던지는 기본 예외(검증 실패, 지원하지 않는 메서드 등)도 같은 형식으로 바꾸려면 `ResponseEntityExceptionHandler`를 상속하거나 `spring.mvc.problemdetails.enabled=true`를 쓴다.
- **자체 형식**: `{code, message, details}`처럼 팀이 정한 구조. 프론트엔드가 `code`로 분기하기 쉽다. 바이브노트가 이 방식이다.
- 어느 쪽이든 **모든 API가 같은 형식**을 쓰는 것이 핵심이다. 섞여 있으면 프론트엔드가 에러를 해석하지 못한다.

## 주의
500 응답에 예외 메시지나 스택 트레이스를 그대로 담으면 내부 정보가 노출된다. 로그에만 남기고 응답은 일반 메시지로 보낸다.

## example

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 자체 형식: {"code":"USER_404","message":"회원을 찾을 수 없습니다"}
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handle(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.status())
                .body(new ErrorResponse(code.name(), code.message()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        log.error("처리되지 않은 예외", e);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("SERVER_500", "서버 오류가 발생했습니다"));
    }
}
```

```java
// ProblemDetail 방식 (RFC 9457)
@ExceptionHandler(BusinessException.class)
public ProblemDetail handle(BusinessException e) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(e.getErrorCode().status(), e.getMessage());
    pd.setTitle("요청을 처리할 수 없습니다");
    pd.setProperty("code", e.getErrorCode().name());
    return pd;
}
// {"type":"about:blank","title":"요청을 처리할 수 없습니다","status":404,"detail":"...","code":"USER_404"}
```
