package com.vibenote.prompteval;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.Level;
import com.vibenote.concept.ConceptDraftService;
import com.vibenote.llm.LiveLlmClients;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmProperties;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 프롬프트 평가 하네스. <b>사람이 요청할 때만</b> {@code ./gradlew promptEval}로 실행한다 (실제 GMS 크레딧·Gemini 무료 한도를 쓴다).
 * {@code test}(CI·git hook·verify)에서는 제외된다. 결과 해석은 {@code prompt-evaluator} 에이전트가 한다.
 *
 * <p>실제 파이프라인과 같은 경로로 돈다 ({@link EvalPipeline}): ZIP 읽기 → 선별 → 비밀 가림 → API 탐지 → 개요 → 파일 설명 → 흐름 →
 * 개념 → 퀴즈. 개념 카탈로그는 Testcontainers DB에 Flyway로 적용한 시드 카드다 (앱과 같다, E-1 AC2). 호출마다 원문 응답·토큰·지연을 기록해
 * {@code .harness/prompt-eval/}에 JSON으로 남긴다.
 *
 * <p>공급자는 환경변수 {@code LLM_PROVIDER}(gms|gemini|ollama)로 고르고 그 하나만 폴백 없이 부른다 (F-27 AC13).
 *
 * <p>옵션 (Gradle {@code -P}):
 *
 * <ul>
 *   <li>{@code fixture}: {@code data/datasets.json}의 저장소 id(예: DS-02-01. 고정 커밋을 codeload에서 받아 캐시)
 *       또는 ZIP 경로
 *   <li>{@code stage}: {@code overview}(개요) · {@code file}(개요 + 파일) · {@code flow}(개요 + 흐름) ·
 *       {@code concept}(개념) · {@code quiz}(개념 + 퀴즈) · {@code all}(다섯 단계)
 *   <li>{@code level}: {@code beginner|junior|senior|all}
 *   <li>{@code batches}: 파일 설명 묶음 수 상한 (기본 전부)
 *   <li>{@code flows}: 수준마다 만드는 흐름 수, 탐지 순서 앞 N개 (기본 3)
 *   <li>{@code pauseMs}: 호출 사이 대기 (기본 4000, 무료 한도의 분당 요청 수 보호)
 * </ul>
 *
 * <p>평가 데이터(split=eval) 결과는 점수로, dev 결과는 "개발용 참고"로 나눠 보고한다 ({@code docs/산출물/09-데이터셋.md} 5절). 이 러너의
 * 출력은 프롬프트·few-shot·fixture 작성에 쓰지 않는다.
 */
@Tag("prompt-eval")
class PromptEvalRunner extends IntegrationTestBase {

    static final Path ROOT = Path.of("..").toAbsolutePath().normalize();
    static final Path OUT = ROOT.resolve(".harness/prompt-eval");
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    @Autowired ConceptDraftService conceptDraftService;

    @Test
    void run() throws Exception {
        String fixture = option("fixture", "DS-02-01");
        String stage = option("stage", "overview").toLowerCase(Locale.ROOT);
        String levelOption = option("level", "all").toUpperCase(Locale.ROOT);
        String batchesOption = option("batches", "all");
        int batches =
                batchesOption.equalsIgnoreCase("all")
                        ? Integer.MAX_VALUE
                        : Integer.parseInt(batchesOption);
        int flows = Integer.parseInt(option("flows", "3"));
        long pauseMs = Long.parseLong(option("pauseMs", "4000"));
        List<Level> levels =
                levelOption.equals("ALL")
                        ? List.of(Level.values())
                        : List.of(Level.valueOf(levelOption));
        EvalPipeline.Options options = new EvalPipeline.Options(stage, levels, batches, flows);

        Binder binder = binder();
        LlmProperties llmProps = binder.bindOrCreate("llm", LlmProperties.class);
        // F-27 AC13: LLM_PROVIDER(gms|gemini|ollama)의 공급자 하나만 폴백 없이 부른다
        Optional<LlmClient> live = LiveLlmClients.single(llmProps);
        assumeTrue(live.isPresent(), llmProps.provider() + " 키가 없어 건너뜀");

        EvalFixture source = fixture(fixture);
        RecordingLlmClient llm = new RecordingLlmClient(live.get(), pauseMs);
        Map<String, Object> report =
                new EvalPipeline(
                                llm,
                                EvalSettings.from(binder),
                                conceptDraftService.findAllForMatching(),
                                llmProps.provider())
                        .run(source, options);

        Files.createDirectories(OUT);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path out = OUT.resolve(stamp + "-" + safe(fixture) + "-" + stage + ".json");
        Files.writeString(out, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        System.out.printf(
                "[prompt-eval] fixture=%s split=%s stage=%s levels=%s index=%d%n",
                fixture, source.split(), stage, levels, ((List<?>) report.get("index")).size());
        System.out.printf("[prompt-eval] totals=%s%n", llm.totals());
        System.out.printf("[prompt-eval] result=%s%n", ROOT.relativize(out));
    }

    /** ZIP 경로면 그대로, 데이터셋 id면 고정 커밋 아카이브를 받아 {@code paths}만 남긴 ZIP을 만든다 (캐시). */
    static EvalFixture fixture(String fixture) throws IOException, InterruptedException {
        Path asPath = ROOT.resolve(fixture);
        if (fixture.toLowerCase(Locale.ROOT).endsWith(".zip") && Files.exists(asPath)) {
            String name = asPath.getFileName().toString().replaceFirst("\\.zip$", "");
            return new EvalFixture(fixture, asPath, name, "unknown", "unknown");
        }
        JsonNode repo = dataset(fixture);
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
        return new EvalFixture(
                fixture,
                filtered,
                name,
                repo.path("split").asString(),
                repo.path("contamination").asString("unknown"));
    }

    /** {@code data/datasets.json}에서 id가 같은 항목. 없으면 null. */
    static JsonNode dataset(String id) {
        return findDataset(JSON.readTree(ROOT.resolve("data/datasets.json").toFile()), id);
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
    static Binder binder() {
        StandardEnvironment environment = new StandardEnvironment();
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        return Binder.get(environment);
    }

    static String option(String name, String fallback) {
        String value = System.getProperty("promptEval." + name);
        return value == null || value.isBlank() ? fallback : value;
    }

    static String safe(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
