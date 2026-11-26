package com.vibenote.llm;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 단계별 출력 JSON 스키마 ({@code classpath:llm-schemas/<stage>.json}, F-27 6절). {@link LlmConfig}가 기동 때 한
 * 번 읽어 GMS({@code response_format.json_schema}, strict)와 Ollama({@code format})에 넘긴다.
 *
 * <p>스키마는 OpenAI strict 규칙을 따른다: object마다 {@code additionalProperties:false}이고 {@code required}가 모든
 * 키다. 선택 필드는 {@code ["<type>","null"]}(객체는 {@code anyOf}에 null)로 두므로 모델이 null로 채운다. 파서는 생략과 null을
 * 같게 다룬다 (AC8). 프롬프트의 "출력 형식"과 같은 모양이어야 한다 ({@code LlmSchemasTest}가 Mock 출력으로 확인).
 */
public final class LlmSchemas {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Map<LlmStage, JsonNode> schemas;

    private LlmSchemas(Map<LlmStage, JsonNode> schemas) {
        this.schemas = schemas;
    }

    /** 6단계 스키마를 모두 읽는다. 하나라도 없으면 기동 실패. */
    public static LlmSchemas load() {
        Map<LlmStage, JsonNode> schemas = new EnumMap<>(LlmStage.class);
        for (LlmStage stage : LlmStage.values()) {
            String path = "llm-schemas/" + stage.key() + ".json";
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                throw new IllegalStateException("LLM 출력 스키마가 없습니다: " + path);
            }
            try (InputStream in = resource.getInputStream()) {
                schemas.put(stage, JSON.readTree(in));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return new LlmSchemas(schemas);
    }

    /** 그 단계의 스키마. 호출부가 고치지 않도록 매번 복사본을 준다. */
    public JsonNode of(LlmStage stage) {
        return schemas.get(stage).deepCopy();
    }
}
