package com.vibenote.quiz.domain;

import java.time.LocalDateTime;

/** quizzes 행 (V1). 분석당 1개 ({@code analysis_id} UNIQUE). */
public class Quiz {

    private Long id;
    private long analysisId;
    private LocalDateTime createdAt;

    public static Quiz of(long analysisId, LocalDateTime now) {
        Quiz quiz = new Quiz();
        quiz.analysisId = analysisId;
        quiz.createdAt = now;
        return quiz;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
