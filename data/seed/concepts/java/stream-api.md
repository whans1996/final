---
name: 스트림 API
category: java
difficulty: JUNIOR
reference: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/stream/package-summary.html
---

## summary

스트림은 컬렉션 데이터를 filter·map·collect 같은 연산의 연결로 처리하는 방법이다. 반복문 대신 "무엇을 할지"를 선언적으로 쓴다.

## body

## 핵심
- 생성(`list.stream()`) → 중간 연산(`filter`, `map`, `sorted`) → 최종 연산(`toList`, `collect`, `count`) 순서로 쓴다.
- 중간 연산은 최종 연산이 호출될 때 실행된다(지연 실행).
- 스트림은 한 번만 소비할 수 있다.

## 최신 문법
- Java 16부터 `.toList()`로 바로 리스트를 만든다. 결과는 **수정할 수 없는 리스트**다.
- 예전 코드의 `.collect(Collectors.toList())`는 수정 가능한 `ArrayList`를 돌려준다. 결과에 `add`하는 코드가 있으면 둘을 바꿔 쓸 때 `UnsupportedOperationException`이 난다.

## 주의
- 스트림 안에서 외부 리스트에 `add`하는 식의 부수 효과는 피한다. 결과는 `toList`·`collect`로 만든다.
- 단순한 반복은 for문이 더 읽기 쉬울 때도 있다.

## example

```java
List<String> names = concepts.stream()
        .filter(c -> c.status() == Status.PUBLISHED)
        .map(Concept::name)
        .toList();                                   // Java 16+, 불변 리스트
```

```java
Map<String, Long> countByCategory = concepts.stream()
        .collect(Collectors.groupingBy(Concept::category, Collectors.counting()));
```
