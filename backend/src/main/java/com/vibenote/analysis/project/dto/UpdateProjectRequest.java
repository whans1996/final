package com.vibenote.analysis.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 프로젝트 이름 바꾸기 (P-1a AC8). 검증 전에 앞뒤 공백을 뗀다. */
@Schema(description = "프로젝트 이름 바꾸기 요청")
public record UpdateProjectRequest(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "새 이름 (앞뒤 공백을 뗀 1~100자)",
                        example = "할 일 앱")
                @NotBlank(message = NAME_MESSAGE)
                @Size(max = 100, message = NAME_MESSAGE)
                String name) {

    public static final String NAME_MESSAGE = "프로젝트 이름은 1~100자로 입력하세요.";

    public UpdateProjectRequest {
        name = name == null ? null : name.strip();
    }
}
