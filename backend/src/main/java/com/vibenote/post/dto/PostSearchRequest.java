package com.vibenote.post.dto;

import com.vibenote.post.BoardType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 글 목록 검색 조건 {@code ?boardType=&keyword=} (F-17 AC2). 페이지는 {@code PageQuery}로 따로 받는다.
 *
 * <p>boardType은 허용되지 않은 값도 {@code details.boardType}으로 알려 주려고 문자열로 받는다 (enum이면 변환 실패가 필드 없는 400이
 * 된다). 둘 다 검증 전에 trim하고, 비면 조건 없음(null)으로 둔다.
 */
@Schema(description = "게시글 검색 조건")
public record PostSearchRequest(
        @Schema(
                        description = "게시판 (QNA: 코드 이해 Q&A, REVIEW: 학습 후기, FREE: 자유). 없으면 전체",
                        allowableValues = {"QNA", "REVIEW", "FREE"},
                        example = "QNA")
                @Pattern(regexp = "QNA|REVIEW|FREE", message = "게시판은 QNA, REVIEW, FREE 중 하나여야 합니다.")
                String boardType,
        @Schema(description = "제목·본문 부분 일치 검색어 (trim 후 50자 이하, 대소문자 무시)", example = "트랜잭션")
                @Size(max = 50, message = "검색어는 50자 이하로 입력하세요.")
                String keyword) {

    public PostSearchRequest {
        boardType = blankToNull(boardType);
        keyword = blankToNull(keyword);
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public BoardType boardTypeValue() {
        return boardType == null ? null : BoardType.valueOf(boardType);
    }
}
