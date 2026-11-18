package com.vibenote.common.exception;

import java.util.Map;

/** 비즈니스 규칙 위반. {@link GlobalExceptionHandler}가 {@link ErrorResponse}로 변환한다. */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, String> details;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public BusinessException(ErrorCode errorCode, Map<String, String> details) {
        this(errorCode, errorCode.message(), details);
    }

    /** 메시지에 설정값 같은 변수가 들어갈 때 (예: ANALYSIS_429의 일일 한도). */
    public BusinessException(ErrorCode errorCode, String message, Map<String, String> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
