package com.vibenote.analysis;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/** F-06 결과 조회 API (docs/specs/F-06-overview-explanation.md AC9~AC11). 행을 직접 넣고 조회한다. */
class AnalysisResultApiTest extends AnalysisTestSupport {

    private static final String OVERVIEW =
            """
            {"summary":"할 일 앱입니다.","techStack":["Spring Boot"],
             "layers":[{"layer":"controller","role":"요청을 받습니다."}],
             "readingOrder":[{"fileId":%d,"path":"web/TodoController.java","reason":"먼저 봅니다."}],
             "apis":[{"method":"GET","path":"/api/todos","fileId":%d}]}
            """;

    private long insertFile(
            long analysisId, String path, String layer, String role, String summary) {
        jdbc.update(
                "INSERT INTO analysis_files (analysis_id, path, layer, role, content, summary)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                analysisId,
                path,
                layer,
                role,
                "class X {}\n",
                summary);
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM analysis_files WHERE analysis_id = ?", Long.class, analysisId);
    }

    private long doneAnalysis(long userId) {
        long id = insertAnalysis(userId, "DONE", null, false, "todo-app");
        jdbc.update("UPDATE analyses SET progress = 100, level = 'JUNIOR' WHERE id = ?", id);
        return id;
    }

    @Test
    void AC9_DONE인_내_분석의_상세와_개요를_준다() throws Exception {
        long userId = createUser("f06ac9", "JUNIOR");
        long id = doneAnalysis(userId);
        long controller = insertFile(id, "web/TodoController.java", "controller", "API", "설명");
        insertFile(id, "web/todo.js", "js", null, null);
        jdbc.update(
                "UPDATE analyses SET overview = ? WHERE id = ?",
                OVERVIEW.formatted(controller, controller),
                id);

        mockMvc.perform(
                        get("/api/analyses/{id}", id)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("todo-app"))
                .andExpect(jsonPath("$.sourceType").value("ZIP"))
                .andExpect(jsonPath("$.level").value("JUNIOR"))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.progress").value(100))
                .andExpect(jsonPath("$.isPublic").value(false))
                .andExpect(jsonPath("$.errorCode").value(nullValue()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.fileCount").value(2))
                .andExpect(jsonPath("$.explainedFileCount").value(1))
                .andExpect(jsonPath("$.overview.summary").value("할 일 앱입니다."))
                .andExpect(jsonPath("$.overview.techStack[0]").value("Spring Boot"))
                .andExpect(jsonPath("$.overview.layers[0].layer").value("controller"))
                .andExpect(jsonPath("$.overview.readingOrder[0].fileId").value(controller))
                .andExpect(jsonPath("$.overview.apis[0].method").value("GET"))
                .andExpect(jsonPath("$.overview.apis[0].path").value("/api/todos"))
                .andExpect(jsonPath("$.overview.apis[0].fileId").value(controller));
    }

    @Test
    void AC9_진행_중이면_overview가_null이다() throws Exception {
        long userId = createUser("f06ac9run", "JUNIOR");
        long id = insertAnalysis(userId, "SELECTING", null, false, "running");

        mockMvc.perform(
                        get("/api/analyses/{id}", id)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SELECTING"))
                .andExpect(jsonPath("$.overview").value(nullValue()))
                .andExpect(jsonPath("$.fileCount").value(0));
    }

    @Test
    void AC9_AC10_AC11_없거나_남의_것이거나_삭제된_분석이면_404_ANALYSIS_404() throws Exception {
        long owner = createUser("f06own", "JUNIOR");
        long other = createUser("f06other", "JUNIOR");
        long mine = doneAnalysis(owner);
        long file = insertFile(mine, "A.java", "other", null, null);
        long deleted = insertAnalysis(owner, "DONE", null, true, "deleted");

        String[][] cases = {
            {String.valueOf(mine), String.valueOf(other)},
            {String.valueOf(deleted), String.valueOf(owner)},
            {"999999999", String.valueOf(owner)}
        };
        for (String[] c : cases) {
            String auth = bearer(Long.parseLong(c[1]));
            for (String url :
                    new String[] {
                        "/api/analyses/" + c[0],
                        "/api/analyses/" + c[0] + "/files",
                        "/api/analyses/" + c[0] + "/files/" + file
                    }) {
                mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, auth))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
            }
        }
    }

    @Test
    void AC9_AC10_AC11_토큰이_없으면_401_AUTH_401() throws Exception {
        for (String url :
                new String[] {
                    "/api/analyses/1", "/api/analyses/1/files", "/api/analyses/1/files/2"
                }) {
            mockMvc.perform(get(url))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_401"));
        }
    }

    @Test
    void AC10_파일_목록을_id_순으로_content_없이_준다() throws Exception {
        long userId = createUser("f06ac10", "JUNIOR");
        long id = doneAnalysis(userId);
        long first = insertFile(id, "web/TodoController.java", "controller", "API", "설명");
        long second = insertFile(id, "web/todo.js", "js", null, null);

        mockMvc.perform(
                        get("/api/analyses/{id}/files", id)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(first))
                .andExpect(jsonPath("$[0].path").value("web/TodoController.java"))
                .andExpect(jsonPath("$[0].layer").value("controller"))
                .andExpect(jsonPath("$[0].role").value("API"))
                .andExpect(jsonPath("$[0].summary").value("설명"))
                .andExpect(jsonPath("$[0].content").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(second))
                .andExpect(jsonPath("$[1].role").value(nullValue()))
                .andExpect(jsonPath("$[1].summary").value(nullValue()));
    }

    @Test
    void AC10_파일_저장_전이면_빈_배열() throws Exception {
        long userId = createUser("f06ac10e", "JUNIOR");
        long id = insertAnalysis(userId, "FILTERING", null, false, "running");

        mockMvc.perform(
                        get("/api/analyses/{id}/files", id)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void AC11_파일_하나를_content와_함께_준다() throws Exception {
        long userId = createUser("f06ac11", "JUNIOR");
        long id = doneAnalysis(userId);
        long file = insertFile(id, "web/TodoController.java", "controller", "API", "설명");

        mockMvc.perform(
                        get("/api/analyses/{id}/files/{fileId}", id, file)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(file))
                .andExpect(jsonPath("$.path").value("web/TodoController.java"))
                .andExpect(jsonPath("$.layer").value("controller"))
                .andExpect(jsonPath("$.role").value("API"))
                .andExpect(jsonPath("$.summary").value("설명"))
                .andExpect(jsonPath("$.content").value("class X {}\n"));
    }

    @Test
    void AC11_다른_분석의_파일이면_404_ANALYSIS_001() throws Exception {
        long userId = createUser("f06ac11x", "JUNIOR");
        long mine = doneAnalysis(userId);
        long otherAnalysis = doneAnalysis(userId);
        long otherFile = insertFile(otherAnalysis, "B.java", "other", null, null);

        mockMvc.perform(
                        get("/api/analyses/{id}/files/{fileId}", mine, otherFile)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_001"))
                .andExpect(jsonPath("$.message").value("파일을 찾을 수 없습니다."));
        mockMvc.perform(
                        get("/api/analyses/{id}/files/{fileId}", mine, 999999999L)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_001"));
    }
}
