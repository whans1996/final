package com.vibenote.analysis.project.dto;

import com.vibenote.analysis.SourceType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** 프로젝트 상세 (P-1a AC7). */
@Schema(description = "프로젝트 상세")
public record ProjectDetailResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "프로젝트 ID",
                        example = "12")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "이름",
                        example = "todo-app")
                String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "입력 종류", example = "URL")
                SourceType sourceType,
        @Schema(
                        description = "정규화한 저장소 주소. ZIP이면 null",
                        example = "https://github.com/octo/todo-app",
                        nullable = true)
                String sourceUrl,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "만든 시각",
                        example = "2026-11-27T10:00:00+09:00")
                OffsetDateTime createdAt,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        requiredMode = Schema.RequiredMode.REQUIRED,
                                        description =
                                                "버전 (번호 내림차순). 삭제하지 않은 분석이 없는 버전은 빠져 번호가 빌 수 있다"))
                List<ProjectVersionResponse> versions,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        requiredMode = Schema.RequiredMode.REQUIRED,
                                        description = "삭제하지 않은 분석 (요청 최신순)"))
                List<ProjectAnalysisResponse> analyses) {}
