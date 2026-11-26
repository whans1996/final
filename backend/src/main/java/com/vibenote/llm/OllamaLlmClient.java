package com.vibenote.llm;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * 로컬 Ollama REST API ({@code POST /api/generate}, 구조화 출력, 비스트리밍). 빈이 아니라 {@link LlmConfig}가 만든다.
 *
 * <p>GMS 폴백 대상이거나 provider=ollama일 때 쓴다. {@code format}에 단계 출력 스키마를 넘겨 형식을 강제한다 (F-27 AC6). 실패는
 * {@link LlmUnavailableException}으로 바꿔 던진다.
 */
public class OllamaLlmClient implements LlmClient {

    private static final String PROVIDER = "Ollama";

    private final RestClient restClient;
    private final String model;

    /** 컨텍스트 길이. 기본(약 4K 토큰)으로는 F-06 개요·묶음 프롬프트가 잘린다 (F-06 AC12). */
    private final int numCtx;

    private final LlmSchemas schemas;

    /**
     * @param builder 타임아웃이 설정된 빌더 ({@link LlmConfig}). 테스트는 MockRestServiceServer를 묶은 빌더를 넘긴다
     */
    public OllamaLlmClient(
            RestClient.Builder builder, LlmProperties.Ollama props, LlmSchemas schemas) {
        this.restClient = builder.baseUrl(props.baseUrl()).build();
        this.model = props.model();
        this.numCtx = props.numCtx();
        this.schemas = schemas;
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
                                            schemas.of(request.stage()),
                                            "stream",
                                            false,
                                            "options",
                                            Map.of("num_ctx", numCtx)))
                            .retrieve()
                            .body(JsonNode.class);
        } catch (RestClientException e) {
            throw LlmUnavailableException.of(PROVIDER, e);
        }
        if (body == null || !body.path("response").isString()) {
            throw LlmUnavailableException.malformed(PROVIDER);
        }
        return new LlmResponse(
                body.path("response").asString(),
                model,
                body.path("prompt_eval_count").asInt(0),
                body.path("eval_count").asInt(0));
    }
}
