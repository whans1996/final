-- NT-1 헤더 알림 (docs/specs/NT-1-header-notifications.md 4절).
-- type은 VARCHAR라 A(NT-2)가 NotificationType 상수를 더할 때 마이그레이션이 필요 없다.
-- 받은 사람이 지워지면(실제로는 탈퇴 때 서비스가 직접 지운다) 함께 지우고, 일으킨 사람은 NULL로 둔다.
CREATE TABLE notifications (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT       NOT NULL,
    type         VARCHAR(40)  NOT NULL,
    actor_id     BIGINT       NULL,
    message      VARCHAR(120) NOT NULL,
    link         VARCHAR(255) NOT NULL,
    read_at      DATETIME     NULL,
    created_at   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notifications_recipient_created (recipient_id, created_at, id),
    KEY idx_notifications_recipient_unread (recipient_id, read_at),
    KEY idx_notifications_created (created_at),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL
);
