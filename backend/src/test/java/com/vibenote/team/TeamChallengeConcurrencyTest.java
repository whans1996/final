package com.vibenote.team;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.team.dto.CreateTeamChallengeRequest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * #168: 팀장이 목표를 만드는 사이 공유자가 공유를 해제(회원 탈퇴도 같은 행 삭제)하고 커밋하면, 공유 확인이 스냅샷을 봐 통과한 뒤 insert의 FK 검사에서
 * 1452로 500이 났다. 공유 확인이 FK 검사와 같은 최신 행을 보고 404 TEAM_006이 되어야 한다.
 */
class TeamChallengeConcurrencyTest extends IntegrationTestBase {

    @Autowired TeamChallengeService challengeService;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;

    @AfterEach
    void cleanUp() {
        // 팀·분석·공유·목표는 users FK CASCADE로 함께 지워진다
        jdbc.update("DELETE FROM users WHERE email LIKE '%@f20cx.test'");
    }

    private long user(String name) {
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                name + "@f20cx.test",
                name);
        return jdbc.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, name + "@f20cx.test");
    }

    private long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    void 목표를_만드는_사이_공유가_해제되고_커밋되면_500이_아니라_TEAM_006이다() throws Exception {
        long owner = user("f20cxowner");
        long sharer = user("f20cxsharer");
        jdbc.update(
                "INSERT INTO teams (name, owner_id, invite_code) VALUES ('경합 팀', ?, 'RACE0001')",
                owner);
        long team =
                jdbc.queryForObject(
                        "SELECT id FROM teams WHERE invite_code = 'RACE0001'", Long.class);
        jdbc.update(
                "INSERT INTO team_members (team_id, user_id, role) VALUES (?, ?, 'OWNER'), (?, ?,"
                        + " 'MEMBER')",
                team,
                owner,
                team,
                sharer);
        jdbc.update(
                "INSERT INTO analyses (user_id, source_type, project_name, title, level, status,"
                        + " progress) VALUES (?, 'ZIP', 'race', '경합 분석', 'JUNIOR', 'DONE', 100)",
                sharer);
        long analysis = lastId();
        jdbc.update(
                "INSERT INTO team_analyses (team_id, analysis_id) VALUES (?, ?)", team, analysis);
        LocalDate today = LocalDate.now(clock);
        CreateTeamChallengeRequest request =
                new CreateTeamChallengeRequest(
                        analysis, today.toString(), today.plusDays(6).toString(), 80);

        try (Connection unsharer =
                        DriverManager.getConnection(
                                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
                Connection monitor =
                        DriverManager.getConnection(
                                MYSQL.getJdbcUrl(), "root", MYSQL.getPassword())) {
            // 공유 해제를 커밋하지 않은 채 행 잠금만 쥔다
            unsharer.setAutoCommit(false);
            try (PreparedStatement delete =
                    unsharer.prepareStatement(
                            "DELETE FROM team_analyses WHERE team_id = ? AND analysis_id = ?")) {
                delete.setLong(1, team);
                delete.setLong(2, analysis);
                assertThat(delete.executeUpdate()).isEqualTo(1);
            }

            CompletableFuture<Object> creating =
                    CompletableFuture.supplyAsync(
                            () -> {
                                try {
                                    return challengeService.create(team, owner, request);
                                } catch (RuntimeException e) {
                                    return e;
                                }
                            });
            awaitLockWait(monitor, creating);
            unsharer.commit();

            Object outcome = creating.get(30, TimeUnit.SECONDS);
            assertThat(outcome).isInstanceOf(BusinessException.class);
            assertThat(((BusinessException) outcome).getErrorCode()).isEqualTo(ErrorCode.TEAM_006);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM team_challenges WHERE team_id = ?",
                                Integer.class,
                                team))
                .isZero();
    }

    /**
     * 목표 만들기가 공유 행에 닿는 문장(공유 확인 또는 목표 insert의 FK 검사)에서 공유 해제의 행 잠금을 기다릴 때까지. 다른 세션의 문장은 root만 볼 수
     * 있고, 지나가는 문장과 구분하려고 같은 문장이 잠시 뒤에도 남아 있는지 본다.
     */
    private static void awaitLockWait(Connection monitor, CompletableFuture<Object> creating)
            throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        try (PreparedStatement waiting =
                monitor.prepareStatement(
                        "SELECT COUNT(*) FROM information_schema.processlist WHERE user = ? AND"
                                + " command = 'Query' AND (info LIKE '%FROM team_analyses%' OR"
                                + " info LIKE 'INSERT INTO team_challenges%')")) {
            waiting.setString(1, MYSQL.getUsername());
            int seen = 0;
            while (System.nanoTime() < deadline) {
                if (creating.isDone()) {
                    throw new IllegalStateException("잠금 대기 없이 끝났다: " + creating.get());
                }
                try (ResultSet rs = waiting.executeQuery()) {
                    rs.next();
                    seen = rs.getInt(1) > 0 ? seen + 1 : 0;
                }
                if (seen >= 5) return;
                Thread.sleep(50);
            }
        }
        throw new IllegalStateException("목표 만들기가 잠금 대기에 들어가지 않았다");
    }
}
