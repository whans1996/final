package com.vibenote.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.concept.ConceptCategories;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 단계별 출력 스키마({@code llm-schemas/<stage>.json})가 OpenAI strict 규칙을 지키고, Mock fixture가 그 스키마에 맞는지 본다
 * (F-27 AC7).
 *
 * <p>스키마 검사기는 이 테스트가 쓰는 키워드({@code type}·{@code properties}·{@code required}·{@code
 * additionalProperties}·{@code items}·{@code anyOf}·{@code enum})만 다룬다 (의존성 추가 없음, spec 7절).
 */
class LlmSchemasTest {

    static final JsonMapper JSON = JsonMapper.builder().build();

    /** fixture 위치. Gradle 테스트의 작업 폴더는 backend다. */
    static final List<Path> FIXTURE_DIRS =
            List.of(
                    Path.of("src/main/resources/llm-fixtures"),
                    Path.of("src/test/resources/llm-fixtures"));

    final LlmSchemas schemas = LlmSchemas.load();

    @Test
    void AC7_모든_단계의_스키마가_있다() {
        for (LlmStage stage : LlmStage.values()) {
            assertThat(schemas.of(stage)).as(stage.key()).isNotNull();
            assertThat(schemas.of(stage).path("type").asString()).isEqualTo("object");
        }
    }

    @Test
    void AC7_모든_object_노드가_strict_규칙을_지킨다() {
        for (LlmStage stage : LlmStage.values()) {
            List<String> errors = new ArrayList<>();
            checkStrict(schemas.of(stage), "$", errors);
            assertThat(errors).as(stage.key()).isEmpty();
        }
    }

