package com.vibenote.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** NT-1 AC8. 지금은 2026-11-29 12:00이라 90일 전은 2026-08-31 12:00이다. */
@Transactional
class NotificationPurgeJobTest extends NotificationTestSupport {

    @Autowired NotificationPurgeJob job;

    @Test
    void AC8_90일보다_오래된_알림만_읽음과_상관없이_지운다() {
        long r = insertUser("ntr");
        long oldUnread = insertNotification(r, FIXED_NOW.minusDays(90).minusSeconds(1), null);
        long oldRead = insertNotification(r, FIXED_NOW.minusDays(120), FIXED_NOW.minusDays(119));
        long exact = insertNotification(r, FIXED_NOW.minusDays(90), null);
        long recent = insertNotification(r, FIXED_NOW.minusDays(1), FIXED_NOW);

        job.purge();

        assertThat(
                        jdbc.queryForList(
                                "SELECT id FROM notifications WHERE recipient_id = ? ORDER BY id",
                                Long.class,
                                r))
                .containsExactly(exact, recent)
                .doesNotContain(oldUnread, oldRead);
    }
}
