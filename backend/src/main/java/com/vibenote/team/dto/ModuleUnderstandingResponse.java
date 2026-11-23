package com.vibenote.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 모듈(레이어)별 팀 정답률 (F-20b AC6). */
@Schema(description = "모듈별 팀 이해도")
public record ModuleUnderstandingResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "레이어",
                        allowableValues = {
                            "controller",
                            "service",
                            "mapper",
                            "vue",
                            "js",
                            "config",
                            "domain",
                            "other"
                        },
                        example = "controller")
                String layer,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "이 레이어의 문항 수",
                        example = "2")
                int questionCount,
        @Schema(description = "푼 멤버들의 정답률(0~100 정수, 반올림). 아무도 풀지 않았으면 null", example = "75")
                Integer correctRate) {}
