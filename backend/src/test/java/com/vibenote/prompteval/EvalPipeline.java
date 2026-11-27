package com.vibenote.prompteval;

import com.vibenote.analysis.concept.ConceptExtractor;
import com.vibenote.analysis.concept.ConceptPrompts;
import com.vibenote.analysis.concept.ConceptResponseParser;
import com.vibenote.analysis.concept.ExtractedConcept;
import com.vibenote.analysis.concept.LinkedConcept;
import com.vibenote.analysis.domain.Analysis;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.AnalysisOverview;
import com.vibenote.analysis.explain.ApiDetector;
import com.vibenote.analysis.explain.ApiEndpoint;
import com.vibenote.analysis.explain.FileExplainer;
import com.vibenote.analysis.explain.FileExplanation;
import com.vibenote.analysis.explain.LlmJsonCall.InvalidResponseException;
import com.vibenote.analysis.explain.OverviewExplainer;
import com.vibenote.analysis.explain.PromptTemplates;
import com.vibenote.analysis.flow.FlowGenerator;
import com.vibenote.analysis.flow.FlowGenerator.FlowSource;
import com.vibenote.analysis.flow.FlowResult;
import com.vibenote.analysis.pipeline.AnalysisFailedException;
import com.vibenote.analysis.pipeline.ImportanceRanker;
import com.vibenote.analysis.pipeline.RankedFile;
import com.vibenote.analysis.pipeline.SecretRedactor;
import com.vibenote.analysis.pipeline.SelectedFile;
import com.vibenote.analysis.pipeline.SourceFile;
import com.vibenote.analysis.pipeline.ZipArchiveReader;
import com.vibenote.common.Level;
import com.vibenote.concept.ConceptStatus;
import com.vibenote.concept.domain.Concept;
import com.vibenote.quiz.GeneratedQuestion;
import com.vibenote.quiz.QuizGenerator;
import com.vibenote.quiz.QuizPrompts;
import com.vibenote.quiz.QuizResponseParser;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 평가 러너 파이프라인 (E-1 AC1~AC4). 앱과 같은 경로로 돈다: ZIP 읽기 → 선별 → 비밀 가림 → API 탐지 → 개요 → 파일 설명 → 흐름 → 개념 →
 * 퀴즈. DB 대신 수준마다 메모리의 파일 목록을 쓰고, 개념 카탈로그는 호출자가 준다 (러너는 Testcontainers DB의 시드 카탈로그).
 *
 * <p>단계마다 LLM 호출 묶음(단위)의 시도 수·첫 시도 통과·최종 성공을 {@code units[]}에 남겨 채점기의 지표 A로 쓴다. 한 단계가 실패하면 그 결과에
 * 의존하는 단계만 건너뛴다 (개요 → 파일·흐름, 개념 → 퀴즈).
 */
final class EvalPipeline {

    private static final ObjectMapper JSON = JsonMapper.builder().build();

    static final List<String> STAGES =
            List.of("overview", "file", "flow", "concept", "quiz", "all");

    /** 단계 옵션 → 실제로 도는 단계. 흐름은 개요 요약·API가, 퀴즈는 개념이 필요하다. */
    private static final Map<String, Set<String>> RUNS =
            Map.of(
                    "overview", Set.of("overview"),
                    "file", Set.of("overview", "file"),
                    "flow", Set.of("overview", "flow"),
                    "concept", Set.of("concept"),
                    "quiz", Set.of("concept", "quiz"),
                    "all", Set.of("overview", "file", "flow", "concept", "quiz"));

    /**
     * @param stage {@link #STAGES} 중 하나
     * @param batches 파일 설명 묶음 수 상한 ({@code Integer.MAX_VALUE}면 전부)
     * @param flows 수준마다 만드는 흐름 수 (탐지 순서 앞 N개)
     */
    record Options(String stage, List<Level> levels, int batches, int flows) {

        Options {
            if (!RUNS.containsKey(stage)) {
                throw new IllegalArgumentException("stage는 " + STAGES + " 중 하나: " + stage);
            }
        }

        boolean runs(String step) {
            return RUNS.get(stage).contains(step);
        }
    }

