package com.vibenote.concept.dto;

import com.vibenote.common.Level;
import com.vibenote.concept.ConceptSort;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 카드 목록 검색 조건 {@code ?keyword=&category=&difficulty=&sort=} (F-09). 페이지는 {@code PageQuery}로 따로 받는다.
 *
 * <p>difficulty·sort는 허용되지 않은 값도 {@code details}에 필드별로 알려 주려고 문자열로 받는다 (enum이면 변환 실패가 필드 없이 400이
 * 된다). keyword는 검증 전에 trim하고, 비면 조건 없음(null)으로 둔다.
 */
@Schema(description = "개념 카드 검색 조건")
public record ConceptSearchRequest(
        @Schema(description = "이름·요약 부분 일치 검색어 (trim 후 50자 이하, 대소문자 무시)", example = "jwt")
                @Size(max = 50, message = "검색어는 50자 이하로 입력하세요.")
                String keyword,
        @Schema(
                        description =
                                "카테고리 (java, spring, web, security, mybatis, vue, database,"
                                    + " javascript, tooling, typescript, react, cs). 없는 값이면 빈 목록",
                        example = "security")
                @Size(max = 50, message = "카테고리는 50자 이하여야 합니다.")
                String category,
        @Schema(
                        description = "난이도",
                        allowableValues = {"BEGINNER", "JUNIOR", "SENIOR"},
                        example = "JUNIOR")
                @Pattern(
                        regexp = "BEGINNER|JUNIOR|SENIOR",
                        message = "난이도는 BEGINNER, JUNIOR, SENIOR 중 하나여야 합니다.")
                String difficulty,
        @Schema(
                        description = "정렬 (POPULAR: 찜 수→평점, LATEST: 최신 등록)",
                        allowableValues = {"POPULAR", "LATEST"},
                        defaultValue = "POPULAR",
                        example = "POPULAR")
                @Pattern(regexp = "POPULAR|LATEST", message = "정렬은 POPULAR, LATEST 중 하나여야 합니다.")
                String sort) {

    public ConceptSearchRequest {
        keyword = blankToNull(keyword);
        category = blankToNull(category);
        difficulty = blankToNull(difficulty);
        sort = blankToNull(sort);
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Level difficultyValue() {
        return difficulty == null ? null : Level.valueOf(difficulty);
    }

    public ConceptSort sortValue() {
        return sort == null ? ConceptSort.POPULAR : ConceptSort.valueOf(sort);
    }
}
