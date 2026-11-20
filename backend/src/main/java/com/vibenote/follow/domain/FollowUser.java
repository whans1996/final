package com.vibenote.follow.domain;

import com.vibenote.common.Level;
import java.time.LocalDateTime;

/** 팔로워·팔로잉 목록의 한 행 (users + follows.created_at). Controller 응답으로 직접 내보내지 않는다. */
public class FollowUser {

    private Long id;
    private String nickname;
    private Level level;
    private LocalDateTime followedAt;

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public Level getLevel() {
        return level;
    }

    public LocalDateTime getFollowedAt() {
        return followedAt;
    }
}
