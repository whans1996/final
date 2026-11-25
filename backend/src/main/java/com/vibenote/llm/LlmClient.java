package com.vibenote.llm;

/**
 * 모든 LLM 호출의 진입점. 빈은 {@link LlmConfig}가 {@code llm.provider} 값으로 하나만 조립한다.
 *
 * <ul>
 *   <li>{@code mock} (기본, 테스트): {@link MockLlmClient}
 *   <li>{@code gms} (서비스): {@link GmsLlmClient} — 실패(4xx·5xx·타임아웃·거부·잘림) 시 재시도 없이 {@link
 *       OllamaLlmClient}(간이 모드)로 폴백 (F-27)
 *   <li>{@code ollama}: {@link OllamaLlmClient}
 *   <li>{@code gemini} (평가 비교용): {@link GeminiLlmClient} — 폴백하지 않는다
 * </ul>
 *
 * <p>폴백까지 모두 실패하면 {@code BusinessException(LLM_503)}을 던진다. 호출 시도마다 {@code llm_usage_logs}에 기록된다.
 *
 * <p><b>{@code @Transactional} 안에서 부르지 않는다.</b> 사용량 기록이 호출자 트랜잭션에 참여하므로, 호출자가 롤백하면 기록도 사라지고 응답을
 * 기다리는 동안(최대 GMS 60초 + Ollama 120초) DB 커넥션을 잡는다.
 */
public interface LlmClient {

    LlmResponse complete(LlmRequest request);
}
