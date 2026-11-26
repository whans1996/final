package com.vibenote.analysis.explain;

import static com.vibenote.analysis.explain.ApiDetectorTest.file;
import static com.vibenote.analysis.explain.FileExplainerTest.PROPS;
import static com.vibenote.analysis.explain.FileExplainerTest.analysis;
import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.concept.ConceptExtractProperties;
import com.vibenote.analysis.concept.ConceptPrompts;
import com.vibenote.analysis.concept.ConceptResponseParser;
import com.vibenote.analysis.concept.ExtractedConcept;
import com.vibenote.analysis.concept.LinkedConcept;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.common.Level;
import com.vibenote.concept.ConceptStatus;
import com.vibenote.concept.domain.Concept;
import com.vibenote.llm.LlmStage;
import com.vibenote.llm.MockLlmClient;
import com.vibenote.quiz.GeneratedQuestion;
import com.vibenote.quiz.QuizPrompts;
import com.vibenote.quiz.QuizProperties;
import com.vibenote.quiz.QuizResponseParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
                        .explain(analysis(Level.JUNIOR), files, files, List.of());

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
        // 새 DB처럼 시드 파일 순서대로 1부터 id를 붙인다
        List<ExtractedConcept> result = extractWithSeedIds(1);

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
        // concept fixture가 연결하는 카드 (id는 시드 적용 이력이 다른 로컬 DB처럼 1000부터, #49)
        List<ExtractedConcept> linked = extractWithSeedIds(1000);
        Map<Long, String> nameById = new HashMap<>();
        for (Map.Entry<String, String> seed : seeds().entrySet()) {
            nameById.put(1000L + nameById.size(), seed.getKey());
        }
        List<LinkedConcept> concepts =
                linked.stream()
                        .filter(c -> c.conceptId() != null)
                        .map(
                                c ->
                                        new LinkedConcept(
                                                c.conceptId(),
                                                nameById.get(c.conceptId()),
                                                "PUBLISHED",
                                                c.contextNote()))
                        .toList();
        QuizPrompts.Input input =
                QuizPrompts.input(
                        concepts,
                        List.of(file(1, "src/F1.java", "service", "class F1 {}")),
                        new QuizProperties(5, 10, 3000, 3000, 24000, 3));
        // QuizGenerator와 같은 방식으로 템플릿 전체를 채운다
        String prompt =
                new PromptTemplates()
                        .render(
                                LlmStage.QUIZ,
                                Level.JUNIOR,
                                Map.of(
                                        "projectName", "realworld",
                                        "concepts", input.concepts(),
                                        "files", input.files()));

        String response = MockLlmClient.linkQuizConceptNames(read("quiz.json"), prompt);
        List<GeneratedQuestion> result =
                QuizResponseParser.parse(
                        JsonMapper.builder().build().readTree(response),
                        input.conceptIds(),
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

    @Test
    void issue49_시드_id가_새_DB와_달라도_concept_fixture는_의도한_카드에_연결된다() throws Exception {
        // 시드 적용 이력이 다른 로컬 DB처럼 id를 1000부터, 파일과 반대 순서로 붙인다
        List<String> names = new ArrayList<>(seeds().keySet());
        Collections.reverse(names);
        Map<Long, String> nameById = new HashMap<>();
        for (int i = 0; i < names.size(); i++) {
            nameById.put(1000L + i, names.get(i));
        }

        List<ExtractedConcept> result = extract(names, 1000);

        assertThat(
                        result.stream()
                                .filter(c -> c.conceptId() != null)
                                .map(c -> nameById.get(c.conceptId())))
                .containsExactly(
                        "@RestController와 요청 매핑",
                        "의존성 주입 (DI)",
                        "Bean Validation (@Valid)",
                        "Mapper 인터페이스와 XML 매퍼",
                        "JWT 기반 인증");
    }

    /** 시드 파일 순서대로 {@code firstId}부터 id를 붙인 카드 목록으로 concept fixture를 처리한다. */
    private static List<ExtractedConcept> extractWithSeedIds(long firstId) throws Exception {
        return extract(new ArrayList<>(seeds().keySet()), firstId);
    }

    /**
     * 실제 EXTRACTING 프롬프트({@link ConceptPrompts#input} + concept 템플릿)를 만들어 {@link MockLlmClient}가
     * fixture의 카드 이름을 id로 바꾼 응답을 파싱한다.
     */
    private static List<ExtractedConcept> extract(List<String> names, long firstId)
            throws Exception {
        Map<String, String> categoryByName = seeds();
        List<Concept> concepts = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            Concept c = new Concept();
            c.setId(firstId + i);
            c.setName(names.get(i));
            c.setCategory(categoryByName.get(names.get(i)));
            c.setStatus(ConceptStatus.PUBLISHED);
            concepts.add(c);
        }
        List<AnalysisFile> files = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            files.add(file(i, "src/F" + i + ".java", "service", "class F" + i + " {}"));
        }
        ConceptPrompts.Input input =
                ConceptPrompts.input(
                        concepts,
                        files,
                        new ConceptExtractProperties(6000, 6000, 3000, 24000, 15, 5));
        assertThat(input.catalogIds()).hasSize(concepts.size());
        // ConceptExtractor와 같은 방식으로 템플릿 전체를 채운다 (머리말·예시 줄이 카드 목록으로 잘못 읽히지 않는지도 본다)
        String prompt =
                new PromptTemplates()
                        .render(
                                LlmStage.CONCEPT,
                                Level.JUNIOR,
                                Map.of(
                                        "projectName", "realworld",
                                        "catalog", input.catalog(),
                                        "fileList", input.fileList(),
                                        "files", input.files()));

        String response = MockLlmClient.linkConceptNames(read("concept.json"), prompt);

        return ConceptResponseParser.parse(
                JsonMapper.builder().build().readTree(response),
                input,
                concepts,
                Level.JUNIOR,
                15,
                5);
    }

    /** {@code R__seed_concepts.sql}의 카드 이름 → 카테고리 (파일 순서). */
    private static Map<String, String> seeds() throws Exception {
        String sql =
                Files.readString(Path.of("src/main/resources/db/migration/R__seed_concepts.sql"));
        Matcher m = Pattern.compile("(?m)^\\('((?:[^']|'')*)', '([a-z]+)', '").matcher(sql);
        Map<String, String> seeds = new LinkedHashMap<>();
        while (m.find()) {
            seeds.put(m.group(1).replace("''", "'"), m.group(2));
        }
        assertThat(seeds).hasSizeGreaterThanOrEqualTo(80);
        return seeds;
    }
}
