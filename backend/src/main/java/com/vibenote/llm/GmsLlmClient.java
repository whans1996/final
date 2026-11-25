package com.vibenote.llm;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * SSAFY GMS(OpenAI 호환 게이트웨이)의 Chat Completions ({@code POST {base-url}/chat/completions}). 빈이 아니라
 * {@link LlmConfig}가 만든다 (F-27).
 *
 * <p>단계 스키마를 {@code response_format: json_schema, strict}로 보내 응답 형식을 강제한다. {@code
 * temperature}·{@code max_tokens}는 보내지 않는다 (gpt-5 계열은 기본값 외 temperature와 {@code max_tokens}를 거부한다).
 * 키는 {@code Authorization} 헤더로만 보낸다. 실패(4xx·5xx·타임아웃, 후보 없음, 거부, 길이 초과로 잘림)는 재시도 없이 {@link
 * LlmUnavailableException}으로 바꿔 던진다.
 */
public class GmsLlmClient implements LlmClient {

    static final String PROVIDER = "GMS";

    private final RestClient restClient;
    private final String model;
    private final Map<String, String> stageModels;
    private final LlmSchemas schemas;

    /**
     * @param builder 타임아웃이 설정된 빌더 ({@link LlmConfig}). 테스트는 MockRestServiceServer를 묶은 빌더를 넘긴다
     */
    public GmsLlmClient(RestClient.Builder builder, LlmProperties.Gms props, LlmSchemas schemas) {
        this.restClient =
                builder.baseUrl(props.baseUrl())
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
                        .build();
        this.model = props.model();
        this.stageModels = props.stageModels();
        this.schemas = schemas;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String stageModel = modelFor(request.stage());
        JsonNode body;
        try {
            body =
                    restClient
                            .post()
                            .uri("/chat/completions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(requestBody(stageModel, request))
                            .retrieve()
                            .body(JsonNode.class);
        } catch (RestClientException e) {
            throw LlmUnavailableException.of(PROVIDER, e);
        }
        if (body == null) {
            throw LlmUnavailableException.malformed(PROVIDER);
        }
        JsonNode choice = body.path("choices").path(0);
        JsonNode message = choice.path("message");
        // 거부 문장에는 프롬프트 일부가 들어 있을 수 있어 메시지에 넣지 않는다
        if (message.path("refusal").isString() && !message.path("refusal").asString().isBlank()) {
            throw new LlmUnavailableException(PROVIDER + " 응답 거부");
        }
        // 길이 초과로 잘린 JSON은 스키마를 지키지 못한다
        if ("length".equals(choice.path("finish_reason").asString(""))) {
            throw new LlmUnavailableException(PROVIDER + " 응답 잘림");
        }
        JsonNode content = message.path("content");
        if (!content.isString()) {
            throw LlmUnavailableException.malformed(PROVIDER);
        }
        JsonNode usage = body.path("usage");
        JsonNode respondedModel = body.path("model");
        return new LlmResponse(
                content.asString(),
                respondedModel.isString() && !respondedModel.asString().isBlank()
                        ? respondedModel.asString()
                        : stageModel,
                usage.path("prompt_tokens").asInt(0),
                usage.path("completion_tokens").asInt(0));
    }

    /** 그 단계에 보낼 모델. {@code stage-models}에 없으면 기본 모델 (AC4). */
    String modelFor(LlmStage stage) {
        return stageModels.getOrDefault(stage.key(), model);
    }

    private Map<String, Object> requestBody(String stageModel, LlmRequest request) {
        return Map.of(
                "model",
                stageModel,
                "messages",
                List.of(Map.of("role", "user", "content", request.prompt())),
                "response_format",
                Map.of(
                        "type",
                        "json_schema",
                        "json_schema",
                        Map.of(
                                "name",
                                request.stage().key(),
                                "strict",
                                true,
                                "schema",
                                schemas.of(request.stage()))));
    }
}
