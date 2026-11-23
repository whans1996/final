package com.vibenote.team.domain;

import java.time.LocalDateTime;

/** teams 행 (F-20a). Controller 응답으로 직접 내보내지 않는다. */
public class Team {

    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String inviteCode;
    private LocalDateTime createdAt;

    public Team() {}

    public Team(
            String name,
            String description,
            long ownerId,
            String inviteCode,
            LocalDateTime createdAt) {
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.inviteCode = inviteCode;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
