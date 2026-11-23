package com.vibenote.team.domain;

import com.vibenote.team.TeamRole;
import java.time.LocalDateTime;

/** 내 팀 목록의 한 행 (teams + 내 team_members + 팀장 닉네임 + 인원). Controller 응답으로 직접 내보내지 않는다. */
public class MyTeam {

    private Long id;
    private String name;
    private String description;
    private TeamRole myRole;
    private int memberCount;
    private String ownerNickname;
    private LocalDateTime joinedAt;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public TeamRole getMyRole() {
        return myRole;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public String getOwnerNickname() {
        return ownerNickname;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}
