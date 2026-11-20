package com.vibenote.reviewplan.dto;

import com.vibenote.reviewplan.ReviewPlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 복습 계획 완료·되돌림 (F-15 AC6). */
@Schema(description = "복습 계획 상태 변경")
public record ReviewPlanStatusRequest(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "바꿀 상태 (TODO 되돌림, DONE 완료)",
                        example = "DONE")
                @NotNull(message = "상태를 보내 주세요.")
                ReviewPlanStatus status) {}
