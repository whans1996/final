package com.vibenote.analysis.structure.http;

import com.vibenote.analysis.explain.ApiEndpoint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * FE 요청 ↔ 탐지한 API (F-21a AC11).
 *
 * <p>서버 경로 = context-path + API path를 세그먼트 단위로 비교한다. API의 {@code {name}}·{@code *}은 1세그먼트({@code
 * {name:regex}}는 FE 세그먼트가 리터럴이면 정규식까지), {@code **}·{@code {*name}}은 0개 이상이고 FE 와일드카드는 API 리터럴과도
 * 맞는다. 후보 중 리터럴 세그먼트 많은 것 → 변수 적은 것 → {@code **} 없는 것을 고르고, 남은 최상위 후보의 파일이 하나면 그 파일이다.
 */
public final class ApiPathMatcher {

    /** 정규식 변수는 이 길이 이하의 세그먼트에만 적용한다 (5.1절). */
    static final int MAX_REGEX_INPUT = 200;

    private static final Comparator<Candidate> MOST_SPECIFIC =
            Comparator.comparingInt(Candidate::literals)
                    .reversed()
                    .thenComparingInt(Candidate::variables)
                    .thenComparing(Candidate::multi);

    private ApiPathMatcher() {}

    public static ApiMatch match(List<ApiEndpoint> apis, String contextPath, FeRequest request) {
        String prefix = contextPath == null ? "" : contextPath;
        List<Candidate> candidates = new ArrayList<>();
        for (ApiEndpoint api : apis) {
            if (api.fileId() == null || !methodMatches(api.method(), request.method())) {
                continue;
            }
            List<ApiSegment> pattern = parse(prefix + "/" + api.path());
            if (matches(pattern, 0, request.path(), 0)) {
                candidates.add(Candidate.of(api.fileId(), pattern));
            }
        }
        if (candidates.isEmpty()) {
            return ApiMatch.NONE;
        }
        Candidate best = candidates.stream().min(MOST_SPECIFIC).orElseThrow();
        List<Long> files =
                candidates.stream()
                        .filter(c -> MOST_SPECIFIC.compare(c, best) == 0)
                        .map(Candidate::fileId)
                        .distinct()
                        .toList();
        return files.size() == 1 ? ApiMatch.matched(files.get(0)) : ApiMatch.AMBIGUOUS;
    }

    private static boolean methodMatches(String apiMethod, String requestMethod) {
        return requestMethod == null
                || "ANY".equals(apiMethod)
                || requestMethod.equalsIgnoreCase(apiMethod);
    }

    private static List<ApiSegment> parse(String path) {
        List<ApiSegment> segments = new ArrayList<>();
        for (String s : path.split("/")) {
            if (!s.isEmpty()) {
                segments.add(ApiSegment.of(s));
            }
        }
        return segments;
    }

    private static boolean matches(List<ApiSegment> pattern, int i, List<PathSegment> path, int j) {
        if (i == pattern.size()) {
            return j == path.size();
        }
        ApiSegment segment = pattern.get(i);
        if (segment.type() == Type.MULTI) {
            for (int k = j; k <= path.size(); k++) {
                if (matches(pattern, i + 1, path, k)) {
                    return true;
                }
            }
            return false;
        }
        if (j == path.size() || !segment.accepts(path.get(j))) {
            return false;
        }
        return matches(pattern, i + 1, path, j + 1);
    }

    private enum Type {
        LITERAL,
        VARIABLE,
        MULTI
    }

    /** API 경로 세그먼트. regex는 {@code {name:regex}}에서 컴파일된 것만 있다. */
    private record ApiSegment(Type type, String literal, Pattern regex) {

        static ApiSegment of(String s) {
            if (s.equals("**") || (s.startsWith("{*") && s.endsWith("}"))) {
                return new ApiSegment(Type.MULTI, null, null);
            }
            if (s.startsWith("{") && s.endsWith("}") && s.indexOf(':') > 0) {
                String regex = s.substring(s.indexOf(':') + 1, s.length() - 1);
                try {
                    return new ApiSegment(Type.VARIABLE, null, Pattern.compile(regex));
                } catch (PatternSyntaxException e) {
                    return new ApiSegment(Type.VARIABLE, null, null);
                }
            }
            if (s.contains("{") || s.contains("*")) {
                return new ApiSegment(Type.VARIABLE, null, null);
            }
            return new ApiSegment(Type.LITERAL, s, null);
        }

        boolean accepts(PathSegment fe) {
            if (fe.wildcard()) {
                return true;
            }
            return switch (type) {
                case LITERAL -> literal.equals(fe.literal());
                case VARIABLE ->
                        regex == null
                                || (fe.literal().length() <= MAX_REGEX_INPUT
                                        && regex.matcher(fe.literal()).matches());
                case MULTI -> true;
            };
        }
    }

    private record Candidate(long fileId, int literals, int variables, boolean multi) {

        static Candidate of(long fileId, List<ApiSegment> pattern) {
            int literals = 0;
            int variables = 0;
            boolean multi = false;
            for (ApiSegment s : pattern) {
                switch (s.type()) {
                    case LITERAL -> literals++;
                    case VARIABLE -> variables++;
                    case MULTI -> multi = true;
                }
            }
            return new Candidate(fileId, literals, variables, multi);
        }
    }
}
