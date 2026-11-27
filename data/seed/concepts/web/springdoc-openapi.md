---
name: OpenAPI 문서화 (springdoc)
category: web
difficulty: BEGINNER
reference: https://springdoc.org/
---

## summary

springdoc-openapi는 스프링 컨트롤러를 분석해 OpenAPI 명세(JSON)와 Swagger UI 화면을 자동으로 만들어 준다. 어노테이션으로 설명과 예시를 보강한다.

## body

## 핵심
- 의존성만 추가하면 `/v3/api-docs`(명세)와 `/swagger-ui/index.html`(화면)이 생긴다.
- `@Tag`(그룹), `@Operation`(요약), `@ApiResponse`(응답 코드별 설명), `@Schema`(필드 설명·예시)를 단다.
- 프론트엔드와 API 계약을 맞추는 기준 문서로 쓸 수 있다.

## 주의
운영 환경에서 Swagger UI를 공개할지 결정해야 한다. 내부 API 구조가 그대로 노출된다.

## example

```java
@Tag(name = "게시판", description = "게시글 CRUD")
@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Operation(summary = "게시글 상세 조회")
    @ApiResponse(responseCode = "404", description = "게시글 없음")
    @GetMapping("/{id}")
    public PostResponse get(@PathVariable long id) { ... }
}
```

```java
public record PostResponse(
        @Schema(description = "게시글 ID", example = "42") long id,
        @Schema(description = "제목", example = "MyBatis 질문") String title) { }
```
