package com.vibenote.challenge.dto;

import com.vibenote.challenge.domain.ChallengeParticipant;
import com.vibenote.common.config.ClockConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/** 내 참여 정보 (F-25 AC2·AC3). 참여하지 않았으면 이 객체 대신 null이다. */
@Schema(description = "내 챌린지 참여·달성도")
public record ChallengeProgressResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "달성도 (기간 안 활동 수, 목표값을 넘지 않는다)",
                        example = "3")
                int progress,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "목표값", example = "10")
                int goalValue,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "달성 시각 (미달성이면 null)",
                        example = "2026-11-24T10:15:00+09:00")
                OffsetDateTime completedAt,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "참여 시각",
                        example = "2026-11-22T21:05:00+09:00")
                OffsetDateTime joinedAt) {

    public static ChallengeProgressResponse of(ChallengeParticipant participant, int goalValue) {
        return new ChallengeProgressResponse(
                participant.getProgress(),
                goalValue,
                atSeoul(participant.getCompletedAt()),
                atSeoul(participant.getJoinedAt()));
    }

    static OffsetDateTime atSeoul(LocalDateTime time) {
        return time == null ? null : time.atZone(ClockConfig.ZONE).toOffsetDateTime();
    }
}
