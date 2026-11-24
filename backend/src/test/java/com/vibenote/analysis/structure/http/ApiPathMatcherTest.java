package com.vibenote.analysis.structure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.vibenote.analysis.explain.ApiEndpoint;
import com.vibenote.analysis.structure.WorkBudget;
import java.time.Duration;
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
        return ApiPathMatcher.match(List.of(apis), contextPath, request, WorkBudget.unlimited());
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
                                api("GET", "/api/items/{code}", 100)))
                .isEqualTo(ApiMatch.matched(100));
    }

    @Test
    void AC11_FE_와일드카드가_API_리터럴과_맞섰으면_후보가_한_파일일_때만_고른다() {
        // 와일드카드가 search일 수도 다른 값일 수도 있다
        assertThat(
                        match(
                                        req("GET", "/api/todos/*"),
                                        "",
                                        api("GET", "/api/todos/search", 100),
                                        api("GET", "/api/todos/{id}", 101))
                                .status())
                .isEqualTo(ApiMatch.Status.AMBIGUOUS);
        assertThat(
                        match(
                                        req("GET", "/api/items/*"),
                                        "",
                                        api("GET", "/api/items/{id}", 100),
                                        api("GET", "/api/items/new", 100),
                                        api("GET", "/api/items/{code}", 101))
                                .status())
                .isEqualTo(ApiMatch.Status.AMBIGUOUS);
        // 변수·이중 별표와만 맞섰으면 지금처럼 가장 구체적인 후보를 고른다
        assertThat(
                        match(
                                req("GET", "/api/todos/*"),
                                "",
                                api("GET", "/api/todos/{id}", 100),
                                api("GET", "/api/**", 101)))
                .isEqualTo(ApiMatch.matched(100));
    }

    @Test
    void AC11_메서드를_모르면_후보가_한_파일일_때만_고른다() {
        assertThat(
                        match(
                                        req(null, "/api/items/new"),
                                        "",
                                        api("GET", "/api/items/new", 100),
                                        api("DELETE", "/api/items/{id}", 101))
                                .status())
                .isEqualTo(ApiMatch.Status.AMBIGUOUS);
        assertThat(
                        match(
                                req(null, "/api/items/new"),
                                "",
                                api("GET", "/api/items/new", 100),
                                api("DELETE", "/api/items/{id}", 100)))
                .isEqualTo(ApiMatch.matched(100));
    }

    @Test
    void AC11_API_목록이_불완전하면_변수_와일드카드_없는_일치만_확정한다() {
        ApiPathMatcher matcher =
                new ApiPathMatcher(
                        List.of(
                                api("GET", "/api/todos/search", 100),
                                api("GET", "/api/todos/{id}", 101),
                                api("GET", "/api/files/**", 102)),
                        "",
                        true);
        WorkBudget budget = WorkBudget.unlimited();

        assertThat(matcher.match(req("GET", "/api/todos/search"), budget))
                .isEqualTo(ApiMatch.matched(100));
        // 목록에 없는 /api/todos/latest 같은 API가 받을 수 있다
        assertThat(matcher.match(req("GET", "/api/todos/5"), budget).status())
                .isEqualTo(ApiMatch.Status.AMBIGUOUS);
        assertThat(matcher.match(req("GET", "/api/files/a"), budget).status())
                .isEqualTo(ApiMatch.Status.AMBIGUOUS);
        assertThat(matcher.match(req("GET", "/api/todos/*"), budget).status())
                .isEqualTo(ApiMatch.Status.AMBIGUOUS);
        assertThat(matcher.match(req("GET", "/api/none"), budget).status())
                .isEqualTo(ApiMatch.Status.NONE);
    }

    @Test
    void AC11_순위는_이중_별표_없는_것_리터럴_많은_것_변수_적은_것이고_별표는_변수보다_무겁다() {
        // 이중 별표가 있으면 리터럴이 많아도 밀린다 (Spring 순서)
        assertThat(
                        match(
                                req("GET", "/api/a/b/c"),
                                "",
                                api("GET", "/api/a/b/**", 100),
                                api("GET", "/api/{x}/{y}/{z}", 101)))
                .isEqualTo(ApiMatch.matched(101));
        assertThat(
                        match(
                                req("GET", "/api/a/b"),
                                "",
                                api("GET", "/api/*/b", 100),
                                api("GET", "/api/{x}/b", 101)))
                .isEqualTo(ApiMatch.matched(101));
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

    @Test
    void AC11_정규식_변수가_백트래킹을_오래_하면_맞지_않은_것으로_본다() {
        FeRequest request = req("GET", "/x/" + "a".repeat(ApiPathMatcher.MAX_REGEX_INPUT));

        ApiMatch result =
                assertTimeoutPreemptively(
                        Duration.ofSeconds(5),
                        () -> match(request, "", api("GET", "/x/{v:a*a*a*a*a*a*a*a*b}", 100)));

        assertThat(result).isEqualTo(ApiMatch.NONE);
    }

    @Test
    void AC11_이중_별표가_많아도_제한_시간_안에_판정한다() {
        FeRequest request = req("GET", "/a".repeat(40));
        String path = "/**".repeat(30) + "/z";

        ApiMatch result =
                assertTimeoutPreemptively(
                        Duration.ofSeconds(5), () -> match(request, "", api("GET", path, 100)));

        assertThat(result).isEqualTo(ApiMatch.NONE);
        assertThat(match(req("GET", "/a/b/z"), "", api("GET", path, 100)))
                .isEqualTo(ApiMatch.matched(100));
    }

    @Test
    void AC11_변수와_글자가_섞인_세그먼트는_세그먼트_전체를_비교한다() {
        assertThat(match(req("GET", "/files/foo.txt"), "", api("GET", "/files/{name}.json", 100)))
                .isEqualTo(ApiMatch.NONE);
        assertThat(match(req("GET", "/files/foo.json"), "", api("GET", "/files/{name}.json", 100)))
                .isEqualTo(ApiMatch.matched(100));
        assertThat(match(req("GET", "/files/foo.txt"), "", api("GET", "/files/*.json", 100)))
                .isEqualTo(ApiMatch.NONE);
        assertThat(match(req("GET", "/files/a.json"), "", api("GET", "/files/*.json", 100)))
                .isEqualTo(ApiMatch.matched(100));
        assertThat(match(req("GET", "/v12/x"), "", api("GET", "/v{n:\\d+}/x", 100)))
                .isEqualTo(ApiMatch.matched(100));
        assertThat(match(req("GET", "/vx/x"), "", api("GET", "/v{n:\\d+}/x", 100)))
                .isEqualTo(ApiMatch.NONE);
        // FE 와일드카드는 섞인 세그먼트와도 맞는다
        assertThat(match(req("GET", "/files/*"), "", api("GET", "/files/{name}.json", 100)))
                .isEqualTo(ApiMatch.matched(100));
    }

    @Test
    void AC11_context_path가_아주_길어도_메모리와_시간_안에_판정한다() {
        String contextPath = "/a".repeat(200_000);
        FeRequest request = req("GET", "/a".repeat(1_000));
        ApiEndpoint[] apis = new ApiEndpoint[30];
        for (int i = 0; i < apis.length; i++) {
            apis[i] = api("GET", "/api/todos/" + i, 100 + i);
        }

        ApiMatch result =
                assertTimeoutPreemptively(
                        Duration.ofSeconds(5), () -> match(request, contextPath, apis));

        assertThat(result).isEqualTo(ApiMatch.NONE);
        assertThat(match(req("GET", "/a/a/x/y"), "/**/**/a", api("GET", "/x/{v}", 100)))
                .isEqualTo(ApiMatch.matched(100));
    }

    @Test
    void AC11_긴_context_path는_API마다_복사하지_않는다() {
        String contextPath = "/a".repeat(500_000);
        List<ApiEndpoint> apis =
                java.util.stream.IntStream.range(0, 2_000)
                        .mapToObj(i -> api("GET", "/api/todos/" + i, 100 + i))
                        .toList();

        ApiMatch result =
                assertTimeoutPreemptively(
                        Duration.ofSeconds(5),
                        () ->
                                new ApiPathMatcher(apis, contextPath)
                                        .match(req("GET", "/api/todos/1"), WorkBudget.unlimited()));

        assertThat(result).isEqualTo(ApiMatch.NONE);
        // context-path 끝과 API 앞의 이중 별표는 하나로 합친다
        assertThat(match(req("GET", "/x/y"), "/x/**", api("GET", "/**/y", 100)))
                .isEqualTo(ApiMatch.matched(100));
        assertThat(match(req("GET", "/x/k/m/y"), "/x/**", api("GET", "/**/y", 100)))
                .isEqualTo(ApiMatch.matched(100));
    }
}
