package com.vibenote.analysis.structure;

import static com.vibenote.analysis.structure.StructureFiles.file;
import static com.vibenote.analysis.structure.StructureFiles.java;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.ApiEndpoint;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * F-21a 5.3절 3: 제곱 경로를 색인으로 바꿔, 업로드한 코드가 계산량을 불려도 기본 작업 예산 안에서 끝까지 계산한다. 예산을 다 쓰면 edgesTruncated와
 * PARSE_ERROR가 나오므로 "잘림 없음·PARSE_ERROR 없음·끝의 호출까지 연결"을 함께 본다. 입력은 테스트 안의 가짜 코드다.
 */
class StructureWorkloadTest {

    /**
     * 단계 예산은 기본값 그대로 두어 계산 단계 수가 선형인지 본다. 마감 시각은 느린 CI에서도 걸리지 않게 늘리고, 예산을 확인하지 않는 반복이 남아 있으면 LIMIT을
     * 넘는다.
     */
    private final StructureAnalyzer analyzer =
            new StructureAnalyzer(
                    150, 100, StructureAnalyzer.DEFAULT_MAX_WORK_STEPS, Duration.ofSeconds(30));

    private static final Duration LIMIT = Duration.ofSeconds(10);

    private static final String HEADER = "import axios from 'axios'\n";

    /** 파일 끝에 두는 호출. 앞의 입력을 모두 계산했으면 HTTP 간선이 된다. */
    private static final String SENTINEL = "axios.get('/api/x')\n";

    private static final List<ApiEndpoint> APIS = List.of(new ApiEndpoint("GET", "/api/x", 9L));

    private AnalysisStructureResponse withinBudget(AnalysisFile... files) {
        AnalysisStructureResponse response =
                assertTimeoutPreemptively(
                        LIMIT, () -> analyzer.analyze(List.of(files), APIS, null));
        assertThat(response.edgesTruncated()).isFalse();
        assertThat(response.unresolved())
                .extracting(UnresolvedLink::reason)
                .doesNotContain(UnresolvedReason.PARSE_ERROR);
        return response;
    }

    private static int lines(String code) {
        return (int) code.chars().filter(c -> c == '\n').count();
    }

    /** HEADER + body + SENTINEL을 계산해 SENTINEL 줄의 HTTP 간선 하나가 나오는지 본다. */
    private void sentinelReached(String path, String body) {
        String code = HEADER + body + SENTINEL;
        AnalysisStructureResponse response =
                withinBudget(file(1, path, path.endsWith(".vue") ? "vue" : "js", code));
        assertThat(response.edges())
                .containsExactly(new StructureEdge(1, 9, EdgeType.HTTP, lines(code)));
    }

