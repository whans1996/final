---
name: switch 식과 instanceof 패턴 매칭
category: java
difficulty: JUNIOR
reference: https://docs.oracle.com/en/java/javase/17/language/switch-expressions-and-statements.html
---

## summary

Java 14의 switch 식은 화살표(->)로 값을 바로 돌려주고 break 누락 실수를 없앤다. Java 16의 instanceof 패턴 매칭은 타입 확인과 형변환을 한 번에 한다.

## body

## 핵심
- switch 식: `case A, B -> 값;` 형태로 쓰고, switch 전체가 값이 된다. 위에서 아래로 떨어지는(fall-through) 동작이 없다.
- enum을 switch 식으로 다룰 때 모든 상수를 처리하면 `default`가 없어도 된다. 상수가 새로 추가되면 컴파일 오류로 알려 준다.
- 여러 줄이 필요하면 블록 안에서 `yield 값;`으로 돌려준다.
- `if (obj instanceof String s)`: 확인이 통과하면 `s`를 바로 String으로 쓴다. 따로 `(String) obj` 형변환을 쓰지 않는다.

## AI 코드에서 보이는 모습
상태 enum(예: 분석 단계, 주문 상태)에 따라 메시지·진행률을 정하는 코드에서 자주 보인다. 예전 스타일 `case X: ... break;`와 섞여 있으면 새 문법으로 정리할 수 있다.

## 주의
- 화살표(`->`)와 콜론(`:`) 스타일을 한 switch 안에서 섞을 수 없다.
- `default`를 습관처럼 넣으면 enum 상수가 추가돼도 컴파일러가 알려 주지 못한다.

## example

```java
enum Status { FETCHING, EXPLAINING, QUIZ, DONE, FAILED }
```

```java
int progress = switch (status) {
    case FETCHING -> 10;
    case EXPLAINING, QUIZ -> 60;
    case DONE -> 100;
    case FAILED -> {
        log.warn("분석 실패");
        yield 0;
    }
};   // 모든 상수를 다뤘으므로 default 불필요
```

```java
// instanceof 패턴 매칭 (Java 16+)
if (ex instanceof BusinessException be) {
    return be.getErrorCode();
}
```
