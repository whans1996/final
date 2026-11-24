package com.vibenote.analysis.structure.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * F-21a AC8 URL 값 → 경로 세그먼트 (docs/specs/F-21a-structure-graph.md). 식 계산 자체는 StructureAnalyzerTest가
 * 본다.
 */
class UrlEvaluatorTest {

    private static final PathSegment ANY = PathSegment.WILDCARD;

    private static PathSegment lit(String s) {
        return PathSegment.literal(s);
    }

    @Test
    void AC8_동적_조각이_들어간_세그먼트_전체가_와일드카드다() {
        UrlValue value =
                UrlValue.literal("/api/todos/t")
                        .plus(UrlValue.dynamic())
                        .plus(UrlValue.literal("x/done"));

        assertThat(value.toPath().segments())
                .containsExactly(lit("api"), lit("todos"), ANY, lit("done"));
        assertThat(value.startsDynamic()).isFalse();
    }

    @Test
    void AC8_물음표와_샵_뒤를_버린다() {
        assertThat(
                        UrlValue.literal("/api/a/")
                                .plus(UrlValue.dynamic())
                                .plus(UrlValue.literal("?page=1"))
                                .toPath()
                                .segments())
                .containsExactly(lit("api"), lit("a"), ANY);
        assertThat(UrlValue.literal("/api/b#top").toPath().segments())
                .containsExactly(lit("api"), lit("b"));
    }

    @Test
    void AC8_호스트를_떼고_절대_URL로_표시한다() {
        UrlValue.UrlPath withPort = UrlValue.literal("https://h.example:8443/api/x").toPath();
        assertThat(withPort.absolute()).isTrue();
        assertThat(withPort.segments()).containsExactly(lit("api"), lit("x"));

        assertThat(UrlValue.literal("http://localhost:8080").toPath().segments()).isEmpty();
        assertThat(UrlValue.literal("//cdn.example/x?y").toPath())
                .isEqualTo(new UrlValue.UrlPath(true, List.of(lit("x"))));
        assertThat(UrlValue.literal("/api").toPath().absolute()).isFalse();
    }

    @Test
    void AC8_맨_앞이_동적_조각이면_경로를_확정할_수_없다() {
        assertThat(UrlValue.dynamic().plus(UrlValue.literal("/todos")).startsDynamic()).isTrue();
        assertThat(UrlValue.literal("").plus(UrlValue.dynamic()).startsDynamic()).isTrue();
        assertThat(UrlValue.literal("/").plus(UrlValue.dynamic()).startsDynamic()).isFalse();
    }
}
