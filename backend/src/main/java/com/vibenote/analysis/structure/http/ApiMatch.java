package com.vibenote.analysis.structure.http;

/**
 * FE 요청을 받는 API 판정 (F-21a AC11).
 *
 * @param fileId MATCHED일 때 컨트롤러 파일 ID
 */
public record ApiMatch(Status status, Long fileId) {

    public enum Status {
        MATCHED,
        AMBIGUOUS,
        NONE
    }

    public static ApiMatch matched(long fileId) {
        return new ApiMatch(Status.MATCHED, fileId);
    }

    public static final ApiMatch AMBIGUOUS = new ApiMatch(Status.AMBIGUOUS, null);
    public static final ApiMatch NONE = new ApiMatch(Status.NONE, null);
}
