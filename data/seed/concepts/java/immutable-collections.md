---
name: 컬렉션과 불변 컬렉션 (List·Set·Map)
category: java
difficulty: BEGINNER
reference: https://docs.oracle.com/en/java/javase/17/core/creating-immutable-lists-sets-and-maps.html
---

## summary

List는 순서가 있는 목록, Set은 중복 없는 집합, Map은 키-값 쌍이다. Java 9부터 List.of·Set.of·Map.of로 수정할 수 없는 컬렉션을 간단히 만든다.

## body

## 핵심
- 구현체 선택: 대부분 `ArrayList`, `HashSet`, `HashMap`. 넣은 순서가 필요하면 `LinkedHashMap`, 정렬이 필요하면 `TreeMap`.
- 변수 타입은 인터페이스(`List`, `Map`)로 선언한다.
- `List.of(...)`, `Map.of(...)`, `List.copyOf(...)`: 불변 컬렉션. 수정하면 `UnsupportedOperationException`.
- `map.getOrDefault`, `computeIfAbsent`, `merge`로 "없으면 넣기"를 짧게 쓴다.

## 주의
- `List.of`·`Map.of`는 null을 넣을 수 없다.
- `Map.of`는 순서를 보장하지 않는다. 화면 출력 순서가 중요하면 `LinkedHashMap`을 쓴다.
- 반복문으로 순회하면서 같은 컬렉션에서 삭제하면 `ConcurrentModificationException`이 난다. `removeIf`를 쓴다.

## example

```java
List<String> levels = List.of("BEGINNER", "JUNIOR", "SENIOR");   // 불변
```

```java
Map<String, List<Concept>> byCategory = new HashMap<>();
for (Concept c : concepts) {
    byCategory.computeIfAbsent(c.category(), k -> new ArrayList<>()).add(c);
}
```

```java
Map<String, Integer> wrongCount = new HashMap<>();
wrongCount.merge(conceptName, 1, Integer::sum);   // 없으면 1, 있으면 +1
```

```java
concepts.removeIf(c -> c.status() == Status.HIDDEN);
```
