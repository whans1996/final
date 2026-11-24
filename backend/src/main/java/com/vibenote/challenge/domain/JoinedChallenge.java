package com.vibenote.challenge.domain;

import java.time.LocalDateTime;

/** 회원이 참여한 챌린지 한 행 (F-25 AC10·AC11): 챌린지 컬럼 + 그 회원의 달성도. */
public class JoinedChallenge extends Challenge {

    private int progress;
    private LocalDateTime completedAt;
    private LocalDateTime joinedAt;

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
