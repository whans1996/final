---
name: 람다와 함수형 인터페이스
category: java
difficulty: JUNIOR
reference: https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html
---

## summary

람다는 메서드 하나짜리 인터페이스(함수형 인터페이스)의 구현을 짧게 쓰는 문법이다. 동작 자체를 값처럼 넘길 수 있게 해 준다.

## body

## 핵심
- 추상 메서드가 하나인 인터페이스면 람다로 구현할 수 있다. `Function`, `Predicate`, `Supplier`, `Consumer`가 대표적이다.
- `(파라미터) -> 식` 형태이며, 메서드 참조(`String::length`)로 더 줄일 수 있다.

## AI 코드에서 보이는 모습
스트림, `Optional`, 정렬 기준(`Comparator`), 콜백에서 자주 나온다.

## 주의
람다 안에서 바깥 지역 변수는 읽기만 할 수 있다(사실상 final). 람다가 길어지면 이름 있는 메서드로 빼는 편이 읽기 쉽다.

## example

```java
List<String> names = List.of("kim", "lee", "park");
```

```java
// 길이가 3보다 큰 이름만, 대문자로
Predicate<String> longName = name -> name.length() > 3;
Function<String, String> upper = String::toUpperCase;
```

```java
List<String> result = names.stream()
        .filter(longName)
        .map(upper)
        .toList();   // [PARK]
```
