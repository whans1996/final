package com.vibenote.analysis.dto;

import com.vibenote.analysis.AnalysisStatus;
import com.vibenote.analysis.SourceType;
import com.vibenote.analysis.domain.Analysis;
import com.vibenote.common.Level;
import com.vibenote.common.config.ClockConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 내 분석 목록 항목 (F-05a AC12). */
@Schema(description = "분석 목록 항목")
public record AnalysisSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분석 ID", example = "42")
                long id,
        @Schema(description = "제목 (처음에는 프로젝트 이름)", example = "todo-app", nullable = true)
                String title,
        @Schema(
                        description = "프로젝트 이름 (ZIP 파일명에서 .zip을 뺀 이름)",
                        example = "todo-app",
                        nullable = true)
                String projectName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "입력 종류", example = "ZIP")
                SourceType sourceType,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "분석 수준",
                        example = "JUNIOR")
                Level level,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "진행 상태",
                        example = "SELECTING")
                AnalysisStatus status,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "진행률 (0~100)",
                        example = "25")
                int progress,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "공개 여부",
                        example = "false")
                boolean isPublic,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "요청 시각",
                        example = "2026-11-24T14:20:00+09:00")
                OffsetDateTime createdAt) {

    public static AnalysisSummaryResponse from(Analysis analysis) {
        return new AnalysisSummaryResponse(
                analysis.getId(),
                analysis.getTitle(),
                analysis.getProjectName(),
                analysis.getSourceType(),
                analysis.getLevel(),
                analysis.getStatus(),
                analysis.getProgress(),
                analysis.getIsPublic(),
                analysis.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
