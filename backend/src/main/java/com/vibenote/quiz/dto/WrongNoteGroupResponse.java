package com.vibenote.quiz.dto;

import com.vibenote.concept.dto.ConceptSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 오답 노트의 개념 묶음 하나 (L-1b AC6). */
@Schema(description = "오답 노트의 개념 묶음")
public record WrongNoteGroupResponse(
        @Schema(description = "관련 개념 카드. 개념이 없거나 공개되지 않은 문항 묶음이면 null")
                ConceptSummaryResponse concept,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "문항 (lastAnsweredAt 내림차순 → questionId 오름차순)")
                List<WrongNoteItemResponse> items) {}
