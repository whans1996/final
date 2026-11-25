package com.vibenote.analysis.structure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;

/** F-21a 5.3절 3: 확인하지 못한 연결은 응답 상한 하나 넘게만 정렬해 보관한다 (넘침 표시에 충분하다). */
class StructureLinksTest {

    @Test
    void AC12_확인하지_못한_연결은_상한보다_하나_많게만_보관한다() {
        StructureLinks links = new StructureLinks(3);
        for (int line = 1_000; line >= 1; line--) {
            links.unresolved(2, line, UnresolvedKind.HTTP_CALL, UnresolvedReason.URL_UNKNOWN, "x");
            links.unresolved(2, line, UnresolvedKind.HTTP_CALL, UnresolvedReason.URL_UNKNOWN, "x");
        }
        links.unresolved(1, 9, UnresolvedKind.HTTP_CALL, UnresolvedReason.URL_UNKNOWN, "y");

        assertThat(links.unresolved())
                .extracting(UnresolvedLink::fileId, UnresolvedLink::line)
                .containsExactly(tuple(1L, 9), tuple(2L, 1), tuple(2L, 2), tuple(2L, 3));
    }
}