    private final RecordingLlmClient llm;
    private final EvalSettings settings;
    private final List<Concept> catalog;
    private final String provider;
    private final OverviewExplainer overviewExplainer;
    private final FileExplainer fileExplainer;
    private final FlowGenerator flowGenerator;
    private final ConceptExtractor conceptExtractor;
    private final QuizGenerator quizGenerator;
    private final List<Map<String, Object>> units = new ArrayList<>();

    /**
     * @param catalog 기존 카드 전체 (상태 무관, 앱의 {@code findAllForMatching}과 같다). 개념 단계를 돌리지 않으면 비어도 된다
     */
    EvalPipeline(
            RecordingLlmClient llm, EvalSettings settings, List<Concept> catalog, String provider) {
        this.llm = llm;
        this.settings = settings;
        this.catalog = catalog;
        this.provider = provider;
        PromptTemplates templates = new PromptTemplates();
        this.overviewExplainer = new OverviewExplainer(llm, templates, settings.explain());
        this.fileExplainer = new FileExplainer(llm, templates, settings.explain());
        // 흐름 하나를 만드는 create()는 FlowService·AnalysisService를 쓰지 않는다
        this.flowGenerator =
                new FlowGenerator(null, null, llm, templates, settings.flow(), settings.explain());
        this.conceptExtractor = new ConceptExtractor(llm, templates, settings.concept());
        this.quizGenerator = new QuizGenerator(llm, templates, settings.quiz());
    }

