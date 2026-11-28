package com.vibenote.notification;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.notification.domain.NotificationRow;
import com.vibenote.notification.dto.NotificationResponse;
import com.vibenote.notification.dto.UnreadCountResponse;
import com.vibenote.user.UserService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 헤더 알림 (NT-1). 알림 생성({@link #notify})은 각 도메인이 이벤트가 생긴 트랜잭션 안에서 부른다 ({@code ActivityService}와 같은
 * 방식이라 호출한 쪽이 롤백되면 알림도 남지 않는다, AC7). 문장은 호출하는 쪽이 주어 없이 만든다.
 */
@Service
public class NotificationService {

    /** 저장 문장 상한 (notifications.message VARCHAR(120)). 넘으면 앞 117자 + "…" (AC6). */
    static final int MESSAGE_MAX = 120;

    static final int MESSAGE_KEEP = 117;

    /** 이보다 오래된 알림은 정리 작업이 지운다 (AC8). */
    static final int RETENTION_DAYS = 90;

    private static final String ELLIPSIS = "…";

    private final NotificationMapper notificationMapper;
    private final UserService userService;
    private final Clock clock;

    public NotificationService(
            NotificationMapper notificationMapper, UserService userService, Clock clock) {
        this.notificationMapper = notificationMapper;
        this.userService = userService;
        this.clock = clock;
    }

    // ── 알림 생성 (공통 계약, A NT-2도 쓴다) ──

    /**
     * 알림 하나를 넣는다. 받는 사람이 일으킨 사람이면, 또는 받는 사람이 탈퇴했거나 없으면 넣지 않는다 (예외 없음, AC6).
     *
     * @param actorId 일으킨 회원. 없으면 null (챌린지 달성 등)
     * @param message 주어 없는 문장. 제목은 {@link #clip}으로 줄여 넣는다
     * @param link FE 라우터 경로 ({@code /}로 시작, {@code //} 금지)
     * @throws IllegalArgumentException link가 앱 안 경로가 아닐 때 (서버 코드 실수)
     */
    @Transactional
    public void notify(
            long recipientId, NotificationType type, Long actorId, String message, String link) {
        Objects.requireNonNull(type, "type");
        requireAppPath(link);
        if (actorId != null && actorId == recipientId) {
            return;
        }
        notificationMapper.insertForActive(
                recipientId, type, actorId, clipMessage(message), link, now());
    }

    /**
     * 같은 (받는 사람, 종류, 일으킨 사람)의 안 읽은 알림이 없을 때만 넣는다. 언팔로우 뒤 다시 팔로우해도 알림이 쌓이지 않게 한다 (AC11). 확인 후 넣는
     * 방식이므로 호출자가 같은 쌍의 쓰기를 직렬화해야 한다 (팔로우는 follows 행이 새로 생길 때만 부른다).
     */
    @Transactional
    public void notifyUnlessUnread(
            long recipientId, NotificationType type, long actorId, String message, String link) {
        if (notificationMapper.existsUnread(recipientId, type, actorId)) {
            return;
        }
        notify(recipientId, type, actorId, message, link);
    }

    /** 제목처럼 문장에 끼워 넣는 값을 max자(코드 포인트)로 줄인다. 넘으면 앞 max-1자 + "…". null은 빈 문자열. */
    public static String clip(String text, int max) {
        if (text == null) {
            return "";
        }
        String trimmed = text.strip();
        if (trimmed.codePointCount(0, trimmed.length()) <= max) {
            return trimmed;
        }
        return trimmed.substring(0, trimmed.offsetByCodePoints(0, max - 1)) + ELLIPSIS;
    }

    // ── 조회·읽음 (AC1~AC5) ──

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(long userId, PageQuery page) {
        userService.getActiveMember(userId);
        long total = notificationMapper.countByRecipient(userId);
        List<NotificationRow> rows =
                total == 0
                        ? List.of()
                        : notificationMapper.findPage(userId, page.size(), page.offset());
        return PageResponse.of(rows.stream().map(NotificationResponse::from).toList(), page, total);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(long userId) {
        userService.getActiveMember(userId);
        return new UnreadCountResponse(notificationMapper.countUnread(userId));
    }

    /** 이미 읽은 알림이면 read_at을 바꾸지 않는다 (멱등, AC3). 남의 것과 없는 것은 구분하지 않는다 (AC5). */
    @Transactional
    public void markRead(long id, long userId) {
        userService.getActiveMember(userId);
        if (notificationMapper.markRead(id, userId, now()) == 0
                && !notificationMapper.existsOwned(id, userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_404);
        }
    }

    @Transactional
    public void markAllRead(long userId) {
        userService.getActiveMember(userId);
        notificationMapper.markAllRead(userId, now());
    }

    // ── 정리 (AC8·AC9) ──

    /** 탈퇴한 회원이 받은 알림을 모두 지운다. 그 회원이 일으킨 남의 알림은 남는다 (AC9). */
    @Transactional
    public void removeAllOf(long userId) {
        notificationMapper.deleteByRecipient(userId);
    }

    /** 만든 지 {@value #RETENTION_DAYS}일보다 오래된 알림을 지운다. 정확히 90일 전 시각의 행은 남는다 (AC8). */
    @Transactional
    public int purgeExpired() {
        return notificationMapper.deleteCreatedBefore(now().minusDays(RETENTION_DAYS));
    }

    private static String clipMessage(String message) {
        String text = message == null ? "" : message.strip();
        if (text.codePointCount(0, text.length()) <= MESSAGE_MAX) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, MESSAGE_KEEP)) + ELLIPSIS;
    }

    private static void requireAppPath(String link) {
        if (link == null || !link.startsWith("/") || link.startsWith("//")) {
            throw new IllegalArgumentException("알림 link는 /로 시작하는 앱 안 경로여야 합니다: " + link);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
