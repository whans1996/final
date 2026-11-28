package com.vibenote.notification.dto;

import com.vibenote.common.config.ClockConfig;
import com.vibenote.notification.NotificationType;
import com.vibenote.notification.domain.NotificationRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 알림 항목 (NT-1 AC1). */
@Schema(description = "알림 항목")
public record NotificationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "알림 ID", example = "41")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "알림 종류 (FE는 모르는 종류도 기본 아이콘으로 그린다)",
                        example = "COMMENT")
                NotificationType type,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "문장 (주어 없음. actor가 있으면 FE가 \"{닉네임}님이 \"를 앞에 붙인다)",
                        example = "회원님의 글에 댓글을 남겼어요 · Spring 질문")
                String message,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "누르면 이동할 FE 경로",
                        example = "/community/12")
                String link,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "일으킨 회원. 없는 알림(챌린지 달성 등)은 null")
                NotificationActorResponse actor,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "읽음 여부",
                        example = "false")
                boolean read,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "알림 시각",
                        example = "2026-11-29T14:02:11+09:00")
                OffsetDateTime createdAt) {

    public static NotificationResponse from(NotificationRow row) {
        NotificationActorResponse actor =
                row.getActorId() == null
                        ? null
                        : new NotificationActorResponse(row.getActorId(), row.getActorNickname());
        return new NotificationResponse(
                row.getId(),
                row.getType(),
                row.getMessage(),
                row.getLink(),
                actor,
                row.getReadAt() != null,
                row.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
