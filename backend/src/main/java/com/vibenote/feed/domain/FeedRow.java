package com.vibenote.feed.domain;

import com.vibenote.feed.ActivityType;
import java.time.LocalDateTime;

/** 피드 쿼리의 한 행 (activities + 활동한 회원 + 대상). Controller 응답으로 직접 내보내지 않는다. */
public class FeedRow {

    private Long id;
    private ActivityType type;
    private LocalDateTime createdAt;
    private Long actorId;
    private String actorNickname;

    /** 링크할 대상 id. REVIEW는 리뷰 id가 아니라 카드 id다. */
    private Long targetId;

    /** ANALYSIS는 분석 제목, 없으면 프로젝트명, 둘 다 없으면 null. */
    private String targetTitle;

    /** REVIEW만 값이 있다. */
    private Integer targetRating;

    public Long getId() {
        return id;
    }

    public ActivityType getType() {
        return type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getActorNickname() {
        return actorNickname;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public Integer getTargetRating() {
        return targetRating;
    }
}