    Map<String, Object> run(EvalFixture fixture, Options options) {
        List<AnalysisFile> baseIndex = select(fixture);
        List<AnalysisFile> baseFiles = baseIndex.stream().filter(AnalysisFile::isSelected).toList();
        List<ApiEndpoint> apis = ApiDetector.detect(baseFiles);
        int expected = expectedCalls(options, baseFiles.size(), apis.size());
        System.out.printf(
                "[prompt-eval] 예상 호출 수 %d (수준 %d개, 형식 재시도·일시 오류 재전송 제외)%n",
                expected, options.levels().size());

        Map<String, Object> results = new LinkedHashMap<>();
        for (Level level : options.levels()) {
            llm.startLevel(level);
            // 파일 설명의 역할(role)이 개념 프롬프트에 들어가므로 수준끼리 섞이지 않게 수준마다 새로 만든다
            List<AnalysisFile> index = baseIndex.stream().map(EvalPipeline::copy).toList();
            List<AnalysisFile> files = index.stream().filter(AnalysisFile::isSelected).toList();
            Map<String, Object> result = runLevel(fixture, options, level, index, files);
            result.put("calls", llm.callsOf(level));
            results.put(key(level), result);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("fixture", fixture.id());
        report.put("fixtureZip", fixture.zip().toAbsolutePath().normalize().toString());
        report.put("provider", provider);
        report.put("model", String.join(", ", llm.models()));
        report.put("split", fixture.split());
        report.put("contamination", fixture.contamination());
        report.put("stage", options.stage());
        report.put("levels", options.levels().stream().map(EvalPipeline::key).toList());
        report.put("expectedCalls", expected);
        report.put("index", baseIndex.stream().map(EvalPipeline::indexRow).toList());
        report.put("apis", apis);
        report.put("results", results);
        report.put("units", units);
        report.put("totals", llm.totals());
        return report;
    }

    private Map<String, Object> runLevel(
            EvalFixture fixture,
            Options options,
            Level level,
            List<AnalysisFile> index,
            List<AnalysisFile> files) {
        Analysis analysis = Analysis.newZip(0L, fixture.name(), level, LocalDateTime.now());
        analysis.setId(0L);
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, String> skipped = new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();

        AnalysisOverview overview = null;
        if (options.runs("overview")) {
            List<ApiEndpoint> apis = ApiDetector.detect(files);
            overview =
                    unit(
                                    "overview",
                                    level,
                                    "overview",
                                    () -> overviewExplainer.explain(analysis, files, index, apis),
                                    o -> true,
                                    errors)
                            .orElse(null);
            result.put("overview", overview);
        }

        if (options.runs("file")) {
            List<Map<String, Object>> rows = new ArrayList<>();
            if (overview == null) {
                skipped.put("file", "overview 실패");
            } else {
                String summary = overview.summary();
                int size = settings.explain().batchSize();
                for (int b = 0; b < options.batches() && (long) b * size < files.size(); b++) {
                    List<AnalysisFile> batch =
                            files.subList(b * size, Math.min(files.size(), (b + 1) * size));
                    List<FileExplanation> out =
                            swallowing(
                                            "file",
                                            level,
                                            String.join(
                                                    ",",
                                                    batch.stream()
                                                            .map(AnalysisFile::getPath)
                                                            .toList()),
                                            () -> fileExplainer.explain(analysis, summary, batch),
                                            root -> FileExplainer.parse(root, batch),
                                            errors)
                                    .orElse(List.of());
                    for (AnalysisFile f : batch) {
                        FileExplanation e =
                                out.stream()
                                        .filter(x -> x.fileId() == f.getId())
                                        .findFirst()
                                        .orElse(null);
                        if (e != null) {
                            // 앱처럼 설명을 파일에 남겨 개념 프롬프트의 파일 목록에 역할이 들어가게 한다
                            f.setRole(e.role());
                            f.setSummary(e.summary());
                        }
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("path", f.getPath());
                        row.put("layer", f.getLayer());
                        row.put("role", e == null ? null : e.role());
                        row.put("explanation", e == null ? null : e.summary());
                        rows.add(row);
                    }
                }
            }
            result.put("files", rows);
        }

        if (options.runs("flow")) {
            List<Map<String, Object>> rows = new ArrayList<>();
            if (overview == null) {
                skipped.put("flow", "overview 실패");
            } else if (overview.apis().isEmpty()) {
                skipped.put("flow", "NO_API");
            } else {
                FlowSource source =
                        new FlowSource(fixture.name(), level, overview.summary(), null, null);
                for (ApiEndpoint api : overview.apis().stream().limit(options.flows()).toList()) {
                    String endpoint = api.method() + " " + api.path();
                    Map<String, String> flowErrors = new LinkedHashMap<>();
                    Optional<FlowResult> flow =
                            unit(
                                            "flow",
                                            level,
                                            endpoint,
                                            () -> flowGenerator.create(source, api, files),
                                            Optional::isPresent,
                                            flowErrors)
                                    .flatMap(Function.identity());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("endpoint", endpoint);
                    row.put("summary", flow.map(FlowResult::summary).orElse(null));
                    row.put("steps", flow.map(FlowResult::steps).orElse(List.of()));
                    row.put(
                            "error",
                            flow.isPresent() ? null : flowErrors.getOrDefault("flow", "형식 오류 2회"));
                    rows.add(row);
                }
            }
            result.put("flows", rows);
        }

        List<ExtractedConcept> extracted = null;
        if (options.runs("concept")) {
            ConceptPrompts.Input input = ConceptPrompts.input(catalog, files, settings.concept());
            extracted =
                    swallowing(
                                    "concept",
                                    level,
                                    "concept",
                                    () -> conceptExtractor.extract(analysis, catalog, files),
                                    root ->
                                            ConceptResponseParser.parse(
                                                    root,
                                                    input,
                                                    catalog,
                                                    level,
                                                    settings.concept().maxLinks(),
                                                    settings.concept().maxDrafts()),
                                    errors)
                            .orElse(null);
            result.put("concepts", extracted == null ? List.of() : conceptRows(extracted, files));
        }

        if (options.runs("quiz")) {
            List<Map<String, Object>> rows = new ArrayList<>();
            if (extracted == null) {
                skipped.put("quiz", "concept 실패");
            } else {
                List<LinkedConcept> linked = linked(extracted);
                Map<Long, String> names = new HashMap<>();
                linked.forEach(c -> names.put(c.conceptId(), c.name()));
                Set<Long> conceptIds =
                        QuizPrompts.input(linked, files, settings.quiz()).conceptIds();
                List<GeneratedQuestion> quiz =
                        swallowing(
                                        "quiz",
                                        level,
                                        "quiz",
                                        () -> quizGenerator.generate(analysis, linked, files),
                                        root ->
                                                QuizResponseParser.parse(
                                                        root,
                                                        conceptIds,
                                                        analysis.getId(),
                                                        settings.quiz().minQuestions(),
                                                        settings.quiz().maxQuestions()),
                                        errors)
                                .orElse(List.of());
                for (GeneratedQuestion q : quiz) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("conceptName", q.conceptId() == null ? null : names.get(q.conceptId()));
                    row.put("question", q.question());
                    row.put("choices", q.choices());
                    row.put("answerIndex", q.answerIndex());
                    row.put("explanation", q.explanation());
                    rows.add(row);
                }
            }
            result.put("quiz", rows);
        }

        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }

