package com.vibenote.analysis.explain;

import com.vibenote.analysis.domain.AnalysisFile;
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
 * {@code @RequestMapping}을 클래스 경로로, 그 뒤의 매핑 어노테이션을 메서드 경로로 읽는다. 경로는 문자열 리터럴만 인식한다(상수는 무시).
 */
public final class ApiDetector {

    public static final int MAX_APIS = 50;

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
        List<ApiEndpoint> found = new ArrayList<>();
        for (AnalysisFile file : files) {
            if (Layer.CONTROLLER.value().equals(file.getLayer())
                    && file.getPath().toLowerCase(Locale.ROOT).endsWith(".java")) {
                found.addAll(detect(file.getId(), stripComments(file.getContent())));
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
        return unique.values().stream().limit(MAX_APIS).toList();
    }

    private static List<ApiEndpoint> detect(Long fileId, String code) {
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
                if (kind.equals("Request") && mapping != null && !mapping.paths().isEmpty()) {
                    classPaths = mapping.paths();
                }
                continue;
            }
            if (mapping == null) {
                continue;
            }
            List<String> methods =
                    kind.equals("Request")
                            ? (mapping.methods().isEmpty() ? List.of("ANY") : mapping.methods())
                            : List.of(kind.toUpperCase(Locale.ROOT));
            List<String> paths = mapping.paths().isEmpty() ? List.of("") : mapping.paths();
            for (String base : classPaths) {
                for (String path : paths) {
                    for (String method : methods) {
                        apis.add(new ApiEndpoint(method, normalize(base + "/" + path), fileId));
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

    /** 경로·메서드 속성. 경로 속성이 있는데 문자열 리터럴이 없으면(상수 등) 알 수 없으므로 null. */
    private static Mapping parse(String args) {
        List<String> paths = new ArrayList<>();
        List<String> methods = new ArrayList<>();
        boolean unknownPath = false;
        for (String segment : splitTopLevel(args)) {
            if (segment.isBlank()) {
                continue;
            }
            Matcher attr = ATTRIBUTE.matcher(segment);
            String name = attr.matches() ? attr.group(1) : "value";
            String value = attr.matches() ? attr.group(2) : segment;
            if (name.equals("value") || name.equals("path")) {
                List<String> literals = literals(value);
                if (literals.isEmpty()) {
                    unknownPath = true;
                }
                paths.addAll(literals);
            } else if (name.equals("method")) {
                Matcher hm = HTTP_METHOD.matcher(value);
                while (hm.find()) {
                    methods.add(hm.group(1));
                }
            }
        }
        if (unknownPath && paths.isEmpty()) {
            return null;
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

    private static List<String> literals(String value) {
        List<String> result = new ArrayList<>();
        Matcher sm = STRING.matcher(value);
        while (sm.find()) {
            result.add(sm.group(1));
        }
        return result;
    }

    /** 슬래시 하나로 시작하고, 연속 슬래시는 하나로, 끝 슬래시는 뗀다(루트는 "/"). */
    static String normalize(String path) {
        String p = SLASHES.matcher("/" + path).replaceAll("/");
        if (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    /** 문자열·문자 리터럴은 두고 {@code //}·{@code /* *\/} 주석을 공백으로 바꾼다. */
    static String stripComments(String code) {
        StringBuilder out = new StringBuilder(code.length());
        int i = 0;
        int n = code.length();
        while (i < n) {
            char c = code.charAt(i);
            char next = i + 1 < n ? code.charAt(i + 1) : '\0';
            if (c == '"' || c == '\'') {
                int end = i + 1;
                while (end < n && code.charAt(end) != c && code.charAt(end) != '\n') {
                    end += code.charAt(end) == '\\' ? 2 : 1;
                }
                end = Math.min(end + 1, n);
                out.append(code, i, end);
                i = end;
            } else if (c == '/' && next == '/') {
                while (i < n && code.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && next == '*') {
                int end = code.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
                out.append(' ');
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private record Mapping(List<String> paths, List<String> methods) {}
}
