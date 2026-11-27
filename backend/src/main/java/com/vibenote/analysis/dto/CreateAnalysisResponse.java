package com.vibenote.analysis.dto;

import com.vibenote.analysis.AnalysisStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/** 분석 요청 접수 결과 (F-05a, P-1a AC1). 진행 상태는 {@code GET /api/analyses/{id}/status}로 폴링한다. */
@Schema(description = "분석 요청 접수 결과")
public record CreateAnalysisResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분석 ID", example = "42")
                long analysisId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "현재 상태 (접수 직후 FETCHING)",
                        example = "FETCHING")
                AnalysisStatus status,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "분석이 붙은 프로젝트 ID (P-1a)",
                        example = "12")
                long projectId) {}
