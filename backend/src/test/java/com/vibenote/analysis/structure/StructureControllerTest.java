package com.vibenote.analysis.structure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtTokenProvider;
import com.vibenote.user.UserService;
import com.vibenote.user.dto.SignupRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

/**
 * F-21a AC12·AC13 GET /api/analyses/{id}/structure (docs/specs/F-21a-structure-graph.md). 행을 직접 넣고 {@code
 * @f21.test} 회원을 지워 정리한다 (analyses → analysis_files는 FK CASCADE). 테스트 코드는 직접 만든다 (eval 데이터 금지).
 */
class StructureControllerTest extends IntegrationTestBase {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Autowired UserService userService;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired JdbcTemplate jdbc;
    @Autowired StructureCache cache;

    @AfterEach
    void cleanUpUsers() {
        jdbc.update("DELETE FROM users WHERE email LIKE '%@f21.test'");
    }

    private long createUser(String name) {
        return userService.signup(
                new SignupRequest(name + "@f21.test", "vibe1234", name, "JUNIOR"));
    }

    private ResultActions getStructure(Long userId, long analysisId) throws Exception {
        var request = get("/api/analyses/{id}/structure", analysisId);
        if (userId != null) {
            request.header(
                    "Authorization", "Bearer " + tokenProvider.createAccessToken(userId, "USER"));
        }
        return mockMvc.perform(request);
    }

    private long analysis(long userId, String status, boolean isPublic) {
        jdbc.update(
                "INSERT INTO analyses (user_id, source_type, project_name, title, level, status,"
                    + " is_public, created_at) VALUES (?, 'ZIP', 'todo-app', 'todo-app', 'JUNIOR',"
                    + " ?, ?, ?)",
                userId,
                status,
                isPublic,
                LocalDateTime.now(KST).withNano(0));
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM analyses WHERE user_id = ?", Long.class, userId);
    }

    private long insertFile(
            long analysisId, String path, String layer, String role, String content) {
        jdbc.update(
                "INSERT INTO analysis_files (analysis_id, path, layer, role, content) VALUES (?, ?,"
                        + " ?, ?, ?)",
                analysisId,
                path,
                layer,
                role,
                content);
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM analysis_files WHERE analysis_id = ?", Long.class, analysisId);
    }

    /** controller·service·view 3개 파일과 개요(GET /api/todos → controller)가 있는 분석. */
    private long[] fullAnalysis(long userId, boolean isPublic) {
        long id = analysis(userId, "DONE", isPublic);
        long controller =
                insertFile(
                        id,
                        "src/main/java/com/example/todo/TodoController.java",
                        "controller",
                        "할 일 API",
                        "package com.example.todo;\n\n"
                                + "public class TodoController {\n"
                                + "  TodoService service;\n"
                                + "}");
        long service =
                insertFile(
                        id,
                        "src/main/java/com/example/todo/TodoService.java",
                        "service",
                        null,
                        "package com.example.todo;\npublic class TodoService {}");
        long view =
                insertFile(
                        id,
                        "frontend/src/views/TodoView.vue",
                        "vue",
                        "할 일 화면",
                        "<script setup>\n"
                                + "import axios from 'axios'\n"
                                + "axios.get('/api/todos')\n"
                                + "import x from './Gone'\n"
                                + "</script>");
        jdbc.update(
                "UPDATE analyses SET progress = 100, overview = ? WHERE id = ?",
                "{\"summary\":\"할 일 앱\",\"techStack\":[],\"layers\":[],\"readingOrder\":[],"
                        + "\"apis\":[{\"method\":\"GET\",\"path\":\"/api/todos\",\"fileId\":"
                        + controller
                        + "}]}",
                id);
        return new long[] {id, controller, service, view};
    }