    /**
     * 실패하면 예외를 던지는 단계(개요·흐름) 하나. 예외나 {@code accepted}가 아닌 결과는 실패로 센다.
     *
     * @return 실패하면 empty
     */
    private <T> Optional<T> unit(
            String stage,
            Level level,
            String key,
            Supplier<T> work,
            Predicate<T> accepted,
            Map<String, String> errors) {
        llm.beginUnit();
        try {
            T value = work.get();
            boolean ok = accepted.test(value);
            record(stage, level, key, ok);
            return Optional.of(value);
        } catch (AnalysisFailedException e) {
            record(stage, level, key, false);
            errors.putIfAbsent(stage, e.code().name());
        } catch (RuntimeException e) {
            // 앱에서는 FallbackLlmClient가 받는 예외(LlmUnavailableException 등)다. 이 단위만 실패로 기록하고 계속한다
            record(stage, level, key, false);
            errors.putIfAbsent(stage, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * 실패하면 빈 목록을 돌려주는 단계(파일 설명·개념·퀴즈) 하나. 마지막 시도가 응답 없이 끝났으면 LLM 실패, 응답을 두 번 받고 결과가 비었으면 마지막 응답을 앱
     * 파서({@code parse})로 다시 읽어 형식 오류 2회인지 형식이 맞은 빈 목록인지 가린다 (#221).
     *
     * @param parse 생성기가 쓰는 앱 파서. 형식이 틀리면 {@link InvalidResponseException}을 던진다
     * @return 실패하면 empty
     */
    private <T extends Collection<?>> Optional<T> swallowing(
            String stage,
            Level level,
            String key,
            Supplier<T> work,
            Function<JsonNode, ?> parse,
            Map<String, String> errors) {
        Optional<T> value = unit(stage, level, key, work, v -> true, errors);
        if (value.isEmpty()) {
            return value;
        }
        RecordingLlmClient.UnitCalls calls = llm.unitCalls();
        boolean ok =
                !calls.lastFailed()
                        && (calls.responses() == 1
                                || !value.get().isEmpty()
                                || parses(calls.lastContent(), parse));
        if (ok) {
            return value;
        }
        units.get(units.size() - 1).put("ok", false);
        units.get(units.size() - 1).put("firstPass", false);
        errors.putIfAbsent(stage, calls.lastFailed() ? "LLM 실패" : "형식 오류 2회");
        return Optional.empty();
    }

    /** 앱의 {@code LlmJsonCall}처럼 원문을 JSON으로 읽어 파서가 받아들이는지. */
    private static boolean parses(String content, Function<JsonNode, ?> parse) {
        if (content == null) {
            return false;
        }
        try {
            parse.apply(JSON.readTree(content));
            return true;
        } catch (JacksonException | InvalidResponseException e) {
            return false;
        }
    }

    private void record(String stage, Level level, String key, boolean ok) {
        RecordingLlmClient.UnitCalls calls = llm.unitCalls();
        Map<String, Object> unit = new LinkedHashMap<>();
        unit.put("stage", stage);
        unit.put("level", key(level));
        unit.put("key", key);
        unit.put("calls", calls.calls());
        unit.put("firstPass", ok && calls.responses() == 1);
        unit.put("ok", ok);
        units.add(unit);
    }

    private List<Map<String, Object>> conceptRows(
            List<ExtractedConcept> extracted, List<AnalysisFile> files) {
        Map<Long, Concept> byId = new HashMap<>();
        catalog.forEach(c -> byId.put(c.getId(), c));
        Map<Long, String> paths = new HashMap<>();
        files.forEach(f -> paths.put(f.getId(), f.getPath()));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ExtractedConcept e : extracted) {
            Concept existing = e.conceptId() == null ? null : byId.get(e.conceptId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", existing != null ? existing.getName() : e.draft().name());
            row.put("conceptId", e.conceptId());
            row.put("draft", e.draft() != null);
            row.put("category", existing != null ? existing.getCategory() : e.draft().category());
            row.put("path", e.fileId() == null ? null : paths.get(e.fileId()));
            row.put("lineHint", e.lineHint());
            row.put("contextNote", e.contextNote());
            rows.add(row);
        }
        return rows;
    }

    /**
     * 앱이 저장 후 퀴즈에 넘기는 연결 개념과 같게 만든다 ({@code ConceptLinkService.save} → {@code findForQuiz}): 초안은 이름
     * 순으로 카탈로그 최대 id 뒤의 임시 id를 받고, HIDDEN 카드는 빠지며, {@code concept_id} 오름차순이다.
     */
    private List<LinkedConcept> linked(List<ExtractedConcept> extracted) {
        Map<Long, Concept> byId = new HashMap<>();
        catalog.forEach(c -> byId.put(c.getId(), c));
        long next = catalog.stream().mapToLong(Concept::getId).max().orElse(0L);
        Map<ExtractedConcept.NewConcept, Long> drafts = new HashMap<>();
        for (ExtractedConcept.NewConcept d :
                extracted.stream()
                        .map(ExtractedConcept::draft)
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(d -> d.name().toLowerCase(Locale.ROOT)))
                        .toList()) {
            drafts.put(d, ++next);
        }
        Set<Long> seen = new LinkedHashSet<>();
        List<LinkedConcept> linked = new ArrayList<>();
        for (ExtractedConcept e : extracted) {
            long id = e.conceptId() != null ? e.conceptId() : drafts.get(e.draft());
            if (!seen.add(id)) {
                continue;
            }
            Concept existing = byId.get(id);
            if (existing == null) {
                linked.add(new LinkedConcept(id, e.draft().name(), "DRAFT", e.contextNote()));
            } else if (existing.getStatus() != ConceptStatus.HIDDEN) {
                linked.add(
                        new LinkedConcept(
                                id,
                                existing.getName(),
                                existing.getStatus().name(),
                                e.contextNote()));
            }
        }
        linked.sort(Comparator.comparingLong(LinkedConcept::conceptId));
        return linked;
    }

    /** 수준 하나의 예상 호출 수 × 수준 수. 형식 재시도·일시 오류 재전송은 빼고 센다. */
    private int expectedCalls(Options options, int selected, int apis) {
        int perLevel = 0;
        if (options.runs("overview")) {
            perLevel++;
        }
        if (options.runs("file")) {
            int size = settings.explain().batchSize();
            perLevel += Math.min(options.batches(), (selected + size - 1) / size);
        }
        if (options.runs("flow")) {
            perLevel += Math.min(options.flows(), apis);
        }
        if (options.runs("concept")) {
            perLevel++;
        }
        if (options.runs("quiz")) {
            perLevel++;
        }
        return perLevel * options.levels().size();
    }

    /** 실제 파이프라인의 SELECTING 단계와 같다 (인덱스 전체에 선별 표시 → 비밀 가림). 파일 id는 저장 순서(선별 파일 먼저)대로 1부터. */
    private List<AnalysisFile> select(EvalFixture fixture) {
        List<SourceFile> read =
                new ZipArchiveReader(settings.analysis()).read(fixture.zip()).files();
        List<RankedFile> ranked =
                new ImportanceRanker(settings.selection())
                        .rank(read, settings.analysis().maxSelectedFiles());
        List<AnalysisFile> files = new ArrayList<>();
        for (RankedFile f : ranked) {
            AnalysisFile row =
                    AnalysisFile.of(
                            0L,
                            new SelectedFile(
                                    f.path(),
                                    f.layer(),
                                    SecretRedactor.redact(f.path(), f.content()),
                                    f.selected(),
                                    f.importance()));
            row.setId((long) files.size() + 1);
            files.add(row);
        }
        return files;
    }

    private static AnalysisFile copy(AnalysisFile f) {
        AnalysisFile row = new AnalysisFile();
        row.setId(f.getId());
        row.setAnalysisId(f.getAnalysisId());
        row.setPath(f.getPath());
        row.setLayer(f.getLayer());
        row.setContent(f.getContent());
        row.setSelected(f.isSelected());
        row.setImportance(f.getImportance());
        return row;
    }

    private static Map<String, Object> indexRow(AnalysisFile f) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("path", f.getPath());
        row.put("layer", f.getLayer());
        row.put("selected", f.isSelected());
        row.put("lines", f.getContent() == null ? 0 : (int) f.getContent().lines().count());
        return row;
    }

    private static String key(Level level) {
        return level.name().toLowerCase(Locale.ROOT);
    }
}
