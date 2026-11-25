package com.vibenote.llm;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * LLM 설정 ({@code llm.*}). 기본값은 {@code application.yml}에 있고 환경변수로 바꾼다. 키는 환경변수 {@code
 * GMS_API_KEY}·{@code GEMINI_API_KEY}로만 받는다.
 *
 * @param provider {@code mock}(기본) | {@code gms} | {@code ollama} | {@code gemini}(평가 비교용, 폴백 없음)
 * @param gms SSAFY GMS(OpenAI 호환) 설정. provider=gms일 때 쓴다 (F-27)
 * @param gemini Gemini 설정. provider=gemini일 때만 쓴다
 * @param ollama Ollama 설정. provider=ollama이거나 GMS 폴백 대상일 때 쓴다
 */
@ConfigurationProperties("llm")
public record LlmProperties(
        @DefaultValue("mock") String provider,
        @DefaultValue Gms gms,
        @DefaultValue Gemini gemini,
        @DefaultValue Ollama ollama) {

    /**
     * @param apiKey GMS 키. {@code Authorization: Bearer} 헤더로만 보낸다
     * @param baseUrl OpenAI 호환 경로 (예: {@code https://gms.ssafy.io/gmsapi/api.openai.com/v1})
     * @param model 기본 모델 (예: {@code gpt-4.1-mini})
     * @param stageModels 단계별 모델 ({@link LlmStage#key()} → 모델). 없는 단계는 {@code model}을 쓴다 (AC4)
     * @param timeout 응답 대기 시간. 넘으면 Ollama로 폴백한다
     */
    public record Gms(
            String apiKey,
            @DefaultValue("https://gms.ssafy.io/gmsapi/api.openai.com/v1") String baseUrl,
            String model,
            Map<String, String> stageModels,
            @DefaultValue("60s") Duration timeout) {

        public Gms {
            stageModels = stageModels == null ? Map.of() : Map.copyOf(stageModels);
        }

        /** 설정을 로그로 찍어도 키가 남지 않게 가린다 (AC2). */
        @Override
        public String toString() {
            return "Gms[apiKey=***, baseUrl=%s, model=%s, stageModels=%s, timeout=%s]"
                    .formatted(baseUrl, model, stageModels, timeout);
        }
    }

    /**
     * @param apiKey Gemini API 키. {@code x-goog-api-key} 헤더로만 보낸다
     * @param model 모델 이름 (예: {@code gemini-3.5-flash-lite})
     * @param baseUrl {@code https://generativelanguage.googleapis.com}
     * @param timeout 응답 대기 시간
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
     * @param timeout 응답 대기 시간. 로컬 모델은 느리므로 GMS보다 길게 둔다
     * @param numCtx 컨텍스트 길이(토큰). 기본값(약 4K)으로는 F-06 개요·묶음 프롬프트가 잘린다 (F-06 AC12)
     */
    public record Ollama(
            @DefaultValue("http://localhost:11434") String baseUrl,
            String model,
            @DefaultValue("120s") Duration timeout,
            @DefaultValue("16384") int numCtx) {}
}
