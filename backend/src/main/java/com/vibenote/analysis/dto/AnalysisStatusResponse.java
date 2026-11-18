package com.vibenote.analysis.dto;

import com.vibenote.analysis.AnalysisStatus;
import com.vibenote.analysis.domain.Analysis;
import com.vibenote.common.config.ClockConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 분석 진행 상태 (F-05a AC13). 화면이 2초마다 폴링한다. */
@Schema(description = "분석 진행 상태")
public record AnalysisStatusResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분석 ID", example = "42")
                long analysisId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "진행 상태",
                        example = "FILTERING")
                AnalysisStatus status,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "진행률 (0~100). FAILED면 실패 직전 값",
                        example = "15")
                int progress,
        @Schema(
                        description = "실패 사유 코드. FAILED가 아니면 null",
                        example = "ANALYSIS_413",
                        nullable = true,
                        allowableValues = {
                            "ANALYSIS_400",
                            "ANALYSIS_413",
                            "ANALYSIS_415",
                            "ANALYSIS_500"
                        })
                String errorCode,
        @Schema(
                        description = "사용자에게 보여 줄 실패 사유. FAILED가 아니면 null",
                        example = "지원하는 코드 파일이 312개입니다. 200개 이하로 줄여서 다시 올려 주세요.",
                        nullable = true)
                String errorMessage,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "마지막 상태 변경 시각",
                        example = "2026-11-24T14:20:01+09:00")
                OffsetDateTime updatedAt) {

    public static AnalysisStatusResponse from(Analysis analysis) {
        return new AnalysisStatusResponse(
                analysis.getId(),
                analysis.getStatus(),
                analysis.getProgress(),
                analysis.getErrorCode(),
                analysis.getErrorMessage(),
                analysis.getUpdatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
