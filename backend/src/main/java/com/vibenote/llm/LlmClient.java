package com.vibenote.llm;

/**
 * 모든 LLM 호출의 진입점. 빈은 {@link LlmConfig}가 {@code llm.provider} 값으로 하나만 조립한다.
 *
 * <ul>
 *   <li>{@code mock} (기본, 테스트): {@link MockLlmClient}
 *   <li>{@code gemini}: {@link GeminiLlmClient} — 실패·한도 초과 시 재시도 없이 {@link OllamaLlmClient}로 폴백
 *   <li>{@code ollama}: {@link OllamaLlmClient}
 * </ul>
 *
 * <p>폴백까지 모두 실패하면 {@code BusinessException(LLM_503)}을 던진다. 호출 시도마다 {@code llm_usage_logs}에 기록된다.
 */
public interface LlmClient {

    LlmResponse complete(LlmRequest request);
}
