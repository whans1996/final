package com.vibenote.analysis.fetch;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

/**
 * 실제 GitHub에서 아카이브를 받는다 (F-05b 8절). 외부 네트워크를 쓰므로 {@code test}에서 빠지고 사람이 {@code ./gradlew
 * liveTest}로만 돌린다. 대상은 DS-02-01(split dev, data/datasets.json). 환경변수 {@code GITHUB_TOKEN}이 있으면 쓴다.
 */
@Tag("live")
class RepositoryArchiveFetcherLiveTest {

    @TempDir Path dir;

    @Test
    void DS_02_01_기본_브랜치_아카이브를_받는다() throws IOException {
        FetchProperties properties =
                new FetchProperties(
                        "https://api.github.com",
                        "https://gitlab.com/api/v4",
                        List.of("api.github.com", "codeload.github.com", "gitlab.com"),
                        System.getenv("GITHUB_TOKEN"),
                        DataSize.ofMegabytes(30),
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(60),
                        3);
        Path target = dir.resolve("analysis-tmp").resolve("1.zip");

        new RepositoryArchiveFetcher(properties)
                .download(
                        RepositoryUrl.parse(
                                "https://github.com/gothinkster/spring-boot-realworld-example-app"),
                        target);

        assertThat(Files.size(target)).isPositive();
        try (ZipFile zip = new ZipFile(target.toFile())) {
            assertThat(zip.stream().map(e -> e.getName()))
                    .anyMatch(name -> name.endsWith(".java"))
                    .allMatch(name -> name.startsWith("gothinkster-spring-boot-realworld"));
        }
    }
}
