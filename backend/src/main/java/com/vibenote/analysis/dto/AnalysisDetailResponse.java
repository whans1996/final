package com.vibenote.analysis.dto;

import com.vibenote.analysis.AnalysisStatus;
import com.vibenote.analysis.SourceType;
import com.vibenote.analysis.domain.AnalysisDetail;
import com.vibenote.analysis.explain.AnalysisOverview;
import com.vibenote.common.Level;
import com.vibenote.common.config.ClockConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 분석 상세 (F-06 AC9). 결과 화면(SC-07)이 DONE이 되면 한 번 불러온다. */
@Schema(description = "분석 상세 (개요 포함)")
public record AnalysisDetailResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분석 ID", example = "42")
                long id,
        @Schema(description = "제목", example = "todo-app", nullable = true) String title,
        @Schema(description = "프로젝트 이름", example = "todo-app", nullable = true) String projectName,
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
                        example = "DONE")
                AnalysisStatus status,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "진행률 (0~100)",
                        example = "100")
                int progress,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "공개 여부",
                        example = "false")
                boolean isPublic,
        @Schema(description = "실패 사유 코드. FAILED가 아니면 null", example = "LLM_503", nullable = true)
                String errorCode,
        @Schema(description = "실패 사유 문구. FAILED가 아니면 null", nullable = true) String errorMessage,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "요청 시각",
                        example = "2026-11-24T14:20:00+09:00")
                OffsetDateTime createdAt,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "선별해 저장한 파일 수",
                        example = "14")
                int fileCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "설명이 만들어진 파일 수",
                        example = "14")
                int explainedFileCount,
        @Schema(description = "프로젝트 개요. 개요 생성 전·실패 시 null", nullable = true)
                AnalysisOverview overview) {

    public static AnalysisDetailResponse from(AnalysisDetail a) {
        return new AnalysisDetailResponse(
                a.getId(),
                a.getTitle(),
                a.getProjectName(),
                a.getSourceType(),
                a.getLevel(),
                a.getStatus(),
                a.getProgress(),
                a.getIsPublic(),
                a.getErrorCode(),
                a.getErrorMessage(),
                a.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime(),
                a.getFileCount(),
                a.getExplainedFileCount(),
                a.getOverview());
    }
}
