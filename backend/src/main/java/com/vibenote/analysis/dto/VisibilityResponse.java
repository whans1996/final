package com.vibenote.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 공개 설정 결과 (F-19 AC4). */
@Schema(description = "분석 공개 설정 결과")
public record VisibilityResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분석 ID", example = "42")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "공개 여부",
                        example = "true")
                boolean isPublic) {}
