package com.vibenote.concept.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 카드 생성 결과 (F-13 AC4). */
@Schema(description = "카드 생성 결과")
public record AdminConceptCreateResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "새 카드 ID",
                        example = "92")
                long id) {}
