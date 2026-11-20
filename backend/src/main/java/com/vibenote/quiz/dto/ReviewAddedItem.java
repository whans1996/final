package com.vibenote.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** 제출로 새로 넣은 복습 계획 하나 (F-14 AC7). */
@Schema(description = "새로 추가한 복습 계획")
public record ReviewAddedItem(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "개념 카드 ID",
                        example = "57")
                long conceptId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "복습 날짜 (제출 다음 날, Asia/Seoul)",
                        example = "2026-11-20")
                LocalDate planDate) {}
