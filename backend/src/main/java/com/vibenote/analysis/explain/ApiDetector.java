package com.vibenote.analysis.explain;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.pipeline.JavaComments;
import com.vibenote.analysis.pipeline.Layer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spring 컨트롤러의 매핑 어노테이션으로 API 목록을 만든다 (F-06 AC7). LLM이 아니라 규칙으로 탐지해 개요의 {@code apis}와 F-07의 선택지로
 * 쓴다.
 *
 * <p>controller layer {@code .java} 파일만 본다. 주석을 지운 뒤, 첫 {@code class}·{@code interface} 선언 앞의
 * {@code @RequestMapping}을 클래스 경로로, 그 뒤의 매핑 어노테이션을 메서드 경로로 읽는다. 경로는 문자열 리터럴 하나나 리터럴 배열만 인식한다. 메서드
 * 경로가 상수·연결식이면 그 메서드를, 클래스 경로가 그러면 그 파일을 건너뛴다 (F-07 AC13).
 */
public final class ApiDetector {

    public static final int MAX_APIS = 50;

    /** analysis_flows.endpoint(200자)에 {@code "{METHOD} {path}"}로 들어가도록 이보다 긴 경로는 뺀다 (F-07 5절). */
    static final int MAX_PATH_LENGTH = 190;

    private static final Pattern MAPPING =
            Pattern.compile("@(?:\\w+\\.)*(Get|Post|Put|Patch|Delete|Request)Mapping\\b");
    private static final Pattern TYPE_DECLARATION =
            Pattern.compile("\\b(?:class|interface|record|enum)\\s+\\w+");
    private static final Pattern STRING = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern HTTP_METHOD =
            Pattern.compile("\\b(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS|TRACE)\\b");
    private static final Pattern ATTRIBUTE =
            Pattern.compile("^\\s*(\\w+)\\s*=(.*)$", Pattern.DOTALL);
    private static final Pattern SLASHES = Pattern.compile("/{2,}");

    private ApiDetector() {}

    public static List<ApiEndpoint> detect(List<AnalysisFile> files) {
        return scan(files).apis();
    }

    /**
     * 탐지 결과와 그 목록이 불완전한지.
     *
     * @param incomplete {@link #MAX_APIS}에서 잘렸거나, 경로를 알 수 없거나 너무 길어 건너뛴 매핑이 있다. 이때 목록에 없는 API가 요청을
     *     받을 수 있다 (F-21a 5.3절 5)
     */
    public record Detection(List<ApiEndpoint> apis, boolean incomplete) {}