    @Test
    void AC7_같은_이름_매개변수가_많아도_이름_조회는_색인으로_한다() {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 30_000; i++) {
            body.append("function f").append(i).append("(url) { axios.get(url) }\n");
        }
        sentinelReached("src/params.js", body.toString());
    }

    @Test
    void AC7_깊게_중첩된_같은_이름_선언_밖의_조회도_색인으로_한다() {
        int n = 40_000;
        String body = "{ let u = 1\n".repeat(n) + "}".repeat(n) + "\n" + "axios.get(u)\n".repeat(n);
        sentinelReached("src/nested.js", body);
    }

    @Test
    void AC7_import가_많아도_import_조회는_맵으로_한다() {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 30_000; i++) {
            body.append("import { x").append(i).append(" } from './m").append(i).append("'\n");
        }
        body.append("y()\n".repeat(30_000));
        sentinelReached("src/imports.js", body.toString());
    }

    @Test
    void AC7_함수가_많은_파일의_var는_둘러싼_함수를_스택으로_찾는다() {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 20_000; i++) {
            body.append("function f").append(i).append("() {}\n");
        }
        for (int i = 0; i < 20_000; i++) {
            body.append("var v").append(i).append(" = 1\n");
        }
        sentinelReached("src/vars.js", body.toString());
    }

    @Test
    void AC7_화살표_함수_사슬의_식_끝은_한_번만_계산한다() {
        StringBuilder body = new StringBuilder("const f = ");
        for (int i = 0; i < 20_000; i++) {
            body.append("a").append(i).append(" => ");
        }
        body.append("1\n");
        sentinelReached("src/arrows.js", body.toString());
    }

    @Test
    void AC7_class_이름이_이어져도_본문_찾기는_한_번만_훑는다() {
        sentinelReached("src/classes.js", "class a ".repeat(20_000) + "{}\n");
    }

    @Test
    void AC7_구조_분해와_이름이_번갈아_이어진_선언도_스택을_넘지_않는다() {
        StringBuilder body = new StringBuilder("const ");
        for (int i = 0; i < 30_000; i++) {
            body.append("a").append(i).append(" = 1, {b").append(i).append("} = c, ");
        }
        body.append("z = 1\n");
        sentinelReached("src/declarators.js", body.toString());
    }

    @Test
    void AC8_중첩된_호출의_detail은_잘라_낸_뒤_공백을_정리한다() {
        int n = 30_000;
        sentinelReached("src/nested-calls.js", "fetch(".repeat(n) + ")".repeat(n) + "\n");
    }

    @Test
    void AC5_vue_script_블록이_많아도_한_번만_훑는다() {
        int n = 100_000;
        String code =
                "<script></script>\n".repeat(n)
                        + "<script>\n"
                        + HEADER
                        + SENTINEL
                        + "</script>\n"
                        + "<!-- -->\n";
        AnalysisStructureResponse response =
                withinBudget(file(1, "src/views/Many.vue", "vue", code));
        assertThat(response.edges()).containsExactly(new StructureEdge(1, 9, EdgeType.HTTP, n + 3));
    }

    @Test
    void AC10_래퍼가_많아도_부른_곳은_receiver_색인으로_찾는다() {
        StringBuilder code = new StringBuilder();
        int n = 8_000;
        for (int i = 0; i < n; i++) {
            code.append("function w").append(i).append("(u) { fetch(u) }\n");
        }
        for (int i = 0; i < n; i++) {
            code.append("w").append(i).append("('/api/x')\n");
        }
        AnalysisStructureResponse response =
                withinBudget(file(1, "src/w.js", "js", code.toString()));
        assertThat(response.edges()).containsExactly(new StructureEdge(1, 9, EdgeType.HTTP, n + 1));
        assertThat(response.unresolved()).isEmpty();
    }

    @Test
    void AC1_메서드와_매개변수가_많은_Java_파일도_이름_해석은_선계산으로_한다() {
        StringBuilder code = new StringBuilder("package com.example.app;\nclass A {\n");
        for (int i = 0; i < 10_000; i++) {
            code.append("  void m").append(i).append("(B b) { b.run(); }\n");
        }
        code.append("}\n");
        AnalysisStructureResponse response =
                withinBudget(
                        java(1, "com.example.app.A", code.toString()),
                        java(2, "com.example.app.B", "package com.example.app;\nclass B {}\n"));
        assertThat(response.edges()).containsExactly(new StructureEdge(1, 2, EdgeType.IMPORT, 3));
    }

    @Test
    void AC1_긴_점_이름이_많아도_사슬마다_한_번만_훑는다() {
        String chain = "a" + ".b".repeat(300);
        StringBuilder code = new StringBuilder("package com.example.app;\nclass A {\n");
        for (int i = 0; i < 500; i++) {
            code.append("  Object x").append(i).append(" = ").append(chain).append(".run();\n");
        }
        code.append("  B b;\n}\n");
        AnalysisStructureResponse response =
                withinBudget(
                        java(1, "com.example.app.A", code.toString()),
                        java(2, "com.example.app.B", "package com.example.app;\nclass B {}\n"));
        assertThat(response.edges()).containsExactly(new StructureEdge(1, 2, EdgeType.IMPORT, 503));
    }
}
