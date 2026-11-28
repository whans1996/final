package com.vibenote.notification;

import com.vibenote.notification.domain.NotificationRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/notification/NotificationMapper.xml */
@Mapper
public interface NotificationMapper {

    /** 받는 사람이 활성 회원일 때만 넣는다 (한 문장, 넣은 행 수 0 또는 1). */
    int insertForActive(
            @Param("recipientId") long recipientId,
            @Param("type") NotificationType type,
            @Param("actorId") Long actorId,
            @Param("message") String message,
            @Param("link") String link,
            @Param("createdAt") LocalDateTime createdAt);

    boolean existsUnread(
            @Param("recipientId") long recipientId,
            @Param("type") NotificationType type,
            @Param("actorId") long actorId);

    long countByRecipient(long recipientId);

    List<NotificationRow> findPage(
            @Param("recipientId") long recipientId,
            @Param("size") int size,
            @Param("offset") long offset);

    long countUnread(long recipientId);

    /** 안 읽은 내 알림이면 읽음으로. 바꾼 행 수. */
    int markRead(
            @Param("id") long id,
            @Param("recipientId") long recipientId,
            @Param("readAt") LocalDateTime readAt);

    boolean existsOwned(@Param("id") long id, @Param("recipientId") long recipientId);

    int markAllRead(@Param("recipientId") long recipientId, @Param("readAt") LocalDateTime readAt);

    int deleteByRecipient(long recipientId);

    int deleteCreatedBefore(LocalDateTime cutoff);
}