    @Test
    void AC12_내_분석의_구조를_계산해_200으로_준다() throws Exception {
        long owner = createUser("owner");
        long[] ids = fullAnalysis(owner, false);
        long id = ids[0];
        long controller = ids[1];
        long service = ids[2];
        long view = ids[3];

        String body =
                getStructure(owner, id)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.nodes.length()").value(3))
                        .andExpect(jsonPath("$.nodes[0].fileId").value(controller))
                        .andExpect(jsonPath("$.nodes[0].name").value("TodoController.java"))
                        .andExpect(jsonPath("$.nodes[0].layer").value("controller"))
                        .andExpect(jsonPath("$.nodes[0].role").value("할 일 API"))
                        .andExpect(jsonPath("$.nodes[1].role").value(nullValue()))
                        .andExpect(jsonPath("$.edges.length()").value(2))
                        .andExpect(jsonPath("$.edges[0].from").value(controller))
                        .andExpect(jsonPath("$.edges[0].to").value(service))
                        .andExpect(jsonPath("$.edges[0].type").value("IMPORT"))
                        .andExpect(jsonPath("$.edges[0].line").value(4))
                        .andExpect(jsonPath("$.edges[1].from").value(view))
                        .andExpect(jsonPath("$.edges[1].to").value(controller))
                        .andExpect(jsonPath("$.edges[1].type").value("HTTP"))
                        .andExpect(jsonPath("$.edges[1].line").value(3))
                        .andExpect(jsonPath("$.unresolved.length()").value(1))
                        .andExpect(jsonPath("$.unresolved[0].fileId").value(view))
                        .andExpect(jsonPath("$.unresolved[0].line").value(4))
                        .andExpect(jsonPath("$.unresolved[0].kind").value("IMPORT"))
                        .andExpect(jsonPath("$.unresolved[0].reason").value("NOT_SELECTED"))
                        .andExpect(
                                jsonPath("$.unresolved[0].detail").value("frontend/src/views/Gone"))
                        .andExpect(jsonPath("$.edgesTruncated").value(false))
                        .andExpect(jsonPath("$.unresolvedTruncated").value(false))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(getStructure(owner, id).andReturn().getResponse().getContentAsString())
                .isEqualTo(body);
    }

    @Test
    void AC12_파일이_없는_진행_중_분석은_빈_배열로_200() throws Exception {
        long owner = createUser("owner");
        long id = analysis(owner, "SELECTING", false);

        getStructure(owner, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes.length()").value(0))
                .andExpect(jsonPath("$.edges.length()").value(0))
                .andExpect(jsonPath("$.unresolved.length()").value(0));
    }

    @Test
    void AC13_공개_DONE_분석은_다른_회원도_주인과_같은_내용을_본다() throws Exception {
        long owner = createUser("owner");
        long viewer = createUser("viewer");
        long id = fullAnalysis(owner, true)[0];

        String mine = getStructure(owner, id).andReturn().getResponse().getContentAsString();
        getStructure(viewer, id).andExpect(status().isOk());
        assertThat(getStructure(viewer, id).andReturn().getResponse().getContentAsString())
                .isEqualTo(mine);
    }

    @Test
    void AC13_비공개_미완료_삭제_탈퇴_주인_없는_분석은_404_토큰_없으면_401() throws Exception {
        long owner = createUser("owner");
        long viewer = createUser("viewer");
        long hidden = fullAnalysis(owner, false)[0];
        long running = analysis(owner, "EXPLAINING", true);
        long deleted = fullAnalysis(owner, true)[0];
        jdbc.update("UPDATE analyses SET deleted_at = NOW() WHERE id = ?", deleted);
        long gone = createUser("gone");
        long withdrawn = fullAnalysis(gone, true)[0];
        jdbc.update("UPDATE users SET status = 'WITHDRAWN' WHERE id = ?", gone);

        for (long id : new long[] {hidden, running, deleted, withdrawn, 999_999_999L}) {
            getStructure(viewer, id)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        }
        getStructure(owner, deleted).andExpect(status().isNotFound());
        getStructure(null, hidden)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void AC12_분석을_지우면_커밋_뒤_구조_캐시를_비운다() throws Exception {
        long owner = createUser("owner");
        long id = fullAnalysis(owner, false)[0];
        getStructure(owner, id).andExpect(status().isOk());
        assertThat(cache.contains(id)).isTrue();

        mockMvc.perform(
                        delete("/api/analyses/{id}", id)
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenProvider.createAccessToken(owner, "USER")))
                .andExpect(status().isNoContent());

        assertThat(cache.contains(id)).isFalse();
        getStructure(owner, id).andExpect(status().isNotFound());
    }
}
