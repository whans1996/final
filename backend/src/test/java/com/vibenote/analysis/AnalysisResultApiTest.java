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

    private void usageLog(long analysisId, String provider, boolean success) {
        jdbc.update(
                "INSERT INTO llm_usage_logs (analysis_id, stage, provider, model, success)"
                        + " VALUES (?, 'file', ?, 'm', ?)",
                analysisId,
                provider,
                success);
    }

    /** F-27 AC10: 성공한 Ollama(간이 모드) 호출이 1건 이상인 분석만 liteMode. */
    @Test
    void F27_AC10_성공한_Ollama_호출이_있는_분석만_liteMode다() throws Exception {
        long owner = createUser("f27ac10", "JUNIOR");
        long other = createUser("f27ac10o", "JUNIOR");
        long lite = doneAnalysis(owner);
        usageLog(lite, "gms", false);
        usageLog(lite, "ollama", true);
        long gmsOnly = doneAnalysis(owner);
        usageLog(gmsOnly, "gms", true);
        usageLog(gmsOnly, "ollama", false);
        long noLogs = doneAnalysis(owner);
        long mock = doneAnalysis(owner);
        usageLog(mock, "mock", true);
        long legacy = doneAnalysis(owner);
        usageLog(legacy, null, true); // V13 이전 행 (provider NULL)
        jdbc.update("UPDATE analyses SET is_public = TRUE WHERE id = ?", lite);

        String[][] cases = {
            {lite + "", owner + "", "true"},
            {lite + "", other + "", "true"},
            {gmsOnly + "", owner + "", "false"},
            {noLogs + "", owner + "", "false"},
            {mock + "", owner + "", "false"},
            {legacy + "", owner + "", "false"}
        };
        for (String[] c : cases) {
            mockMvc.perform(
                            get("/api/analyses/{id}", Long.parseLong(c[0]))
                                    .header(
                                            HttpHeaders.AUTHORIZATION,
                                            bearer(Long.parseLong(c[1]))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liteMode").value(Boolean.parseBoolean(c[2])));
        }
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

    /** L-1a AC4: 상세에 퀴즈 상태와 남은 재생성 횟수(3 − 사용, 최소 0)를 준다. */
    @Test
    void L1a_AC4_상세에_quizStatus와_남은_재생성_횟수를_준다() throws Exception {
        long userId = createUser("l1aac4d", "JUNIOR");
        long failed = doneAnalysis(userId);
        jdbc.update(
                "UPDATE analyses SET quiz_status = 'FAILED', quiz_regenerations = 1 WHERE id = ?",
                failed);
        long exhausted = doneAnalysis(userId);
        jdbc.update(
                "UPDATE analyses SET quiz_status = 'FAILED', quiz_regenerations = 5 WHERE id = ?",
                exhausted);
        long running = insertAnalysis(userId, "EXPLAINING", null, false, "running");

        mockMvc.perform(
                        get("/api/analyses/{id}", failed)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizStatus").value("FAILED"))
                .andExpect(jsonPath("$.quizRegenerationsLeft").value(2));
        mockMvc.perform(
                        get("/api/analyses/{id}", exhausted)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(jsonPath("$.quizRegenerationsLeft").value(0));
        mockMvc.perform(
                        get("/api/analyses/{id}", running)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(jsonPath("$.quizStatus").value(nullValue()))
                .andExpect(jsonPath("$.quizRegenerationsLeft").value(3));
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

    /** 인덱스에만 있고 설명 대상으로 선별되지 않은 파일 (F-26a). */
    private long insertIndexOnlyFile(long analysisId, String path, String layer) {
        long id = insertFile(analysisId, path, layer, null, null);
        jdbc.update("UPDATE analysis_files SET selected = FALSE WHERE id = ?", id);
        return id;
    }

    @Test
    void F26a_AC8_선별되지_않은_파일_상세는_404() throws Exception {
        long userId = createUser("f26ac8api", "JUNIOR");
        long id = doneAnalysis(userId);
        long selected = insertFile(id, "web/TodoController.java", "controller", "API", "설명");
        long indexOnly = insertIndexOnlyFile(id, "src/test/java/TodoTest.java", "test");

        mockMvc.perform(
                        get("/api/analyses/{id}/files/{fileId}", id, indexOnly)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_001"));
        mockMvc.perform(
                        get("/api/analyses/{id}/files", id)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(selected));
        mockMvc.perform(
                        get("/api/analyses/{id}", id)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileCount").value(1));
    }
}
