package com.vibenote.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 안 읽은 알림 수 (NT-1 AC2). */
@Schema(description = "안 읽은 알림 수")
public record UnreadCountResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "안 읽은 알림 수",
                        example = "3")
                long count) {}
