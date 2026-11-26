package com.vibenote.prompteval;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** E-1 AC11: 채점기 테스트의 합성 라벨·보고서는 dev 분할 저장소에서만 만든다 (평가 데이터 누수 방지, 절대 규칙 11). */
class EvalFixturesTest {

    static final Path RESOURCES = Path.of("src/test/resources/prompt-eval/synthetic");

    static String splitOf(String id) {
        JsonNode repo = PromptEvalRunner.dataset(id);
        return repo == null ? "없음" : repo.path("split").asString();
    }

    @Test
    void AC11_테스트_fixture는_dev_분할에서만_만든다() throws IOException {
        // 검사가 실제로 split을 읽는지: eval 저장소 id는 dev가 아니다
        assertThat(splitOf("DS-02-05")).isEqualTo("eval");
        assertThat(splitOf(GradeFixtures.FIXTURE)).isEqualTo("dev");

        List<Path> files;
        try (Stream<Path> walk = Files.walk(RESOURCES)) {
            files =
                    walk.filter(p -> p.toString().endsWith(".json"))
                            .filter(
                                    p ->
                                            p.getParent().endsWith("labels")
                                                    || p.getParent().endsWith("reports"))
                            .toList();
        }
        assertThat(files).isNotEmpty();
        for (Path file : files) {
            JsonNode json = GradeFixtures.JSON.readTree(file.toFile());
            // 보고서는 fixture, 라벨(LABEL_SCHEMA.md)은 id
            String id =
                    json.has("id") ? json.path("id").asString() : json.path("fixture").asString();
            assertThat(splitOf(id)).as(file + "의 fixture " + id).isEqualTo("dev");
            assertThat(file.getFileName().toString()).as(file.toString()).startsWith(id);
            if (json.has("split")) {
                assertThat(json.path("split").asString()).as(file.toString()).isEqualTo("dev");
            }
            if (json.has("source")) {
                // 라벨은 datasets.json의 고정 커밋으로 만든 것이다
                assertThat(json.path("source").path("commit").asString())
                        .as(file.toString())
                        .isEqualTo(PromptEvalRunner.dataset(id).path("commit").asString());
            }
        }
    }
}
