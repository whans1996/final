---
name: sealed 클래스와 인터페이스
category: java
difficulty: JUNIOR
reference: https://docs.oracle.com/en/java/javase/17/language/sealed-classes-and-interfaces.html
---

## summary

sealed(Java 17)는 어떤 클래스가 이 타입을 상속·구현할 수 있는지 permits로 정해 두는 문법이다. 하위 타입이 닫혀 있으므로 "가능한 경우가 이것뿐"임을 컴파일러가 안다.

## body

## 핵심
- `sealed interface Shape permits Circle, Square { }`: Circle과 Square만 Shape를 구현할 수 있다.
- 허용된 하위 타입은 `final`, `sealed`, `non-sealed` 중 하나를 선언해야 한다. record는 자동으로 final이라 잘 어울린다.
- 결과 타입(성공·실패), 이벤트 종류, 결제 수단처럼 경우의 수가 정해진 모델에 쓴다.
- Java 21의 패턴 매칭 switch와 함께 쓰면 `default` 없이 모든 경우를 처리했는지 컴파일러가 확인한다.

## 주의
- 같은 패키지(모듈을 쓰면 같은 모듈)에 있어야 permits로 묶을 수 있다.
- 나중에 하위 타입을 자주 추가해야 하는 확장 지점에는 맞지 않는다.

## example

```java
public sealed interface PaymentMethod permits Card, BankTransfer, Point { }
```

```java
public record Card(String number) implements PaymentMethod { }
public record BankTransfer(String bank, String account) implements PaymentMethod { }
public record Point(long amount) implements PaymentMethod { }
```

```java
// Java 17: instanceof 패턴 매칭으로 분기
String describe(PaymentMethod m) {
    if (m instanceof Card c) return "카드 " + c.number().substring(12);
    if (m instanceof BankTransfer b) return b.bank() + " 이체";
    if (m instanceof Point p) return p.amount() + "포인트";
    throw new IllegalStateException();   // Java 21 switch에서는 불필요
}
```
