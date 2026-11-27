package com.vibenote.prompteval;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 러너·채점기가 함께 쓰는 경로·옵션·데이터셋 조회. Spring 컨텍스트·Testcontainers를 띄우지 않는다 ({@link PromptEvalRunner}는
 * {@code IntegrationTestBase}를 상속해 클래스를 건드리기만 해도 MySQL 컨테이너가 뜨므로, 채점기와 단위 테스트는 이 클래스만 쓴다).
 */
final class EvalPaths {

    static final Path ROOT = Path.of("..").toAbsolutePath().normalize();
    static final Path OUT = ROOT.resolve(".harness/prompt-eval");
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private EvalPaths() {}

    /** {@code data/datasets.json}에서 id가 같은 항목. 없으면 null. */
    static JsonNode dataset(String id) {
        return findDataset(datasets(), id);
    }

    /**
     * ZIP으로 준 fixture. 파일 이름(확장자 뺀 값)을 id로 쓰고, split·오염은 datasets.json에서 그 id를, 없으면 {@code -}로 끊은
     * 앞쪽 id를 찾는다 (예: 하위 항목이 없는 {@code DS-03-01.zip} → {@code DS-03}). 못 찾으면 {@code unknown}.
     */
    static EvalFixture zipFixture(Path zip) {
        String id = zip.getFileName().toString().replaceFirst("(?i)\\.zip$", "");
        JsonNode datasets = datasets();
        JsonNode entry = null;
        for (String candidate = id;
                entry == null && !candidate.isEmpty();
                candidate =
                        candidate.contains("-")
                                ? candidate.substring(0, candidate.lastIndexOf('-'))
                                : "") {
            entry = findDataset(datasets, candidate);
        }
        String split = entry == null ? "unknown" : entry.path("split").asString("unknown");
        String contamination =
                entry == null ? "unknown" : entry.path("contamination").asString("unknown");
        return new EvalFixture(id, zip, id, split, contamination);
    }

    /**
     * 보고서의 {@code fixtureZip}. 러너를 돌린 PC의 절대 경로라 다른 PC에서 채점하면 없을 수 있으므로, 없으면 {@code repos}(러너 캐시
     * {@code .harness/prompt-eval/repos/})에서 같은 파일 이름을 찾는다. 둘 다 없으면 null.
     */
    static Path locateZip(String recorded, Path repos) {
        if (recorded == null || recorded.isBlank()) {
            return null;
        }
        Path path = Path.of(recorded);
        if (Files.exists(path)) {
            return path;
        }
        Path cached = repos.resolve(path.getFileName().toString());
        return Files.exists(cached) ? cached : null;
    }

    /** 앱과 같은 방식(application.yml + ~/.vibenote/.env)으로 설정을 읽는다. */
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

    static boolean isZip(String fixture) {
        return fixture.toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    private static JsonNode datasets() {
        return JSON.readTree(ROOT.resolve("data/datasets.json").toFile());
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
}
