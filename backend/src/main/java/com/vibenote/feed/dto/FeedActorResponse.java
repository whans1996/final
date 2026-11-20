package com.vibenote.feed.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 피드 항목의 활동한 회원 (F-04 AC6). */
@Schema(description = "활동한 회원")
public record FeedActorResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "회원 ID", example = "7")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "닉네임",
                        example = "java러버")
                String nickname) {}
