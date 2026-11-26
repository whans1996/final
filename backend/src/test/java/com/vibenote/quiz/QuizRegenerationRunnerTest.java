package com.vibenote.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import com.vibenote.analysis.AnalysisService;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmResponse;
import com.vibenote.llm.LlmStage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * L-1a 퀴즈 재생성 실행 (docs/specs/L-1a-rerun-quiz-regenerate.md AC6·AC8). 실행기를 거치지 않고 {@link
 * QuizRegenerationRunner#run}을 직접 부른다. 스파이 구성은 {@link QuizRegenerateApiTest}와 같게 두어 컨텍스트를 함께 쓴다.
 */
class QuizRegenerationRunnerTest extends QuizTestSupport {

    @MockitoSpyBean LlmClient llmClient;

    @MockitoSpyBean QuizSaveService quizSaveService;

    @Autowired QuizRegenerationRunner runner;

    @Autowired AnalysisService analysisService;

    @Autowired PlatformTransactionManager transactionManager;

    @Autowired DataSource dataSource;

    /** 재생성을 시작한(GENERATING, 1회 사용) DONE 분석. */
    private long generatingAnalysis(long userId) {
        long id = insertAnalysis(userId, false);
        jdbc.update(
                "UPDATE analyses SET quiz_status = 'GENERATING', quiz_regenerations = 1 WHERE id ="
                        + " ?",
                id);
        jdbc.update(
                "INSERT INTO analysis_files (analysis_id, path, layer, content) VALUES (?,"
                        + " 'web/TodoController.java', 'controller', 'class TodoController {}')",
                id);
        return id;
    }

    private Map<String, Object> row(long id) {
        return jdbc.queryForMap(
                "SELECT status, quiz_status, quiz_regenerations, deleted_at FROM analyses WHERE id"
                        + " = ?",
                id);
    }

    private void assertFailedWithoutRefund(long id) {
        Map<String, Object> row = row(id);
        assertThat(row.get("quiz_status")).isEqualTo("FAILED");
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("quiz_regenerations")).isEqualTo(1);
        assertThat(count("quizzes", "analysis_id = ?", id)).isZero();
    }

    @Test
    void AC6_LLM_503으로_빈_결과면_FAILED이고_횟수는_돌려주지_않는다() {
        long id = generatingAnalysis(createUser("l1aac6l"));
        doThrow(new BusinessException(ErrorCode.LLM_503))
                .when(llmClient)
                .complete(argThat(r -> r != null && r.stage() == LlmStage.QUIZ));

        runner.run(id);

        assertFailedWithoutRefund(id);
    }

    @Test
    void AC6_형식_오류가_2회면_FAILED() {
        long id = generatingAnalysis(createUser("l1aac6f"));
        doReturn(new LlmResponse("{\"questions\": []}", "fake", 0, 0))
                .when(llmClient)
                .complete(argThat(r -> r != null && r.stage() == LlmStage.QUIZ));

        runner.run(id);

        assertFailedWithoutRefund(id);
    }

    @Test
    void AC6_저장_중_예기치_못한_예외면_FAILED() {
        long id = generatingAnalysis(createUser("l1aac6d"));
        doThrow(new DataIntegrityViolationException("boom"))
                .when(quizSaveService)
                .saveRegenerated(anyLong(), anyList());

        runner.run(id);

        assertFailedWithoutRefund(id);
    }

    @Test
    void AC6_실행기가_가득_차_거절하면_FAILED() throws Exception {
        long id = generatingAnalysis(createUser("l1aac6r"));
        TaskExecutor full =
                task -> {
                    throw new TaskRejectedException("full");
                };
        QuizRegenerationLauncher launcher =
                new QuizRegenerationLauncher(runner, analysisService, full);
        AtomicReference<String> seenByOtherConnection = new AtomicReference<>();

        // 실제 경로처럼 요청 트랜잭션이 커밋된 뒤(AFTER_COMMIT) 부르고, 그 자리에서 다른 커넥션으로 읽는다.
        // 끝난 트랜잭션에 참여하면 FAILED가 아직 커밋되지 않아 GENERATING이 보인다 (PR 리뷰).
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status ->
                                TransactionSynchronizationManager.registerSynchronization(
                                        new TransactionSynchronization() {
                                            @Override
                                            public void afterCommit() {
                                                launcher.launch(
                                                        new QuizRegenerationRequestedEvent(id));
                                                seenByOtherConnection.set(
                                                        quizStatusOnNewConnection(id));
                                            }
                                        }));

        assertThat(seenByOtherConnection.get()).isEqualTo("FAILED");
        assertFailedWithoutRefund(id);
    }

    /** 스레드에 묶인 트랜잭션 자원을 거치지 않고 풀에서 새 커넥션을 받아 커밋된 값만 읽는다. */
    private String quizStatusOnNewConnection(long id) {
        try (Connection con = dataSource.getConnection();
                PreparedStatement ps =
                        con.prepareStatement("SELECT quiz_status FROM analyses WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void AC8_생성_중에_분석이_삭제되면_퀴즈를_넣지_않고_예외도_없다() {
        long id = generatingAnalysis(createUser("l1aac8d"));
        jdbc.update("UPDATE analyses SET deleted_at = NOW() WHERE id = ?", id);

        runner.run(id);

        assertThat(count("quizzes", "analysis_id = ?", id)).isZero();
        assertThat(row(id).get("quiz_status")).isEqualTo("GENERATING");
    }

    @Test
    void AC8_생성_중에_회원이_탈퇴하면_퀴즈를_넣지_않는다() {
        long userId = createUser("l1aac8w");
        long id = generatingAnalysis(userId);
        // 탈퇴는 회원 행을 WITHDRAWN으로 바꾸고 분석을 모두 soft delete한다 (#91)
        jdbc.update("UPDATE users SET status = 'WITHDRAWN' WHERE id = ?", userId);
        analysisService.deleteAllOf(userId);

        runner.run(id);

        assertThat(count("quizzes", "analysis_id = ?", id)).isZero();
        assertThat(row(id).get("deleted_at")).isNotNull();
    }
}
