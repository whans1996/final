package com.vibenote.llm;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * LLM 공급자 하나가 응답하지 못함 (HTTP 4xx·5xx, 타임아웃, 연결 실패, 응답 형식 이상). {@link FallbackLlmClient}가 받아 다음 공급자로
 * 넘기거나 {@code LLM_503}으로 바꾼다.
 *
 * <p>메시지는 {@code "<공급자> <상태 코드 또는 예외 종류>"}만 담고 원인 예외를 붙이지 않는다. 원인 메시지에는 요청 URL·응답 본문이 들어 있어 키나
 * 프롬프트가 로그·{@code llm_usage_logs.error_message}로 새어 나갈 수 있기 때문이다 (F-00 AC5).
 */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message) {
        super(message);
    }

    /**
     * RestClient 예외를 상태 코드(예: {@code Gemini 429}) 또는 원인 종류(예: {@code Gemini
     * SocketTimeoutException})로 줄인다.
     */
    static LlmUnavailableException of(String provider, RestClientException e) {
        if (e instanceof RestClientResponseException response) {
            return new LlmUnavailableException(provider + " " + response.getStatusCode().value());
        }
        if (e instanceof ResourceAccessException && e.getCause() != null) {
            return new LlmUnavailableException(
                    provider + " " + e.getCause().getClass().getSimpleName());
        }
        return new LlmUnavailableException(provider + " " + e.getClass().getSimpleName());
    }

    static LlmUnavailableException malformed(String provider) {
        return new LlmUnavailableException(provider + " 응답 형식 오류");
    }
}
