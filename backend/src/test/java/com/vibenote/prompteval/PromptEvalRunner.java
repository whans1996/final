package com.vibenote.prompteval;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.vibenote.analysis.AnalysisProperties;
import com.vibenote.analysis.domain.Analysis;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.AnalysisOverview;
import com.vibenote.analysis.explain.ApiDetector;
import com.vibenote.analysis.explain.ApiEndpoint;
import com.vibenote.analysis.explain.ExplainProperties;
import com.vibenote.analysis.explain.FileExplainer;
import com.vibenote.analysis.explain.FileExplanation;
import com.vibenote.analysis.explain.OverviewExplainer;
import com.vibenote.analysis.explain.PromptTemplates;
import com.vibenote.analysis.pipeline.AnalysisFailedException;
import com.vibenote.analysis.pipeline.ImportanceRanker;
import com.vibenote.analysis.pipeline.RankedFile;
import com.vibenote.analysis.pipeline.SecretRedactor;
import com.vibenote.analysis.pipeline.SelectedFile;
import com.vibenote.analysis.pipeline.SelectionProperties;
import com.vibenote.analysis.pipeline.SourceFile;
import com.vibenote.analysis.pipeline.ZipArchiveReader;
import com.vibenote.common.Level;
import com.vibenote.llm.LiveLlmClients;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmProperties;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 프롬프트 평가 하네스. <b>사람이 요청할 때만</b> {@code ./gradlew promptEval}로 실행한다 (실제 Gemini 무료 한도를 쓴다). {@code
 * test}(CI·git hook·verify)에서는 제외된다. 결과 해석은 {@code prompt-evaluator} 에이전트가 한다.
 *
 * <p>실제 파이프라인과 같은 경로로 돈다: ZIP 읽기({@link ZipArchiveReader}) → 선별({@link FileSelector}) → 비밀
 * 가림({@link SecretRedactor}) → API 탐지 → {@link OverviewExplainer} → {@link FileExplainer}. 호출마다 원문
 * 응답·토큰·지연을 기록해 {@code .harness/prompt-eval/}에 JSON으로 남긴다.
 *
 * <p>옵션 (Gradle {@code -P}):
 *
 * <ul>
 *   <li>{@code fixture}: {@code data/datasets.json}의 저장소 id(예: DS-02-01. 고정 커밋을 codeload에서 받아 캐시)
 *       또는 ZIP 경로
 *   <li>{@code stage}: {@code overview}(수준당 1회) 또는 {@code file}(개요 + 파일 묶음 {@code batches}개)
 *   <li>{@code level}: {@code beginner|junior|senior|all}
 *   <li>{@code batches}: file 단계에서 설명할 묶음 수 (기본 1)
 *   <li>{@code pauseMs}: 호출 사이 대기 (기본 4000, 무료 한도의 분당 요청 수 보호)
 * </ul>
 *
 * <p>평가 데이터(split=eval) 결과는 점수로, dev 결과는 "개발용 참고"로 나눠 보고한다 ({@code docs/산출물/09-데이터셋.md} 5절). 이 러너의
 * 출력은 프롬프트·few-shot·fixture 작성에 쓰지 않는다.
 */
