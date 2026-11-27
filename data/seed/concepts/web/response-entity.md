---
name: ResponseEntity와 HTTP 상태 코드
category: web
difficulty: BEGINNER
reference: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html
---

## summary

ResponseEntity는 응답 본문과 함께 HTTP 상태 코드와 헤더를 직접 정할 수 있는 반환 타입이다. 상태 코드는 요청 결과를 클라이언트에게 알리는 약속이다.

## body

## 핵심
- 2xx 성공(200 OK, 201 Created, 202 Accepted, 204 No Content), 4xx 클라이언트 오류(400, 401, 403, 404, 409), 5xx 서버 오류.
- 생성은 201과 `Location` 헤더, 비동기 접수는 202, 본문 없는 삭제는 204가 자주 쓰인다.
- 객체만 반환하면 기본은 200이다.

## 주의
에러인데 200과 `{"success": false}`를 보내면 클라이언트와 모니터링이 실패를 알아채기 어렵다. 상태 코드로 표현한다.

## example

```java
@PostMapping("/api/posts")
public ResponseEntity<PostResponse> create(@RequestBody PostRequest req) {
    PostResponse created = postService.create(req);
    return ResponseEntity
            .created(URI.create("/api/posts/" + created.id()))
            .body(created);
}
```

```java
@DeleteMapping("/api/posts/{id}")
public ResponseEntity<Void> delete(@PathVariable long id) {
    postService.delete(id);
    return ResponseEntity.noContent().build();
}
```
