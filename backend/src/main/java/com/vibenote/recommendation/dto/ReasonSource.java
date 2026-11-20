package com.vibenote.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 추천 이유의 출처. */
@Schema(description = "추천 이유 출처 (AI: LLM이 만든 이유(캐시 포함), RULE: LLM 실패 시 규칙 문장)")
public enum ReasonSource {
    AI,
    RULE
}
