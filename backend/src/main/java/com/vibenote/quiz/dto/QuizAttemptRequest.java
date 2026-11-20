package com.vibenote.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 풀이 제출 (F-14 AC6·AC8). 그 퀴즈의 문항마다 정확히 한 번씩 답한다 (어기면 QUIZ_400). */
@Schema(description = "풀이 제출")
public record QuizAttemptRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "문항별 답 (문항마다 한 번씩)")
                @NotEmpty(message = "답을 보내 주세요.")
                List<@NotNull(message = "답을 보내 주세요.") @Valid QuizAnswerRequest> answers) {}
