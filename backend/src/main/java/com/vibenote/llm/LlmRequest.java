package com.vibenote.llm;

import com.vibenote.common.Level;

/**
 * LLM 호출 요청.
 *
 * @param stage 분석 단계
 * @param level 설명 수준
 * @param prompt 템플릿을 채운 최종 프롬프트 (출력은 JSON으로 요구한다)
 */
public record LlmRequest(LlmStage stage, Level level, String prompt) {}
