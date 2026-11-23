package com.vibenote.team.domain;

import com.vibenote.common.Level;
import com.vibenote.team.TeamRole;
import java.time.LocalDateTime;

/** 팀 상세의 멤버 한 행 (team_members + users 닉네임·수준). Controller 응답으로 직접 내보내지 않는다. */
public class TeamMember {

    private Long userId;
    private String nickname;
    private Level level;
    private TeamRole role;
    private LocalDateTime joinedAt;

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public Level getLevel() {
        return level;
    }

    public TeamRole getRole() {
        return role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}
