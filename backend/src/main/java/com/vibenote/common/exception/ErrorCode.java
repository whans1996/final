package com.vibenote.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 목록. 형식은 {@code <도메인>_<HTTP 상태 또는 3자리 번호>} (docs/conventions.md 3절).
 *
 * <p>새 코드는 spec의 "API 계약 &gt; 에러" 표에 먼저 적고 여기에 추가한다. message는 사용자에게 그대로 보여 줄 수 있는 한국어 문장이다.
 */
public enum ErrorCode {
    COMMON_400(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    COMMON_404(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없습니다."),
    COMMON_405(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
    COMMON_413(HttpStatus.PAYLOAD_TOO_LARGE, "업로드할 수 있는 크기를 넘었습니다."),
    COMMON_500(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),

    AUTH_401(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    AUTH_403(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
