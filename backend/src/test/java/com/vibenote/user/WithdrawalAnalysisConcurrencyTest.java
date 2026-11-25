package com.vibenote.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.user.dto.SignupRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * F-03 AC6 (#91, PR #142 Codex): 탈퇴 트랜잭션이 스냅샷을 잡은 뒤 다른 요청이 커밋한 분석도 지운다. REPEATABLE READ에서 일반
 * SELECT는 첫 읽기 시점의 스냅샷을 보므로, 정리 대상 id는 최신 커밋을 읽는 잠금 읽기로 얻어야 한다. 문장 순서를 한 스레드에서 고정한다.
 */
class WithdrawalAnalysisConcurrencyTest extends IntegrationTestBase {

    private static final String PASSWORD = "vibe1234";

    @Autowired UserService userService;
    @Autowired UserWithdrawalService withdrawalService;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;

    private long me;

    @AfterEach
    void cleanUp() {
        jdbc.update(
                "DELETE FROM analysis_files WHERE analysis_id IN (SELECT id FROM analyses WHERE"
                        + " user_id = ?)",
                me);
        jdbc.update("DELETE FROM analyses WHERE user_id = ?", me);
        jdbc.update("DELETE FROM users WHERE id = ?", me);
    }

    @Test
    void AC6_탈퇴가_스냅샷을_잡은_뒤_커밋된_분석도_삭제된다() {
        me =
                userService.signup(
                        new SignupRequest("race@f03wd.test", PASSWORD, "f03race", "BEGINNER"));
        TransactionTemplate withdrawal = new TransactionTemplate(txManager);
        TransactionTemplate request = new TransactionTemplate(txManager);
        request.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        withdrawal.executeWithoutResult(
                s -> {
                    // 탈퇴의 첫 읽기(비밀번호 확인 전 회원 조회)처럼 스냅샷을 만든다
                    jdbc.queryForObject("SELECT status FROM users WHERE id = ?", String.class, me);
                    // 탈퇴가 회원 행을 잠그기 전에 분석 요청이 커밋된다
                    request.executeWithoutResult(
                            r -> {
                                jdbc.update(
                                        "INSERT INTO analyses (user_id, source_type, project_name,"
                                                + " title, level, status) VALUES (?, 'ZIP',"
                                                + " 'todo-app', 'todo-app', 'BEGINNER',"
                                                + " 'SELECTING')",
                                        me);
                                jdbc.update(
                                        "INSERT INTO analysis_files (analysis_id, path, layer,"
                                                + " content) VALUES (LAST_INSERT_ID(), 'A.java',"
                                                + " 'controller', 'class A {}')");
                            });
                    withdrawalService.withdraw(me, PASSWORD);
                });

        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM analyses WHERE user_id = ? AND deleted_at IS"
                                        + " NOT NULL AND status = 'FAILED'",
                                Integer.class,
                                me))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM analysis_files f JOIN analyses a ON a.id ="
                                        + " f.analysis_id WHERE a.user_id = ?",
                                Integer.class,
                                me))
                .isZero();
    }
}
