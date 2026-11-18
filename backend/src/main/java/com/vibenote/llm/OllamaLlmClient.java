package com.vibenote.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 로컬 Ollama REST API ({@code POST /api/generate}, JSON 모드, 비스트리밍). 빈이 아니라 {@link LlmConfig}가 만든다.
 *
 * <p>Gemini 폴백 대상이거나 provider=ollama일 때 쓴다. 실패는 {@link LlmUnavailableException}으로 바꿔 던진다.
 */
public class OllamaLlmClient implements LlmClient {

    private static final String PROVIDER = "Ollama";

    private final RestClient restClient;
    private final String model;

    /**
     * @param builder 타임아웃이 설정된 빌더 ({@link LlmConfig}). 테스트는 MockRestServiceServer를 묶은 빌더를 넘긴다
     */
    public OllamaLlmClient(RestClient.Builder builder, LlmProperties.Ollama props) {
        this.restClient = builder.baseUrl(props.baseUrl()).build();
        this.model = props.model();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        JsonNode body;
        try {
            body =
                    restClient
                            .post()
                            .uri("/api/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(
                                    Map.of(
                                            "model",
                                            model,
                                            "prompt",
                                            request.prompt(),
                                            "format",
                                            "json",
                                            "stream",
                                            false))
                            .retrieve()
                            .body(JsonNode.class);
        } catch (RestClientException e) {
            throw LlmUnavailableException.of(PROVIDER, e);
        }
        if (body == null || !body.path("response").isTextual()) {
            throw LlmUnavailableException.malformed(PROVIDER);
        }
        return new LlmResponse(
                body.path("response").asText(),
                model,
                body.path("prompt_eval_count").asInt(),
                body.path("eval_count").asInt());
    }
}
