package com.vibenote.analysis.concept;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 분석의 개념 목록 (F-08 AC7). 페이지가 아니며 items는 최대 15개다. */
@Schema(description = "분석에 연결된 개념 카드")
public record AnalysisConceptsResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "공개(PUBLISHED) 카드. 파일이 있는 것 먼저 파일 ID 순, 그다음 카드 ID 순")
                List<AnalysisConceptItem> items,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "관리자 검수를 기다리는 새 개념(AI 초안) 수",
                        example = "1")
                int pendingCount) {}
