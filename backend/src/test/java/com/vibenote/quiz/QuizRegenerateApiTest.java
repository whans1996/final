package com.vibenote.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.exception.ErrorCode;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmStage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;

/**
 * L-1a 퀴즈 재생성 요청 (docs/specs/L-1a-rerun-quiz-regenerate.md AC5·AC7). 생성은 분석 실행기에서 Mock LLM
 * fixture(quiz.json)로 돈다.
 */
class QuizRegenerateApiTest extends QuizTestSupport {

    @MockitoSpyBean LlmClient llmClient;

    /** {@link QuizRegenerationRunnerTest}와 컨텍스트를 함께 쓰려고 같은 스파이를 둔다. */
    @MockitoSpyBean QuizSaveService quizSaveService;

    private ResultActions regenerate(long userId, long analysisId) throws Exception {
        return mockMvc.perform(
                post("/api/analyses/{id}/quiz", analysisId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }

    /** 퀴즈 생성에 실패한 DONE 분석 (파일 1개). */
    private long failedQuizAnalysis(long userId, int used) {
        long id = insertAnalysis(userId, false);
        jdbc.update(
                "UPDATE analyses SET quiz_status = 'FAILED', quiz_regenerations = ? WHERE id = ?",
                used,
                id);
        jdbc.update(
                "INSERT INTO analysis_files (analysis_id, path, layer, content) VALUES (?,"
                        + " 'web/TodoController.java', 'controller', 'class TodoController {}')",
                id);
        return id;
    }

    private Map<String, Object> row(long id) {
        return jdbc.queryForMap(
                "SELECT status, quiz_status, quiz_regenerations FROM analyses WHERE id = ?", id);
    }

    /** 생성이 끝날 때(GENERATING이 아닐 때)까지 기다린다. */
    private String awaitQuizStatus(long id) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            String status =
                    jdbc.queryForObject(
                            "SELECT quiz_status FROM analyses WHERE id = ?", String.class, id);
            if (!"GENERATING".equals(status)) {
                return status;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("퀴즈 재생성이 끝나지 않음: " + id);
    }

    private List<LlmRequest> quizRequests(long analysisId) {
        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient, atLeast(0)).complete(captor.capture());
        return captor.getAllValues().stream()
                .filter(r -> r.stage() == LlmStage.QUIZ && r.analysisId() == analysisId)
                .toList();
    }

    @Test
    void AC5_실패한_퀴즈를_다시_만들면_202이고_생성_뒤_퀴즈를_조회할_수_있다() throws Exception {
        long userId = createUser("l1aac5");
        long id = failedQuizAnalysis(userId, 1);

        regenerate(userId, id)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.analysisId").value(id))
                .andExpect(jsonPath("$.quizStatus").value("GENERATING"))
                .andExpect(jsonPath("$.quizRegenerationsLeft").value(1));

        assertThat(awaitQuizStatus(id)).isEqualTo("READY");
        Map<String, Object> row = row(id);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("quiz_regenerations")).isEqualTo(2);
        assertThat(count("quizzes", "analysis_id = ?", id)).isEqualTo(1);
        mockMvc.perform(
                        get("/api/analyses/{id}/quiz", id)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").value(id))
                .andExpect(jsonPath("$.questions.length()").value(6));
        List<LlmRequest> requests = quizRequests(id);
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).prompt()).contains("TodoController");
        assertThat(
                        jdbc.queryForList(
                                "SELECT user_id FROM llm_usage_logs WHERE analysis_id = ? AND stage"
                                        + " = 'quiz'",
                                Long.class,
                                id))
                .containsExactly(userId);
    }

    @Test
    void AC7_DONE이_아니거나_퀴즈가_있거나_만드는_중이거나_QUIZ_전이면_409_QUIZ_409() throws Exception {
        long userId = createUser("l1aac7");
        long failedAnalysis = failedQuizAnalysis(userId, 0);
        jdbc.update("UPDATE analyses SET status = 'FAILED' WHERE id = ?", failedAnalysis);
        long ready = failedQuizAnalysis(userId, 0);
        jdbc.update("UPDATE analyses SET quiz_status = 'READY' WHERE id = ?", ready);
        long generating = failedQuizAnalysis(userId, 3);
        jdbc.update("UPDATE analyses SET quiz_status = 'GENERATING' WHERE id = ?", generating);
        long beforeQuiz = failedQuizAnalysis(userId, 0);
        jdbc.update("UPDATE analyses SET quiz_status = NULL WHERE id = ?", beforeQuiz);

        for (long id : List.of(failedAnalysis, ready, generating, beforeQuiz)) {
            regenerate(userId, id)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("QUIZ_409"))
                    .andExpect(jsonPath("$.message").value("퀴즈를 다시 만들 수 없는 상태입니다. 화면을 새로 고쳐 주세요."));
        }
        assertThat(row(ready).get("quiz_regenerations")).isEqualTo(0);
        assertThat(row(generating).get("quiz_regenerations")).isEqualTo(3);
    }

    @Test
    void AC7_재생성_3회를_다_썼으면_429_QUIZ_429() throws Exception {
        long userId = createUser("l1aac7l");
        long id = failedQuizAnalysis(userId, 3);

        regenerate(userId, id)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("QUIZ_429"))
                .andExpect(jsonPath("$.message").value("이 분석의 퀴즈 다시 만들기 횟수(3회)를 모두 썼습니다."));

        Map<String, Object> row = row(id);
        assertThat(row.get("quiz_status")).isEqualTo("FAILED");
        assertThat(row.get("quiz_regenerations")).isEqualTo(3);
        assertThat(quizRequests(id)).isEmpty();
    }

    @Test
    void AC7_없거나_삭제됐거나_남의_분석이면_팀_공유여도_404() throws Exception {
        long owner = createUser("l1aac7o");
        long member = createUser("l1aac7m");
        long shared = failedQuizAnalysis(owner, 0);
        jdbc.update(
                "INSERT INTO teams (name, invite_code, owner_id) VALUES ('l1a팀', 'L1AAC7XX', ?)",
                owner);
        long teamId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update(
                "INSERT INTO team_members (team_id, user_id, role) VALUES (?, ?, 'OWNER'), (?, ?,"
                        + " 'MEMBER')",
                teamId,
                owner,
                teamId,
                member);
        jdbc.update(
                "INSERT INTO team_analyses (team_id, analysis_id) VALUES (?, ?)", teamId, shared);
        long deleted = failedQuizAnalysis(owner, 0);
        jdbc.update("UPDATE analyses SET deleted_at = NOW() WHERE id = ?", deleted);

        regenerate(member, shared)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ANALYSIS_404.name()));
        regenerate(owner, deleted).andExpect(status().isNotFound());
        regenerate(owner, 999_999_999L).andExpect(status().isNotFound());
        assertThat(row(shared).get("quiz_status")).isEqualTo("FAILED");
    }

    @Test
    void AC7_동시에_요청하면_하나만_202이고_LLM은_1회다() throws Exception {
        long userId = createUser("l1aac7c");
        long id = failedQuizAnalysis(userId, 0);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                results.add(
                        threads.submit(
                                () -> {
                                    start.await();
                                    return regenerate(userId, id)
                                            .andReturn()
                                            .getResponse()
                                            .getStatus();
                                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : results) {
                statuses.add(f.get(20, TimeUnit.SECONDS));
            }
            assertThat(statuses).containsExactlyInAnyOrder(202, 409);
        } finally {
            threads.shutdownNow();
        }
        assertThat(awaitQuizStatus(id)).isEqualTo("READY");
        assertThat(row(id).get("quiz_regenerations")).isEqualTo(1);
        assertThat(quizRequests(id)).hasSize(1);
    }
}
