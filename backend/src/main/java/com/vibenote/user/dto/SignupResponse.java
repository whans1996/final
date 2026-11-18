package com.vibenote.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 결과")
public record SignupResponse(@Schema(description = "가입한 회원 ID", example = "1") long id) {}
