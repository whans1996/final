package com.vibenote.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 분석 주인 (F-19 AC9·AC11). */
@Schema(description = "분석 주인")
public record AnalysisOwnerResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "회원 ID", example = "7")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "닉네임",
                        example = "vibecoder")
                String nickname) {}
