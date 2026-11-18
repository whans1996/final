package com.vibenote.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 회원 탈퇴 요청 (F-03). 남은 Access 토큰만으로 탈퇴할 수 없게 현재 비밀번호를 다시 받는다. */
@Schema(description = "회원 탈퇴 요청")
public record WithdrawRequest(
        @Schema(description = "현재 비밀번호", example = "vibe1234") @NotBlank(message = PASSWORD_MESSAGE)
                String password) {

    public static final String PASSWORD_MESSAGE = "비밀번호를 입력하세요.";
}
