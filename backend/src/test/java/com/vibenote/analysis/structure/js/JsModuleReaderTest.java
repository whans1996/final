package com.vibenote.analysis.structure.js;

import static com.vibenote.analysis.structure.StructureFiles.file;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.structure.EdgeType;
import com.vibenote.analysis.structure.StructureEdge;
import com.vibenote.analysis.structure.StructureLinks;
import com.vibenote.analysis.structure.UnresolvedKind;
import com.vibenote.analysis.structure.UnresolvedLink;
import com.vibenote.analysis.structure.UnresolvedReason;
import com.vibenote.analysis.structure.WorkBudget;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** F-21a AC5·AC6 JS·Vue 모듈 import (docs/specs/F-21a-structure-graph.md). */
class JsModuleReaderTest {

    private static AnalysisFile js(long id, String path, String content) {
        return file(id, path, path.endsWith(".vue") ? "vue" : "js", content);
    }

    private static StructureLinks read(AnalysisFile... files) {
        StructureLinks links = new StructureLinks();
        JsModuleReader.read(List.of(files), links, WorkBudget.unlimited());
        return links;
    }

    private static StructureEdge imp(long from, long to, int line) {
        return new StructureEdge(from, to, EdgeType.IMPORT, line);
    }

    private static UnresolvedLink notSelected(long fileId, int line, String path) {
        return new UnresolvedLink(
                fileId, line, UnresolvedKind.IMPORT, UnresolvedReason.NOT_SELECTED, path);
    }

    @Test
    void AC6_상대_경로와_alias를_확장자_후보_순서로_풀어_간선을_만든다() {
        StructureLinks links =
                read(
                        js(
                                1,
                                "frontend/src/views/TodoView.vue",
                                """
                                <template><div /></template>
                                <script setup>
                                import { fetchTodos } from '@/api/todoApi'
                                import TodoItem from '../components/TodoItem.vue'
                                import { ref } from 'vue'
                                import axios from 'axios'
                                const Lazy = () => import('./LazyView')
                                const other = import(name)
                                import x from './Missing'
                                // import y from './Commented'
                                </script>
                                """),
                        js(
                                2,
                                "frontend/src/api/todoApi.js",
                                """
                                export * from './http'
                                export { BASE } from './config'
                                const utils = require('../utils')
                                import '@/styles/main.css'
                                """),
                        js(
                                3,
                                "frontend/src/components/TodoItem.vue",
                                "<script>export default {}</script>"),
                        js(4, "frontend/src/views/LazyView.vue", "<template><p /></template>"),
                        js(5, "frontend/src/api/http.js", "export const http = 1"),
                        js(6, "frontend/src/utils/index.js", "module.exports = {}"),
                        js(7, "frontend/src/api/config.mjs", "export const BASE = '/api'"),
                        js(8, "frontend/src/api/http.vue", "<script></script>"));

        assertThat(links.edges())
                .containsExactlyInAnyOrder(
                        imp(1, 2, 3),
                        imp(1, 3, 4),
                        imp(1, 4, 7),
                        imp(2, 5, 1),
                        imp(2, 7, 2),
                        imp(2, 6, 3));
        // 코드 확장자가 아닌 자산(.css 등)은 선별 대상이 될 수 없어 남기지 않는다
        assertThat(links.unresolved())
                .containsExactly(notSelected(1, 9, "frontend/src/views/Missing"));
    }

    @Test
    void AC6_alias는_가장_가까운_상위_src_기준이고_없으면_무시한다() {
        StructureLinks links =
                read(
                        js(
                                1,
                                "apps/web/src/pages/a/Page.vue",
                                "<script>\nimport api from '@/api/x'\n</script>"),
                        js(2, "apps/web/src/api/x.js", ""),
                        js(3, "src/api/x.js", ""),
                        js(
                                4,
                                "scripts/tool.js",
                                "import a from '@/api/x'\nimport b from '../../outside'"));

        assertThat(links.edges()).containsExactly(imp(1, 2, 2));
        assertThat(links.unresolved()).isEmpty();
    }

    @Test
    void AC5_토큰으로_나눌_수_없는_파일은_PARSE_ERROR이고_간선이_없다() {
        StructureLinks links =
                read(
                        js(1, "src/a.js", "import b from './b'\nconst s = 'never closed\n"),
                        js(2, "src/b.js", "export default 1"));

        assertThat(links.edges()).isEmpty();
        assertThat(links.unresolved()).hasSize(1);
        UnresolvedLink error = links.unresolved().get(0);
        assertThat(error.fileId()).isEqualTo(1);
        assertThat(error.line()).isEqualTo(2);
        assertThat(error.kind()).isEqualTo(UnresolvedKind.PARSE);
        assertThat(error.reason()).isEqualTo(UnresolvedReason.PARSE_ERROR);
        assertThat(error.detail()).isNotBlank();
    }

    @Test
    void AC6_from_없는_import_export가_반복돼도_제한_시간_안에_읽는다() {
        String code =
                "import a\n".repeat(50_000)
                        + "import { a\n".repeat(30_000)
                        + "export { a\n".repeat(30_000)
                        + "import x from './x.js'\n";

        StructureLinks links =
                assertTimeoutPreemptively(
                        Duration.ofSeconds(5),
                        () -> read(js(1, "src/a.js", code), js(2, "src/x.js", "")));

        assertThat(links.edges()).containsExactly(imp(1, 2, 110_001));
    }
}
