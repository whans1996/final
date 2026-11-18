package com.vibenote.analysis.dto;

import com.vibenote.analysis.domain.AnalysisFile;
import io.swagger.v3.oas.annotations.media.Schema;

/** 분석 파일 목록 항목 (F-06 AC10). 코드 내용은 단건 조회에서만 준다. */
@Schema(description = "분석 파일 목록 항목")
public record AnalysisFileSummaryResponse(
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
        @Schema(
                        description = "한 줄 역할. 설명을 만들지 못했으면 null",
                        example = "할 일 목록·등록·완료 API를 제공하는 컨트롤러",
                        nullable = true)
                String role,
        @Schema(
                        description = "파일 설명. 설명을 만들지 못했으면 null",
                        example = "@RestController가 붙어 있어 메서드의 반환값이 JSON으로 응답됩니다.",
                        nullable = true)
                String summary) {

    public static AnalysisFileSummaryResponse from(AnalysisFile f) {
        return new AnalysisFileSummaryResponse(
                f.getId(), f.getPath(), f.getLayer(), f.getRole(), f.getSummary());
    }
}
