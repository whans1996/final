package com.vibenote.analysis.structure;

import static com.vibenote.analysis.structure.StructureFiles.file;
import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.ApiEndpoint;
import com.vibenote.analysis.structure.http.ContextPathReader;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** F-21a AC7~AC12 구조 계산 (docs/specs/F-21a-structure-graph.md). 입력은 테스트 안의 가짜 프로젝트다. */
class StructureAnalyzerTest {

    private final StructureAnalyzer analyzer = new StructureAnalyzer(150, 100);

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
                                export const abs = () => http.get('http://h/api/todos/1')
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

        assertThat(http(response)).containsExactly(edge(71, 100, 2), edge(71, 102, 6));
        assertThat(httpUnresolved(response))
                .containsExactly(
                        unknown(71, 3, UnresolvedReason.NO_MATCHING_API, "DELETE /app/api/todos"),
                        unknown(71, 4, UnresolvedReason.AMBIGUOUS, "GET /app/api/items/1"));
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
}
