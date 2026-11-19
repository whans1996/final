package com.vibenote.analysis.explain;

import static com.vibenote.analysis.explain.ApiDetectorTest.file;
import static com.vibenote.analysis.explain.FileExplainerTest.PROPS;
import static com.vibenote.analysis.explain.FileExplainerTest.analysis;
import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.concept.ConceptPrompts;
import com.vibenote.analysis.concept.ConceptResponseParser;
import com.vibenote.analysis.concept.ExtractedConcept;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.common.Level;
import com.vibenote.concept.ConceptStatus;
import com.vibenote.concept.domain.Concept;
import com.vibenote.quiz.GeneratedQuestion;
import com.vibenote.quiz.QuizResponseParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

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

    @Test
    void AC10_F08_concept_fixture는_시드_카드_연결_3개_이상과_새_개념_1개를_담는다() throws Exception {
        List<Concept> seeds = new ArrayList<>();
        for (long id = 1; id <= 85; id++) {
            Concept c = new Concept();
            c.setId(id);
            c.setName("시드 " + id);
            c.setCategory("spring");
            c.setStatus(ConceptStatus.PUBLISHED);
            seeds.add(c);
        }
        Set<Long> catalog = Set.copyOf(seeds.stream().map(Concept::getId).toList());
        ConceptPrompts.Input input =
                new ConceptPrompts.Input("", "", "", catalog, List.of(1L, 2L, 3L), Set.of(1, 2, 3));

        List<ExtractedConcept> result =
                ConceptResponseParser.parse(
                        JsonMapper.builder().build().readTree(read("concept.json")),
                        input,
                        seeds,
                        Level.JUNIOR,
                        15,
                        5);

        assertThat(result.stream().filter(c -> c.conceptId() != null))
                .hasSizeGreaterThanOrEqualTo(3);
        assertThat(result.stream().filter(c -> c.draft() != null)).hasSize(1);
        assertThat(result)
                .allSatisfy(c -> assertThat(c.contextNote()).isNotBlank())
                .anySatisfy(c -> assertThat(c.lineHint()).isNotNull());
        assertThat(
                        result.stream()
                                .filter(c -> c.draft() != null)
                                .findFirst()
                                .orElseThrow()
                                .draft()
                                .body())
                .startsWith("## 핵심")
                .contains("## 주의");
    }

    @Test
    void AC11_F14_quiz_fixture는_6문항이고_시드_카드_4개_이상과_null_1개를_가리킨다() throws Exception {
        // concept.json이 가리키는 시드 카드 id (#49: 로컬 DB에 따라 다를 수 있어 목록 밖이면 파서가 NULL로 바꾼다)
        Set<Long> seedIds = Set.of(18L, 39L, 42L, 57L, 62L);

        List<GeneratedQuestion> result =
                QuizResponseParser.parse(
                        JsonMapper.builder().build().readTree(read("quiz.json")),
                        seedIds,
                        1L,
                        5,
                        10);

        assertThat(result).hasSize(6);
        assertThat(
                        result.stream()
                                .filter(q -> q.conceptId() != null)
                                .map(GeneratedQuestion::conceptId)
                                .distinct())
                .hasSizeGreaterThanOrEqualTo(4);
        assertThat(result.stream().filter(q -> q.conceptId() == null)).hasSize(1);
    }
}
