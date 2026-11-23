package com.vibenote.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 팀 목표 만들기 요청 (F-20c AC1·AC2). 검증은 팀장 확인 뒤 Service가 한다. 날짜를 문자열로 받는 이유는 형식 오류도 403보다 뒤에, 필드별
 * details로 알리기 위해서다 (Jackson이 먼저 실패하면 details 없는 400이 된다).
 */
@Schema(description = "팀 목표 만들기 요청")
public record CreateTeamChallengeRequest(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "팀에 공유된 분석 ID",
                        example = "42")
                @NotNull(message = "목표를 정할 분석을 골라 주세요.")
                @Min(value = 1, message = "목표를 정할 분석을 골라 주세요.")
                Long analysisId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        type = "string",
                        format = "date",
                        description = "시작일 (YYYY-MM-DD, 오늘 이후)",
                        example = "2026-11-25")
                String startDate,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        type = "string",
                        format = "date",
                        description = "종료일 (YYYY-MM-DD, 시작일 이후, 기간은 양끝 포함 최대 30일)",
                        example = "2026-12-01")
                String endDate,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "목표 정답률 (%)",
                        minimum = "1",
                        maximum = "100",
                        example = "80")
                @NotNull(message = "목표 정답률을 입력해 주세요.")
                @Min(value = 1, message = "목표 정답률은 1~100 사이여야 합니다.")
                @Max(value = 100, message = "목표 정답률은 1~100 사이여야 합니다.")
                Integer targetRate) {}
