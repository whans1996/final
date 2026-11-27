---
name: 요청 파라미터 바인딩 (@PathVariable·@RequestParam·@RequestBody)
category: web
difficulty: BEGINNER
reference: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/arguments.html
---

## summary

컨트롤러 메서드 파라미터에 어노테이션을 붙여 URL 경로, 쿼리 문자열, 요청 본문(JSON)의 값을 자바 객체로 받는다.

## body

## 핵심
- `@PathVariable`: `/users/{id}`의 `id`처럼 경로의 일부.
- `@RequestParam`: `?page=2&keyword=spring` 같은 쿼리 문자열. `required = false`, `defaultValue`로 선택값을 지정한다.
- `@RequestBody`: 요청 본문 JSON을 객체(주로 record DTO)로 변환한다.

## 주의
- 식별자는 경로, 검색·필터·정렬 조건은 쿼리, 생성·수정 데이터는 본문에 두는 것이 일반적이다.
- 본문 JSON 형식이 틀리면 400 응답이 난다. 전역 예외 처리에서 일관된 에러 형식으로 바꿔 준다.

## example

```java
@GetMapping("/api/concepts")
public PageResponse<ConceptResponse> search(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") int page) { ... }
```

```java
@PatchMapping("/api/posts/{id}")
public PostResponse update(@PathVariable long id, @RequestBody PostUpdateRequest req) { ... }
```
