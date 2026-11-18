package com.vibenote.analysis.explain;

import static com.vibenote.analysis.explain.ApiDetectorTest.file;
import static com.vibenote.analysis.explain.FileExplainerTest.PROPS;
import static com.vibenote.analysis.explain.FileExplainerTest.analysis;
import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.common.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 앱(mock 프로필)이 쓰는 main fixture가 spec 5.3절 스키마를 통과하는지 본다 (F-06 spec 8절).
 *
 * <p>테스트 classpath에서는 같은 이름의 test fixture가 먼저 잡히므로 main 파일을 경로로 직접 읽는다.
 */
class MainFixturesTest {

    private static final Path FIXTURES = Path.of("src/main/resources/llm-fixtures");

    private static String read(String name) throws Exception {
        return Files.readString(FIXTURES.resolve(name));
    }

    @Test
    void overview_fixture는_검증을_통과하고_항목이_버려지지_않는다() throws Exception {
        List<AnalysisFile> files = new ArrayList<>();
        files.add(
                file(
                        1,
                        "src/main/java/io/spring/api/ArticlesApi.java",
                        "controller",
                        "class A {}"));
        files.add(
                file(
                        2,
                        "src/main/java/io/spring/application/article/ArticleCommandService.java",
                        "service",
                        "class B {}"));
        files.add(
                file(
                        3,
                        "src/main/java/io/spring/infrastructure/mybatis/mapper/ArticleMapper.java",
                        "mapper",
                        "interface C {}"));
        FakeLlmClient llm = new FakeLlmClient().answer(read("overview.json"));

        AnalysisOverview overview =
                new OverviewExplainer(llm, new PromptTemplates(), PROPS)
                        .explain(analysis(Level.JUNIOR), files, List.of());

        assertThat(llm.requests).hasSize(1);
        assertThat(overview.summary()).isNotBlank();
        assertThat(overview.techStack()).hasSize(8);
        assertThat(overview.layers()).hasSize(5);
        assertThat(overview.readingOrder())
                .extracting(AnalysisOverview.ReadingStep::fileId)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void file_fixture는_6개_묶음_모두에_설명을_채운다() throws Exception {
        List<AnalysisFile> batch = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            batch.add(file(i, "src/F" + i + ".java", "service", "class F" + i + " {}"));
        }
        FakeLlmClient llm = new FakeLlmClient().answer(read("file.json"));

        List<FileExplanation> result =
                new FileExplainer(llm, new PromptTemplates(), PROPS)
                        .explain(analysis(Level.JUNIOR), "블로그", batch);

        assertThat(llm.requests).hasSize(1);
        assertThat(result).hasSize(6);
        assertThat(result)
                .allSatisfy(
                        e -> {
                            assertThat(e.role()).isNotBlank();
                            assertThat(e.summary()).isNotBlank();
                        });
    }
}
