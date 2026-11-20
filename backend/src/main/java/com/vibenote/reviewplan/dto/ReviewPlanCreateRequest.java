package com.vibenote.reviewplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/** 복습 계획 직접 추가 (F-15 AC4·AC5). */
@Schema(description = "복습 계획 직접 추가")
public record ReviewPlanCreateRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "카드 ID", example = "12")
                @NotNull(message = "카드를 골라 주세요.")
                @Positive(message = "카드를 골라 주세요.")
                Long conceptId,
        @Schema(
                        nullable = true,
                        description = "복습할 날짜 (오늘 ~ 오늘+365일). 생략하면 내일",
                        example = "2026-11-21")
                LocalDate planDate) {}
