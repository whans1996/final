---
name: 제네릭
category: java
difficulty: JUNIOR
reference: https://docs.oracle.com/javase/tutorial/java/generics/
---

## summary

제네릭은 클래스나 메서드가 다룰 타입을 파라미터로 받는 문법이다(List<String>). 잘못된 타입을 컴파일 시점에 막고 형변환을 없앤다.

## body

## 핵심
- `class Box<T>`, `<T> T first(List<T> list)`처럼 타입 파라미터를 선언한다.
- `List<String>`에는 String만 들어가므로 꺼낼 때 형변환이 필요 없다.
- 와일드카드: `List<? extends Number>`는 읽기 전용(생산자), `List<? super Integer>`는 쓰기용(소비자)으로 쓴다.
- 다이아몬드 `new ArrayList<>()`로 오른쪽 타입을 생략한다.

## AI 코드에서 보이는 모습
공통 응답 래퍼 `ApiResponse<T>`, 페이지 응답 `PageResponse<T>`, `ResponseEntity<T>`, MyBatis 결과 `List<T>`에서 자주 보인다.

## 주의
- 로 타입(`List list`)을 쓰면 타입 검사가 꺼져 실행 중 `ClassCastException`이 날 수 있다.
- 제네릭 타입 정보는 실행 시점에 지워진다(타입 소거). `new T()`나 `instanceof List<String>`은 할 수 없다.

## example

```java
public record PageResponse<T>(List<T> items, long total, int page, int size) {

    public <R> PageResponse<R> map(Function<T, R> mapper) {
        return new PageResponse<>(items.stream().map(mapper).toList(), total, page, size);
    }
}
```

```java
PageResponse<User> users = userService.findPage(page);
PageResponse<UserResponse> body = users.map(UserResponse::from);
```
