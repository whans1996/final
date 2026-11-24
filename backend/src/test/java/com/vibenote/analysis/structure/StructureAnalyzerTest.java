package com.vibenote.analysis.structure;

import static com.vibenote.analysis.structure.StructureFiles.file;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.ApiEndpoint;
import com.vibenote.analysis.structure.http.ContextPathReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** F-21a AC7~AC12 구조 계산 (docs/specs/F-21a-structure-graph.md). 입력은 테스트 안의 가짜 프로젝트다. */
class StructureAnalyzerTest {

    private final StructureAnalyzer analyzer = new StructureAnalyzer(150, 100);

    /** 업로드한 코드로 계산량을 불릴 수 없음을 보는 테스트의 제한 시간. 정상 구현은 1초 안에 끝난다. */
    private static final Duration LIMIT = Duration.ofSeconds(5);

    private static AnalysisFile js(long id, String path, String content) {
        return file(id, path, path.endsWith(".vue") ? "vue" : "js", content);
    }

    private static ApiEndpoint api(String method, String path, long fileId) {
        return new ApiEndpoint(method, path, fileId);
    }

    private AnalysisStructureResponse run(List<ApiEndpoint> apis, AnalysisFile... files) {
        return analyzer.analyze(List.of(files), apis, null);
    }

    private static List<StructureEdge> http(AnalysisStructureResponse response) {
        return response.edges().stream().filter(e -> e.type() == EdgeType.HTTP).toList();
    }

    private static List<UnresolvedLink> httpUnresolved(AnalysisStructureResponse response) {
        return response.unresolved().stream()
                .filter(u -> u.kind() == UnresolvedKind.HTTP_CALL)
                .toList();
    }

    private static StructureEdge edge(long from, long to, int line) {
        return new StructureEdge(from, to, EdgeType.HTTP, line);
    }

    private static UnresolvedLink unknown(
            long fileId, int line, UnresolvedReason reason, String detail) {
        return new UnresolvedLink(fileId, line, UnresolvedKind.HTTP_CALL, reason, detail);
    }

    @Test
    void AC7_axios_같은_파일_인스턴스_request_config_fetch를_HTTP_호출로_본다() {
        AnalysisStructureResponse response =
                run(
                        List.of(
                                api("GET", "/api/todos", 100),
                                api("POST", "/api/todos", 101),
                                api("PUT", "/api/todos/{id}", 102),
                                api("PATCH", "/api/todos/{id}", 103),
                                api("DELETE", "/api/todos/{id}", 104)),
                        js(
                                10,
                                "src/api/todo.js",
                                """
                                import axios from 'axios'
                                const http = axios.create({ baseURL: '/api' })
                                export const list = () => axios.get('/api/todos')
                                export const create = (t) => http.post('/todos', t)
                                export const update = (t) => http.request({ url: '/todos/1', method: 'put' })
                                export const patch = (t) => http({ url: '/todos/1', method: 'PATCH', data: t })
                                export const remove = () => fetch('/api/todos/1', { method: 'DELETE' })
                                export const plain = () => fetch('/api/todos')
                                """));

        assertThat(http(response))
                .containsExactly(
                        edge(10, 100, 3),
                        edge(10, 101, 4),
                        edge(10, 102, 5),
                        edge(10, 103, 6),
                        edge(10, 104, 7));
        assertThat(httpUnresolved(response)).isEmpty();
    }

    @Test
    void AC7_선별_파일에서_import한_axios_create_인스턴스를_받는_쪽으로_본다() {
        AnalysisStructureResponse response =
                run(
                        List.of(
                                api("GET", "/api/todos", 100),
                                api("POST", "/api/todos", 101),
                                api("DELETE", "/api/todos/{id}", 102)),
                        js(
                                20,
                                "src/api/http.js",
                                "import axios from 'axios'\n"
                                        + "export default axios.create({ baseURL: '/api' })"),
                        js(
                                21,
                                "src/api/client.js",
                                "import axios from 'axios'\n"
                                    + "export const client = axios.create({ baseURL: '/api' })"),
                        js(
                                22,
                                "src/api/plain.js",
                                "import axios from 'axios'\n"
                                        + "const api = axios.create({ baseURL: '/api' })\n"
                                        + "export default api"),
                        js(
                                23,
                                "src/views/TodoView.vue",
                                """
                                <script setup>
                                import http from '@/api/http'
                                import { client } from '@/api/client'
                                import api from '@/api/plain'
                                http.get('/todos')
                                client.post('/todos', {})
                                api.delete(`/todos/${id}`)
                                </script>
                                """));

        assertThat(http(response))
                .containsExactly(edge(23, 100, 5), edge(23, 101, 6), edge(23, 102, 7));
        assertThat(httpUnresolved(response)).isEmpty();
    }

