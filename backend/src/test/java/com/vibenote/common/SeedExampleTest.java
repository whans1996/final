package com.vibenote.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.SeedExampleChecker.Card;
import com.vibenote.common.SeedExampleChecker.Issue;
import com.vibenote.common.SeedExampleChecker.Result;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * K-3a AC7·AC8: 실제 시드 카드({@code data/seed/concepts}) 전체의 java·sql·xml·yaml 예제를 검사한다. 오류가 하나라도 있으면
 * 실패하고, 경고·오류를 {@code build/seed-check/result.json}에 남겨 {@code scripts/seed-check.mjs --be}가 보고서에
 * 합친다.
 *
 * <p>환경변수 {@code SEED_SRC}(카드 폴더), {@code SEED_CARD}(slug 또는 category/slug)로 범위를 바꿀 수 있다.
 */
class SeedExampleTest {

    @Test
    void AC7_AC8_시드_카드_예제에_오류가_없다() throws Exception {
        Path root = Path.of("..").toAbsolutePath().normalize();
        String srcEnv = System.getenv("SEED_SRC");
        String only = System.getenv("SEED_CARD");
        Path src = srcEnv != null ? Path.of(srcEnv) : root.resolve("data/seed/concepts");
        List<Card> cards =
                SeedExampleChecker.readCards(root, src).stream()
                        .filter(c -> only == null || c.file().endsWith("/" + only + ".md"))
                        .toList();

        Result result;
        try (Connection conn =
                DriverManager.getConnection(
                        IntegrationTestBase.MYSQL.getJdbcUrl(),
                        "root",
                        IntegrationTestBase.MYSQL.getPassword())) {
            result = SeedExampleChecker.checkAll(cards, conn);
        }
        Path out = Path.of("build/seed-check/result.json");
        Files.createDirectories(out.getParent());
        JsonMapper.builder()
                .build()
                .writerWithDefaultPrettyPrinter()
                .writeValue(
                        out.toFile(),
                        Map.of("issues", result.issues(), "checked", result.checked()));

        List<Issue> errors =
                result.issues().stream().filter(i -> i.severity().equals("error")).toList();
        assertThat(cards).as("카드 파일 (%s)", src).isNotEmpty();
        assertThat(errors)
                .as(
                        "시드 카드 예제 오류 (전체: node scripts/seed-check.mjs --be)%n%s",
                        errors.stream()
                                .map(i -> i.file() + ":" + i.line() + " " + i.message())
                                .collect(Collectors.joining("\n")))
                .isEmpty();
    }
}
