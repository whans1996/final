package com.vibenote.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 팀 만들기 응답 (F-20a AC1). */
@Schema(description = "팀 만들기 응답")
public record TeamCreatedResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "팀 ID", example = "12")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "초대 코드 8자",
                        example = "K7M2QX9P")
                String inviteCode) {}
