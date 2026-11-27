---
name: enum 활용
category: java
difficulty: JUNIOR
reference: https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html
---

## summary

enum은 정해진 상수 집합을 타입으로 만든다. 단순 상수를 넘어 필드·생성자·메서드를 가질 수 있어, 상태별 값과 동작을 한곳에 모은다.

## body

## 핵심
- 문자열 상수 대신 enum을 쓰면 오타를 컴파일 시점에 잡는다.
- 상수마다 값(HTTP 상태, 메시지, 점수)을 생성자로 넣어 둔다.
- `values()`로 전체 목록, `valueOf("JUNIOR")`로 문자열 변환.
- MyBatis는 enum을 이름 문자열로 저장·조회한다(MySQL `ENUM`·`VARCHAR` 컬럼과 잘 맞는다). Jackson도 이름으로 직렬화한다.

## AI 코드에서 보이는 모습
에러 코드(`ErrorCode.USER_NOT_FOUND(404, "...")`), 상태(`DRAFT·PUBLISHED`), 수준(`BEGINNER·JUNIOR·SENIOR`)에 자주 쓰인다.

## 주의
- `ordinal()`(선언 순서 번호)을 DB에 저장하면 상수 순서를 바꿀 때 데이터가 어긋난다. 이름으로 저장한다.
- 잘못된 문자열을 `valueOf`하면 `IllegalArgumentException`이 난다. 요청 파라미터 변환 실패는 400으로 처리한다.

## example

```java
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() { return status; }
    public String message() { return message; }
}
```

```java
throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
```
