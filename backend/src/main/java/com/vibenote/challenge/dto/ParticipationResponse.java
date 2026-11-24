package com.vibenote.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 참여·취소 결과 (F-25 AC3·AC9). */
@Schema(description = "챌린지 참여 상태")
public record ParticipationResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "참여 여부",
                        example = "true")
                boolean joined,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "참여자 수", example = "3")
                long participantCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "내 참여·달성도 (참여하지 않았으면 null)")
                ChallengeProgressResponse participation) {}
