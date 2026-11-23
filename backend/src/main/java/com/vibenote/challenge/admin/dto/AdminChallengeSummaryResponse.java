package com.vibenote.challenge.admin.dto;

import com.vibenote.challenge.ChallengeGoalType;
import com.vibenote.challenge.ChallengeStatus;
import com.vibenote.challenge.domain.Challenge;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** 관리자 챌린지 목록 한 행 (F-24 AC4). 설명·시각은 넣지 않는다. */
@Schema(description = "관리자 챌린지 목록 항목")
public record AdminChallengeSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "챌린지 ID", example = "7")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "제목",
                        example = "12월 퀴즈 챌린지")
                String title,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "대표 이미지 공개 주소 (없으면 null)",
                        example = "/uploads/challenges/3f2b9c1e-8a4d-4f7e-9b1a-2c5d6e7f8a90.png")
                String imageUrl,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "시작일",
                        example = "2026-12-01")
                LocalDate startDate,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "종료일 (포함)",
                        example = "2026-12-31")
                LocalDate endDate,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description =
                                "목표 유형 (REVIEW_COUNT 개념 복습, QUIZ_COUNT 퀴즈, ANALYSIS_COUNT 분석)",
                        example = "QUIZ_COUNT")
                ChallengeGoalType goalType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "목표값", example = "10")
                int goalValue,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description =
                                "상태 (UPCOMING 예정, ONGOING 진행 중, ENDED 종료). 오늘(Asia/Seoul) 기준으로 계산",
                        example = "UPCOMING")
                ChallengeStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "참여자 수", example = "0")
                long participantCount) {

    public static AdminChallengeSummaryResponse of(Challenge challenge, LocalDate today) {
        return new AdminChallengeSummaryResponse(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getImageUrl(),
                challenge.getStartDate(),
                challenge.getEndDate(),
                challenge.getGoalType(),
                challenge.getGoalValue(),
                challenge.statusOn(today),
                challenge.getParticipantCount());
    }
}
