package com.vibenote.llm;

/**
 * LLM 호출 결과. 호출부가 {@code content}를 JSON으로 파싱하고, 실패하면 1회 재시도한다 (backend/CLAUDE.md "LLM").
 *
 * @param content 모델이 돌려준 원문 (JSON 문자열이어야 한다)
 * @param model 실제로 응답한 모델 이름 (폴백 여부 확인용, llm_usage_logs에 기록)
 * @param inputTokens 입력 토큰 수 (모르면 0)
 * @param outputTokens 출력 토큰 수 (모르면 0)
 */
public record LlmResponse(String content, String model, int inputTokens, int outputTokens) {}
