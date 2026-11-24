package com.vibenote.analysis.structure.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.explain.ApiEndpoint;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** F-21a AC11 FE 요청 ↔ 탐지한 API 경로 비교 (docs/specs/F-21a-structure-graph.md). */
class ApiPathMatcherTest {

    /** "*"는 FE 와일드카드 세그먼트. */
    private static FeRequest req(String method, String path) {
        List<PathSegment> segments =
                Arrays.stream(path.split("/"))
                        .filter(s -> !s.isEmpty())
                        .map(s -> s.equals("*") ? PathSegment.WILDCARD : PathSegment.literal(s))
                        .toList();
        return new FeRequest(method, segments);
    }

    private static ApiEndpoint api(String method, String path, long fileId) {
        return new ApiEndpoint(method, path, fileId);
    }

    private static ApiMatch match(FeRequest request, String contextPath, ApiEndpoint... apis) {
        return ApiPathMatcher.match(List.of(apis), contextPath, request);
    }

    @Test
    void AC11_리터럴_세그먼트가_많은_API를_고른다() {
        ApiMatch result =
                match(
                        req("GET", "/api/todos/search"),
                        "",
                        api("GET", "/api/todos/{id}", 101),
                        api("GET", "/api/todos/search", 100));

        assertThat(result).isEqualTo(ApiMatch.matched(100));
    }

    @Test
    void AC11_FE_와일드카드는_변수와_리터럴_세그먼트_모두와_맞는다() {
        assertThat(match(req("DELETE", "/api/todos/*"), "", api("DELETE", "/api/todos/{id}", 100)))
                .isEqualTo(ApiMatch.matched(100));
        assertThat(match(req("GET", "/api/*/list"), "", api("GET", "/api/todos/list", 100)))
                .isEqualTo(ApiMatch.matched(100));
    }

    @Test
    void AC11_메서드가_다르면_NO_MATCH이고_ANY와_메서드_모름은_메서드를_보지_않는다() {
        assertThat(match(req("DELETE", "/api/todos"), "", api("GET", "/api/todos", 100)).status())
                .isEqualTo(ApiMatch.Status.NONE);
        assertThat(match(req("DELETE", "/api/todos"), "", api("ANY", "/api/todos", 100)))
                .isEqualTo(ApiMatch.matched(100));
        assertThat(match(req(null, "/api/todos"), "", api("POST", "/api/todos", 100)))
                .isEqualTo(ApiMatch.matched(100));
    }

    @Test
    void AC11_최상위_후보가_여러_파일이면_AMBIGUOUS이고_같은_파일이면_간선() {
        assertThat(
                        match(
                                        req("GET", "/api/items/*"),
                                        "",
                                        api("GET", "/api/items/{id}", 100),
                                        api("GET", "/api/items/{code}", 101))
                                .status())
                .isEqualTo(ApiMatch.Status.AMBIGUOUS);
        assertThat(
                        match(
                                req("GET", "/api/items/*"),
                                "",
                                api("GET", "/api/items/{id}", 100),
                                api("GET", "/api/items/new", 100),
                                api("GET", "/api/items/{code}", 101)))
                .isEqualTo(ApiMatch.matched(100));
    }

    @Test
    void AC11_이중_별표는_0개_이상이고_더_구체적인_후보에_밀린다() {
        assertThat(match(req("GET", "/api/files/a/b"), "", api("GET", "/api/files/**", 100)))
                .isEqualTo(ApiMatch.matched(100));
        assertThat(match(req("GET", "/api/files"), "", api("GET", "/api/files/{*rest}", 100)))
                .isEqualTo(ApiMatch.matched(100));
        assertThat(
                        match(
                                req("GET", "/api/files/a"),
                                "",
                                api("GET", "/api/**", 101),
                                api("GET", "/api/files/{name}", 100)))
                .isEqualTo(ApiMatch.matched(100));
    }

    @Test
    void AC11_정규식_변수는_FE_리터럴이면_정규식까지_맞아야_한다() {
        ApiEndpoint numeric = api("GET", "/api/todos/{id:\\d+}", 100);

        assertThat(match(req("GET", "/api/todos/12"), "", numeric))
                .isEqualTo(ApiMatch.matched(100));
        assertThat(match(req("GET", "/api/todos/abc"), "", numeric).status())
                .isEqualTo(ApiMatch.Status.NONE);
        assertThat(match(req("GET", "/api/todos/*"), "", numeric)).isEqualTo(ApiMatch.matched(100));
        // 컴파일되지 않는 정규식은 1세그먼트 변수로 본다
        assertThat(match(req("GET", "/api/todos/abc"), "", api("GET", "/api/todos/{id:[}", 100)))
                .isEqualTo(ApiMatch.matched(100));
    }

    @Test
    void AC11_context_path를_API_경로_앞에_붙여_비교한다() {
        assertThat(match(req("GET", "/app/api/todos"), "/app/", api("GET", "/api/todos", 100)))
                .isEqualTo(ApiMatch.matched(100));
        assertThat(match(req("GET", "/api/todos"), "/app", api("GET", "/api/todos", 100)).status())
                .isEqualTo(ApiMatch.Status.NONE);
    }

    @Test
    void AC11_요청_표기는_메서드와_별표_경로다() {
        assertThat(req("GET", "/api/todos/*").toString()).isEqualTo("GET /api/todos/*");
        assertThat(req(null, "/").toString()).isEqualTo("/");
    }
}
