package com.vibenote.challenge.domain;

import com.vibenote.challenge.ChallengeGoalType;
import com.vibenote.challenge.ChallengeStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * challenges 행. Controller 응답으로 직접 내보내지 않는다. participantCount는 목록·상세 조회만, joined는 회원용 목록(F-25)만
 * 채운다.
 */
public class Challenge {

    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private ChallengeGoalType goalType;
    private int goalValue;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long participantCount;
    private boolean joined;

    public ChallengeStatus statusOn(LocalDate today) {
        return ChallengeStatus.of(startDate, endDate, today);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public ChallengeGoalType getGoalType() {
        return goalType;
    }

    public void setGoalType(ChallengeGoalType goalType) {
        this.goalType = goalType;
    }

    public int getGoalValue() {
        return goalValue;
    }

    public void setGoalValue(int goalValue) {
        this.goalValue = goalValue;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(long participantCount) {
        this.participantCount = participantCount;
    }

    public boolean isJoined() {
        return joined;
    }

    public void setJoined(boolean joined) {
        this.joined = joined;
    }
}
