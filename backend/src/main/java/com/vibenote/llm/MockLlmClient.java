package com.vibenote.llm;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 개발·테스트용 LLM. 외부 호출 없이 {@code classpath:llm-fixtures/<stage>.json}을 돌려준다. 빈이 아니라 {@link
 * LlmConfig}가 만든다.
 *
 * <p>결정적이어야 하므로 랜덤을 쓰지 않는다. fixture는 dev·demo 데이터로만 만든다 (eval 데이터 금지, CLAUDE.md 11번).
 *
 * <p>CONCEPT 단계만 fixture를 고쳐서 돌려준다: 기존 카드를 DB id 대신 이름({@code conceptName})으로 적고, 프롬프트의 카드 목록에서 id를
 * 찾아 바꾼다 ({@link #linkConceptNames}). 시드 카드 id는 DB마다 다르기 때문이다 (#49).
 */
public class MockLlmClient implements LlmClient {

    static final String MODEL = "mock";

    /** EXTRACTING 프롬프트 카드 목록의 {@code id | 이름 | 카테고리} 줄 ({@code ConceptPrompts.input}). */
    private static final Pattern CATALOG_LINE =
            Pattern.compile("(?m)^(\\d+) \\| (.+) \\| [^|\\r\\n]+$");

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** fixture 경로({@code llm-fixtures/<stage>.json}) → 내용. */
    private final Function<String, String> fixtures;

    public MockLlmClient() {
        this(MockLlmClient::read);
    }

    /** 테스트용: classpath 대신 주어진 함수로 fixture를 읽는다. */
    MockLlmClient(Function<String, String> fixtures) {
        this.fixtures = fixtures;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String content = fixtures.apply("llm-fixtures/" + request.stage().key() + ".json");
        if (request.stage() == LlmStage.CONCEPT) {
            content = linkConceptNames(content, request.prompt());
        }
        return new LlmResponse(content, MODEL, 0, 0);
    }

    private static String read(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new IllegalStateException("Mock LLM fixture가 없습니다: " + path);
        }
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * concept fixture의 {@code concepts[].conceptName}을 프롬프트 카드 목록에서 같은 이름의 카드 id({@code
     * conceptId})로 바꾼다. 목록에 없는 이름의 항목은 뺀다 (모델이 연결하지 않은 것처럼). {@code conceptId}·{@code new} 항목은 그대로
     * 둔다.
     *
     * @param fixture concept 단계 fixture JSON
     * @param prompt EXTRACTING 프롬프트 (카드 목록 줄을 포함)
     */
    public static String linkConceptNames(String fixture, String prompt) {
        Map<String, Long> idByName = new HashMap<>();
        Matcher m = CATALOG_LINE.matcher(prompt == null ? "" : prompt);
        while (m.find()) {
            idByName.putIfAbsent(m.group(2), Long.parseLong(m.group(1)));
        }
        JsonNode root = JSON.readTree(fixture);
        if (!(root instanceof ObjectNode obj) || !root.path("concepts").isArray()) {
            return fixture;
        }
        ArrayNode linked = JSON.createArrayNode();
        for (JsonNode item : root.path("concepts")) {
            JsonNode name = item.get("conceptName");
            if (name == null || !(item instanceof ObjectNode entry)) {
                linked.add(item);
                continue;
            }
            Long id = idByName.get(name.asString());
            if (id == null) {
                continue;
            }
            ObjectNode copy = entry.deepCopy();
            copy.remove("conceptName");
            copy.put("conceptId", id);
            linked.add(copy);
        }
        obj.set("concepts", linked);
        return JSON.writeValueAsString(obj);
    }
}
