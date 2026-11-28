package com.vibenote.interview;

import java.time.LocalDateTime;

/** analysis_interviews 행 (V17). 분석당 1개. */
public class AnalysisInterview {

    private Long id;
    private long analysisId;
    private InterviewStatus status;
    private String errorCode;
    private int regenerations;
    private LocalDateTime generatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AnalysisInterview pending(long analysisId, LocalDateTime now) {
        AnalysisInterview row = new AnalysisInterview();
        row.analysisId = analysisId;
        row.status = InterviewStatus.PENDING;
        row.createdAt = now;
        row.updatedAt = now;
        return row;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(long analysisId) {
        this.analysisId = analysisId;
    }

    public InterviewStatus getStatus() {
        return status;
    }

    public void setStatus(InterviewStatus status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public int getRegenerations() {
        return regenerations;
    }

    public void setRegenerations(int regenerations) {
        this.regenerations = regenerations;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
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
}
