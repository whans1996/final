package com.vibenote.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Gemini REST API ({@code POST /v1beta/models/{model}:generateContent}). 빈이 아니라 {@link LlmConfig}가
 * 만든다.
 *
 * <p>키는 쿼리 파라미터가 아니라 {@code x-goog-api-key} 헤더로 보낸다 (URL이 예외 메시지·접근 로그에 남아도 키가 새지 않게). 실패는 재시도 없이
 * {@link LlmUnavailableException}으로 바꿔 던진다.
 */
public class GeminiLlmClient implements LlmClient {

    private static final String PROVIDER = "Gemini";

    private final RestClient restClient;
    private final String model;

    /**
     * @param builder 타임아웃이 설정된 빌더 ({@link LlmConfig}). 테스트는 MockRestServiceServer를 묶은 빌더를 넘긴다
     */
    public GeminiLlmClient(RestClient.Builder builder, LlmProperties.Gemini props) {
        this.restClient =
                builder.baseUrl(props.baseUrl())
                        .defaultHeader("x-goog-api-key", props.apiKey())
                        .build();
        this.model = props.model();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        JsonNode body;
        try {
            body =
                    restClient
                            .post()
                            .uri("/v1beta/models/{model}:generateContent", model)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(requestBody(request.prompt()))
                            .retrieve()
                            .body(JsonNode.class);
        } catch (RestClientException e) {
            throw LlmUnavailableException.of(PROVIDER, e);
        }
        if (body == null) {
            throw LlmUnavailableException.malformed(PROVIDER);
        }
        // 안전 필터 차단 등으로 후보가 없으면 text가 없다
        JsonNode text = body.at("/candidates/0/content/parts/0/text");
        if (!text.isTextual()) {
            throw LlmUnavailableException.malformed(PROVIDER);
        }
        JsonNode usage = body.path("usageMetadata");
        return new LlmResponse(
                text.asText(),
                model,
                usage.path("promptTokenCount").asInt(),
                usage.path("candidatesTokenCount").asInt());
    }

    private static Map<String, Object> requestBody(String prompt) {
        return Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json"));
    }
}
