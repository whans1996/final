package com.vibenote.analysis.flow;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 실행 흐름의 단계 하나 (F-07 AC4). {@code analysis_flows.steps} JSON 배열의 항목이자 응답의 {@code steps[]}다. 검증·정리를
 * 마친 값 객체라 응답에 그대로 쓴다.
 */
@Schema(description = "실행 흐름 단계")
public record FlowStep(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "단계 번호 (1부터)",
                        example = "1")
                Integer no,
        @Schema(
                        description = "파일 layer. 분석에 없는 경로면 null",
                        example = "controller",
                        nullable = true,
                        allowableValues = {
                            "controller",
                            "service",
                            "mapper",
                            "vue",
                            "js",
                            "config",
                            "domain",
                            "other"
                        })
                String layer,
        @Schema(description = "파일 ID. 분석에 없는 경로면 null", example = "101", nullable = true)
                Long fileId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "파일 경로",
                        example = "src/main/java/com/example/todo/TodoController.java")
                String path,
        @Schema(
                        description = "클래스.메서드 (100자 이내)",
                        example = "TodoController.create",
                        nullable = true)
                String symbol,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "단계 이름 (60자 이내)",
                        example = "요청 받기")
                String title,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "단계 설명 (600자 이내)",
                        example = "@PostMapping이 붙은 create 메서드가 JSON 본문을 @RequestBody로 받습니다.")
                String description) {}
