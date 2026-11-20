package com.vibenote.reviewplan.domain;

import com.vibenote.reviewplan.ReviewPlanReason;
import com.vibenote.reviewplan.ReviewPlanStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** review_plans 행 (V1). {@code doneAt}은 DONE일 때만 값이 있다. */
public class ReviewPlan {

    private Long id;
    private long userId;
    private long conceptId;
    private LocalDate planDate;
    private ReviewPlanStatus status;
    private ReviewPlanReason reason;
    private LocalDateTime doneAt;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getConceptId() {
        return conceptId;
    }

    public void setConceptId(long conceptId) {
        this.conceptId = conceptId;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public ReviewPlanStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewPlanStatus status) {
        this.status = status;
    }

    public ReviewPlanReason getReason() {
        return reason;
    }

    public void setReason(ReviewPlanReason reason) {
        this.reason = reason;
    }

    public LocalDateTime getDoneAt() {
        return doneAt;
    }

    public void setDoneAt(LocalDateTime doneAt) {
        this.doneAt = doneAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
