package com.vibenote.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** 오답 노트의 문항 하나 (L-1b AC5·AC6). 선택지·위치는 저장된 순서 기준이다. */
@Schema(description = "마지막 답이 틀린 문항")
public record WrongNoteItemResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "문항 ID", example = "22")
                long questionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "퀴즈 ID", example = "3")
                long quizId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분석 ID", example = "7")
                long analysisId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "분석 제목 (없으면 프로젝트 이름, 둘 다 없으면 \"제목 없는 분석\")",
                        example = "JWT 기반 인증")
                String analysisTitle,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "질문",
                        example = "JwtTokenFilter가 Authorization 헤더에서 토큰을 꺼낸 뒤 하는 일로 알맞은 것은?")
                String question,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "선택지 4개")
                List<String> choices,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "마지막으로 고른 위치",
                        example = "3")
                int selectedIndex,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "정답 위치", example = "1")
                int answerIndex,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "해설",
                        example =
                                "jwtService.getSubFromToken()으로 사용자 id를 꺼내 SecurityContextHolder에"
                                        + " 넣습니다.")
                String explanation,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "내가 이 문항을 틀린 횟수",
                        example = "2")
                int wrongCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "마지막으로 답한 시각",
                        example = "2026-11-30T21:14:03+09:00")
                OffsetDateTime lastAnsweredAt) {}
