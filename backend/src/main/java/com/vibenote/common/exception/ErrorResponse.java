package com.vibenote.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "실패 응답 (docs/conventions.md 2절)")
public record ErrorResponse(
        @Schema(description = "에러 코드", example = "COMMON_400") String code,
        @Schema(description = "사용자에게 보여 줄 한국어 메시지", example = "요청 값이 올바르지 않습니다.") String message,
        @Schema(
                        description = "검증 실패 시 필드별 사유. 없으면 null",
                        example = "{\"email\": \"이메일 형식이 아닙니다.\"}")
                Map<String, String> details) {

    public static ErrorResponse of(ErrorCode code) {
        return new ErrorResponse(code.name(), code.message(), null);
    }

    public static ErrorResponse of(ErrorCode code, Map<String, String> details) {
        return new ErrorResponse(code.name(), code.message(), details);
    }
}
