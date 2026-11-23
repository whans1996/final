package com.vibenote.team;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** F-20a AC6: 9명인 팀에 두 회원이 동시에 가입하면 teams 행 잠금으로 정확히 한 명만 성공하고 10명이 된다. */
class TeamJoinConcurrencyTest extends IntegrationTestBase {

    @Autowired TeamService teamService;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        jdbc.update(
                "DELETE FROM teams WHERE owner_id IN (SELECT id FROM users WHERE email LIKE"
                        + " '%@f20cc.test')");
        jdbc.update("DELETE FROM users WHERE email LIKE '%@f20cc.test'");
    }

    private long user(String name) {
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                name + "@f20cc.test",
                name);
        return jdbc.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, name + "@f20cc.test");
    }

    @Test
    void AC6_9명인_팀에_두_회원이_동시에_가입하면_한_명만_성공한다() throws Exception {
        long owner = user("f20ccowner");
        jdbc.update(
                "INSERT INTO teams (name, owner_id, invite_code) VALUES ('동시 팀', ?, 'CONC0001')",
                owner);
        long team =
                jdbc.queryForObject(
                        "SELECT id FROM teams WHERE invite_code = 'CONC0001'", Long.class);
        jdbc.update(
                "INSERT INTO team_members (team_id, user_id, role) VALUES (?, ?, 'OWNER')",
                team,
                owner);
        for (int i = 1; i <= 8; i++) {
            jdbc.update(
                    "INSERT INTO team_members (team_id, user_id, role) VALUES (?, ?, 'MEMBER')",
                    team,
                    user("f20ccm" + i));
        }
        long a = user("f20cca");
        long b = user("f20ccb");
        CountDownLatch start = new CountDownLatch(1);

        List<CompletableFuture<String>> results = new ArrayList<>();
        for (long joiner : new long[] {a, b}) {
            results.add(
                    CompletableFuture.supplyAsync(
                            () -> {
                                await(start);
                                try {
                                    teamService.join(joiner, "CONC0001");
                                    return "OK";
                                } catch (BusinessException e) {
                                    return e.getErrorCode().name();
                                }
                            }));
        }
        start.countDown();

        List<String> outcomes = new ArrayList<>();
        for (CompletableFuture<String> result : results) {
            outcomes.add(result.get(30, TimeUnit.SECONDS));
        }
        assertThat(outcomes).containsExactlyInAnyOrder("OK", ErrorCode.TEAM_002.name());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM team_members WHERE team_id = ?",
                                Integer.class,
                                team))
                .isEqualTo(10);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("래치 대기 시간 초과");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
