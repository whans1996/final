package com.vibenote.concept.admin.dto;

import com.vibenote.common.Level;
import com.vibenote.concept.ConceptStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 관리자 카드 생성·수정 요청 (F-13 AC4·AC5). PUT은 전체 교체다. source·wishCount·avgRating은 받지 않는다 (AC7). 단 {@code
 * levels}는 없거나 null이면 PUT이 기존 수준별 요약·본문을 그대로 둔다 (K-1 AC3).
 *
 * <p>허용값 검증 실패를 {@code details}에 필드별로 담으려고 category·difficulty·status는 문자열로 받는다. 모든 값은 검증 전에
 * trim하고, 선택 값(body·exampleCode·referenceUrl)은 비면 null이 된다.
 */
@Schema(description = "관리자 카드 생성·수정 요청")
public record AdminConceptRequest(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "개념 이름 (1~100자, 시드 카드는 바꿀 수 없음)",
                        example = "낙관적 락")
                @NotBlank(message = "이름을 입력하세요.")
                @Size(max = 100, message = "이름은 100자 이하로 입력하세요.")
                String name,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "카테고리",
                        allowableValues = {
                            "java",
                            "spring",
                            "web",
                            "security",
                            "mybatis",
                            "vue",
                            "database",
                            "javascript",
                            "tooling",
                            "typescript",
                            "react",
                            "cs"
                        },
                        example = "spring")
                @NotBlank(message = "카테고리를 고르세요.")
                @Pattern(
                        regexp = CATEGORY_REGEX,
                        message =
                                "카테고리는 java, spring, web, security, mybatis, vue, database,"
                                        + " javascript, tooling, typescript, react, cs 중 하나여야"
                                        + " 합니다.")
                String category,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "난이도",
                        allowableValues = {"BEGINNER", "JUNIOR", "SENIOR"},
                        example = "SENIOR")
                @NotBlank(message = "난이도를 고르세요.")
                @Pattern(
                        regexp = "BEGINNER|JUNIOR|SENIOR",
                        message = "난이도는 BEGINNER, JUNIOR, SENIOR 중 하나여야 합니다.")
                String difficulty,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "한 줄 요약 (1~500자)",
                        example = "충돌이 드물다고 보고 저장 시점에 버전으로 충돌을 확인하는 동시성 제어 방식이다.")
                @NotBlank(message = "요약을 입력하세요.")
                @Size(max = 500, message = "요약은 500자 이하로 입력하세요.")
                String summary,
        @Schema(
                        nullable = true,
                        description = "본문 Markdown (10,000자 이하, 비면 null)",
                        example = "## 핵심\n- 엔티티에 버전 컬럼을 두고 UPDATE 조건에 넣는다.")
                @Size(max = 10_000, message = "본문은 10,000자 이하로 입력하세요.")
                String body,
        @Schema(
                        nullable = true,
                        description = "예제 코드 (10,000자 이하, 비면 null)",
                        example =
                                "UPDATE orders SET status = ?, version = version + 1 WHERE id = ?"
                                        + " AND version = ?")
                @Size(max = 10_000, message = "예제 코드는 10,000자 이하로 입력하세요.")
                String exampleCode,
        @Schema(
                        nullable = true,
                        description = "공식 문서 출처 링크 (http·https만, 500자 이하, 비면 null)",
                        example =
                                "https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html")
                @Size(max = 500, message = "출처 링크는 500자 이하로 입력하세요.")
                @Pattern(
                        regexp = "(?i)https?://.+",
                        message = "출처 링크는 http:// 또는 https://로 시작해야 합니다.")
                String referenceUrl,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "상태. 생성은 DRAFT·PUBLISHED만, 공개·숨김 카드는 DRAFT로 되돌릴 수 없음",
                        allowableValues = {"DRAFT", "PUBLISHED", "HIDDEN"},
                        example = "PUBLISHED")
                @NotBlank(message = "상태를 고르세요.")
                @Pattern(
                        regexp = "DRAFT|PUBLISHED|HIDDEN",
                        message = "상태는 DRAFT, PUBLISHED, HIDDEN 중 하나여야 합니다.")
                String status,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        nullable = true,
                                        description =
                                                "수준별 요약·본문 0~3개 (level은 서로 달라야 함). 있으면 6칸을 통째로 바꾸고"
                                                        + " 배열에 없는 수준은 비운다. 없거나 null이면 PUT은 기존 값을"
                                                        + " 유지하고 POST는 모두 비운다 (K-1)"))
                @Size(max = 3, message = "수준별 요약·본문은 3개까지입니다.")
                List<@NotNull(message = "수준별 항목이 비었습니다.") @Valid AdminConceptLevelRequest> levels) {

    /** {@code ConceptCategories.ALL}과 같은 12종 (K-1 AC5, FE utils/conceptCategories.js). */
    static final String CATEGORY_REGEX =
            "java|spring|web|security|mybatis|vue|database|javascript|tooling|typescript|react|cs";

    public AdminConceptRequest {
        name = strip(name);
        category = strip(category);
        difficulty = strip(difficulty);
        summary = strip(summary);
        body = blankToNull(body);
        exampleCode = blankToNull(exampleCode);
        referenceUrl = blankToNull(referenceUrl);
        status = strip(status);
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }

    private static String blankToNull(String value) {
        String stripped = strip(value);
        return stripped == null || stripped.isEmpty() ? null : stripped;
    }

    public Level difficultyValue() {
        return Level.valueOf(difficulty);
    }

    public ConceptStatus statusValue() {
        return ConceptStatus.valueOf(status);
    }
}
