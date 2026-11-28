package com.vibenote.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 오답 노트 (L-1b AC5~AC8). 마지막 답이 틀린 문항을 개념별로 묶는다. */
@Schema(description = "오답 노트")
public record WrongNoteResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "마지막 답이 틀린 문항 전체 수 (groups에는 최근 200개까지만 담는다)",
                        example = "3")
                int totalCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description =
                                "개념별 묶음. 공개 개념 묶음은 문항 수 내림차순 → 가장 최근 lastAnsweredAt 내림차순 → 개념 id"
                                        + " 오름차순, 개념이 없거나 공개되지 않은 문항 묶음(concept null)은 맨 뒤. 없으면 []")
                List<WrongNoteGroupResponse> groups) {}
