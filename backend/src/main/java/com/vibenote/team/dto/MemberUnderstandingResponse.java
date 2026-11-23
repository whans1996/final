package com.vibenote.team.dto;

import com.vibenote.quiz.dto.LastAttemptResponse;
import com.vibenote.team.TeamRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 멤버별 최근 풀이 결과 (F-20b AC6). 안 풀었으면 lastAttempt null, modules 빈 배열. */
@Schema(description = "멤버별 이해도")
public record MemberUnderstandingResponse(
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
        @Schema(description = "가장 최근 풀이 (안 풀었으면 null)") LastAttemptResponse lastAttempt,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "레이어별 결과 (모듈 순서, 안 풀었으면 빈 배열)")
                List<MemberModuleResponse> modules) {}
