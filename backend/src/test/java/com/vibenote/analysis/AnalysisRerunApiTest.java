package com.vibenote.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

/**
 * L-1a 실패한 URL 분석 재실행 (docs/specs/L-1a-rerun-quiz-regenerate.md AC1~AC3). 파이프라인은 mock이라 새 분석은
 * FETCHING에 머문다.
 */
class AnalysisRerunApiTest extends AnalysisTestSupport {

    private static final String URL = "https://github.com/octo/todo-app";

    @MockitoBean AnalysisPipeline pipeline;

    private ResultActions rerun(long userId, long id) throws Exception {
        return mockMvc.perform(
                post("/api/analyses/{id}/rerun", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }

    /** 실패한 URL 분석 (JUNIOR, 제목·메모 있음, 공개). 파일·흐름·개념 연결을 하나씩 둔다. */
    private long failedUrlAnalysis(long userId) {
        long id = insertUrlAnalysis(userId, URL);
        jdbc.update(
                "UPDATE analyses SET status = 'FAILED', progress = 40, level = 'JUNIOR', title ="
                        + " '내 제목', memo = '메모', error_code = 'LLM_503', error_message = 'x',"
                        + " created_at = ? WHERE id = ?",
                LocalDateTime.now(KST).withNano(0).minusMinutes(5),
                id);
        jdbc.update(
                "INSERT INTO analysis_files (analysis_id, path, layer, content)"
                        + " VALUES (?, 'A.java', 'controller', 'class A {}')",
                id);
        jdbc.update(
                "INSERT INTO analysis_flows (analysis_id, endpoint, status, created_at)"
                        + " VALUES (?, 'GET /a', 'FAILED', NOW())",
                id);
        return id;
    }

    private long maxId(long userId) {
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM analyses WHERE user_id = ?", Long.class, userId);
    }

    private int count(String table, long analysisId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE analysis_id = ?",
                Integer.class,
                analysisId);
    }

    @Test
    void AC1_실패한_URL_분석을_재실행하면_새_분석이_202로_시작된다() throws Exception {
        long userId = createUser("l1aac1", "SENIOR");
        long id = failedUrlAnalysis(userId);
        jdbc.update("UPDATE analyses SET is_public = TRUE WHERE id = ?", id);

        String body =
                rerun(userId, id)
                        .andExpect(status().isAccepted())
                        .andExpect(jsonPath("$.status").value("FETCHING"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long newId = objectMapper.readTree(body).get("analysisId").asLong();

        assertThat(newId).isNotEqualTo(id).isEqualTo(maxId(userId));
        Map<String, Object> row = analysisRow(newId);
        assertThat(row.get("user_id")).isEqualTo(userId);
        assertThat(row.get("source_type")).isEqualTo("URL");
        assertThat(row.get("source_url")).isEqualTo(URL);
        assertThat(row.get("project_name")).isEqualTo("repo");
        assertThat(row.get("level")).isEqualTo("JUNIOR");
        assertThat(row.get("title")).isEqualTo("내 제목");
        assertThat(row.get("memo")).isEqualTo("메모");
        assertThat(row.get("status")).isEqualTo("FETCHING");
        assertThat(row.get("progress")).isEqualTo(0);
        assertThat(row.get("is_public")).isEqualTo(false);
        assertThat(row.get("error_code")).isNull();
        assertThat(row.get("deleted_at")).isNull();
        assertThat(row.get("quiz_status")).isNull();
        assertThat(row.get("quiz_regenerations")).isEqualTo(0);
        verify(pipeline, timeout(5000)).run(newId);

        Map<String, Object> old = analysisRow(id);
        assertThat(old.get("deleted_at")).isNotNull();
        assertThat(old.get("status")).isEqualTo("FAILED");
        assertThat(count("analysis_files", id)).isZero();
        assertThat(count("analysis_flows", id)).isZero();
        assertThat(count("analysis_concepts", id)).isZero();
    }

    @Test
    void AC2_오늘_한도를_다_썼으면_429이고_원본은_그대로다() throws Exception {
        long userId = createUser("l1aac2", "JUNIOR");
        long id = failedUrlAnalysis(userId);
        for (int i = 0; i < 5; i++) {
            insertAnalysis(userId, "DONE", null, i == 0, "done" + i);
        }
        long before = maxId(userId);

        rerun(userId, id)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(ErrorCode.ANALYSIS_429.name()));

        assertThat(maxId(userId)).isEqualTo(before);
        Map<String, Object> old = analysisRow(id);
        assertThat(old.get("status")).isEqualTo("FAILED");
        assertThat(old.get("deleted_at")).isNull();
        assertThat(count("analysis_files", id)).isEqualTo(1);
        verify(pipeline, never()).run(anyLong());
    }

    @Test
    void AC2_재실행한_분석은_오늘_요청_1회로_센다() throws Exception {
        long userId = createUser("l1aac2c", "JUNIOR");
        for (int i = 0; i < 4; i++) {
            insertAnalysis(userId, "DONE", null, false, "done" + i);
        }
        long first = failedUrlAnalysis(userId);
        long second = failedUrlAnalysis(userId);

        rerun(userId, first).andExpect(status().isAccepted());
        rerun(userId, second)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(ErrorCode.ANALYSIS_429.name()));
    }

    @Test
    void AC3_ZIP이거나_FAILED가_아닌_분석은_409_ANALYSIS_008() throws Exception {
        long userId = createUser("l1aac3", "JUNIOR");
        long zip = insertAnalysis(userId, "FAILED", null, false, "zip");
        long done = failedUrlAnalysis(userId);
        jdbc.update("UPDATE analyses SET status = 'DONE' WHERE id = ?", done);
        long running = failedUrlAnalysis(userId);
        jdbc.update("UPDATE analyses SET status = 'EXPLAINING' WHERE id = ?", running);
        long before = maxId(userId);

        for (long id : List.of(zip, done, running)) {
            rerun(userId, id)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_008"))
                    .andExpect(
                            jsonPath("$.message")
                                    .value(
                                            "실패한 저장소 주소 분석만 다시 실행할 수 있습니다. ZIP은 파일을 다시"
                                                    + " 올려 주세요."));
            assertThat(analysisRow(id).get("deleted_at")).as("id %d", id).isNull();
        }
        assertThat(maxId(userId)).isEqualTo(before);
    }

    @Test
    void AC3_없거나_남의_것이거나_삭제된_분석은_404() throws Exception {
        long owner = createUser("l1aac3o", "JUNIOR");
        long other = createUser("l1aac3x", "JUNIOR");
        long mine = failedUrlAnalysis(owner);
        jdbc.update("UPDATE analyses SET is_public = TRUE WHERE id = ?", mine);
        long deleted = failedUrlAnalysis(owner);
        jdbc.update("UPDATE analyses SET deleted_at = NOW() WHERE id = ?", deleted);

        rerun(other, mine)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ANALYSIS_404.name()));
        rerun(owner, deleted).andExpect(status().isNotFound());
        rerun(owner, 999_999_999L).andExpect(status().isNotFound());

        assertThat(analysisRow(mine).get("deleted_at")).isNull();
        assertThat(maxId(owner)).isEqualTo(deleted);
    }

    @Test
    void AC3_동시에_재실행하면_하나만_202이고_새_행은_1개다() throws Exception {
        long userId = createUser("l1aac3c", "JUNIOR");
        long id = failedUrlAnalysis(userId);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                results.add(
                        threads.submit(
                                () -> {
                                    start.await();
                                    return rerun(userId, id).andReturn().getResponse().getStatus();
                                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : results) {
                statuses.add(f.get(20, TimeUnit.SECONDS));
            }
            assertThat(statuses).containsExactlyInAnyOrder(202, 404);
        } finally {
            threads.shutdownNow();
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM analyses WHERE user_id = ? AND id <> ?",
                                Integer.class,
                                userId,
                                id))
                .isEqualTo(1);
    }
}
