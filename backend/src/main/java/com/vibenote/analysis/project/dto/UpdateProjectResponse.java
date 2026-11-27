package com.vibenote.analysis.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 프로젝트 이름 바꾸기 결과 (P-1a AC8). */
@Schema(description = "바뀐 프로젝트 이름")
public record UpdateProjectResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "프로젝트 ID",
                        example = "12")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "바뀐 이름",
                        example = "할 일 앱")
                String name) {}
