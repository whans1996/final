package com.vibenote.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.analysis.flow.FlowResult;
import com.vibenote.analysis.flow.FlowService;
import com.vibenote.analysis.flow.FlowStep;
import com.vibenote.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** F-19 분석 수정·공개·삭제 (docs/specs/F-19-analysis-manage.md AC1~AC8, AC11). 행을 직접 넣고 API로 바꾼다. */
class AnalysisManageApiTest extends AnalysisTestSupport {

    @Autowired FlowService flowService;

    /** posts.user_id FK는 RESTRICT라 회원 정리(상위 클래스 @AfterEach) 전에 AC6의 게시글을 지운다. */
    @AfterEach
    void cleanUpPosts() {
        jdbc.update(
                "DELETE p FROM posts p JOIN users u ON u.id = p.user_id WHERE u.email LIKE"
                        + " '%@f05a.test'");
    }

    private ResultActions patchInfo(long userId, long id, String body) throws Exception {
        return mockMvc.perform(
                patch("/api/analyses/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private ResultActions patchVisibility(long userId, long id, String body) throws Exception {
        return mockMvc.perform(
                patch("/api/analyses/{id}/visibility", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private ResultActions deleteAnalysis(long userId, long id) throws Exception {
        return mockMvc.perform(
                delete("/api/analyses/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }

    private String nickname(long userId) {
        return jdbc.queryForObject("SELECT nickname FROM users WHERE id = ?", String.class, userId);
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private void follow(long follower, long following) {
        jdbc.update(
                "INSERT INTO follows (follower_id, following_id) VALUES (?, ?)",
                follower,
                following);
    }

    private void activity(long userId, long analysisId, LocalDateTime at) {
        jdbc.update(
                "INSERT INTO activities (user_id, type, target_id, created_at) VALUES (?,"
                        + " 'ANALYSIS', ?, ?)",
                userId,
                analysisId,
                at);
    }

    @Test
    void AC1_AC11_주인이_제목과_메모를_고치면_200과_바뀐_상세를_준다() throws Exception {
        long u = createUser("f19ac1", "JUNIOR");
        long id = insertAnalysis(u, "EXPLAINING", null, false, "todo-app");

        patchInfo(u, id, "{\"title\":\"  할 일 앱 백엔드 \",\"memo\":\"JWT 필터\\n순서 다시 보기\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("할 일 앱 백엔드"))
                .andExpect(jsonPath("$.memo").value("JWT 필터\n순서 다시 보기"))
                .andExpect(jsonPath("$.mine").value(true))
                .andExpect(jsonPath("$.owner.id").value(u))
                .andExpect(jsonPath("$.owner.nickname").value(nickname(u)))
                .andExpect(jsonPath("$.status").value("EXPLAINING"));

        // AC11: 상세·목록에도 메모가 온다
        mockMvc.perform(get("/api/analyses/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(u)))
                .andExpect(jsonPath("$.memo").value("JWT 필터\n순서 다시 보기"))
                .andExpect(jsonPath("$.mine").value(true))
                .andExpect(jsonPath("$.owner.id").value(u));
        mockMvc.perform(get("/api/analyses").header(HttpHeaders.AUTHORIZATION, bearer(u)))
                .andExpect(jsonPath("$.items[0].memo").value("JWT 필터\n순서 다시 보기"));
        // 파이프라인 진행에는 영향이 없다
        assertThat(analysisRow(id).get("status")).isEqualTo("EXPLAINING");
    }

    @Test
    void AC1_빈_문자열_공백_누락은_null로_저장한다() throws Exception {
        long u = createUser("f19ac1n", "JUNIOR");
        long id = insertAnalysis(u, "DONE", null, false, "todo-app");
        jdbc.update("UPDATE analyses SET memo = '예전 메모' WHERE id = ?", id);

        patchInfo(u, id, "{\"title\":\"   \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(nullValue()))
                .andExpect(jsonPath("$.memo").value(nullValue()));
        assertThat(analysisRow(id).get("title")).isNull();
        assertThat(analysisRow(id).get("memo")).isNull();

        patchInfo(u, id, "{\"title\":\"\",\"memo\":\"\"}").andExpect(status().isOk());
        assertThat(analysisRow(id).get("title")).isNull();
        assertThat(analysisRow(id).get("memo")).isNull();
    }

    @Test
    void AC2_제목_101자_메모_1001자면_400이고_저장하지_않는다() throws Exception {
        long u = createUser("f19ac2", "JUNIOR");
        long id = insertAnalysis(u, "DONE", null, false, "todo-app");

        patchInfo(
                        u,
                        id,
                        "{\"title\":\"%s\",\"memo\":\"%s\"}"
                                .formatted("가".repeat(101), "나".repeat(1001)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.title").exists())
                .andExpect(jsonPath("$.details.memo").exists());
        assertThat(analysisRow(id).get("title")).isEqualTo("todo-app");

        // 경계: 100자·1000자는 저장된다
        patchInfo(
                        u,
                        id,
                        "{\"title\":\"%s\",\"memo\":\"%s\"}"
                                .formatted("가".repeat(100), "나".repeat(1000)))
                .andExpect(status().isOk());
        assertThat(analysisRow(id).get("title")).isEqualTo("가".repeat(100));
        assertThat(analysisRow(id).get("memo")).isEqualTo("나".repeat(1000));
    }

    @Test
    void AC3_남의_분석_삭제된_분석_없는_id는_세_API_모두_404() throws Exception {
        long u = createUser("f19ac3", "JUNIOR");
        long other = createUser("f19ac3o", "JUNIOR");
        long othersPublic = insertAnalysis(other, "DONE", null, false, "others");
        jdbc.update("UPDATE analyses SET is_public = TRUE WHERE id = ?", othersPublic);
        long deleted = insertAnalysis(u, "DONE", null, true, "deleted");

        for (long id : new long[] {othersPublic, deleted, 999_999_999L}) {
            patchInfo(u, id, "{\"title\":\"x\"}")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
            patchVisibility(u, id, "{\"isPublic\":false}")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
            deleteAnalysis(u, id)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        }
        assertThat(analysisRow(othersPublic).get("title")).isEqualTo("others");
        assertThat(analysisRow(othersPublic).get("deleted_at")).isNull();
    }

    @Test
    void AC4_DONE_분석을_공개하면_팔로워_피드에_원래_활동이_보이고_비공개면_빠진다() throws Exception {
        long u = createUser("f19ac4", "JUNIOR");
        long f = createUser("f19ac4f", "JUNIOR");
        follow(f, u);
        long id = insertAnalysis(u, "DONE", null, false, "todo-app");
        LocalDateTime doneAt = LocalDateTime.of(2026, 11, 20, 10, 0);
        activity(u, id, doneAt);

        mockMvc.perform(get("/api/feed").header(HttpHeaders.AUTHORIZATION, bearer(f)))
                .andExpect(jsonPath("$.items[*].target.id", not(hasItem((int) id))));

        patchVisibility(u, id, "{\"isPublic\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.isPublic").value(true));

        mockMvc.perform(get("/api/feed").header(HttpHeaders.AUTHORIZATION, bearer(f)))
                .andExpect(jsonPath("$.items[0].type").value("ANALYSIS"))
                .andExpect(jsonPath("$.items[0].target.id").value(id))
                .andExpect(jsonPath("$.items[0].createdAt").value("2026-11-20T10:00:00+09:00"));
        assertThat(count("SELECT COUNT(*) FROM activities WHERE user_id = ?", u)).isEqualTo(1);

        patchVisibility(u, id, "{\"isPublic\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(false));
        mockMvc.perform(get("/api/feed").header(HttpHeaders.AUTHORIZATION, bearer(f)))
                .andExpect(jsonPath("$.items[*].target.id", not(hasItem((int) id))));
    }

    @Test
    void AC5_DONE이_아니면_공개는_409_비공개는_200_null은_400() throws Exception {
        long u = createUser("f19ac5", "JUNIOR");
        long running = insertAnalysis(u, "EXTRACTING", null, false, "running");
        long failed = insertAnalysis(u, "FAILED", null, false, "failed");

        for (long id : new long[] {running, failed}) {
            patchVisibility(u, id, "{\"isPublic\":true}")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_006"))
                    .andExpect(jsonPath("$.message").value(ErrorCode.ANALYSIS_006.message()));
            assertThat(analysisRow(id).get("is_public")).isEqualTo(false);
            patchVisibility(u, id, "{\"isPublic\":false}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isPublic").value(false));
        }
        for (String body : new String[] {"{}", "{\"isPublic\":null}"}) {
            patchVisibility(u, running, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_400"))
                    .andExpect(jsonPath("$.details.isPublic").exists());
        }
    }

    @Test
    void AC6_DONE_분석을_지우면_자식은_지우고_퀴즈_풀이는_남긴다() throws Exception {
        long u = createUser("f19ac6", "JUNIOR");
        long f = createUser("f19ac6f", "JUNIOR");
        follow(f, u);
        long id = insertAnalysis(u, "DONE", null, false, "todo-app");
        jdbc.update("UPDATE analyses SET is_public = TRUE WHERE id = ?", id);
        activity(u, id, LocalDateTime.of(2026, 11, 20, 10, 0));
        jdbc.update(
                "INSERT INTO analysis_files (analysis_id, path, layer, content) VALUES (?,"
                        + " 'A.java', 'controller', 'class A {}')",
                id);
        long file = count("SELECT MAX(id) FROM analysis_files WHERE analysis_id = ?", id);
        long concept = count("SELECT MIN(id) FROM concepts");
        jdbc.update(
                "INSERT INTO analysis_concepts (analysis_id, concept_id, file_id) VALUES (?, ?, ?)",
                id,
                concept,
                file);
        jdbc.update(
                "INSERT INTO analysis_flows (analysis_id, endpoint, status, created_at) VALUES"
                        + " (?, 'GET /api/todos', 'PENDING', NOW())",
                id);
        jdbc.update("INSERT INTO quizzes (analysis_id) VALUES (?)", id);
        long quiz = count("SELECT id FROM quizzes WHERE analysis_id = ?", id);
        jdbc.update(
                "INSERT INTO quiz_questions (quiz_id, concept_id, question, choices, answer_index)"
                        + " VALUES (?, ?, 'Q', '[\"a\",\"b\",\"c\",\"d\"]', 0)",
                quiz,
                concept);
        long question = count("SELECT MAX(id) FROM quiz_questions WHERE quiz_id = ?", quiz);
        jdbc.update(
                "INSERT INTO quiz_attempts (quiz_id, user_id, score, total) VALUES (?, ?, 1, 2)",
                quiz,
                u);
        long attempt = count("SELECT MAX(id) FROM quiz_attempts WHERE quiz_id = ?", quiz);
        jdbc.update(
                "INSERT INTO quiz_answers (attempt_id, question_id, selected_index, is_correct)"
                        + " VALUES (?, ?, 0, TRUE)",
                attempt,
                question);
        jdbc.update(
                "INSERT INTO posts (user_id, board_type, title, content, analysis_id) VALUES (?,"
                        + " 'QNA', '질문', '내용', ?)",
                u,
                id);
        long post = count("SELECT MAX(id) FROM posts WHERE user_id = ?", u);

        deleteAnalysis(u, id).andExpect(status().isNoContent());

        assertThat(analysisRow(id).get("deleted_at")).isNotNull();
        for (String table :
                new String[] {"analysis_concepts", "analysis_flows", "analysis_files"}) {
            assertThat(count("SELECT COUNT(*) FROM " + table + " WHERE analysis_id = ?", id))
                    .as(table)
                    .isZero();
        }
        assertThat(count("SELECT COUNT(*) FROM quizzes WHERE id = ?", quiz)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM quiz_questions WHERE quiz_id = ?", quiz))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM quiz_attempts WHERE quiz_id = ?", quiz))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM quiz_answers WHERE attempt_id = ?", attempt))
                .isEqualTo(1);

        mockMvc.perform(get("/api/analyses").header(HttpHeaders.AUTHORIZATION, bearer(u)))
                .andExpect(jsonPath("$.total").value(0));
        mockMvc.perform(
                        get("/api/users/{id}/stats", u)
                                .header(HttpHeaders.AUTHORIZATION, bearer(u)))
                .andExpect(jsonPath("$.analysisCount").value(0))
                .andExpect(jsonPath("$.quizAccuracy").value(50));
        mockMvc.perform(get("/api/feed").header(HttpHeaders.AUTHORIZATION, bearer(f)))
                .andExpect(jsonPath("$.items[*].target.id", not(hasItem((int) id))));
        mockMvc.perform(get("/api/posts/{id}", post).header(HttpHeaders.AUTHORIZATION, bearer(f)))
                .andExpect(jsonPath("$.analysis.id").value(id))
                .andExpect(jsonPath("$.analysis.viewable").value(false));
        // 일일 한도는 돌아오지 않는다 (countRequestedSince는 삭제 행도 센다)
        assertThat(
                        count(
                                "SELECT COUNT(*) FROM analyses WHERE user_id = ? AND status <>"
                                        + " 'FAILED'",
                                u))
                .isEqualTo(1);

        deleteAnalysis(u, id)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
    }

    @Test
    void AC7_진행_중이면_409이고_아무것도_지우지_않으며_FAILED는_204() throws Exception {
        long u = createUser("f19ac7", "JUNIOR");
        for (String stage :
                new String[] {
                    "FETCHING", "FILTERING", "SELECTING", "EXPLAINING", "EXTRACTING", "QUIZ"
                }) {
            long id = insertAnalysis(u, stage, null, false, "running");
            jdbc.update(
                    "INSERT INTO analysis_files (analysis_id, path, layer, content) VALUES (?,"
                            + " 'A.java', 'controller', 'class A {}')",
                    id);

            deleteAnalysis(u, id)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_005"))
                    .andExpect(jsonPath("$.message").value(ErrorCode.ANALYSIS_005.message()));
            assertThat(analysisRow(id).get("deleted_at")).as(stage).isNull();
            assertThat(count("SELECT COUNT(*) FROM analysis_files WHERE analysis_id = ?", id))
                    .isEqualTo(1);
        }

        long failed = insertAnalysis(u, "FAILED", null, false, "failed");
        deleteAnalysis(u, failed).andExpect(status().isNoContent());
        assertThat(analysisRow(failed).get("deleted_at")).isNotNull();
    }

    @Test
    void AC8_삭제_뒤_늦게_온_흐름_결과는_저장되지_않고_예외도_없다() throws Exception {
        long u = createUser("f19ac8", "JUNIOR");
        long id = insertAnalysis(u, "DONE", null, false, "todo-app");
        for (String endpoint : new String[] {"GET /api/todos", "POST /api/todos"}) {
            jdbc.update(
                    "INSERT INTO analysis_flows (analysis_id, endpoint, status, created_at) VALUES"
                            + " (?, ?, 'PENDING', NOW())",
                    id,
                    endpoint);
        }
        List<Long> flows =
                jdbc.queryForList(
                        "SELECT id FROM analysis_flows WHERE analysis_id = ? ORDER BY id",
                        Long.class,
                        id);

        deleteAnalysis(u, id).andExpect(status().isNoContent());

        flowService.complete(
                flows.get(0),
                new FlowResult(
                        "요약",
                        List.of(new FlowStep(1, "controller", null, "A.java", null, "t", "d"))));
        flowService.markFailed(flows.get(1), ErrorCode.LLM_503);
        assertThat(count("SELECT COUNT(*) FROM analysis_flows WHERE analysis_id = ?", id)).isZero();
    }
}
