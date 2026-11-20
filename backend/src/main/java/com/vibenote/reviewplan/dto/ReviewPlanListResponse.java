package com.vibenote.reviewplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** 기간 안의 복습 계획과 밀린 복습 (F-15 AC1·AC2). */
@Schema(description = "복습 계획 목록")
public record ReviewPlanListResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "오늘 (서비스 시계, Asia/Seoul)",
                        example = "2026-11-20")
                LocalDate today,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "기간 시작 (포함)",
                        example = "2026-11-20")
                LocalDate from,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "기간 끝 (포함)",
                        example = "2026-11-26")
                LocalDate to,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "밀린 복습: 오늘 이전 날짜의 TODO (기간과 무관, 날짜·id 오름차순)")
                List<ReviewPlanItemResponse> overdue,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "기간 안의 계획 (TODO·DONE, 날짜·id 오름차순)")
                List<ReviewPlanItemResponse> items) {}
