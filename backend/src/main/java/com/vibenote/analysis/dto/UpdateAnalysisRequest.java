package com.vibenote.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 분석 제목·메모 수정 요청 (F-19 AC1·AC2). 두 필드 모두 보낸 값으로 바꾼다 (누락 = null).
 *
 * <p>검증 전에 앞뒤 공백을 자르고, 빈 문자열이면 null로 바꾼다. 제목이 null이면 화면은 프로젝트 이름 → "제목 없는 분석"으로 대신 보여 준다.
 */
@Schema(description = "분석 제목·메모 수정 요청")
public record UpdateAnalysisRequest(
        @Schema(description = "제목 (100자 이하). 비우면 null", example = "할 일 앱 백엔드", nullable = true)
                @Size(max = 100, message = TITLE_MESSAGE)
                String title,
        @Schema(
                        description = "메모 (1000자 이하). 비우면 null",
                        example = "JWT 필터 순서 다시 보기",
                        nullable = true)
                @Size(max = 1000, message = MEMO_MESSAGE)
                String memo) {

    public static final String TITLE_MESSAGE = "제목은 100자 이하로 입력하세요.";
    public static final String MEMO_MESSAGE = "메모는 1000자 이하로 입력하세요.";

    public UpdateAnalysisRequest {
        title = blankToNull(title);
        memo = blankToNull(memo);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
