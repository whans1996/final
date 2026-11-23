package com.vibenote.analysis.fetch;

/** 저장소 주소가 4.1절 규칙을 어겼다. 메시지가 그대로 COMMON_400 {@code details.sourceUrl}이 된다 (F-05b AC2). */
public class InvalidRepositoryUrlException extends RuntimeException {

    public InvalidRepositoryUrlException(String message) {
        super(message);
    }
}
