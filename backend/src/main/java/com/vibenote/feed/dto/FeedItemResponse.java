package com.vibenote.feed.dto;

import com.vibenote.feed.ActivityType;
import com.vibenote.feed.domain.FeedRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 피드 항목 (F-04 AC6). */
@Schema(description = "피드 항목 (활동 하나)")
public record FeedItemResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "활동 ID", example = "91")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "활동 종류",
                        example = "REVIEW")
                ActivityType type,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "활동 시각",
                        example = "2026-11-20T13:40:00")
                LocalDateTime createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) FeedActorResponse actor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) FeedTargetResponse target) {

    static final String UNTITLED_ANALYSIS = "제목 없는 분석";

    public static FeedItemResponse from(FeedRow row) {
        String title = row.getTargetTitle();
        if (title == null && row.getType() == ActivityType.ANALYSIS) {
            title = UNTITLED_ANALYSIS;
        }
        return new FeedItemResponse(
                row.getId(),
                row.getType(),
                row.getCreatedAt(),
                new FeedActorResponse(row.getActorId(), row.getActorNickname()),
                new FeedTargetResponse(row.getTargetId(), title, row.getTargetRating()));
    }
}
