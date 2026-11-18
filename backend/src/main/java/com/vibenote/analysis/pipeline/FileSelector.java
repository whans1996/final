package com.vibenote.analysis.pipeline;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** layer 판정과 선별 (F-05a 5.2절). 이후 단계(F-06 설명 등)는 여기서 고른 파일만 쓴다. */
public final class FileSelector {

    private static final Pattern APPLICATION_CONFIG =
            Pattern.compile("application[^/]*\\.(yml|yaml|properties)");
    private static final Set<String> DOMAIN_DIRS = Set.of("domain", "entity", "dto", "model");

    private FileSelector() {}

    /** 판정 순서는 선별 우선순위와 같다 (예: SecurityConfig.java는 domain/ 아래에 있어도 config). */
    public static Layer layer(String path) {
        int slash = path.lastIndexOf('/');
        String name = path.substring(slash + 1);
        String lower = name.toLowerCase(Locale.ROOT);
        List<String> dirs = slash < 0 ? List.of() : List.of(path.substring(0, slash).split("/"));

        if (name.endsWith("Controller.java")) {
            return Layer.CONTROLLER;
        }
        if (name.endsWith("Service.java") || name.endsWith("ServiceImpl.java")) {
            return Layer.SERVICE;
        }
        if (name.endsWith("Mapper.java")
                || name.endsWith("Mapper.xml")
                || name.endsWith("Dao.java")
                || (lower.endsWith(".xml") && dirs.contains("mapper"))) {
            return Layer.MAPPER;
        }
        if (lower.endsWith(".vue")) {
            return Layer.VUE;
        }
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) {
            return Layer.JS;
        }
        if (name.endsWith("Config.java")
                || APPLICATION_CONFIG.matcher(lower).matches()
                || lower.startsWith("build.gradle")
                || lower.equals("package.json")
                || lower.equals("pom.xml")) {
            return Layer.CONFIG;
        }
        if (lower.endsWith(".java") && dirs.stream().anyMatch(DOMAIN_DIRS::contains)) {
            return Layer.DOMAIN;
        }
        return Layer.OTHER;
    }

    /** layer 우선순위, 같은 layer 안에서는 경로 오름차순으로 상위 limit개. */
    public static List<SelectedFile> select(List<SourceFile> files, int limit) {
        return files.stream()
                .map(f -> new SelectedFile(f.path(), layer(f.path()), f.content()))
                .sorted(Comparator.comparing(SelectedFile::layer).thenComparing(SelectedFile::path))
                .limit(limit)
                .toList();
    }
}
