package com.vibenote.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 알림을 일으킨 회원의 지금 정보 (NT-1 AC1). */
@Schema(description = "알림을 일으킨 회원")
public record NotificationActorResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "회원 ID", example = "7")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "지금 닉네임 (탈퇴했으면 마스킹 값 #탈퇴{id})",
                        example = "java러버")
                String nickname) {}
