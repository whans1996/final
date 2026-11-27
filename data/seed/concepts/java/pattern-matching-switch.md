---
name: 패턴 매칭 switch와 record 패턴 (Java 21)
category: java
difficulty: SENIOR
reference: https://docs.oracle.com/en/java/javase/21/language/pattern-matching-switch.html
---

## summary

Java 21부터 switch에서 값뿐 아니라 타입으로 분기하고(case Circle c), record를 분해(case Point(int x, int y))할 수 있다. sealed 인터페이스와 함께 쓰면 모든 경우를 처리했는지 컴파일러가 확인한다.

## body

## 핵심
- **Java 21부터** 쓸 수 있다. 바이브노트 백엔드는 Java 17이라 이 문법이 컴파일되지 않지만, 분석하는 저장소에서는 만날 수 있다.
- `case 타입 변수 ->`: 타입별 분기와 형변환을 한 번에 한다.
- `case Record(타입 a, 타입 b) ->`: record의 구성 요소를 바로 꺼낸다.
- `case X x when 조건 ->`: 가드 조건을 붙인다.
- `sealed interface Shape permits Circle, Square`처럼 하위 타입을 닫아 두면 `default` 없이도 빠진 경우를 컴파일 오류로 알려 준다.
- `case null ->`로 null도 switch 안에서 처리할 수 있다.

## Java 17에서는
같은 일을 `if (shape instanceof Circle c) ... else if ...` 연쇄로 한다.

## 주의
- 위쪽 case가 아래 case를 항상 가리면(더 넓은 타입이 먼저) 컴파일 오류가 난다. 좁은 타입부터 쓴다.

## example

```java
sealed interface Result permits Success, Failure { }
record Success(String data) implements Result { }
record Failure(String code, String message) implements Result { }
```

```java
String render(Result result) {
    return switch (result) {
        case Success(String data) -> "성공: " + data;
        case Failure(String code, String msg) when code.startsWith("LLM") -> "AI 오류: " + msg;
        case Failure(String code, String msg) -> "오류(" + code + "): " + msg;
    };   // sealed라 default 불필요
}
```
