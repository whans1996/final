package com.vibenote.analysis.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.fetch.RepositoryCommit;
import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.TestProjects;
import com.vibenote.user.UserService;
import com.vibenote.user.dto.SignupRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * P-1a AC2 (PR #258 Codex P1, #257): 같은 저장소를 두 수준으로 동시에 분석해도 같은 커밋은 버전 하나다. REPEATABLE READ에서
 * FETCHING 트랜잭션이 프로젝트를 잠그기 전에 일반 SELECT로 스냅샷을 잡으면, 잠금을 기다린 뒤에도 다른 분석이 커밋한 버전을 보지 못해 같은 SHA·번호를
 * INSERT하다 유니크 키에 걸린다. 버전 조회·번호 계산은 최신 커밋을 읽어야 한다. 문장 순서를 한 스레드에서 고정한다
 * (WithdrawalAnalysisConcurrencyTest와 같은 방식).
 */
class ProjectVersionConcurrencyTest extends IntegrationTestBase {

    private static final String SHA = "9f2c4e1a7b3d5f60812c9e4a1b2c3d4e5f607182";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 28, 17, 30);

    @Autowired UserService userService;
    @Autowired ProjectService projectService;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;

    private long me;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM projects WHERE user_id = ?", me);
        jdbc.update("DELETE FROM users WHERE id = ?", me);
    }

    private long project() {
        me =
                userService.signup(
                        new SignupRequest("race@p1a.test", "vibe1234", "p1arace", "BEGINNER"));
        return TestProjects.url(jdbc, me, "https://github.com/octo/race-app");
    }

    private TransactionTemplate outer() {
        return new TransactionTemplate(txManager);
    }

    private TransactionTemplate concurrent() {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx;
    }

    @Test
    void AC2_스냅샷을_잡은_뒤_다른_분석이_같은_커밋_버전을_커밋해도_그_버전에_붙는다() {
        long projectId = project();
        RepositoryCommit commit = new RepositoryCommit(SHA, "feat: JWT 로그인", NOW);

        Long[] ids = new Long[2];
        outer().executeWithoutResult(
                        s -> {
                            // attachVersion의 findForPipeline처럼 프로젝트를 잠그기 전에 일반 SELECT로 스냅샷을 만든다
                            jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM project_versions WHERE project_id = ?",
                                    Integer.class,
                                    projectId);
                            // 다른 수준의 분석이 먼저 같은 커밋 버전을 만들고 커밋한다
                            ids[0] =
                                    concurrent()
                                            .execute(
                                                    r ->
                                                            projectService.versionFor(
                                                                    projectId, commit, NOW));
                            ids[1] = projectService.versionFor(projectId, commit, NOW);
                        });

        assertThat(ids[1]).isEqualTo(ids[0]);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM project_versions WHERE project_id = ?",
                                Integer.class,
                                projectId))
                .isEqualTo(1);
    }

    @Test
    void AC3_스냅샷을_잡은_뒤_다른_분석이_버전을_커밋해도_다음_번호를_쓴다() {
        long projectId = project();

        Long[] ids = new Long[2];
        outer().executeWithoutResult(
                        s -> {
                            jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM project_versions WHERE project_id = ?",
                                    Integer.class,
                                    projectId);
                            // 커밋 조회에 실패한 두 분석(AC3)이 각각 커밋 정보 없는 새 버전을 만든다
                            ids[0] =
                                    concurrent()
                                            .execute(
                                                    r ->
                                                            projectService.versionFor(
                                                                    projectId, null, NOW));
                            ids[1] = projectService.versionFor(projectId, null, NOW);
                        });

        assertThat(
                        jdbc.queryForList(
                                "SELECT number FROM project_versions WHERE project_id = ? ORDER BY"
                                        + " number",
                                Integer.class,
                                projectId))
                .containsExactly(1, 2);
        assertThat(ids[1]).isNotEqualTo(ids[0]);
    }
}
