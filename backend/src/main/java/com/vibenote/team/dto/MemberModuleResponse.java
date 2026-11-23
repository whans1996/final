package com.vibenote.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 멤버 한 명의 최근 풀이에서 레이어별 맞힌 수 (F-20b AC6). */
@Schema(description = "멤버의 모듈별 결과")
public record MemberModuleResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "레이어",
                        example = "controller")
                String layer,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "맞힌 문항 수", example = "1")
                int correct,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "문항 수", example = "2")
                int total) {}
