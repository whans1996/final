package com.vibenote.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 분석을 공유한 사람 (= 분석 주인). */
@Schema(description = "공유한 사람")
public record SharedByResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "회원 ID", example = "3")
                long userId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "닉네임",
                        example = "vibecoder")
                String nickname) {}
