package com.vibenote.analysis.explain;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 프로젝트 개요 (F-06 5.3절 저장 형태). {@code analyses.overview} JSON 컬럼 값이자 상세 조회 응답의 {@code overview}다. DB
 * 행이 아니라 검증·정리를 마친 값 객체라 응답에 그대로 쓴다.
 */
@Schema(description = "프로젝트 개요 (AI 생성 + 규칙 기반 API 목록)")
public record AnalysisOverview(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "프로젝트 요약 (800자 이내)",
                        example = "할 일을 등록하고 완료 표시하는 Todo 웹 서비스입니다.")
                String summary,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        requiredMode = Schema.RequiredMode.REQUIRED,
                                        description = "기술 스택 (최대 10개, 항목 40자 이내)"),
                        schema = @Schema(example = "Spring Boot"))
                List<String> techStack,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        requiredMode = Schema.RequiredMode.REQUIRED,
                                        description = "레이어별 역할"))
                List<LayerRole> layers,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        requiredMode = Schema.RequiredMode.REQUIRED,
                                        description = "처음 읽을 파일 순서 (최대 5개)"))
                List<ReadingStep> readingOrder,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        requiredMode = Schema.RequiredMode.REQUIRED,
                                        description =
                                                "컨트롤러 매핑 어노테이션으로 탐지한 API (path → method 순, 최대"
                                                        + " 50개)"))
                List<ApiEndpoint> apis) {

    @Schema(description = "레이어 하나의 역할")
    public record LayerRole(
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "레이어",
                            example = "controller",
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
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "역할 (300자 이내)",
                            example = "HTTP 요청을 받아 입력을 검사하고 Service를 부릅니다.")
                    String role) {}

    @Schema(description = "읽는 순서 항목")
    public record ReadingStep(
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "파일 ID (analysis_files.id)",
                            example = "101")
                    Long fileId,
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "파일 경로",
                            example = "src/main/java/com/example/todo/TodoController.java")
                    String path,
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "이 순서로 읽는 이유 (200자 이내)",
                            example = "어떤 API가 있는지 먼저 봅니다.")
                    String reason) {}
}
