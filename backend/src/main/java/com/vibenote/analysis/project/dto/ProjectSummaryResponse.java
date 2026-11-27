package com.vibenote.analysis.project.dto;

import com.vibenote.analysis.SourceType;
import com.vibenote.common.Level;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** 내 프로젝트 목록 항목 (P-1a AC6). */
@Schema(description = "프로젝트 목록 항목")
public record ProjectSummaryResponse(
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
                        description = "삭제하지 않은 분석이 붙은 버전 수",
                        example = "2")
                int versionCount,
        @Schema(
                        description =
                                "번호가 가장 큰 버전 (삭제하지 않은 분석이 붙은 것). 첫 분석이 아직 FETCHING이거나 실패했으면 null",
                        nullable = true)
                ProjectVersionResponse latestVersion,
        @ArraySchema(
                        schema = @Schema(implementation = Level.class),
                        arraySchema =
                                @Schema(
                                        requiredMode = Schema.RequiredMode.REQUIRED,
                                        description = "삭제하지 않은 분석의 수준 (중복 없이 BEGINNER → SENIOR 순)"))
                List<Level> levels,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "마지막 분석 요청 시각",
                        example = "2026-11-28T17:30:00+09:00")
                OffsetDateTime lastAnalyzedAt) {}
