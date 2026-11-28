package com.vibenote.concept.admin.dto;

import com.vibenote.common.Level;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 관리자 카드 요청의 수준별 요약·본문 하나 (K-1 AC3·AC4). 값은 검증 전에 trim하고 비면 null이 된다. */
@Schema(description = "관리자 카드 요청의 수준별 요약·본문")
public record AdminConceptLevelRequest(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "수준 (배열 안에서 서로 달라야 함)",
                        allowableValues = {"BEGINNER", "JUNIOR", "SENIOR"},
                        example = "JUNIOR")
                @NotBlank(message = "수준을 고르세요.")
                @Pattern(
                        regexp = "BEGINNER|JUNIOR|SENIOR",
                        message = "수준은 BEGINNER, JUNIOR, SENIOR 중 하나여야 합니다.")
                String level,
        @Schema(
                        nullable = true,
                        description = "그 수준의 한 줄 요약 (300자 이하, 비면 null)",
                        example = "생성자로 받은 빈을 컨테이너가 주입합니다.")
                @Size(max = 300, message = "수준별 요약은 300자 이하로 입력하세요.")
                String summary,
        @Schema(
                        nullable = true,
                        description = "그 수준의 본문 Markdown (10,000자 이하, 비면 null)",
                        example = "## 핵심\n- 빈은 타입으로 찾는다.")
                @Size(max = 10_000, message = "수준별 본문은 10,000자 이하로 입력하세요.")
                String body) {

    public AdminConceptLevelRequest {
        level = level == null ? null : level.strip();
        summary = blankToNull(summary);
        body = blankToNull(body);
    }

    private static String blankToNull(String value) {
        String stripped = value == null ? null : value.strip();
        return stripped == null || stripped.isEmpty() ? null : stripped;
    }

    public Level levelValue() {
        return Level.valueOf(level);
    }
}
