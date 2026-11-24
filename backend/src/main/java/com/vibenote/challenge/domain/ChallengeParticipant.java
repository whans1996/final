package com.vibenote.challenge.domain;

import java.time.LocalDateTime;

/** challenge_participants 행 (F-25). 달성하면 completedAt이 채워지고 그 뒤로 progress를 다시 세지 않는다. */
public class ChallengeParticipant {

    private Long challengeId;
    private Long userId;
    private int progress;
    private LocalDateTime completedAt;
    private LocalDateTime joinedAt;

    public boolean isCompleted() {
        return completedAt != null;
    }

    public Long getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(Long challengeId) {
        this.challengeId = challengeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
