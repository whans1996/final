package com.vibenote.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 90일보다 오래된 알림을 하루 한 번 지운다 (NT-1 AC8). 읽음 여부와 상관없다. */
@Component
class NotificationPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationPurgeJob.class);

    private final NotificationService notificationService;

    NotificationPurgeJob(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 기본 매일 03:30(Asia/Seoul). {@code notification.purge-cron}이 {@code -}이면 돌지 않는다 (통합 테스트). */
    @Scheduled(cron = "${notification.purge-cron:0 30 3 * * *}", zone = "Asia/Seoul")
    void purge() {
        int deleted = notificationService.purgeExpired();
        if (deleted > 0) {
            log.info("오래된 알림 {}개를 지웠다", deleted);
        }
    }
}
