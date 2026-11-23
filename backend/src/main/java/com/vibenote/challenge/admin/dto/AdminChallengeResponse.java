package com.vibenote.challenge.admin.dto;

import com.vibenote.challenge.ChallengeGoalType;
import com.vibenote.challenge.ChallengeStatus;
import com.vibenote.challenge.domain.Challenge;
import com.vibenote.common.config.ClockConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 관리자 챌린지 상세 (F-24 AC5). 수정 응답도 이 형식이다. */
@Schema(description = "관리자 챌린지 상세")
public record AdminChallengeResponse(
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
                        description = "설명 (없으면 null)",
                        example = "분석한 프로젝트의 퀴즈를 12월 안에 10번 풀어 보세요.")
                String description,
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
                long participantCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "등록 시각",
                        example = "2026-11-24T08:50:00+09:00")
                OffsetDateTime createdAt,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "수정 시각",
                        example = "2026-11-24T08:50:00+09:00")
                OffsetDateTime updatedAt) {

    public static AdminChallengeResponse of(Challenge challenge, LocalDate today) {
        return new AdminChallengeResponse(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getDescription(),
                challenge.getImageUrl(),
                challenge.getStartDate(),
                challenge.getEndDate(),
                challenge.getGoalType(),
                challenge.getGoalValue(),
                challenge.statusOn(today),
                challenge.getParticipantCount(),
                challenge.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime(),
                challenge.getUpdatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
