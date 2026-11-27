package com.vibenote.analysis.fetch;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 기본 브랜치 최신 커밋 (P-1a AC2·AC9). 외부 응답에서 읽은 값이라 SHA는 형식을 확인한 뒤에만 경로에 넣는다.
 *
 * @param sha 40자 소문자 16진수
 * @param message 커밋 메시지 첫 줄 (최대 {@value #MAX_MESSAGE_LENGTH}자). 비었으면 null
 * @param committedAt 커밋 시각 (Asia/Seoul). 읽지 못했으면 null
 */
public record RepositoryCommit(String sha, String message, LocalDateTime committedAt) {

    /** {@code project_versions.commit_message VARCHAR(200)}. */
    public static final int MAX_MESSAGE_LENGTH = 200;

    private static final Pattern SHA = Pattern.compile("^[0-9a-f]{40}$");

    public RepositoryCommit {
        if (!isSha(sha)) {
            throw new IllegalArgumentException("커밋 SHA 형식이 아님");
        }
        message = firstLine(message);
    }

    public static boolean isSha(String value) {
        return value != null && SHA.matcher(value).matches();
    }

    /** 첫 줄의 앞뒤 공백을 떼고 200자로 자른다. 서로게이트 쌍 중간에서 자르지 않는다. */
    static String firstLine(String message) {
        if (message == null) {
            return null;
        }
        String line = message.lines().findFirst().orElse("").strip();
        if (line.isEmpty()) {
            return null;
        }
        if (line.length() <= MAX_MESSAGE_LENGTH) {
            return line;
        }
        int end = MAX_MESSAGE_LENGTH;
        if (Character.isHighSurrogate(line.charAt(end - 1))) {
            end--;
        }
        return line.substring(0, end);
    }
}
