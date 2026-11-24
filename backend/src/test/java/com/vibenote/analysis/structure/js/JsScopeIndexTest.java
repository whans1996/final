package com.vibenote.analysis.structure.js;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** F-21a 5.3절 3 이름 조회 색인: 범위 시작이 가장 늦은 보이는 선언을 고른다 (예전 전체 훑기와 같은 결과). */
class JsScopeIndexTest {

    private static JsDecl decl(String tag, int start, int end) {
        return new JsDecl(tag, JsDecl.Kind.VARIABLE, start, end, List.of(), null);
    }

    /** 색인 전의 정의: 보이는 것 중 시작이 가장 늦은 것, 같으면 범위가 좁은 것, 그것도 같으면 먼저 등록한 것. */
    private static JsDecl naive(List<JsDecl> decls, int offset) {
        JsDecl best = null;
        for (JsDecl d : decls) {
            if (!d.visibleAt(offset)) {
                continue;
            }
            if (best == null
                    || d.scopeStart() > best.scopeStart()
                    || (d.scopeStart() == best.scopeStart() && d.scopeEnd() < best.scopeEnd())) {
                best = d;
            }
        }
        return best;
    }

    @Test
    void AC7_형제_범위가_끝난_뒤에는_바깥_선언을_찾는다() {
        JsDecl file = decl("file", -1, Integer.MAX_VALUE);
        JsDecl outer = decl("outer", 10, 100);
        JsDecl a = decl("a", 20, 30);
        JsDecl b = decl("b", 40, 50);
        JsScopeIndex index = new JsScopeIndex(List.of(file, outer, a, b));

        assertThat(index.lookup(5)).isSameAs(file);
        assertThat(index.lookup(25)).isSameAs(a);
        assertThat(index.lookup(35)).isSameAs(outer);
        assertThat(index.lookup(45)).isSameAs(b);
        assertThat(index.lookup(60)).isSameAs(outer);
        assertThat(index.lookup(100)).isSameAs(file);
    }

    @Test
    void AC7_같은_범위면_먼저_등록한_것_파일_범위가_없으면_null() {
        JsDecl first = decl("first", 10, 20);
        JsDecl second = decl("second", 10, 20);
        JsScopeIndex index = new JsScopeIndex(List.of(first, second));

        assertThat(index.lookup(15)).isSameAs(first);
        assertThat(index.lookup(5)).isNull();
        assertThat(index.lookup(20)).isNull();
    }

    @Test
    void AC7_짝이_맞지_않는_범위가_섞여도_전체_훑기와_같다() {
        // 결정적인 의사 난수로 중첩·형제·부분 겹침·빈 범위를 섞는다
        long seed = 42;
        for (int round = 0; round < 200; round++) {
            List<JsDecl> decls = new ArrayList<>();
            int count = 1 + (int) ((seed = next(seed)) % 30);
            for (int k = 0; k < count; k++) {
                int start = (int) ((seed = next(seed)) % 60) - 1;
                int length = (int) ((seed = next(seed)) % 40);
                int end = (seed = next(seed)) % 10 == 0 ? Integer.MAX_VALUE : start + length;
                decls.add(decl("d" + k, start, end));
            }
            JsScopeIndex index = new JsScopeIndex(decls);
            for (int offset = -1; offset < 110; offset++) {
                assertThat(index.lookup(offset))
                        .as("round %d offset %d %s", round, offset, decls)
                        .isSameAs(naive(decls, offset));
            }
        }
    }

    private static long next(long seed) {
        return (seed * 6364136223846793005L + 1442695040888963407L) >>> 1;
    }
}
