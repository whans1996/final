package com.vibenote.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 분석이 붙은 프로젝트 (P-1a AC12). 이름은 프로젝트의 지금 이름이다. */
@Schema(description = "분석이 붙은 프로젝트")
public record AnalysisProjectRefResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "프로젝트 ID",
                        example = "12")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "프로젝트 이름",
                        example = "todo-app")
                String name) {}
