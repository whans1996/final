package com.vibenote.analysis.flow;

import java.time.LocalDateTime;

/** analysis_flows 행 (V1, V5). endpoint는 {@code "{METHOD} {path}"}. */
public class AnalysisFlow {

    private Long id;
    private long analysisId;
    private String endpoint;
    private FlowStatus status;
    private String summary;
    private FlowStep[] steps;
    private String errorCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AnalysisFlow pending(long analysisId, String endpoint, LocalDateTime now) {
        AnalysisFlow flow = new AnalysisFlow();
        flow.analysisId = analysisId;
        flow.endpoint = endpoint;
        flow.status = FlowStatus.PENDING;
        flow.createdAt = now;
        flow.updatedAt = now;
        return flow;
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

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public FlowStatus getStatus() {
        return status;
    }

    public void setStatus(FlowStatus status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public FlowStep[] getSteps() {
        return steps;
    }

    public void setSteps(FlowStep[] steps) {
        this.steps = steps;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
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
