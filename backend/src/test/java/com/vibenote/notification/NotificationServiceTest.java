package com.vibenote.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/** NT-1 알림 생성 공통 서비스 (AC6). */
@Transactional
class NotificationServiceTest extends NotificationTestSupport {

    @Test
    void AC6_활성_회원에게_안_읽은_알림_하나를_넣는다() {
        long r = insertUser("ntr");
        long actor = insertUser("nta");

        notificationService.notify(
                r, NotificationType.FOLLOW, actor, "회원님을 팔로우했어요", "/users/" + actor);

        Map<String, Object> row =
                jdbc.queryForMap("SELECT * FROM notifications WHERE recipient_id = ?", r);
        assertThat(row.get("type")).isEqualTo("FOLLOW");
        assertThat(((Number) row.get("actor_id")).longValue()).isEqualTo(actor);
        assertThat(row.get("message")).isEqualTo("회원님을 팔로우했어요");
        assertThat(row.get("link")).isEqualTo("/users/" + actor);
        assertThat(row.get("read_at")).isNull();
        assertThat(row.get("created_at")).isEqualTo(FIXED_NOW);
    }

    @Test
    void AC6_actor가_없어도_넣는다() {
        long r = insertUser("ntr");

        notificationService.notify(
                r, NotificationType.CHALLENGE_COMPLETE, null, "챌린지를 달성했어요 · C", "/challenges/1");

        assertThat(countFor(r)).isEqualTo(1);
    }

    @Test
    void AC6_내가_한_일은_나에게_알리지_않는다() {
        long r = insertUser("ntr");

        notificationService.notify(r, NotificationType.COMMENT, r, "m", "/community/1");

        assertThat(countFor(r)).isZero();
    }

    @Test
    void AC6_탈퇴했거나_없는_회원에게는_예외_없이_넣지_않는다() {
        long r = insertUser("ntr");
        withdrawRow(r);

        notificationService.notify(r, NotificationType.COMMENT, null, "m", "/community/1");
        notificationService.notify(
                r + 100_000, NotificationType.COMMENT, null, "m", "/community/1");

        assertThat(countFor(r)).isZero();
        assertThat(countFor(r + 100_000)).isZero();
    }

    @Test
    void AC6_120자를_넘는_문장은_117자와_말줄임표로_자른다() {
        long r = insertUser("ntr");
        String exact = "가".repeat(120);
        String longer = "나".repeat(121);

        notificationService.notify(r, NotificationType.COMMENT, null, exact, "/community/1");
        notificationService.notify(r, NotificationType.COMMENT, null, longer, "/community/2");

        assertThat(messageOf(r, "/community/1")).isEqualTo(exact);
        assertThat(messageOf(r, "/community/2")).isEqualTo("나".repeat(117) + "…");
    }

    @Test
    void AC6_앱_밖_링크는_IllegalArgumentException() {
        long r = insertUser("ntr");

        for (String bad :
                new String[] {"https://evil.test", "//evil.test", "community/1", "", null}) {
            assertThatThrownBy(
                            () ->
                                    notificationService.notify(
                                            r, NotificationType.COMMENT, null, "m", bad))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(countFor(r)).isZero();
    }

    @Test
    void AC6_clip은_max자를_넘으면_max_빼기_1자와_말줄임표() {
        assertThat(NotificationService.clip("a".repeat(40), 40)).isEqualTo("a".repeat(40));
        assertThat(NotificationService.clip("a".repeat(41), 40)).isEqualTo("a".repeat(39) + "…");
        assertThat(NotificationService.clip(null, 40)).isEmpty();
    }

    private String messageOf(long recipientId, String link) {
        return jdbc.queryForObject(
                "SELECT message FROM notifications WHERE recipient_id = ? AND link = ?",
                String.class,
                recipientId,
                link);
    }
}
