package com.vibenote.reviewplan.dto;

import com.vibenote.common.config.ClockConfig;
import com.vibenote.concept.dto.ConceptSummaryResponse;
import com.vibenote.reviewplan.ReviewPlanReason;
import com.vibenote.reviewplan.ReviewPlanStatus;
import com.vibenote.reviewplan.domain.ReviewPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 복습 계획 항목 (F-15 3절 공통 항목). */
@Schema(description = "복습 계획 항목")
public record ReviewPlanItemResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "계획 ID", example = "31")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "복습할 날짜",
                        example = "2026-11-21")
                LocalDate planDate,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "상태 (TODO 할 일, DONE 완료)",
                        example = "TODO")
                ReviewPlanStatus status,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "추가된 이유 (WRONG 퀴즈 오답, WISH 찜, MANUAL 직접 추가)",
                        example = "WRONG")
                ReviewPlanReason reason,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "완료 시각 (DONE일 때만, 아니면 null)",
                        example = "2026-11-20T10:00:00+09:00")
                OffsetDateTime doneAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "카드 요약")
                ConceptSummaryResponse concept) {

    public static ReviewPlanItemResponse of(ReviewPlan plan, ConceptSummaryResponse concept) {
        return new ReviewPlanItemResponse(
                plan.getId(),
                plan.getPlanDate(),
                plan.getStatus(),
                plan.getReason(),
                plan.getDoneAt() == null
                        ? null
                        : plan.getDoneAt().atZone(ClockConfig.ZONE).toOffsetDateTime(),
                concept);
    }
}