    static Stream<Path> fixtures() throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path dir : FIXTURE_DIRS) {
            try (Stream<Path> list = Files.list(dir)) {
                list.filter(p -> p.toString().endsWith(".json")).forEach(files::add);
            }
        }
        return files.stream().sorted();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void AC7_Mock_fixture가_단계_스키마에_맞는다(Path fixture) throws IOException {
        String name = fixture.getFileName().toString();
        LlmStage stage = LlmStage.valueOf(name.replace(".json", "").toUpperCase());
        String content = Files.readString(fixture, StandardCharsets.UTF_8);

        // Mock은 fixture의 이름(conceptName)을 프롬프트 목록의 id로 바꿔 돌려준다. 파서가 받는 것은 그 결과다
        String output =
                new MockLlmClient(path -> content)
                        .complete(new LlmRequest(stage, null, promptFor(stage, content)))
                        .content();

        List<String> errors = new ArrayList<>();
        validate(schemas.of(stage), JSON.readTree(output), "$", errors);
        assertThat(errors).as(fixture.toString()).isEmpty();
    }

    /** fixture에 나오는 이름이 모두 연결되도록 Mock이 읽는 목록 줄을 만든다. */
    static String promptFor(LlmStage stage, String fixture) {
        List<String> names = new ArrayList<>();
        JSON.readTree(fixture).findValues("conceptName").forEach(n -> names.add(n.asString()));
        StringBuilder prompt = new StringBuilder();
        for (int i = 0; i < Math.max(names.size(), 3); i++) {
            String name = i < names.size() ? names.get(i) : "개념" + i;
            switch (stage) {
                case CONCEPT ->
                        prompt.append(i + 1).append(" | ").append(name).append(" | spring\n");
                case QUIZ -> prompt.append(i + 1).append(" | ").append(name).append('\n');
                case RECOMMEND ->
                        prompt.append(i + 1).append(" | ").append(name).append(" | web\n");
                default -> {}
            }
        }
        return prompt.toString();
    }

    /**
     * OpenAI strict: object마다 {@code additionalProperties:false}, {@code required}가 properties 키
     * 전부.
     */
    static void checkStrict(JsonNode schema, String at, List<String> errors) {
        if (types(schema).contains("object")) {
            if (!schema.path("additionalProperties").isBoolean()
                    || schema.path("additionalProperties").asBoolean()) {
                errors.add(at + ": additionalProperties:false 아님");
            }
            Set<String> keys = new HashSet<>(schema.path("properties").propertyNames());
            Set<String> required = new HashSet<>();
            schema.path("required").forEach(r -> required.add(r.asString()));
            if (keys.isEmpty() || !keys.equals(required)) {
                errors.add(at + ": required " + required + " ≠ properties " + keys);
            }
            for (String key : keys) {
                checkStrict(schema.path("properties").path(key), at + "." + key, errors);
            }
        }
        if (schema.has("items")) {
            checkStrict(schema.get("items"), at + "[]", errors);
        }
        int i = 0;
        for (JsonNode option : schema.path("anyOf")) {
            checkStrict(option, at + "|" + i++, errors);
        }
    }

    /**
     * 값이 스키마에 맞는지 본다. strict 모드는 선택 필드를 null로 채우지만 fixture는 그 전에 만든 것이라 생략할 수 있다. 그래서 null을 허용하는
     * 필드만 생략을 허용한다 (파서가 생략과 null을 같게 다룬다, AC8).
     */
    static void validate(JsonNode schema, JsonNode value, String at, List<String> errors) {
        if (schema.has("anyOf")) {
            for (JsonNode option : schema.get("anyOf")) {
                List<String> sub = new ArrayList<>();
                validate(option, value, at, sub);
                if (sub.isEmpty()) {
                    return;
                }
            }
            errors.add(at + ": anyOf 어느 것에도 맞지 않음");
            return;
        }
        Set<String> types = types(schema);
        String actual = typeOf(value);
        if (!types.contains(actual) && !(actual.equals("integer") && types.contains("number"))) {
            errors.add(at + ": " + types + " 아님 (" + actual + ")");
            return;
        }
        if (schema.has("enum") && actual.equals("string")) {
            Set<String> allowed = new HashSet<>();
            schema.get("enum").forEach(e -> allowed.add(e.asString()));
            if (!allowed.contains(value.asString())) {
                errors.add(at + ": enum 밖의 값 " + value.asString());
            }
        }
        if (actual.equals("object")) {
            JsonNode properties = schema.path("properties");
            for (String key : value.propertyNames()) {
                if (!properties.has(key)) {
                    errors.add(at + ": 스키마에 없는 키 " + key);
                }
            }
            for (JsonNode r : schema.path("required")) {
                String key = r.asString();
                if (!value.has(key) && !nullable(properties.path(key))) {
                    errors.add(at + ": 필수 키 없음 " + key);
                }
            }
            for (String key : properties.propertyNames()) {
                if (value.has(key)) {
                    validate(properties.get(key), value.get(key), at + "." + key, errors);
                }
            }
        } else if (actual.equals("array")) {
            int i = 0;
            for (JsonNode item : value) {
                validate(schema.get("items"), item, at + "[" + i++ + "]", errors);
            }
        }
    }

    static boolean nullable(JsonNode schema) {
        if (types(schema).contains("null")) {
            return true;
        }
        for (JsonNode option : schema.path("anyOf")) {
            if (types(option).contains("null")) {
                return true;
            }
        }
        return false;
    }

    static Set<String> types(JsonNode schema) {
        Set<String> types = new HashSet<>();
        JsonNode type = schema.path("type");
        if (type.isString()) {
            types.add(type.asString());
        } else {
            type.forEach(t -> types.add(t.asString()));
        }
        return types;
    }

    static String typeOf(JsonNode value) {
        if (value.isNull()) {
            return "null";
        }
        if (value.isObject()) {
            return "object";
        }
        if (value.isArray()) {
            return "array";
        }
        if (value.isString()) {
            return "string";
        }
        if (value.isBoolean()) {
            return "boolean";
        }
        return value.isIntegralNumber() ? "integer" : "number";
    }

    /** K-1 AC6(#163): 새 초안의 카테고리·난이도는 enum이고 수준별 요약 3개가 필수다. */
    @Test
    void K1_AC6_concept_스키마의_새_초안은_카테고리_난이도_enum과_수준_요약을_가진다() {
        JsonNode draft =
                schemas.of(LlmStage.CONCEPT)
                        .at("/properties/concepts/items/properties/new/anyOf/0");

        List<String> categories = new ArrayList<>();
        draft.at("/properties/category/enum").forEach(e -> categories.add(e.asString()));
        assertThat(categories).containsExactlyElementsOf(ConceptCategories.ORDERED);
        assertThat(draft.at("/properties/difficulty/enum").valueStream().map(JsonNode::asString))
                .containsExactly("BEGINNER", "JUNIOR", "SENIOR");

        JsonNode levels = draft.at("/properties/levelSummaries");
        assertThat(levels.path("type").asString()).isEqualTo("object");
        assertThat(levels.path("required").valueStream().map(JsonNode::asString))
                .containsExactlyInAnyOrder("beginner", "junior", "senior");
        assertThat(draft.path("required").valueStream().map(JsonNode::asString))
                .contains("levelSummaries");
    }

    @Test
    void K1_AC6_enum_밖의_값은_스키마_검사에서_걸린다() {
        JsonNode schema = JSON.readTree("{\"type\": \"string\", \"enum\": [\"a\"]}");
        List<String> errors = new ArrayList<>();

        validate(schema, JSON.readTree("\"b\""), "$", errors);

        assertThat(errors).hasSize(1);
    }
}
