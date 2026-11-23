package com.vibenote.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 초대 코드 재발급 응답 (F-20a AC7). 이전 코드는 바로 무효다. */
@Schema(description = "초대 코드 재발급 응답")
public record InviteCodeResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "새 초대 코드 8자",
                        example = "P4ZR8NWE")
                String inviteCode) {}