    @Test
    void AC7_라우터_Map_route_path_axios가_아닌_객체의_get은_HTTP_호출이_아니다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                30,
                                "src/router/index.js",
                                """
                                import { createRouter } from 'vue-router'
                                const routes = [{ path: '/api/todos', component: X }]
                                const router = createRouter({ routes })
                                router.push('/api/todos')
                                const map = new Map()
                                map.get('/api/todos')
                                export function go(route) { return route.path }
                                const store = { get(url) { return url } }
                                store.get('/api/todos')
                                """),
                        js(
                                31,
                                "src/views/A.vue",
                                """
                                <script>
                                import api from './notAxios'
                                export default {
                                  mounted() { this.$http.get('/api/todos'); api.get('/api/todos') }
                                }
                                </script>
                                """),
                        js(
                                32,
                                "src/views/notAxios.js",
                                "export default { get(url) { return url } }"));

        assertThat(http(response)).isEmpty();
        assertThat(httpUnresolved(response)).isEmpty();
    }

    @Test
    void AC8_문자열_템플릿_연결_const_import한_const로_URL을_계산한다() {
        AnalysisStructureResponse response =
                run(
                        List.of(
                                api("GET", "/api/todos/{id}", 100),
                                api("GET", "/api/todos", 101),
                                api("GET", "/api/todos/{id}/done", 102)),
                        js(
                                40,
                                "src/api/config.js",
                                "export const BASE = '/api'\nexport const TODOS = BASE + '/todos'"),
                        js(
                                41,
                                "src/api/todo.js",
                                """
                                import axios from 'axios'
                                import { BASE, TODOS } from './config'
                                const API = '/api'
                                export const a = (id) => axios.get(`${API}/todos/${id}`)
                                export const b = () => axios.get(BASE + '/todos?page=1#top')
                                export const c = () => axios.get('http://localhost:8080/api/todos')
                                export const d = (id) => axios.get('/api/todos/' + id + '/done')
                                export const e = (id) => axios.get(`/api/todos/t${id}`)
                                export const f = () => axios.get(TODOS)
                                export const g = () => axios.get(`${import.meta.env.VITE_API_URL}/todos`)
                                export const h = (url) => axios.get(url)
                                """));

        assertThat(http(response))
                .containsExactly(edge(41, 100, 4), edge(41, 101, 5), edge(41, 102, 7));
        assertThat(httpUnresolved(response))
                .containsExactly(
                        unknown(
                                41,
                                10,
                                UnresolvedReason.URL_UNKNOWN,
                                "axios.get(`${import.meta.env.VITE_API_URL}/todos`)"),
                        unknown(41, 11, UnresolvedReason.URL_UNKNOWN, "axios.get(url)"));
    }

    @Test
    void AC9_baseURL은_config_인스턴스_defaults_순이고_env면_BASE_URL_UNKNOWN() {
        AnalysisStructureResponse response =
                run(
                        List.of(
                                api("GET", "/api/todos", 100),
                                api("GET", "/api/todos/{id}", 101),
                                api("GET", "/v2/todos", 102)),
                        js(
                                50,
                                "src/main.js",
                                "import axios from 'axios'\n"
                                        + "axios.defaults.baseURL = 'http://localhost:8080/api'"),
                        js(
                                51,
                                "src/api/a.js",
                                "import axios from 'axios'\n"
                                        + "export const list = () => axios.get('/todos')"),
                        js(
                                52,
                                "src/api/env.js",
                                """
                                import axios from 'axios'
                                const http = axios.create({ baseURL: import.meta.env.VITE_API_URL })
                                export const list = () => http.get('/todos')
                                export const abs = () => http.get('http://localhost:8080/api/todos/1')
                                const v2 = axios.create({ baseURL: '/api' })
                                export const over = () => v2.get('/todos', { baseURL: '/v2' })
                                """));

        assertThat(http(response))
                .containsExactly(edge(51, 100, 2), edge(52, 101, 4), edge(52, 102, 6));
        assertThat(httpUnresolved(response))
                .containsExactly(
                        unknown(52, 3, UnresolvedReason.BASE_URL_UNKNOWN, "http.get('/todos')"));
    }

    @Test
    void AC10_url이_래퍼_매개변수면_부른_곳마다_한_단계_인라인하고_두_단계는_URL_UNKNOWN() {
        AnalysisStructureResponse response =
                run(
                        List.of(
                                api("GET", "/api/todos", 100),
                                api("POST", "/api/todos", 101),
                                api("GET", "/api/todos/{id}", 102)),
                        js(
                                60,
                                "src/api/http.js",
                                """
                                import axios from 'axios'
                                const http = axios.create({ baseURL: '/api' })
                                export function request(method, url, data) {
                                  return http({ method, url, data })
                                }
                                function unused(url) { return http.get(url) }
                                export const api = (url) => request('get', url)
                                """),
                        js(
                                61,
                                "src/api/todo.js",
                                """
                                import { request, api } from './http'
                                export const list = () => request('get', '/todos')
                                export const create = (t) => request('post', '/todos', t)
                                export const two = () => api('/todos')
                                """),
                        js(
                                62,
                                "src/api/local.js",
                                """
                                import axios from 'axios'
                                function get(url) { return axios.get(url) }
                                get('/api/todos/1')
                                """));

        assertThat(http(response))
                .containsExactly(edge(61, 100, 2), edge(61, 101, 3), edge(62, 102, 3));
        assertThat(httpUnresolved(response))
                .containsExactly(
                        unknown(60, 6, UnresolvedReason.URL_UNKNOWN, "http.get(url)"),
                        unknown(60, 7, UnresolvedReason.URL_UNKNOWN, "request('get', url)"));
    }

    @Test
    void AC11_context_path_메서드_불일치_AMBIGUOUS_메서드_모름() {
        AnalysisFile yml =
                file(
                        70,
                        "src/main/resources/application.yml",
                        "config",
                        "server:\n  port: 8080\n  servlet:\n    context-path: /app\n");
        AnalysisFile client =
                js(
                        71,
                        "frontend/src/api/a.js",
                        """
                        import axios from 'axios'
                        axios.get('/app/api/todos')
                        axios.delete('/app/api/todos')
                        axios.get('/app/api/items/1')
                        const m = pick()
                        axios({ url: '/app/api/todos/search', method: m })
                        """);
        List<AnalysisFile> files = List.of(yml, client);
        List<ApiEndpoint> apis =
                List.of(
                        api("GET", "/api/todos", 100),
                        api("POST", "/api/todos/search", 102),
                        api("GET", "/api/todos/{id}", 103),
                        api("GET", "/api/items/{id}", 100),
                        api("GET", "/api/items/{code}", 101));

        AnalysisStructureResponse response =
                analyzer.analyze(files, apis, ContextPathReader.read(files));

        // 메서드를 모르면 후보가 한 파일일 때만 고른다 (5.3절 5): POST search(102)와 GET {id}(103)
        assertThat(http(response)).containsExactly(edge(71, 100, 2));
        assertThat(httpUnresolved(response))
                .containsExactly(
                        unknown(71, 3, UnresolvedReason.NO_MATCHING_API, "DELETE /app/api/todos"),
                        unknown(71, 4, UnresolvedReason.AMBIGUOUS, "GET /app/api/items/1"),
                        unknown(71, 6, UnresolvedReason.AMBIGUOUS, "/app/api/todos/search"));
    }

    @Test
    void AC11_context_path는_리터럴일_때만_읽는다() {
        assertThat(
                        ContextPathReader.read(
                                List.of(
                                        file(
                                                1,
                                                "src/main/resources/application.properties",
                                                "config",
                                                "server.port=8080\n"
                                                        + "server.servlet.context-path=/app\n"))))
                .isEqualTo("/app");
        assertThat(
                        ContextPathReader.read(
                                List.of(
                                        file(
                                                1,
                                                "src/main/resources/application.yml",
                                                "config",
                                                "server:\n"
                                                        + "  servlet:\n"
                                                        + "    context-path: ${CTX:/x}\n"))))
                .isEmpty();
        assertThat(ContextPathReader.read(List.of())).isEmpty();
    }

    @Test
    void AC12_노드는_모든_파일_간선은_중복_제거_정렬_unresolved는_파일_줄_순() {
        AnalysisFile controller =
                file(
                        3,
                        "src/main/java/com/example/todo/TodoController.java",
                        "controller",
                        "package com.example.todo;\n"
                                + "class TodoController {\n"
                                + "  TodoService a;\n"
                                + "  TodoService b;\n"
                                + "}");
        controller.setRole("할 일 API");
        AnalysisFile service =
                file(
                        2,
                        "src/main/java/com/example/todo/TodoService.java",
                        "service",
                        "package com.example.todo;\n"
                                + "import com.example.todo.valid.V;\n"
                                + "class TodoService {}");
        AnalysisFile view =
                js(
                        1,
                        "frontend/src/views/TodoView.vue",
                        "<script setup>\n"
                                + "import x from './Missing'\n"
                                + "import axios from 'axios'\n"
                                + "axios.get('/api/todos')\n"
                                + "</script>");
        AnalysisFile readme = file(4, "README.md", "other", "# hi");

        AnalysisStructureResponse response =
                analyzer.analyze(
                        List.of(controller, service, view, readme),
                        List.of(api("GET", "/api/todos", 3)),
                        null);

        assertThat(response.nodes())
                .containsExactly(
                        new StructureNode(
                                1, "frontend/src/views/TodoView.vue", "TodoView.vue", "vue", null),
                        new StructureNode(
                                2,
                                "src/main/java/com/example/todo/TodoService.java",
                                "TodoService.java",
                                "service",
                                null),
                        new StructureNode(
                                3,
                                "src/main/java/com/example/todo/TodoController.java",
                                "TodoController.java",
                                "controller",
                                "할 일 API"),
                        new StructureNode(4, "README.md", "README.md", "other", null));
        assertThat(response.edges())
                .containsExactly(
                        new StructureEdge(1, 3, EdgeType.HTTP, 4),
                        new StructureEdge(3, 2, EdgeType.IMPORT, 3));
        assertThat(response.unresolved())
                .containsExactly(
                        new UnresolvedLink(
                                1,
                                2,
                                UnresolvedKind.IMPORT,
                                UnresolvedReason.NOT_SELECTED,
                                "frontend/src/views/Missing"),
                        new UnresolvedLink(
                                2,
                                2,
                                UnresolvedKind.IMPORT,
                                UnresolvedReason.NOT_SELECTED,
                                "com.example.todo.valid.V"));
        assertThat(response.edgesTruncated()).isFalse();
        assertThat(response.unresolvedTruncated()).isFalse();
        assertThat(
                        analyzer.analyze(
                                List.of(controller, service, view, readme),
                                List.of(api("GET", "/api/todos", 3)),
                                null))
                .isEqualTo(response);
    }

    @Test
    void AC12_상한을_넘으면_앞에서_자르고_truncated를_표시한다() {
        List<AnalysisFile> files = new ArrayList<>();
        StringBuilder imports = new StringBuilder();
        for (int i = 2; i <= 5; i++) {
            files.add(js(i, "src/m" + i + ".js", ""));
            imports.append("import m").append(i).append(" from './m").append(i).append("'\n");
            imports.append("import n").append(i).append(" from './n").append(i).append("'\n");
        }
        files.add(js(1, "src/main.js", imports.toString()));

        AnalysisStructureResponse response = new StructureAnalyzer(2, 3).analyze(files, null, null);

        assertThat(response.edges())
                .containsExactly(
                        new StructureEdge(1, 2, EdgeType.IMPORT, 1),
                        new StructureEdge(1, 3, EdgeType.IMPORT, 3));
        assertThat(response.edgesTruncated()).isTrue();
        assertThat(response.unresolved()).extracting(UnresolvedLink::line).containsExactly(2, 4, 6);
        assertThat(response.unresolvedTruncated()).isTrue();
    }

    @Test
    void AC12_개요가_없으면_HTTP_판정을_건너뛰고_파일이_없으면_빈_배열() {
        AnalysisStructureResponse noOverview =
                run(
                        null,
                        js(
                                1,
                                "src/a.js",
                                "import axios from 'axios'\naxios.get('/api/x')\naxios.get(u)"));

        assertThat(http(noOverview)).isEmpty();
        assertThat(httpUnresolved(noOverview)).isEmpty();

        AnalysisStructureResponse empty = analyzer.analyze(List.of(), List.of(), null);
        assertThat(empty.nodes()).isEmpty();
        assertThat(empty.edges()).isEmpty();
        assertThat(empty.unresolved()).isEmpty();
    }

    @Test
    void AC8_자기_참조나_불어나는_const는_계산_예산을_넘으면_URL_UNKNOWN이다() {
        StringBuilder chain =
                new StringBuilder("import axios from 'axios'\nconst a0 = '/aaaaaaaaaa'\n");
        for (int i = 1; i <= 7; i++) {
            chain.append("const a").append(i).append(" = ");
            chain.append(String.join(" + ", Collections.nCopies(10, "a" + (i - 1))));
            chain.append('\n');
        }
        chain.append("axios.get(a7)\n");

        AnalysisStructureResponse response =
                assertTimeoutPreemptively(
                        LIMIT,
                        () ->
                                run(
                                        List.of(api("GET", "/api/x", 9)),
                                        js(
                                                1,
                                                "src/self.js",
                                                "import axios from 'axios'\n"
                                                    + "const u = u + u + u + u + u + u + u + u\n"
                                                    + "axios.get(u)\n"),
                                        js(2, "src/grow.js", chain.toString())));

        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::fileId, UnresolvedLink::reason)
                .containsExactly(
                        tuple(1L, UnresolvedReason.URL_UNKNOWN),
                        tuple(2L, UnresolvedReason.URL_UNKNOWN));
    }

    @Test
    void AC12_config가_자기_자신이나_서로를_가리켜도_200_응답을_만든다() {
        AnalysisStructureResponse response =
                assertTimeoutPreemptively(
                        LIMIT,
                        () ->
                                run(
                                        List.of(api("GET", "/api/x", 9)),
                                        js(
                                                1,
                                                "src/a.js",
                                                """
                                                import axios from 'axios'
                                                const cfg = cfg
                                                axios(cfg)
                                                const a = b
                                                const b = a
                                                axios.request(a)
                                                """)));

        assertThat(http(response)).isEmpty();
        assertThat(httpUnresolved(response)).extracting(UnresolvedLink::line).containsExactly(3, 6);
    }

    @Test
    void AC5_템플릿_중첩이_너무_깊으면_그_파일은_PARSE_ERROR다() {
        String deep = "const x = " + "`${".repeat(100_000) + "1" + "}`".repeat(100_000) + "\n";

        AnalysisStructureResponse response =
                assertTimeoutPreemptively(LIMIT, () -> run(List.of(), js(1, "src/deep.js", deep)));

        assertThat(response.unresolved())
                .extracting(UnresolvedLink::kind, UnresolvedLink::reason)
                .containsExactly(tuple(UnresolvedKind.PARSE, UnresolvedReason.PARSE_ERROR));
    }

    @Test
    void AC12_선언과_호출이_많은_큰_파일도_제한_시간_안에_계산한다() {
        StringBuilder code = new StringBuilder("import axios from 'axios'\n");
        for (int i = 0; i < 30_000; i++) {
            code.append("const c").append(i).append(" = '/api/x'\n");
        }
        for (int i = 0; i < 30_000; i++) {
            code.append("axios.get(c").append(i).append(")\n");
        }

        AnalysisStructureResponse response =
                assertTimeoutPreemptively(
                        LIMIT,
                        () ->
                                run(
                                        List.of(api("GET", "/api/x", 9)),
                                        js(1, "src/big.js", code.toString()),
                                        file(
                                                9,
                                                "src/main/java/X.java",
                                                "controller",
                                                "class X {}")));

        assertThat(http(response)).containsExactly(edge(1, 9, 30_002));
    }

    @Test
    void AC8_URL_UNKNOWN_detail은_줄바꿈과_탭을_공백_하나로_접는다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/x", 9)),
                        js(
                                1,
                                "src/a.js",
                                "import axios from 'axios'\naxios.get(\n\t\tbase + '/x'\n)\n"));

        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::detail)
                .containsExactly("axios.get( base + '/x' )");
    }

    @Test
    void AC10_래퍼와_같은_이름이_가려진_곳의_호출은_인라인하지_않는다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100), api("GET", "/api/items", 101)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                function request(url) { return axios.get(url) }
                                function unrelated(request) { request('/api/todos') }
                                request('/api/items')
                                """),
                        js(
                                3,
                                "src/http.js",
                                """
                                import axios from 'axios'
                                export function get(url) { return axios.get(url) }
                                """),
                        js(
                                4,
                                "src/c.js",
                                """
                                import { get } from './http.js'
                                function f(get) { get('/api/todos') }
                                get('/api/items')
                                """));

        assertThat(http(response)).containsExactly(edge(1, 101, 4), edge(4, 101, 3));
    }

    @Test
    void AC7_config_전개_뒤에_직접_적지_않은_속성은_값을_모른다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100), api("POST", "/api/todos", 101)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                const opts = { method: 'POST' }
                                axios({ url: '/api/todos', ...opts })
                                axios({ ...opts, url: '/api/todos', method: 'GET' })
                                axios({ ...opts, baseURL: '/api', url: '/todos', method: 'GET' })
                                axios({ ...opts, baseURL: '/api', url: '/todos' })
                                """));

        // 전개가 덮어쓸 수 있는 url(3)·baseURL(4)·method(6)는 값을 모른다. 전개 뒤에 직접 적은 값만 확정한다(5)
        assertThat(http(response)).containsExactly(edge(1, 100, 5));
        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::line, UnresolvedLink::reason)
                .containsExactly(
                        tuple(3, UnresolvedReason.URL_UNKNOWN),
                        tuple(4, UnresolvedReason.BASE_URL_UNKNOWN),
                        tuple(6, UnresolvedReason.AMBIGUOUS));
    }

    @Test
    void AC8_숫자끼리_더한_값은_문자열로_잇지_않고_동적_조각이다() {
        AnalysisStructureResponse response =
                run(
                        List.of(
                                api("GET", "/api/todos/12", 100),
                                api("GET", "/api/todos/3", 101),
                                api("GET", "/api/todos/11", 102)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                axios.get('/api/todos/' + (1 + 2))
                                const n = 1
                                axios.get('/api/todos/' + n + 1)
                                axios.get(`/api/todos/${n + 2}`)
                                axios.get('/api/todos/' + 1.50)
                                """));

        assertThat(http(response)).containsExactly(edge(1, 102, 4));
        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::line, UnresolvedLink::reason)
                .containsExactly(
                        tuple(2, UnresolvedReason.AMBIGUOUS),
                        tuple(5, UnresolvedReason.AMBIGUOUS),
                        tuple(6, UnresolvedReason.AMBIGUOUS));
    }

    @Test
    void AC7_require로_가져온_axios도_받는_쪽으로_본다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                const axios = require('axios')
                                axios.get('/api/todos')
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 2));
    }

    @Test
    void AC7_axios_이름이_가려진_곳의_create는_인스턴스가_아니다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                function f(axios) { const http = axios.create({ baseURL: '/api' }); http.get('/todos') }
                                """),
                        js(
                                2,
                                "src/b.js",
                                """
                                const ax = require('axios')
                                const http = ax.create({ baseURL: '/api' })
                                http.get('/todos')
                                """));

        assertThat(http(response)).containsExactly(edge(2, 100, 3));
        assertThat(httpUnresolved(response)).isEmpty();
    }

    @Test
    void AC9_config를_계산하지_못하면_baseURL을_모른다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                const http = axios.create(getConfig())
                                http.get('/api/todos')
                                const plain = axios.create()
                                plain.get('/api/todos')
                                axios.get('/api/todos', getOptions())
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 5));
        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::line, UnresolvedLink::reason)
                .containsExactly(
                        tuple(3, UnresolvedReason.BASE_URL_UNKNOWN),
                        tuple(6, UnresolvedReason.BASE_URL_UNKNOWN));
    }

    @Test
    void AC7_템플릿_안의_호출도_HTTP_호출로_본다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100), api("GET", "/api/items", 101)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                const a = `${fetch('/api/todos')}`
                                const b = `x${`${axios.get('/api/items')}`}`
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 2), edge(1, 101, 3));
    }

    @Test
    void AC5_vue_HTML_주석_안의_script는_코드가_아니다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100), api("GET", "/api/items", 101)),
                        js(
                                1,
                                "src/views/A.vue",
                                """
                                <template>
                                  <!-- <script>fetch('/api/todos')</script> -->
                                  <div/>
                                </template>
                                <script setup>
                                fetch('/api/items')
                                </script>
                                """));

        assertThat(http(response)).containsExactly(edge(1, 101, 6));
    }

    @Test
    void AC9_axios_이름이_가려진_곳의_defaults_baseURL은_쓰지_않는다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                function unrelated(axios) { axios.defaults.baseURL = '/wrong' }
                                axios.get('/api/todos')
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 3));
    }

    @Test
    void AC7_config의_계산된_키와_접근자는_전개처럼_값을_모르게_한다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100), api("POST", "/api/todos", 101)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                axios({ url: '/api/todos', ['method']: 'POST' })
                                axios({ url: '/api/todos', method: 'POST', get method() { return 'GET' } })
                                axios({ method: 'GET', url: '/api/todos' })
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 4));
        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::line, UnresolvedLink::reason)
                .containsExactly(
                        tuple(2, UnresolvedReason.URL_UNKNOWN),
                        tuple(3, UnresolvedReason.URL_UNKNOWN));
    }

    @Test
    void AC8_baseURL을_붙인_뒤_점_세그먼트를_정규화한다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/todos", 100), api("GET", "/api/todos/{id}", 101)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                fetch('http://localhost/api/../todos')
                                const http = axios.create({ baseURL: '/api/v1' })
                                http.get('../todos/./1')
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 2), edge(1, 101, 4));
    }

    @Test
    void AC7_블록_안_var는_함수_전체에서_axios를_가린다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                function f() { if (true) { var axios = new Map() } axios.get('/api/todos') }
                                """),
                        js(
                                2,
                                "src/b.js",
                                """
                                import axios from 'axios'
                                function g() { if (true) { let axios = 1 } axios.get('/api/todos') }
                                """));

        assertThat(http(response)).containsExactly(edge(2, 100, 2));
        assertThat(httpUnresolved(response)).isEmpty();
    }

    @Test
    void AC8_문자열_하나뿐인_URL도_길이_상한을_넘으면_URL_UNKNOWN이다() {
        String longPath = "/a".repeat(2_000);
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                "import axios from 'axios'\n"
                                        + "axios.get('"
                                        + longPath
                                        + "')\n"
                                        + "const LONG = `"
                                        + longPath
                                        + "`\n"
                                        + "axios.get(LONG)\n"));

        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::line, UnresolvedLink::reason)
                .containsExactly(
                        tuple(2, UnresolvedReason.URL_UNKNOWN),
                        tuple(4, UnresolvedReason.URL_UNKNOWN));
    }

    @Test
    void AC7_catch_매개변수는_그_블록에서_axios를_가린다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                try { throw new Map() } catch (axios) { axios.get('/api/todos') }
                                axios.get('/api/todos')
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 3));
        assertThat(httpUnresolved(response)).isEmpty();
    }

    @Test
    void AC10_나머지_매개변수는_인자_하나로_인라인하지_않는다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                function request(...url) { return fetch(url) }
                                request('/api/todos', '/other')
                                """));

        assertThat(http(response)).isEmpty();
        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::line, UnresolvedLink::reason)
                .containsExactly(tuple(2, UnresolvedReason.URL_UNKNOWN));
    }

    @Test
    void AC7_const_config의_url_method_baseURL을_바꾸면_값을_모른다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                const cfg = { url: '/api/todos', method: 'GET' }
                                cfg.url = '/api/other'
                                axios(cfg)
                                const ok = { url: '/api/todos', method: 'GET' }
                                ok.headers = {}
                                axios(ok)
                                const c2 = { url: '/api/todos' }
                                Object.assign(c2, other)
                                axios(c2)
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 7));
        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::line, UnresolvedLink::reason)
                .containsExactly(
                        tuple(4, UnresolvedReason.URL_UNKNOWN),
                        tuple(10, UnresolvedReason.URL_UNKNOWN));
    }

    @Test
    void AC11_context_path는_마지막_값을_쓰고_마지막이_플레이스홀더면_모른다() {
        String key = "server.servlet.context-path=";
        assertThat(
                        ContextPathReader.read(
                                List.of(
                                        file(
                                                1,
                                                "src/main/resources/application.properties",
                                                "config",
                                                key + "/old\n" + key + "/new\n"))))
                .isEqualTo("/new");
        assertThat(
                        ContextPathReader.read(
                                List.of(
                                        file(
                                                1,
                                                "src/main/resources/application.properties",
                                                "config",
                                                key + "/old\n" + key + "${CTX}\n"))))
                .isEmpty();
        assertThat(
                        ContextPathReader.read(
                                List.of(
                                        file(
                                                1,
                                                "src/main/resources/application.yml",
                                                "config",
                                                "server:\n"
                                                        + "  servlet:\n"
                                                        + "    context-path: /old\n"
                                                        + "---\n"
                                                        + "server:\n"
                                                        + "  servlet:\n"
                                                        + "    context-path: /new\n"))))
                .isEqualTo("/new");
    }

    @Test
    void AC10_래퍼_호출_곱이_커도_제한_시간_안에_한_간선으로_모은다() {
        StringBuilder code = new StringBuilder("function request(url) {\n");
        code.append("  fetch(url)\n".repeat(3_000));
        code.append("}\n");
        code.append("request('/api/todos')\n".repeat(3_000));

        AnalysisStructureResponse response =
                assertTimeoutPreemptively(
                        LIMIT,
                        () ->
                                run(
                                        List.of(api("GET", "/api/todos", 100)),
                                        js(1, "src/a.js", code.toString())));

        assertThat(http(response)).containsExactly(edge(1, 100, 3_003));
    }

    @Test
    void AC7_구조_분해_매개변수도_axios를_가린다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                function f({ axios }) { axios.get('/api/todos') }
                                const g = ([axios, b = axios]) => axios.get('/api/todos')
                                axios.get('/api/todos')
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 4));
        assertThat(httpUnresolved(response)).isEmpty();
    }

    @Test
    void AC7_for_머리의_let_const는_루프_안에서만_가린다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                function f() {
                                  for (let axios of []) {}
                                  for (const axios of []) axios.get('/api/x');
                                  axios.get('/api/todos')
                                }
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 5));
        assertThat(httpUnresolved(response)).isEmpty();
    }

    @Test
    void AC10_다시_대입한_매개변수는_부른_곳의_인자로_바꾸지_않는다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                function request(url) { url = '/api/other'; return fetch(url) }
                                request('/api/todos')
                                const get = (path = '/x') => fetch(path)
                                get('/api/todos')
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 4));
        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::line, UnresolvedLink::reason)
                .containsExactly(tuple(1, UnresolvedReason.URL_UNKNOWN));
    }

    @Test
    void AC7_이름_있는_함수_식의_이름은_그_함수_안에서만_보인다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100), api("GET", "/api/items", 101)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                const helper = function fetch() {}
                                fetch('/api/todos')
                                const h2 = function axios() {}
                                axios.get('/api/items')
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 3), edge(1, 101, 5));
    }

    @Test
    void AC7_구조_분해_선언도_axios를_가린다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                function f() { const {axios} = {axios: new Map()}; axios.get('/api/todos') }
                                function g() { let [x, { y: axios = 1 }] = []; axios.get('/api/todos') }
                                function h() { const { a, axios: { get } } = {}; axios.get('/api/todos') }
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 4));
        assertThat(httpUnresolved(response)).isEmpty();
    }

    @Test
    void AC7_지역_class_선언도_axios를_가린다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                function f() { class axios { static get(x) { return x } } axios.get('/api/todos') }
                                const C = class fetch { m() { fetch('/api/todos') } }
                                fetch('/api/todos')
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 4));
        assertThat(httpUnresolved(response)).isEmpty();
    }

    // ---- 5.3절 3: 분석 전체 작업 예산·파일별 예외 ----

    private static UnresolvedLink parseError(long fileId, String detail) {
        return new UnresolvedLink(
                fileId, 1, UnresolvedKind.PARSE, UnresolvedReason.PARSE_ERROR, detail);
    }

    @Test
    void AC12_작업_예산을_다_쓰면_남은_파일은_PARSE_ERROR이고_edgesTruncated를_켠다() {
        AnalysisStructureResponse response =
                new StructureAnalyzer(150, 100, 1_000, Duration.ofMinutes(1))
                        .analyze(
                                List.of(
                                        js(1, "src/a.js", "import b from './b'\n"),
                                        js(2, "src/big.js", "const x = 1\n".repeat(5_000)),
                                        js(3, "src/c.js", "import b from './b'\n"),
                                        js(4, "src/b.js", "export default 1\n")),
                                null,
                                null);

        assertThat(response.edges()).containsExactly(new StructureEdge(1, 4, EdgeType.IMPORT, 1));
        assertThat(response.unresolved())
                .containsExactly(
                        parseError(2, WorkBudget.EXHAUSTED_DETAIL),
                        parseError(3, WorkBudget.EXHAUSTED_DETAIL),
                        parseError(4, WorkBudget.EXHAUSTED_DETAIL));
        assertThat(response.edgesTruncated()).isTrue();
    }

    @Test
    void AC12_마감_시각을_넘기면_남은_파일은_PARSE_ERROR다() {
        AnalysisStructureResponse response =
                new StructureAnalyzer(150, 100, Long.MAX_VALUE, Duration.ZERO)
                        .analyze(
                                List.of(js(1, "src/big.js", "const x = 1\n".repeat(5_000))),
                                null,
                                null);

        assertThat(response.unresolved())
                .containsExactly(parseError(1, WorkBudget.EXHAUSTED_DETAIL));
        assertThat(response.edgesTruncated()).isTrue();
    }

    @Test
    void AC12_HTTP_판정_중_예산을_다_쓰면_남은_호출은_URL_UNKNOWN이다() {
        // 읽기는 작고, 호출마다 API 5천 개와 비교하는 경로 비교가 예산을 쓴다
        String code =
                "import axios from 'axios'\n"
                        + "axios.get('/api/z/1')\n".repeat(300)
                        + "axios.get('/api/x')\n";
        List<ApiEndpoint> apis =
                new ArrayList<>(Collections.nCopies(5_000, api("GET", "/api/y/{v}", 8)));
        apis.add(api("GET", "/api/x", 9));

        AnalysisStructureResponse response =
                new StructureAnalyzer(150, 1_000, 200_000, Duration.ofMinutes(1))
                        .analyze(List.of(js(1, "src/a.js", code)), apis, null);

        assertThat(response.unresolved())
                .extracting(UnresolvedLink::kind)
                .containsOnly(UnresolvedKind.HTTP_CALL);
        assertThat(http(response)).isEmpty();
        assertThat(httpUnresolved(response).getLast())
                .isEqualTo(unknown(1, 302, UnresolvedReason.URL_UNKNOWN, "axios.get('/api/x')"));
        assertThat(response.edgesTruncated()).isTrue();
    }

    /** content를 읽으면 오류가 나는 파일 (읽기 단계의 예상하지 못한 예외 대신). */
    private static AnalysisFile failing(long id, String path, String layer, Throwable error) {
        AnalysisFile f =
                new AnalysisFile() {
                    @Override
                    public String getContent() {
                        if (error instanceof RuntimeException e) {
                            throw e;
                        }
                        throw (Error) error;
                    }
                };
        f.setId(id);
        f.setPath(path);
        f.setLayer(layer);
        return f;
    }

    @Test
    void AC3_파일_하나를_읽다가_예상하지_못한_오류가_나도_그_파일만_PARSE_ERROR다() {
        AnalysisStructureResponse response =
                run(
                        List.of(),
                        js(1, "src/a.js", "import b from './b'\n"),
                        failing(2, "src/broken.js", "js", new IllegalStateException("boom")),
                        failing(3, "src/main/java/A.java", "service", new StackOverflowError()),
                        js(4, "src/b.js", "export default 1\n"),
                        failing(
                                5,
                                "src/main/resources/mapper/A.xml",
                                "mapper",
                                new IllegalStateException("boom")));

        assertThat(response.edges()).containsExactly(new StructureEdge(1, 4, EdgeType.IMPORT, 1));
        assertThat(response.unresolved())
                .containsExactly(
                        parseError(2, StructureLinks.FAILED_DETAIL),
                        parseError(3, StructureLinks.FAILED_DETAIL),
                        parseError(5, StructureLinks.FAILED_DETAIL));
        assertThat(response.edgesTruncated()).isFalse();
    }

    // ---- 5.3절 1: 이름은 안전하게 실패 ----

    @Test
    void AC7_매개변수의_모델링하지_않은_이름도_가리고_HTTP였을_이름이면_URL_UNKNOWN이다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100), api("GET", "/api/items", 101)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                const url = '/api/todos'
                                function request(u) { return fetch(u) }
                                const f = (url: string) => fetch(url)
                                function g(axios: any) { axios.get('/api/todos') }
                                const h = (fetch: any) => fetch('/api/todos')
                                const k = (request: any) => request('/api/todos')
                                axios.get(url)
                                request('/api/items')
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 8), edge(1, 101, 9));
        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::line, UnresolvedLink::reason)
                .containsExactly(
                        tuple(4, UnresolvedReason.URL_UNKNOWN),
                        tuple(5, UnresolvedReason.URL_UNKNOWN),
                        tuple(6, UnresolvedReason.URL_UNKNOWN),
                        tuple(7, UnresolvedReason.URL_UNKNOWN));
    }

    @Test
    void AC7_키워드_계산된_비공개_이름_메서드와_구조_분해_catch의_매개변수도_axios를_가린다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                const o = { delete(axios) { axios.get('/api/todos') } }
                                class A { ['x'](axios) { axios.get('/api/todos') } }
                                class B { #m(axios) { axios.get('/api/todos') } }
                                try {} catch ({ axios }) { axios.get('/api/todos') }
                                axios.get('/api/todos')
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 6));
        assertThat(httpUnresolved(response)).isEmpty();
    }

    @Test
    void AC10_구조_분해나_for_머리로_다시_대입한_매개변수는_부른_곳의_인자로_바꾸지_않는다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                function a(url) { [url] = ['/x']; return fetch(url) }
                                function b(url) { ({ url } = { url: '/x' }); return fetch(url) }
                                function c(url) { for (url of ['/x']) {} return fetch(url) }
                                a('/api/todos')
                                b('/api/todos')
                                c('/api/todos')
                                """));

        assertThat(http(response)).isEmpty();
        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::line, UnresolvedLink::reason)
                .containsExactly(
                        tuple(1, UnresolvedReason.URL_UNKNOWN),
                        tuple(2, UnresolvedReason.URL_UNKNOWN),
                        tuple(3, UnresolvedReason.URL_UNKNOWN));
    }

    @Test
    void AC10_다시_대입되는_let_var_래퍼는_인라인하지_않는다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                let get = (url) => fetch(url)
                                var load = function (url) { return fetch(url) }
                                get = window.other
                                load = null
                                get('/api/todos')
                                load('/api/todos')
                                """));

        assertThat(http(response)).isEmpty();
        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::line, UnresolvedLink::reason)
                .containsExactly(
                        tuple(1, UnresolvedReason.URL_UNKNOWN),
                        tuple(2, UnresolvedReason.URL_UNKNOWN));
    }

    @Test
    void AC10_다른_파일의_부른_곳은_최상위_래퍼에만_잇는다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100), api("GET", "/api/items", 101)),
                        js(
                                3,
                                "src/http.js",
                                """
                                export function get(url) { return fetch('/api/items') }
                                function outer() { function get(url) { return fetch(url) } return get }
                                """),
                        js(
                                4,
                                "src/c.js",
                                """
                                import { get } from './http.js'
                                get('/api/todos')
                                """));

        assertThat(http(response)).containsExactly(edge(3, 101, 1));
        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::fileId, UnresolvedLink::line, UnresolvedLink::reason)
                .containsExactly(tuple(3L, 2, UnresolvedReason.URL_UNKNOWN));
    }

    @Test
    void AC10_부른_곳의_전개_인자부터_뒤_매개변수는_값을_모른다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                function req(a, url) { return fetch(url) }
                                const args = ['/x', '/api/todos']
                                req(...args)
                                req('/x', ...args)
                                req(...args, '/api/todos')
                                """));

        assertThat(http(response)).isEmpty();
        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::line, UnresolvedLink::reason)
                .containsExactly(
                        tuple(3, UnresolvedReason.URL_UNKNOWN),
                        tuple(4, UnresolvedReason.URL_UNKNOWN),
                        tuple(5, UnresolvedReason.URL_UNKNOWN));
    }

    @Test
    void AC7_with_안의_호출은_URL_UNKNOWN이다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100)),
                        js(
                                1,
                                "src/a.js",
                                """
                                const url = '/api/todos'
                                with (obj) { fetch(url) }
                                with (obj) fetch('/api/todos')
                                fetch(url)
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 4));
        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::line, UnresolvedLink::reason)
                .containsExactly(
                        tuple(2, UnresolvedReason.URL_UNKNOWN),
                        tuple(3, UnresolvedReason.URL_UNKNOWN));
    }

    @Test
    void AC7_require로_얻은_axios_이름은_그_선언이_보이는_곳에서만_axios다() {
        AnalysisStructureResponse response =
                run(
                        List.of(api("GET", "/api/todos", 100), api("GET", "/api/items", 101)),
                        js(
                                1,
                                "src/a.js",
                                """
                                function f() { const axios = require('axios'); return axios.get('/api/todos') }
                                axios.get('/api/items')
                                """));

        assertThat(http(response)).containsExactly(edge(1, 100, 1));
        assertThat(httpUnresolved(response)).isEmpty();
    }

    // ---- 5.3절 5: 흔한 코드의 틀린 간선 ----

    @Test
    void AC10_매개변수가_붙은_URL도_부른_곳이_있으면_인라인을_먼저_한다() {
        AnalysisStructureResponse response =
                run(
                        List.of(
                                api("GET", "/api/todos", 100),
                                api("GET", "/api/items/{id}", 101),
                                api("GET", "/api/users", 102),
                                api("GET", "/api/notes/{id}", 103)),
                        js(
                                1,
                                "src/a.js",
                                """
                                const BASE = '/api'
                                function get(p) { return fetch(BASE + p) }
                                get('/todos')
                                get('/items/1')
                                function note(id) { return fetch('/api/notes/' + id) }
                                """));

        // 부른 곳이 없는 래퍼는 지금처럼 동적 조각을 와일드카드로 비교한다
        assertThat(http(response))
                .containsExactly(edge(1, 100, 3), edge(1, 101, 4), edge(1, 103, 5));
        assertThat(httpUnresolved(response)).isEmpty();
    }

    @Test
    void AC8_호스트_자리에_동적_조각이_있으면_URL_UNKNOWN이다() {
        AnalysisStructureResponse response =
                run(
                        List.of(
                                api("GET", "/api/a", 100),
                                api("GET", "/api/b", 101),
                                api("GET", "/api/c", 102),
                                api("GET", "/api/d", 103),
                                api("GET", "/api/x", 104)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                function h(host) { return fetch('http://' + host + '/api/a') }
                                h('localhost:8080')
                                const HOST = getHost()
                                fetch(`https://${HOST}/api/x`)
                                fetch('http://localhost:' + port + '/api/b')
                                fetch('http' + s + '://localhost/api/c')
                                const api = axios.create({ baseURL: `http://${HOST}/api` })
                                api.get('/x')
                                fetch('//' + HOST + '/api/x')
                                fetch('http://user' + u + '@localhost/api/d')
                                """));

        // 포트·userinfo·스킴의 동적 조각은 호스트를 바꾸지 않는다 (동적 조각은 놓인 URL 구성 요소 안에 머문다고 본다)
        assertThat(http(response))
                .containsExactly(
                        edge(1, 100, 3), edge(1, 101, 6), edge(1, 102, 7), edge(1, 103, 11));
        assertThat(httpUnresolved(response))
                .extracting(UnresolvedLink::line, UnresolvedLink::reason)
                .containsExactly(
                        tuple(5, UnresolvedReason.URL_UNKNOWN),
                        tuple(9, UnresolvedReason.BASE_URL_UNKNOWN),
                        tuple(10, UnresolvedReason.URL_UNKNOWN));
    }

    @Test
    void AC11_절대_URL의_호스트가_로컬도_설정된_baseURL_호스트도_아니면_NO_MATCHING_API다() {
        AnalysisStructureResponse response =
                run(
                        List.of(
                                api("GET", "/api/todos", 100),
                                api("GET", "/api/items", 101),
                                api("GET", "/api/users", 102),
                                api("GET", "/api/notes", 103),
                                api("GET", "/api/tags", 104),
                                api("POST", "/api/todos", 105)),
                        js(
                                1,
                                "src/a.js",
                                """
                                import axios from 'axios'
                                const gh = axios.create({ baseURL: 'https://api.myapp.test/api' })
                                fetch('https://api.myapp.test/api/todos')
                                fetch('https://API.MyApp.test:443/api/items')
                                fetch('https://api.github.com/api/todos')
                                fetch('http://127.0.0.1:8080/api/users')
                                axios.post('//cdn.example/api/todos')
                                fetch('https://prod.myapp.test/api/notes')
                                fetch('http://[::1]:8080/api/tags')
                                """),
                        js(
                                2,
                                "src/main.js",
                                """
                                import axios from 'axios'
                                axios.defaults.baseURL = 'https://prod.myapp.test'
                                """));

        assertThat(http(response))
                .containsExactly(
                        edge(1, 100, 3),
                        edge(1, 101, 4),
                        edge(1, 102, 6),
                        edge(1, 103, 8),
                        edge(1, 104, 9));
        assertThat(httpUnresolved(response))
                .containsExactly(
                        unknown(
                                1,
                                5,
                                UnresolvedReason.NO_MATCHING_API,
                                "GET api.github.com/api/todos"),
                        unknown(
                                1,
                                7,
                                UnresolvedReason.NO_MATCHING_API,
                                "POST cdn.example/api/todos"));
    }
}
