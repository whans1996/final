package com.vibenote.analysis.structure;

import io.swagger.v3.oas.annotations.media.Schema;

/** 구조 그래프의 파일 하나 (F-21a 3절). */
@Schema(description = "선별 파일")
public record StructureNode(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "파일 ID", example = "101")
                long fileId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "저장소 기준 경로",
                        example = "src/main/java/com/example/todo/TodoController.java")
                String path,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "파일 이름",
                        example = "TodoController.java")
                String name,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "레이어 (analysis_files.layer 소문자)",
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
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        nullable = true,
                        description = "한 줄 역할 (F-06). 없으면 null",
                        example = "할 일 API 요청을 받아 서비스로 넘깁니다")
                String role) {}
