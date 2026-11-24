package com.vibenote.challenge.dto;

import com.vibenote.challenge.ChallengeGoalType;
import com.vibenote.challenge.ChallengeStatus;
import com.vibenote.challenge.domain.Challenge;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** 회원용 챌린지 상세 (F-25 AC2). */
@Schema(description = "챌린지 상세")
public record ChallengeDetailResponse(
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
                        example = "분석한 프로젝트의 퀴즈를 기간 안에 10번 풀어 보세요.")
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
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "참여자 수", example = "2")
                long participantCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "내 참여·달성도 (비로그인이거나 참여하지 않았으면 null)")
                ChallengeProgressResponse participation) {

    public static ChallengeDetailResponse of(
            Challenge challenge, LocalDate today, ChallengeProgressResponse participation) {
        return new ChallengeDetailResponse(
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
                participation);
    }
}
