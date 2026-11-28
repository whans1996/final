package com.vibenote.notification;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtTokenProvider;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * NT-1 통합 테스트 공통 (docs/specs/NT-1-header-notifications.md). 지금은 2026-11-29 12:00(KST)이다. 회원은
 * {@code @nt1.test} 이메일로 넣는다. 하위 클래스가 같은 컨텍스트를 쓰도록 시계 고정을 여기에 둔다.
 */
abstract class NotificationTestSupport extends IntegrationTestBase {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 11, 29, 12, 0, 0);

    private static final AtomicInteger SEQ = new AtomicInteger();

    @TestBean Clock clock;

    static Clock clock() {
        return Clock.fixed(FIXED_NOW.atZone(KST).toInstant(), KST);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired NotificationService notificationService;

    long insertUser(String name) {
        String unique = name + SEQ.incrementAndGet() + "x" + (System.nanoTime() % 100_000);
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                unique + "@nt1.test",
                unique);
        return lastId();
    }

    String nickname(long userId) {
        return jdbc.queryForObject("SELECT nickname FROM users WHERE id = ?", String.class, userId);
    }

    void withdrawRow(long userId) {
        jdbc.update(
                "UPDATE users SET status = 'WITHDRAWN', withdrawn_at = NOW(),"
                        + " nickname = CONCAT('#탈퇴', id) WHERE id = ?",
                userId);
    }

    String bearer(long userId) {
        return "Bearer " + tokenProvider.createAccessToken(userId, "USER");
    }

    /** 알림 행을 직접 넣는다. readAt이 null이면 안 읽음. */
    long insertNotification(
            long recipientId,
            String type,
            Long actorId,
            String message,
            String link,
            LocalDateTime createdAt,
            LocalDateTime readAt) {
        jdbc.update(
                "INSERT INTO notifications (recipient_id, type, actor_id, message, link,"
                        + " created_at, read_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                recipientId,
                type,
                actorId,
                message,
                link,
                createdAt,
                readAt);
        return lastId();
    }

    long insertNotification(long recipientId, LocalDateTime createdAt, LocalDateTime readAt) {
        return insertNotification(
                recipientId, "COMMENT", null, "메시지", "/community/1", createdAt, readAt);
    }

    int countFor(long recipientId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE recipient_id = ?",
                Integer.class,
                recipientId);
    }

    LocalDateTime readAt(long id) {
        return jdbc.queryForObject(
                "SELECT read_at FROM notifications WHERE id = ?", LocalDateTime.class, id);
    }

    long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
