package com.vibenote.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 분석 공개 설정 요청 (F-19 AC4·AC5). */
@Schema(description = "분석 공개 설정 요청")
public record UpdateVisibilityRequest(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "공개 여부",
                        example = "true")
                @NotNull(message = "공개 여부를 보내 주세요.")
                Boolean isPublic) {}
