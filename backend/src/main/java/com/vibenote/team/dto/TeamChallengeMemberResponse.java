package com.vibenote.team.dto;

import com.vibenote.quiz.dto.LastAttemptResponse;
import com.vibenote.team.TeamRole;
import io.swagger.v3.oas.annotations.media.Schema;

/** 팀 목표의 멤버 한 명 (F-20c AC5). attempt는 기간 안의 가장 최근 풀이다. */
@Schema(description = "팀 목표의 멤버별 진행")
public record TeamChallengeMemberResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "회원 ID", example = "3")
                long userId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "닉네임",
                        example = "vibecoder")
                String nickname,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "역할",
                        allowableValues = {"OWNER", "MEMBER"},
                        example = "OWNER")
                TeamRole role,
        @Schema(description = "기간 안의 가장 최근 풀이 (없으면 null)") LastAttemptResponse attempt,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "달성 여부 (score × 100 ≥ 목표 정답률 × total)",
                        example = "false")
                boolean achieved) {}
