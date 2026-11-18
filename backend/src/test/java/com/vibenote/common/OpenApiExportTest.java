package com.vibenote.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Separators;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * 실제 앱의 {@code /v3/api-docs}를 키 정렬 JSON으로 {@code docs/api/openapi.json}에 저장한다.
 *
 * <p>API를 바꾸면 이 테스트가 파일을 갱신하므로 같은 커밋에 포함한다. CI는 테스트 후 파일이 달라지면 실패한다.
 */
class OpenApiExportTest extends IntegrationTestBase {

    private static final Path OUTPUT = Path.of("..", "docs", "api", "openapi.json");

    @Test
    void openapi_json을_추출한다() throws Exception {
        String body =
                mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);

        ObjectMapper sorted =
                JsonMapper.builder().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();
        Map<?, ?> tree = sorted.readValue(body, Map.class);
        assertThat(tree.get("info")).asString().contains("VibeNote API");

        // 운영체제와 상관없이 LF 줄바꿈으로 저장해야 Windows와 CI(Linux)의 결과가 같다.
        // `"key" : value` 구분자는 Jackson 2 기본 형식을 명시해 라이브러리가 바뀌어도 파일 전체 diff가 생기지 않게 한다.
        Separators separators =
                Separators.createDefaultInstance()
                        .withObjectNameValueSpacing(Separators.Spacing.BOTH);
        DefaultPrettyPrinter printer =
                new DefaultPrettyPrinter(separators)
                        .withObjectIndenter(new DefaultIndenter("  ", "\n"));
        String json = sorted.writer().with(printer).writeValueAsString(tree) + "\n";

        if (!Files.exists(OUTPUT) || !Files.readString(OUTPUT).equals(json)) {
            Files.writeString(OUTPUT, json, StandardCharsets.UTF_8);
        }
    }
}
