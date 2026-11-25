package com.vibenote.analysis.pipeline;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * layer 판정과 선별 (F-05a 5.2절, F-26a 5.3절). 이후 단계(F-06 설명 등)는 여기서 고른 파일만 쓴다.
 *
 * <p>판정 규칙 중 경로만 보는 것(doc·test·migration)은 ZIP을 읽기 전에 인덱스 묶음을 나누는 데도 쓴다 ({@link #layer}에 content
 * null).
 */
public final class FileSelector {

    private static final Pattern APPLICATION_CONFIG =
            Pattern.compile("application[^/]*\\.(yml|yaml|properties)");
    private static final Set<String> DOMAIN_DIRS = Set.of("domain", "entity", "dto", "model");
    private static final Set<String> TEST_DIRS = Set.of("test", "tests", "__tests__", "e2e");
    private static final Pattern TEST_NAME =
            Pattern.compile(".+\\.(?:test|spec)\\.(?:java|js|mjs|vue|ts|tsx|jsx)");
    private static final Pattern JAVA_TEST_NAME = Pattern.compile(".+(?:Test|Tests|IT)\\.java");
    private static final Set<String> MIGRATION_DIRS = Set.of("migration", "migrations");
    private static final Pattern MIGRATION_FILE = Pattern.compile(".+\\.(?:sql|xml|ya?ml)");
    private static final Pattern FLYWAY_NAME = Pattern.compile("V\\d+(?:[._]\\d+)*__.+\\.sql");
    private static final Pattern JS_CONFIG = Pattern.compile(".+\\.config\\.(?:js|mjs|cjs|ts)");
    private static final Pattern JS_EXTENSION = Pattern.compile(".+\\.(?:js|mjs|ts|tsx|jsx)");

    /** {@code @RestControllerAdvice}·{@code @ControllerAdvice}는 단어 경계에서 걸러진다. */
    private static final Pattern CONTROLLER_ANNOTATION =
            Pattern.compile("@(?:\\w+\\.)*(?:Rest)?Controller\\b");

    /** {@code @ServiceActivator} 등은 단어 경계에서 걸러진다. */
    private static final Pattern SERVICE_ANNOTATION = Pattern.compile("@(?:\\w+\\.)*Service\\b");

    private static final Pattern MAPPER_ANNOTATION =
            Pattern.compile("@(?:\\w+\\.)*(?:Repository|Mapper)\\b");

    /** {@code @ConfigurationProperties}는 단어 경계에서 걸러진다. */
    private static final Pattern CONFIGURATION_ANNOTATION =
            Pattern.compile("@(?:\\w+\\.)*Configuration\\b");

    private static final Pattern DOMAIN_ANNOTATION =
            Pattern.compile("@(?:\\w+\\.)*(?:Entity|Table)\\b");

    private FileSelector() {}

    /**
     * 판정 순서는 F-26a 5.3절 표와 같다 (위에서 먼저 맞는 것). 예: SecurityConfig.java는 domain/ 아래에 있어도 config, {@code
     * vite.config.js}는 js가 아니라 config. 어노테이션은 {@code .java}의 주석 밖에서만 본다. 파일명이 {@code
     * *Controller.java}가 아니어도 {@code @RestController}·{@code @Controller}가 있으면 controller다 (F-07
     * AC12, 예: {@code ArticlesApi.java}).
     *
     * @param content null이면 경로만으로 판정한다 (어노테이션 규칙을 건너뛴다)
     */
    public static Layer layer(String path, String content) {
        int slash = path.lastIndexOf('/');
        String name = path.substring(slash + 1);
        String lower = name.toLowerCase(Locale.ROOT);
        List<String> dirs = slash < 0 ? List.of() : List.of(path.substring(0, slash).split("/"));

        if (lower.endsWith(".md")) {
            return Layer.DOC;
        }
        if (dirs.stream().anyMatch(TEST_DIRS::contains)
                || TEST_NAME.matcher(lower).matches()
                || JAVA_TEST_NAME.matcher(name).matches()) {
            return Layer.TEST;
        }
        if ((MIGRATION_FILE.matcher(lower).matches() && isMigrationDir(dirs))
                || FLYWAY_NAME.matcher(name).matches()) {
            return Layer.MIGRATION;
        }

        String code = lower.endsWith(".java") && content != null ? JavaComments.strip(content) : "";
        if (name.endsWith("Controller.java") || CONTROLLER_ANNOTATION.matcher(code).find()) {
            return Layer.CONTROLLER;
        }
        if (name.endsWith("Service.java")
                || name.endsWith("ServiceImpl.java")
                || SERVICE_ANNOTATION.matcher(code).find()) {
            return Layer.SERVICE;
        }
        if (name.endsWith("Mapper.java")
                || name.endsWith("Mapper.xml")
                || name.endsWith("Dao.java")
                || name.endsWith("Repository.java")
                || (lower.endsWith(".xml") && dirs.contains("mapper"))
                || MAPPER_ANNOTATION.matcher(code).find()) {
            return Layer.MAPPER;
        }
        if (lower.endsWith(".vue")) {
            return Layer.VUE;
        }
        if (name.endsWith("Config.java")
                || APPLICATION_CONFIG.matcher(lower).matches()
                || lower.startsWith("build.gradle")
                || lower.equals("package.json")
                || lower.equals("pom.xml")
                || JS_CONFIG.matcher(lower).matches()
                || CONFIGURATION_ANNOTATION.matcher(code).find()) {
            return Layer.CONFIG;
        }
        if (JS_EXTENSION.matcher(lower).matches()) {
            return Layer.JS;
        }
        if (lower.endsWith(".java")
                && (dirs.stream().anyMatch(DOMAIN_DIRS::contains)
                        || DOMAIN_ANNOTATION.matcher(code).find())) {
            return Layer.DOMAIN;
        }
        return Layer.OTHER;
    }

    /** {@code db/migration}·{@code migrations}·{@code migration}·{@code db/changelog} 아래. */
    private static boolean isMigrationDir(List<String> dirs) {
        for (int i = 0; i < dirs.size(); i++) {
            if (MIGRATION_DIRS.contains(dirs.get(i))
                    || (dirs.get(i).equals("changelog") && i > 0 && dirs.get(i - 1).equals("db"))) {
                return true;
            }
        }
        return false;
    }

    /** layer 우선순위, 같은 layer 안에서는 경로 오름차순으로 상위 limit개. test·migration·doc은 고르지 않는다. */
    public static List<SelectedFile> select(List<SourceFile> files, int limit) {
        return files.stream()
                .map(f -> new SelectedFile(f.path(), layer(f.path(), f.content()), f.content()))
                .filter(f -> !f.layer().indexOnly())
                .sorted(Comparator.comparing(SelectedFile::layer).thenComparing(SelectedFile::path))
                .limit(limit)
                .toList();
    }
}
