package com.vibenote.concept.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/** 상태별 카드 수 (F-13 AC2). 카드가 없는 상태도 0으로 세 키를 모두 준다. */
@Schema(description = "상태별 카드 수")
public record AdminConceptStatusCountsResponse(
        @JsonProperty("DRAFT")
                @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "검수 대기",
                        example = "3")
                long draft,
        @JsonProperty("PUBLISHED")
                @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "공개",
                        example = "85")
                long published,
        @JsonProperty("HIDDEN")
                @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "숨김",
                        example = "2")
                long hidden) {}
