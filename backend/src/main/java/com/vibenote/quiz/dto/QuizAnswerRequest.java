package com.vibenote.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 문항 하나의 답 (F-14 AC6·AC8). */
@Schema(description = "문항 하나의 답")
public record QuizAnswerRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "문항 ID", example = "21")
                @NotNull(message = "문항을 지정해 주세요.")
                Long questionId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "고른 선택지 위치 (0~3)",
                        example = "0",
                        minimum = "0",
                        maximum = "3")
                @NotNull(message = "선택지를 골라 주세요.")
                @Min(value = 0, message = "선택지는 0~3 중 하나입니다.")
                @Max(value = 3, message = "선택지는 0~3 중 하나입니다.")
                Integer selectedIndex) {}
