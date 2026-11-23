package com.vibenote.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 초대 코드로 가입 응답 (F-20a AC5). */
@Schema(description = "초대 코드로 가입 응답")
public record JoinTeamResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "가입한 팀 ID",
                        example = "12")
                long teamId) {}
