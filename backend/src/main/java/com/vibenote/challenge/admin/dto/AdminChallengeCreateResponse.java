package com.vibenote.challenge.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 챌린지 등록 결과 (F-24 AC1). */
@Schema(description = "챌린지 등록 결과")
public record AdminChallengeCreateResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "새 챌린지 ID",
                        example = "7")
                long id) {}
