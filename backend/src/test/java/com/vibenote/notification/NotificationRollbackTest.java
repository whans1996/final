package com.vibenote.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * NT-1 AC7. 클래스에 {@code @Transactional}을 붙이지 않는다 (붙이면 호출이 테스트 트랜잭션에 합류해 롤백을 확인할 수 없다). 넣은 회원을 지워
 * 정리한다 (알림은 CASCADE).
 */
class NotificationRollbackTest extends NotificationTestSupport {

    @Autowired PlatformTransactionManager txManager;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM users WHERE email LIKE '%@nt1.test'");
    }

    @Test
    void AC7_호출한_쪽_트랜잭션이_롤백되면_알림도_남지_않는다() {
        long r = insertUser("ntr");
        TransactionTemplate tx = new TransactionTemplate(txManager);

        assertThatThrownBy(
                        () ->
                                tx.executeWithoutResult(
                                        status -> {
                                            notificationService.notify(
                                                    r,
                                                    NotificationType.COMMENT,
                                                    null,
                                                    "m",
                                                    "/community/1");
                                            throw new IllegalStateException("댓글 저장 뒤 실패");
                                        }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(countFor(r)).isZero();

        tx.executeWithoutResult(
                status ->
                        notificationService.notify(
                                r, NotificationType.COMMENT, null, "m", "/community/1"));
        assertThat(countFor(r)).isEqualTo(1);
    }
}
