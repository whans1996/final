package com.vibenote.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 공유 결과 (F-20b AC1). */
@Schema(description = "팀 분석 공유 결과")
public record TeamAnalysisSharedResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분석 ID", example = "42")
                long analysisId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "공유 시각",
                        example = "2026-11-25T14:20:00+09:00")
                OffsetDateTime sharedAt) {}
