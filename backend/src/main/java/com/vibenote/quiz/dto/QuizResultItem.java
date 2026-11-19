package com.vibenote.quiz.dto;

import com.vibenote.concept.dto.ConceptSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 채점 결과의 문항 하나 (F-14 AC6). */
@Schema(description = "문항별 채점 결과")
public record QuizResultItem(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "문항 ID", example = "22")
                long questionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "문항 번호", example = "2")
                int no,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "질문",
                        example = "JwtTokenFilter가 Authorization 헤더에서 토큰을 꺼낸 뒤 하는 일로 알맞은 것은?")
                String question,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "선택지 4개")
                List<String> choices,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "내가 고른 위치",
                        example = "3")
                int selectedIndex,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "정답 위치", example = "1")
                int answerIndex,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "맞혔는지",
                        example = "false")
                boolean correct,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "해설",
                        example =
                                "jwtService.getSubFromToken()으로 사용자 id를 꺼내 SecurityContextHolder에"
                                        + " 넣습니다.")
                String explanation,
        @Schema(description = "관련 개념 카드. 없거나 공개되지 않은 카드면 null") ConceptSummaryResponse concept) {}
