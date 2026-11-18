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
    COMMON_413(HttpStatus.CONTENT_TOO_LARGE, "업로드할 수 있는 크기를 넘었습니다."),
    COMMON_415(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다."),
    COMMON_500(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),

    AUTH_401(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    AUTH_403(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    /** 로그인 실패. 이메일 없음·비밀번호 불일치·탈퇴 회원을 구분하지 않는다 (F-02). */
    AUTH_001(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    /** Refresh 토큰 없음·무효·만료·회전됨. FE는 재시도하지 않고 로그인 화면으로 보낸다 (F-02). */
    AUTH_002(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인하세요."),

    /** 이메일·닉네임 중복. details에 해당 필드를 담는다 (F-01). */
    USER_409(HttpStatus.CONFLICT, "이미 사용 중인 정보가 있습니다."),
    /** 없는 회원 또는 탈퇴 회원의 공개 프로필·통계 (F-03). */
    USER_404(HttpStatus.NOT_FOUND, "존재하지 않거나 탈퇴한 사용자입니다."),
    /** 탈퇴 시 비밀번호 재확인 실패. details.password에 사유를 담는다 (F-03). */
    USER_001(HttpStatus.BAD_REQUEST, "비밀번호가 올바르지 않습니다."),

    /** 설정된 LLM이 폴백까지 모두 응답하지 못함. 호출자는 파이프라인 단계를 FAILED로 표시한다 (F-00). */
    LLM_503(HttpStatus.SERVICE_UNAVAILABLE, "AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.");

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
