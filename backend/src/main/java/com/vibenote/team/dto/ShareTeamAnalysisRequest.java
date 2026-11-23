package com.vibenote.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 내 완료 분석을 팀에 공유 요청 (F-20b AC1·AC2). 검증은 멤버 확인 뒤 Service가 한다. */
@Schema(description = "팀에 분석 공유 요청")
public record ShareTeamAnalysisRequest(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "공유할 내 분석 ID (DONE, 퀴즈 있음)",
                        example = "42")
                @NotNull(message = "공유할 분석을 골라 주세요.")
                @Min(value = 1, message = "공유할 분석을 골라 주세요.")
                Long analysisId) {}
