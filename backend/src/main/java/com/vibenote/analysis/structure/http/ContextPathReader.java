package com.vibenote.analysis.structure.http;

import com.vibenote.analysis.domain.AnalysisFile;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * 선별된 {@code application.yml}·{@code application.properties}의 {@code server.servlet.context-path}
 * (F-21a AC11). 리터럴 값일 때만 쓰고 {@code ${…}}이거나 프로필 문서에만 있으면 쓰지 않는다.
 */
public final class ContextPathReader {

    private static final String KEY = "server.servlet.context-path";

    private ContextPathReader() {}

    /** context-path. 없으면 빈 문자열. */
    public static String read(List<AnalysisFile> files) {
        return files.stream()
                .sorted(Comparator.comparing(AnalysisFile::getId))
                .map(ContextPathReader::read)
                .flatMap(Optional::stream)
                .findFirst()
                .orElse("");
    }

    private static Optional<String> read(AnalysisFile file) {
        String name = file.getPath().substring(file.getPath().lastIndexOf('/') + 1);
        String content = file.getContent() == null ? "" : file.getContent();
        if (name.equals("application.properties")) {
            // 같은 키가 여러 번이면 마지막 값이 쓰인다
            String last = null;
            for (String line : content.split("\\R")) {
                int sep = indexOfSeparator(line);
                if (sep > 0 && line.substring(0, sep).strip().equals(KEY)) {
                    last = line.substring(sep + 1).strip();
                }
            }
            return literal(last);
        }
        if (name.equals("application.yml") || name.equals("application.yaml")) {
            try {
                LoaderOptions options = new LoaderOptions();
                options.setMaxAliasesForCollections(10);
                // 프로필 없는 문서가 여럿이면 뒤 문서가 앞 문서를 덮어쓴다
                Object last = null;
                for (Object doc : new Yaml(new SafeConstructor(options)).loadAll(content)) {
                    if (doc instanceof Map<?, ?> map && !hasProfile(map)) {
                        Object value = path(map, KEY);
                        if (value != null) {
                            last = value;
                        }
                    }
                }
                return literal(last);
            } catch (RuntimeException e) {
                // 읽지 못하는 YAML이면 context-path가 없는 것으로 본다
            }
        }
        return Optional.empty();
    }

    private static int indexOfSeparator(String line) {
        int eq = line.indexOf('=');
        int colon = line.indexOf(':');
        return eq < 0 ? colon : (colon < 0 ? eq : Math.min(eq, colon));
    }

    private static boolean hasProfile(Map<?, ?> doc) {
        return path(doc, "spring.config.activate.on-profile") != null
                || path(doc, "spring.profiles") != null;
    }

    /** 점 경로로 중첩 맵이나 평평한 키를 찾는다. */
    private static Object path(Map<?, ?> map, String dotted) {
        if (map.containsKey(dotted)) {
            return map.get(dotted);
        }
        int dot = dotted.indexOf('.');
        while (dot > 0) {
            Object child = map.get(dotted.substring(0, dot));
            if (child instanceof Map<?, ?> nested) {
                Object found = path(nested, dotted.substring(dot + 1));
                if (found != null) {
                    return found;
                }
            }
            dot = dotted.indexOf('.', dot + 1);
        }
        return null;
    }

    private static Optional<String> literal(Object value) {
        if (!(value instanceof String s) || s.isBlank() || s.contains("${")) {
            return Optional.empty();
        }
        String path = s.strip();
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(path.startsWith("/") ? path : "/" + path);
    }
}
