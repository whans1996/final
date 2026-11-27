package com.vibenote.analysis.project.dto;

import com.vibenote.analysis.AnalysisStatus;
import com.vibenote.analysis.domain.Analysis;
import com.vibenote.common.Level;
import com.vibenote.quiz.QuizStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 프로젝트 상세의 분석 하나 (P-1a AC7). */
@Schema(description = "프로젝트에 붙은 분석")
public record ProjectAnalysisResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분석 ID", example = "128")
                long id,
        @Schema(
                        description = "붙은 버전 ID. 아직 버전이 없는 FETCHING·FAILED 분석은 null",
                        example = "31",
                        nullable = true)
                Long versionId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "분석 수준",
                        example = "BEGINNER")
                Level level,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "진행 상태",
                        example = "DONE")
                AnalysisStatus status,
        @Schema(description = "퀴즈 상태. QUIZ 단계 전이면 null", example = "READY", nullable = true)
                QuizStatus quizStatus,
        @Schema(description = "분석 제목", example = "todo-app", nullable = true) String title,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "요청 시각",
                        example = "2026-11-28T17:30:00+09:00")
                OffsetDateTime createdAt) {

    public static ProjectAnalysisResponse from(Analysis analysis) {
        return new ProjectAnalysisResponse(
                analysis.getId(),
                analysis.getProjectVersionId(),
                analysis.getLevel(),
                analysis.getStatus(),
                analysis.getQuizStatus(),
                analysis.getTitle(),
                ProjectVersionResponse.offset(analysis.getCreatedAt()));
    }
}
