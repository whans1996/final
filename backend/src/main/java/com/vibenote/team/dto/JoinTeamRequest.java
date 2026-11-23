package com.vibenote.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;

/** 초대 코드로 가입 요청 (F-20a AC5). 코드는 앞뒤 공백을 빼고 대문자로 바꿔 비교한다. */
@Schema(description = "초대 코드로 가입 요청")
public record JoinTeamRequest(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "초대 코드 8자 (대소문자·앞뒤 공백 무시)",
                        example = "K7M2QX9P")
                @NotBlank(message = "초대 코드를 입력해 주세요.")
                String inviteCode) {

    public JoinTeamRequest {
        if (inviteCode != null) inviteCode = inviteCode.strip().toUpperCase(Locale.ROOT);
    }
}
