package com.vibenote.llm;

/**
 * 모든 LLM 호출의 진입점. 구현체는 {@code llm.provider} 값으로 고른다.
 *
 * <ul>
 *   <li>{@code mock} (기본, 테스트): {@link MockLlmClient}
 *   <li>{@code gemini}: GeminiLlmClient — 실패·한도 초과 시 Ollama로 폴백 (분석 파이프라인 spec에서 추가)
 *   <li>{@code ollama}: OllamaLlmClient (분석 파이프라인 spec에서 추가)
 * </ul>
 */
public interface LlmClient {

    LlmResponse complete(LlmRequest request);
}
