package com.vibenote.concept.admin.dto;

import com.vibenote.concept.ConceptSource;
import com.vibenote.concept.ConceptStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 관리자 카드 목록 조건 {@code ?status=&source=&keyword=} (F-13 AC1). 페이지는 {@code PageQuery}로 따로 받는다.
 *
 * <p>status·source는 허용되지 않은 값을 {@code details}에 필드별로 알리려고 문자열로 받는다 (F-09 {@code
 * ConceptSearchRequest}와 같은 방식). 값은 검증 전에 trim하고, 비면 조건 없음(null)으로 둔다.
 */
@Schema(description = "관리자 카드 목록 조건")
public record AdminConceptSearchRequest(
        @Schema(
                        description = "상태 (없으면 전체)",
                        allowableValues = {"DRAFT", "PUBLISHED", "HIDDEN"},
                        example = "DRAFT")
                @Pattern(
                        regexp = "DRAFT|PUBLISHED|HIDDEN",
                        message = "상태는 DRAFT, PUBLISHED, HIDDEN 중 하나여야 합니다.")
                String status,
        @Schema(
                        description = "출처 (없으면 전체)",
                        allowableValues = {"SEED", "AI", "ADMIN"},
                        example = "AI")
                @Pattern(regexp = "SEED|AI|ADMIN", message = "출처는 SEED, AI, ADMIN 중 하나여야 합니다.")
                String source,
        @Schema(description = "이름·요약 부분 일치 검색어 (trim 후 50자 이하, 대소문자 무시)", example = "락")
                @Size(max = 50, message = "검색어는 50자 이하로 입력하세요.")
                String keyword) {

    public AdminConceptSearchRequest {
        status = blankToNull(status);
        source = blankToNull(source);
        keyword = blankToNull(keyword);
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public ConceptStatus statusValue() {
        return status == null ? null : ConceptStatus.valueOf(status);
    }

    public ConceptSource sourceValue() {
        return source == null ? null : ConceptSource.valueOf(source);
    }
}
