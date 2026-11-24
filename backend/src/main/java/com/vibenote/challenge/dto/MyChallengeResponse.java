package com.vibenote.challenge.dto;

import com.vibenote.challenge.ChallengeGoalType;
import com.vibenote.challenge.ChallengeStatus;
import com.vibenote.challenge.domain.JoinedChallenge;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 회원이 참여한 챌린지 항목 (F-25 AC10·AC11): 챌린지 요약과 그 회원의 달성도. */
@Schema(description = "참여한 챌린지 항목")
public record MyChallengeResponse(
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
                        example = "2026-11-20")
                LocalDate startDate,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "종료일 (포함)",
                        example = "2026-11-30")
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
                        example = "ONGOING")
                ChallengeStatus status,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "달성도 (기간 안 활동 수, 목표값을 넘지 않는다)",
                        example = "3")
                int progress,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "달성 시각 (미달성이면 null)",
                        example = "2026-11-24T10:15:00+09:00")
                OffsetDateTime completedAt,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "참여 시각",
                        example = "2026-11-22T21:05:00+09:00")
                OffsetDateTime joinedAt) {

    public static MyChallengeResponse of(JoinedChallenge challenge, LocalDate today) {
        return new MyChallengeResponse(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getImageUrl(),
                challenge.getStartDate(),
                challenge.getEndDate(),
                challenge.getGoalType(),
                challenge.getGoalValue(),
                challenge.statusOn(today),
                challenge.getProgress(),
                ChallengeProgressResponse.atSeoul(challenge.getCompletedAt()),
                ChallengeProgressResponse.atSeoul(challenge.getJoinedAt()));
    }
}