    public static Detection scan(List<AnalysisFile> files) {
        List<ApiEndpoint> found = new ArrayList<>();
        boolean[] skipped = {false};
        for (AnalysisFile file : files) {
            if (Layer.CONTROLLER.value().equals(file.getLayer())
                    && file.getPath().toLowerCase(Locale.ROOT).endsWith(".java")) {
                found.addAll(detect(file.getId(), JavaComments.strip(file.getContent()), skipped));
            }
        }
        Map<String, ApiEndpoint> unique = new LinkedHashMap<>();
        found.stream()
                .sorted(
                        Comparator.comparing(ApiEndpoint::path)
                                .thenComparing(ApiEndpoint::method)
                                .thenComparing(
                                        ApiEndpoint::fileId,
                                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(api -> unique.putIfAbsent(api.method() + " " + api.path(), api));
        return new Detection(
                unique.values().stream().limit(MAX_APIS).toList(),
                skipped[0] || unique.size() > MAX_APIS);
    }

    /** skipped[0]은 건너뛴 매핑이 있으면 true가 된다. */
    private static List<ApiEndpoint> detect(Long fileId, String code, boolean[] skipped) {
        Matcher type = TYPE_DECLARATION.matcher(code);
        int typeStart = type.find() ? type.start() : 0;

        List<String> classPaths = List.of("");
        List<ApiEndpoint> apis = new ArrayList<>();
        Matcher m = MAPPING.matcher(code);
        while (m.find()) {
            String kind = m.group(1);
            String args = arguments(code, m.end());
            Mapping mapping = parse(args);
            if (m.start() < typeStart) {
                if (kind.equals("Request") && mapping == null) {
                    // 클래스 경로가 상수 등이라 알 수 없으면 틀린 경로를 확정된 API처럼 남기지 않는다
                    skipped[0] = true;
                    return List.of();
                }
                if (kind.equals("Request") && !mapping.paths().isEmpty()) {
                    classPaths = mapping.paths();
                }
                continue;
            }
            if (mapping == null) {
                skipped[0] = true;
                continue;
            }
            List<String> methods =
                    kind.equals("Request")
                            ? (mapping.methods().isEmpty() ? List.of("ANY") : mapping.methods())
                            : List.of(kind.toUpperCase(Locale.ROOT));
            List<String> paths = mapping.paths().isEmpty() ? List.of("") : mapping.paths();
            for (String base : classPaths) {
                for (String path : paths) {
                    String full = normalize(base + "/" + path);
                    if (full.length() > MAX_PATH_LENGTH) {
                        skipped[0] = true;
                        continue;
                    }
                    for (String method : methods) {
                        apis.add(new ApiEndpoint(method, full, fileId));
                    }
                }
            }
        }
        return apis;
    }

    /** 어노테이션 바로 뒤 괄호 안 내용. 괄호가 없으면 빈 문자열. */
    private static String arguments(String code, int from) {
        int i = from;
        while (i < code.length() && Character.isWhitespace(code.charAt(i))) {
            i++;
        }
        if (i >= code.length() || code.charAt(i) != '(') {
            return "";
        }
        int depth = 0;
        boolean inString = false;
        for (int j = i; j < code.length(); j++) {
            char c = code.charAt(j);
            if (inString) {
                if (c == '\\') {
                    j++;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return code.substring(i + 1, j);
            }
        }
        return code.substring(i + 1);
    }

    /**
     * 경로·메서드 속성. 경로 속성 값이 문자열 리터럴 하나나 {@code {리터럴, …}} 배열이 아니면(연결식·상수 등) 경로를 알 수 없으므로 null (F-07
     * AC13).
     */
    private static Mapping parse(String args) {
        List<String> paths = new ArrayList<>();
        List<String> methods = new ArrayList<>();
        for (String segment : splitTopLevel(args)) {
            if (segment.isBlank()) {
                continue;
            }
            Matcher attr = ATTRIBUTE.matcher(segment);
            String name = attr.matches() ? attr.group(1) : "value";
            String value = attr.matches() ? attr.group(2) : segment;
            if (name.equals("value") || name.equals("path")) {
                List<String> literals = literals(value);
                if (literals == null) {
                    return null;
                }
                paths.addAll(literals);
            } else if (name.equals("method")) {
                Matcher hm = HTTP_METHOD.matcher(value);
                while (hm.find()) {
                    methods.add(hm.group(1));
                }
            }
        }
        return new Mapping(paths, methods);
    }

    /** 괄호·중괄호·문자열 밖의 쉼표로 나눈다. */
    private static List<String> splitTopLevel(String args) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        int start = 0;
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == '{' || c == '(') {
                depth++;
            } else if (c == '}' || c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(args.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(args.substring(start));
        return parts;
    }

    /** 리터럴 하나 또는 리터럴 배열의 값. 그 밖의 식이거나 빈 배열이면 null. */
    private static List<String> literals(String value) {
        String v = value.strip();
        List<String> parts =
                v.startsWith("{") && v.endsWith("}")
                        ? splitTopLevel(v.substring(1, v.length() - 1))
                        : List.of(v);
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            Matcher sm = STRING.matcher(part.strip());
            if (!sm.matches()) {
                return null;
            }
            result.add(sm.group(1));
        }
        return result.isEmpty() ? null : result;
    }

    /** 슬래시 하나로 시작하고, 연속 슬래시는 하나로, 끝 슬래시는 뗀다(루트는 "/"). */
    static String normalize(String path) {
        String p = SLASHES.matcher("/" + path).replaceAll("/");
        if (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private record Mapping(List<String> paths, List<String> methods) {}
}
