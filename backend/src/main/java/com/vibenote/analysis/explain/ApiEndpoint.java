package com.vibenote.analysis.explain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 규칙으로 탐지한 API 하나 (F-06 AC7).
 *
 * @param method HTTP 메서드. method 없는 {@code @RequestMapping}은 {@code ANY}
 * @param path 클래스 경로 + 메서드 경로 (슬래시 하나로 정규화)
 * @param fileId 매핑이 있는 파일 (analysis_files.id)
 */
@Schema(description = "탐지한 API")
public record ApiEndpoint(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "HTTP 메서드 (method 없는 @RequestMapping은 ANY)",
                        example = "GET")
                String method,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "경로",
                        example = "/api/todos/{id}")
                String path,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "매핑이 있는 파일 ID",
                        example = "101")
                Long fileId) {}
