package com.vibenote.analysis;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * F-19 공개 분석 타인 열람과 상태 sourceUrl (docs/specs/F-19-analysis-manage.md AC9, AC10, AC12). 행을 직접 넣고 다른
 * 회원으로 조회한다.
 */
class AnalysisPublicViewApiTest extends AnalysisTestSupport {

    private long fileId;

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    /** 파일·개념 연결·흐름·퀴즈가 있는 DONE 분석. */
    private long fullAnalysis(long ownerId, boolean isPublic) {
        long id = insertAnalysis(ownerId, "DONE", null, false, "todo-app");
        jdbc.update(
                "UPDATE analyses SET progress = 100, is_public = ?, memo = '주인 메모',"
                        + " overview = '{\"summary\":\"할 일 앱\",\"techStack\":[],\"layers\":[],"
                        + "\"readingOrder\":[],\"apis\":[]}' WHERE id = ?",
                isPublic,
                id);
        jdbc.update(
                "INSERT INTO analysis_files (analysis_id, path, layer, role, content, summary)"
                    + " VALUES (?, 'web/TodoController.java', 'controller', 'API', 'class A {}',"
                    + " '설명')",
                id);
        fileId = count("SELECT MAX(id) FROM analysis_files WHERE analysis_id = ?", id);
        jdbc.update(
                "INSERT INTO analysis_concepts (analysis_id, concept_id, file_id) VALUES (?,"
                        + " (SELECT MIN(id) FROM concepts WHERE status = 'PUBLISHED'), ?)",
                id,
                fileId);
        jdbc.update(
                "INSERT INTO analysis_flows (analysis_id, endpoint, status, created_at) VALUES"
                        + " (?, 'GET /api/todos', 'PENDING', NOW())",
                id);
        return id;
    }

    private String[] viewUrls(long id) {
        return new String[] {
            "/api/analyses/" + id,
            "/api/analyses/" + id + "/status",
            "/api/analyses/" + id + "/files",
            "/api/analyses/" + id + "/files/" + fileId,
            "/api/analyses/" + id + "/concepts",
            "/api/analyses/" + id + "/flows"
        };
    }

    @Test
    void AC9_공개_DONE_분석은_다른_회원도_6개_GET을_볼_수_있다() throws Exception {
        long u = createUser("f19ac9", "JUNIOR");
        long w = createUser("f19ac9w", "JUNIOR");
        long id = fullAnalysis(u, true);

        for (String url : viewUrls(id)) {
            mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer(w)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/analyses/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(w)))
                .andExpect(jsonPath("$.mine").value(false))
                .andExpect(jsonPath("$.owner.id").value(u))
                .andExpect(jsonPath("$.owner.nickname").value("f19ac9"))
                .andExpect(jsonPath("$.memo").value(nullValue()))
                .andExpect(jsonPath("$.overview.summary").value("할 일 앱"))
                .andExpect(jsonPath("$.fileCount").value(1));
        mockMvc.perform(
                        get("/api/analyses/{id}/files/{fileId}", id, fileId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(w)))
                .andExpect(jsonPath("$.content").value("class A {}"));
        mockMvc.perform(
                        get("/api/analyses/{id}/flows", id)
                                .header(HttpHeaders.AUTHORIZATION, bearer(w)))
                .andExpect(jsonPath("$[0].method").value("GET"))
                .andExpect(jsonPath("$[0].path").value("/api/todos"));
        mockMvc.perform(
                        get("/api/analyses/{id}/concepts", id)
                                .header(HttpHeaders.AUTHORIZATION, bearer(w)))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void AC10_비공개_DONE아님_삭제_탈퇴_주인이면_다른_회원에게_6개_GET_모두_404() throws Exception {
        long u = createUser("f19ac10", "JUNIOR");
        long w = createUser("f19ac10w", "JUNIOR");
        long hidden = fullAnalysis(u, false);
        long hiddenFile = fileId;
        long failed = fullAnalysis(u, true);
        jdbc.update("UPDATE analyses SET status = 'FAILED' WHERE id = ?", failed);
        long deleted = fullAnalysis(u, true);
        jdbc.update("UPDATE analyses SET deleted_at = NOW() WHERE id = ?", deleted);

        long gone = createUser("f19ac10g", "JUNIOR");
        long withdrawn = fullAnalysis(gone, true);
        jdbc.update("UPDATE users SET status = 'WITHDRAWN' WHERE id = ?", gone);

        for (long id : new long[] {hidden, failed, deleted, withdrawn}) {
            fileId = count("SELECT MAX(id) FROM analysis_files WHERE analysis_id = ?", id);
            for (String url : viewUrls(id)) {
                mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer(w)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
            }
        }
        // 주인 본인은 비공개·FAILED도 본다
        fileId = hiddenFile;
        for (String url : viewUrls(hidden)) {
            mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer(u)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void AC10_공개여도_흐름_생성_퀴즈_조회_풀이는_주인만() throws Exception {
        long u = createUser("f19ac10q", "JUNIOR");
        long w = createUser("f19ac10qw", "JUNIOR");
        long id = fullAnalysis(u, true);
        jdbc.update("INSERT INTO quizzes (analysis_id) VALUES (?)", id);
        long quiz = count("SELECT id FROM quizzes WHERE analysis_id = ?", id);
        jdbc.update(
                "INSERT INTO quiz_questions (quiz_id, question, choices, answer_index) VALUES (?,"
                        + " 'Q', '[\"a\",\"b\",\"c\",\"d\"]', 0)",
                quiz);
        long question = count("SELECT MAX(id) FROM quiz_questions WHERE quiz_id = ?", quiz);

        mockMvc.perform(
                        post("/api/analyses/{id}/flows", id)
                                .header(HttpHeaders.AUTHORIZATION, bearer(w))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"endpoints\":[{\"method\":\"GET\",\"path\":\"/api/todos\"}]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        mockMvc.perform(
                        get("/api/analyses/{id}/quiz", id)
                                .header(HttpHeaders.AUTHORIZATION, bearer(w)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        mockMvc.perform(
                        post("/api/quizzes/{quizId}/attempts", quiz)
                                .header(HttpHeaders.AUTHORIZATION, bearer(w))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"answers\":[{\"questionId\":%d,\"selectedIndex\":0}]}"
                                                .formatted(question)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUIZ_404"));
    }

    @Test
    void AC12_수집_실패한_URL_분석의_상태에_sourceUrl이_있고_ZIP은_null() throws Exception {
        long u = createUser("f19ac12", "JUNIOR");
        long url = insertUrlAnalysis(u, "https://github.com/octo/todo-app");
        jdbc.update(
                "UPDATE analyses SET status = 'FAILED', error_code = 'ANALYSIS_004' WHERE id = ?",
                url);
        long zip = insertAnalysis(u, "FAILED", null, false, "todo-app");

        mockMvc.perform(
                        get("/api/analyses/{id}/status", url)
                                .header(HttpHeaders.AUTHORIZATION, bearer(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.sourceUrl").value("https://github.com/octo/todo-app"));
        mockMvc.perform(
                        get("/api/analyses/{id}/status", zip)
                                .header(HttpHeaders.AUTHORIZATION, bearer(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceUrl").value(nullValue()));
    }
}
