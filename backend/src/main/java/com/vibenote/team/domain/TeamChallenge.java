package com.vibenote.team.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 팀 목표 한 행 (team_challenges + analyses 제목, F-20c). Controller 응답으로 직접 내보내지 않는다. */
public class TeamChallenge {

    private Long id;
    private Long analysisId;
    private String title;
    private String projectName;
    private LocalDate startDate;
    private LocalDate endDate;
    private int targetRate;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public String getTitle() {
        return title;
    }

    public String getProjectName() {
        return projectName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getTargetRate() {
        return targetRate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
