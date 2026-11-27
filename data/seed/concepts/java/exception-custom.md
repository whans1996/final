---
name: 예외 처리와 커스텀 예외
category: java
difficulty: BEGINNER
reference: https://docs.oracle.com/javase/tutorial/essential/exceptions/
---

## summary

예외는 정상 흐름을 벗어난 상황을 호출한 쪽에 알리는 방법이다. try-catch로 처리하거나 위로 던지고, 의미가 분명한 커스텀 예외로 원인을 드러낸다.

## body

## 핵심
- checked 예외(`Exception` 계열)는 컴파일러가 처리를 강제하고, unchecked 예외(`RuntimeException` 계열)는 강제하지 않는다.
- 스프링 애플리케이션은 보통 `RuntimeException`을 상속한 비즈니스 예외를 만들고, 전역 핸들러가 응답으로 바꾼다.

## 주의
- `catch (Exception e) {}`처럼 예외를 삼키면 문제가 조용히 사라진다. 최소한 로그를 남기거나 다시 던진다.
- 다시 던질 때는 원래 예외를 cause로 넘겨야 스택 트레이스가 이어진다.

## example

```java
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
```

```java
public User findUser(long id) {
    User user = userMapper.findById(id);
    if (user == null) {
        throw new NotFoundException("회원이 없습니다: " + id);
    }
    return user;
}
```