@Tag("prompt-eval")
class PromptEvalRunner {

    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path OUT = ROOT.resolve(".harness/prompt-eval");
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    @Test
    void run() throws Exception {
        String fixture = option("fixture", "DS-02-01");
        String stage = option("stage", "overview").toLowerCase(Locale.ROOT);
        String levelOption = option("level", "all").toUpperCase(Locale.ROOT);
        int batches = Integer.parseInt(option("batches", "1"));
        long pauseMs = Long.parseLong(option("pauseMs", "4000"));
        if (!stage.equals("overview") && !stage.equals("file")) {
            throw new IllegalArgumentException("stage는 overview 또는 file: " + stage);
        }
        List<Level> levels =
                levelOption.equals("ALL")
                        ? List.of(Level.values())
                        : List.of(Level.valueOf(levelOption));

        Binder binder = binder();
        LlmProperties.Gemini gemini = binder.bindOrCreate("llm", LlmProperties.class).gemini();
        assumeTrue(gemini.apiKey() != null && !gemini.apiKey().isBlank(), "GEMINI_API_KEY가 없어 건너뜀");
        AnalysisProperties analysisProps =
                binder.bindOrCreate("analysis", AnalysisProperties.class);
        ExplainProperties explainProps =
                binder.bindOrCreate("analysis.explain", ExplainProperties.class);

        Fixture source = fixture(fixture);
        SelectionProperties selectionProps =
                binder.bindOrCreate("analysis.selection", SelectionProperties.class);
        List<AnalysisFile> index = select(source.zip(), analysisProps, selectionProps);
        List<AnalysisFile> files = index.stream().filter(AnalysisFile::isSelected).toList();
        List<ApiEndpoint> apis = ApiDetector.detect(files);

        RecordingLlmClient llm = new RecordingLlmClient(LiveLlmClients.gemini(gemini), pauseMs);
        PromptTemplates templates = new PromptTemplates();
        OverviewExplainer overviewExplainer = new OverviewExplainer(llm, templates, explainProps);
        FileExplainer fileExplainer = new FileExplainer(llm, templates, explainProps);

        Map<String, Object> results = new LinkedHashMap<>();
        for (Level level : levels) {
            Analysis analysis = Analysis.newZip(0L, source.name(), level, LocalDateTime.now());
            analysis.setId(0L);
            Map<String, Object> result = new LinkedHashMap<>();
            llm.startLevel(level);
            try {
                AnalysisOverview overview = overviewExplainer.explain(analysis, files, index, apis);
                result.put("overview", overview);
                if (stage.equals("file")) {
                    List<Map<String, Object>> explained = new ArrayList<>();
                    int size = explainProps.batchSize();
                    for (int b = 0; b < batches && b * size < files.size(); b++) {
                        List<AnalysisFile> batch =
                                files.subList(b * size, Math.min(files.size(), (b + 1) * size));
                        List<FileExplanation> out =
                                fileExplainer.explain(analysis, overview.summary(), batch);
                        for (AnalysisFile f : batch) {
                            FileExplanation e =
                                    out.stream()
                                            .filter(x -> x.fileId() == f.getId())
                                            .findFirst()
                                            .orElse(null);
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("path", f.getPath());
                            row.put("layer", f.getLayer());
                            row.put("role", e == null ? null : e.role());
                            row.put("explanation", e == null ? null : e.summary());
                            explained.add(row);
                        }
                    }
                    result.put("files", explained);
                }
            } catch (AnalysisFailedException e) {
                result.put("error", e.code().name());
            } catch (RuntimeException e) {
                // 앱에서는 FallbackLlmClient가 받는 예외(LlmUnavailableException)다. 이 수준만 실패로 기록하고 다음 수준으로
                // 간다
                result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            result.put("calls", llm.callsOf(level));
            results.put(level.name().toLowerCase(Locale.ROOT), result);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("fixture", fixture);
        report.put("split", source.split());
        report.put("contamination", source.contamination());
        report.put("stage", stage);
        report.put(
                "selectedFiles",
                files.stream().map(f -> f.getPath() + " [" + f.getLayer() + "]").toList());
        report.put("apis", apis);
        report.put("results", results);
        report.put("totals", llm.totals());

        Files.createDirectories(OUT);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path out = OUT.resolve(stamp + "-" + safe(fixture) + "-" + stage + ".json");
        Files.writeString(out, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        System.out.printf(
                "[prompt-eval] fixture=%s split=%s stage=%s levels=%s files=%d apis=%d%n",
                fixture, source.split(), stage, levels, files.size(), apis.size());
        System.out.printf("[prompt-eval] totals=%s%n", llm.totals());
        System.out.printf("[prompt-eval] result=%s%n", ROOT.relativize(out));
    }

    /** 실제 파이프라인의 SELECTING 단계와 같다 (인덱스 전체에 선별 표시 → 비밀 가림). 파일 id는 저장 순서(선별 파일 먼저)대로 1부터. */
    private static List<AnalysisFile> select(
            Path zip, AnalysisProperties props, SelectionProperties selectionProps) {
        List<SourceFile> read = new ZipArchiveReader(props).read(zip).files();
        List<RankedFile> ranked =
                new ImportanceRanker(selectionProps).rank(read, props.maxSelectedFiles());
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

    private record Fixture(Path zip, String name, String split, String contamination) {}

    /** ZIP 경로면 그대로, 데이터셋 id면 고정 커밋 아카이브를 받아 {@code paths}만 남긴 ZIP을 만든다 (캐시). */
    private static Fixture fixture(String fixture) throws IOException, InterruptedException {
        Path asPath = ROOT.resolve(fixture);
        if (fixture.toLowerCase(Locale.ROOT).endsWith(".zip") && Files.exists(asPath)) {
            String name = asPath.getFileName().toString().replaceFirst("\\.zip$", "");
            return new Fixture(asPath, name, "unknown", "unknown");
        }
        JsonNode repo =
                findDataset(JSON.readTree(ROOT.resolve("data/datasets.json").toFile()), fixture);
        if (repo == null) {
            throw new IllegalArgumentException("data/datasets.json에 없는 fixture: " + fixture);
        }
        String url = repo.path("url").asString().replaceFirst("/$", "");
        String commit = repo.path("commit").asString();
        String[] parts = url.split("/");
        String name = parts[parts.length - 1];
        Path cache = OUT.resolve("repos");
        Files.createDirectories(cache);
        Path archive = cache.resolve(name + "-" + commit + ".zip");
        if (!Files.exists(archive)) {
            URI uri =
                    URI.create(
                            "https://codeload.github.com/"
                                    + parts[parts.length - 2]
                                    + "/"
                                    + name
                                    + "/zip/"
                                    + commit);
            HttpResponse<Path> response =
                    HttpClient.newBuilder()
                            .followRedirects(HttpClient.Redirect.NORMAL)
                            .build()
                            .send(
                                    HttpRequest.newBuilder(uri).build(),
                                    HttpResponse.BodyHandlers.ofFile(archive));
            if (response.statusCode() != 200) {
                Files.deleteIfExists(archive);
                throw new IOException(uri + " 응답 " + response.statusCode());
            }
        }
        List<String> paths = new ArrayList<>();
        repo.path("paths").forEach(p -> paths.add(p.asString().replaceAll("^/+|/+$", "")));
        Path filtered = cache.resolve(safe(fixture) + ".zip");
        filter(archive, filtered, paths);
        return new Fixture(
                filtered,
                name,
                repo.path("split").asString(),
                repo.path("contamination").asString("unknown"));
    }

    private static JsonNode findDataset(JsonNode node, String id) {
        if (node.isObject() && id.equals(node.path("id").asString(null))) {
            return node;
        }
        for (JsonNode child : node) {
            JsonNode found = findDataset(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** 아카이브 최상위 폴더 아래에서 {@code paths}(빈 문자열이면 전체)에 속한 항목만 복사한다. */
    private static void filter(Path archive, Path target, List<String> paths) throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile());
                OutputStream os = Files.newOutputStream(target);
                ZipOutputStream out = new ZipOutputStream(os)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                int slash = name.indexOf('/');
                String inner = slash < 0 ? name : name.substring(slash + 1);
                boolean keep =
                        !entry.isDirectory()
                                && paths.stream()
                                        .anyMatch(p -> p.isEmpty() || inner.startsWith(p + "/"));
                if (!keep) {
                    continue;
                }
                out.putNextEntry(new ZipEntry(name));
                try (InputStream in = zip.getInputStream(entry)) {
                    in.transferTo(out);
                }
                out.closeEntry();
            }
        }
    }

    /** 앱과 같은 방식(application.yml + ../.env)으로 설정을 읽는다. */
    private static Binder binder() {
        StandardEnvironment environment = new StandardEnvironment();
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        return Binder.get(environment);
    }

    private static String option(String name, String fallback) {
        String value = System.getProperty("promptEval." + name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String safe(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /** 호출마다 단계·시도·토큰·지연·원문을 기록하고, 호출 사이에 {@code pauseMs}만큼 쉰다. */
    static final class RecordingLlmClient implements LlmClient {

        private final LlmClient delegate;
        private final long pauseMs;
        private final Map<Level, List<Map<String, Object>>> calls = new LinkedHashMap<>();
        private Level current;
        private boolean first = true;

        RecordingLlmClient(LlmClient delegate, long pauseMs) {
            this.delegate = delegate;
            this.pauseMs = pauseMs;
        }

        void startLevel(Level level) {
            current = level;
            calls.put(level, new ArrayList<>());
        }

        List<Map<String, Object>> callsOf(Level level) {
            return calls.getOrDefault(level, List.of());
        }

        Map<String, Object> totals() {
            List<Map<String, Object>> all = calls.values().stream().flatMap(List::stream).toList();
            Map<String, Object> totals = new LinkedHashMap<>();
            totals.put("calls", all.size());
            totals.put("inputTokens", sum(all, "inputTokens"));
            totals.put("outputTokens", sum(all, "outputTokens"));
            totals.put("latencyMs", sum(all, "latencyMs"));
            return totals;
        }

        private static long sum(List<Map<String, Object>> rows, String key) {
            return rows.stream()
                    .map(r -> r.get(key))
                    .filter(Number.class::isInstance)
                    .mapToLong(v -> ((Number) v).longValue())
                    .sum();
        }

        /** 일시적인 서버 오류만 다시 보낸다. 429(한도 초과)는 기다려도 그날은 풀리지 않으므로 다시 보내지 않는다. */
        private static final List<String> TRANSIENT =
                List.of(" 500", " 502", " 503", " 504", "Timeout");

        private static final long[] BACKOFF_MS = {20_000, 40_000};

        @Override
        public LlmResponse complete(LlmRequest request) {
            for (int retry = 0; ; retry++) {
                try {
                    return attempt(request, retry == 0 ? pauseMs : BACKOFF_MS[retry - 1]);
                } catch (RuntimeException e) {
                    String message = String.valueOf(e.getMessage());
                    boolean transientError = TRANSIENT.stream().anyMatch(message::contains);
                    if (!transientError || retry >= BACKOFF_MS.length) {
                        throw e;
                    }
                    System.out.printf(
                            "[prompt-eval] %s → %d초 뒤 다시 시도%n", message, BACKOFF_MS[retry] / 1000);
                }
            }
        }

        private LlmResponse attempt(LlmRequest request, long waitMs) {
            if (!first && waitMs > 0) {
                sleep(waitMs);
            }
            first = false;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stage", request.stage().name());
            row.put("promptChars", request.prompt().length());
            long start = System.nanoTime();
            try {
                LlmResponse response = delegate.complete(request);
                row.put("model", response.model());
                row.put("inputTokens", response.inputTokens());
                row.put("outputTokens", response.outputTokens());
                row.put("latencyMs", (System.nanoTime() - start) / 1_000_000);
                row.put("content", response.content());
                return response;
            } catch (RuntimeException e) {
                row.put("latencyMs", (System.nanoTime() - start) / 1_000_000);
                row.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
                throw e;
            } finally {
                calls.get(current).add(row);
            }
        }

        private static void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
