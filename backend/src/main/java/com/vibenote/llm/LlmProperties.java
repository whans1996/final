package com.vibenote.llm;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * LLM 설정 ({@code llm.*}). 기본값은 {@code application.yml}에 있고 환경변수로 바꾼다. 키는 환경변수 {@code
 * GEMINI_API_KEY}로만 받는다.
 *
 * @param provider {@code mock}(기본) | {@code gemini} | {@code ollama}
 * @param gemini Gemini 설정. provider=gemini일 때만 쓴다
 * @param ollama Ollama 설정. provider=ollama이거나 Gemini 폴백 대상일 때 쓴다
 */
@ConfigurationProperties("llm")
public record LlmProperties(
        @DefaultValue("mock") String provider,
        @DefaultValue Gemini gemini,
        @DefaultValue Ollama ollama) {

    /**
     * @param apiKey Gemini API 키. {@code x-goog-api-key} 헤더로만 보낸다
     * @param model 모델 이름 (예: {@code gemini-3.5-flash-lite})
     * @param baseUrl {@code https://generativelanguage.googleapis.com}
     * @param timeout 응답 대기 시간. 넘으면 Ollama로 폴백한다
     */
    public record Gemini(
            String apiKey,
            String model,
            @DefaultValue("https://generativelanguage.googleapis.com") String baseUrl,
            @DefaultValue("60s") Duration timeout) {

        /** 설정을 로그로 찍어도 키가 남지 않게 가린다. */
        @Override
        public String toString() {
            return "Gemini[apiKey=***, model=%s, baseUrl=%s, timeout=%s]"
                    .formatted(model, baseUrl, timeout);
        }
    }

    /**
     * @param baseUrl Ollama 서버 주소 (예: {@code http://localhost:11434})
     * @param model 모델 이름 (예: {@code qwen2.5-coder:7b})
     * @param timeout 응답 대기 시간. 로컬 모델은 느리므로 Gemini보다 길게 둔다
     */
    public record Ollama(
            @DefaultValue("http://localhost:11434") String baseUrl,
            String model,
            @DefaultValue("120s") Duration timeout) {}
}
