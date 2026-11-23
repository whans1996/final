package com.vibenote.challenge.admin.dto;

import com.vibenote.challenge.ChallengeGoalType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * 관리자 챌린지 등록·수정 요청 multipart 파트 (F-24 AC1·AC2·AC6). PUT은 전체 교체다.
 *
 * <p>형식 오류도 {@code details}에 필드별로 담으려고 날짜·목표값·불리언을 문자열로 받는다. 검증은 서비스가 {@code Validator}로 한다 (없는 챌린지
 * 404를 검증 400보다 먼저 판단하려고 Controller에서 {@code @Valid}를 쓰지 않는다). 기간·이미지 교차 검증도 서비스가 한다.
 */
@Schema(description = "관리자 챌린지 등록·수정 요청 (multipart/form-data)")
public record AdminChallengeRequest(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "제목 (앞뒤 공백 제거 후 1~100자)",
                        example = "12월 퀴즈 챌린지")
                @NotBlank(message = "제목을 입력하세요.")
                @Size(max = 100, message = "제목은 100자 이하로 입력하세요.")
                String title,
        @Schema(
                        nullable = true,
                        description = "설명 (2,000자 이하, 비면 null)",
                        example = "분석한 프로젝트의 퀴즈를 12월 안에 10번 풀어 보세요.")
                @Size(max = 2000, message = "설명은 2,000자 이하로 입력하세요.")
                String description,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        type = "string",
                        format = "date",
                        description = "시작일 yyyy-MM-dd (과거 허용). 시작한 챌린지는 바꿀 수 없음",
                        example = "2026-12-01")
                @NotBlank(message = "시작일을 입력하세요.")
                @Pattern(regexp = DATE_REGEX, message = DATE_MESSAGE)
                String startDate,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        type = "string",
                        format = "date",
                        description =
                                "종료일 yyyy-MM-dd. 시작일 이후, 기간(양 끝 포함) 90일 이하. 등록할 때와 종료일을 바꿀 때는 오늘"
                                        + " 이후",
                        example = "2026-12-31")
                @NotBlank(message = "종료일을 입력하세요.")
                @Pattern(regexp = DATE_REGEX, message = DATE_MESSAGE)
                String endDate,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description =
                                "목표 유형 (REVIEW_COUNT 개념 복습, QUIZ_COUNT 퀴즈, ANALYSIS_COUNT 분석). 시작한"
                                        + " 챌린지는 바꿀 수 없음",
                        allowableValues = {"REVIEW_COUNT", "QUIZ_COUNT", "ANALYSIS_COUNT"},
                        example = "QUIZ_COUNT")
                @NotBlank(message = "목표 유형을 고르세요.")
                @Pattern(regexp = GOAL_TYPE_REGEX, message = GOAL_TYPE_MESSAGE)
                String goalType,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        type = "integer",
                        minimum = "1",
                        maximum = "1000",
                        description = "목표값 (1~1,000). 시작한 챌린지는 바꿀 수 없음",
                        example = "10")
                @NotBlank(message = "목표값을 입력하세요.")
                @Pattern(regexp = GOAL_VALUE_REGEX, message = GOAL_VALUE_MESSAGE)
                String goalValue,
        @Schema(
                        type = "string",
                        format = "binary",
                        nullable = true,
                        description =
                                "대표 이미지 (jpg·png·webp, 2MB 이하, 확장자·MIME·내용이 일치). 수정 때 생략하면 기존 이미지를"
                                        + " 유지한다")
                MultipartFile image,
        @Schema(
                        description = "수정 전용. true면 기존 이미지를 지운다 (image와 함께 보낼 수 없음)",
                        allowableValues = {"true", "false"},
                        defaultValue = "false")
                @Pattern(regexp = "true|false", message = "removeImage는 true 또는 false여야 합니다.")
                String removeImage) {

    static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}";
    public static final String DATE_MESSAGE = "날짜는 yyyy-MM-dd 형식의 올바른 날짜로 입력하세요.";
    static final String GOAL_TYPE_REGEX = "REVIEW_COUNT|QUIZ_COUNT|ANALYSIS_COUNT";
    static final String GOAL_TYPE_MESSAGE =
            "목표 유형은 REVIEW_COUNT, QUIZ_COUNT, ANALYSIS_COUNT 중 하나여야 합니다.";
    static final String GOAL_VALUE_REGEX = "[1-9]\\d{0,2}|1000";
    static final String GOAL_VALUE_MESSAGE = "목표값은 1~1,000 사이의 정수로 입력하세요.";

    public AdminChallengeRequest {
        title = strip(title);
        description = blankToNull(description);
        startDate = strip(startDate);
        endDate = strip(endDate);
        goalType = strip(goalType);
        goalValue = strip(goalValue);
        removeImage = blankToNull(removeImage);
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }

    private static String blankToNull(String value) {
        String stripped = strip(value);
        return stripped == null || stripped.isEmpty() ? null : stripped;
    }

    public ChallengeGoalType goalTypeValue() {
        return ChallengeGoalType.valueOf(goalType);
    }

    public int goalValueAsInt() {
        return Integer.parseInt(goalValue);
    }

    public boolean removeImageRequested() {
        return "true".equals(removeImage);
    }
}
