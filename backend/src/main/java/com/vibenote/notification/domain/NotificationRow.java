package com.vibenote.notification.domain;

import com.vibenote.notification.NotificationType;
import java.time.LocalDateTime;

/** 알림 목록 쿼리의 한 행 (notifications + 일으킨 회원의 지금 닉네임). Controller 응답으로 직접 내보내지 않는다. */
public class NotificationRow {

    private Long id;
    private NotificationType type;
    private String message;
    private String link;

    /** 일으킨 회원이 없는 알림(챌린지 달성 등)은 null. */
    private Long actorId;

    /** 탈퇴한 회원이면 마스킹 닉네임({@code #탈퇴{id}}) 그대로다. */
    private String actorNickname;

    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorNickname() {
        return actorNickname;
    }

    public void setActorNickname(String actorNickname) {
        this.actorNickname = actorNickname;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
