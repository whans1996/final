package com.vibenote.analysis.dto;

import com.vibenote.analysis.domain.AnalysisFile;
import io.swagger.v3.oas.annotations.media.Schema;

/** 분석 파일 하나 (F-06 AC11). 키·비밀 설정값은 저장 전에 [REDACTED]로 가려져 있다. */
@Schema(description = "분석 파일 (코드 포함)")
public record AnalysisFileResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "파일 ID", example = "101")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "프로젝트 루트 기준 경로",
                        example = "src/main/java/com/example/todo/TodoController.java")
                String path,
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
        @Schema(description = "한 줄 역할. 설명을 만들지 못했으면 null", nullable = true) String role,
        @Schema(description = "파일 설명. 설명을 만들지 못했으면 null", nullable = true) String summary,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "코드 내용",
                        example = "package com.example.todo;\n")
                String content) {

    public static AnalysisFileResponse from(AnalysisFile f) {
        return new AnalysisFileResponse(
                f.getId(), f.getPath(), f.getLayer(), f.getRole(), f.getSummary(), f.getContent());
    }
}
