package com.vibenote.feed;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** 활동 기록. {@code recordOnce}는 같은 (회원, 종류, 대상) 활동이 있으면 넣지 않는다 (F-12 AC5). */
@Transactional
class ActivityServiceTest extends IntegrationTestBase {

    @Autowired ActivityService activityService;
    @Autowired JdbcTemplate jdbc;

    private long insertUser(String name) {
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                name + "@activity.test",
                name);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private int count(long userId, String type, long targetId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM activities WHERE user_id = ? AND type = ? AND target_id = ?",
                Integer.class,
                userId,
                type,
                targetId);
    }

    @Test
    void recordOnce_같은_활동이_있으면_넣지_않는다() {
        long u = insertUser("actonce1");
        long other = insertUser("actonce2");

        activityService.recordOnce(u, ActivityType.WISH, 10L);
        activityService.recordOnce(u, ActivityType.WISH, 10L);
        activityService.recordOnce(u, ActivityType.WISH, 11L);
        activityService.recordOnce(other, ActivityType.WISH, 10L);
        activityService.recordOnce(u, ActivityType.REVIEW, 10L);

        assertThat(count(u, "WISH", 10L)).isEqualTo(1);
        assertThat(count(u, "WISH", 11L)).isEqualTo(1);
        assertThat(count(other, "WISH", 10L)).isEqualTo(1);
        assertThat(count(u, "REVIEW", 10L)).isEqualTo(1);
    }
}
