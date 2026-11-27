---
name: @RestController와 요청 매핑
category: web
difficulty: BEGINNER
reference: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html
---

## summary

@RestController는 메서드 반환값을 JSON 같은 응답 본문으로 바로 쓰는 컨트롤러다. @GetMapping·@PostMapping 등으로 URL과 HTTP 메서드를 메서드에 연결한다.

## body

## 핵심
- `@RestController` = `@Controller` + `@ResponseBody`.
- 클래스의 `@RequestMapping("/api/posts")`가 공통 경로, 메서드의 `@GetMapping("/{id}")`가 세부 경로다.
- 반환한 객체는 Jackson이 JSON으로 바꾼다.

## 주의
같은 URL과 메서드 조합이 두 군데 매핑되면 애플리케이션이 시작되지 않는다.

## example

```java
@RestController
@RequestMapping("/api/posts")
public class PostController {

    @GetMapping
    public List<PostResponse> list() { return postService.findAll(); }

    @PostMapping
    public PostResponse create(@RequestBody PostRequest req) { return postService.create(req); }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) { postService.delete(id); }
}
```
