package com.vibenote.analysis.structure.http;

import com.vibenote.analysis.explain.ApiEndpoint;
import com.vibenote.analysis.structure.WorkBudget;
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
 *
 * <p>API 경로는 분석마다 한 번 나누고 정규식을 컴파일한다 (요청마다 다시 하지 않는다, 5.3절 3).
 */
public final class ApiPathMatcher {

    /** 정규식 변수는 이 길이 이하의 세그먼트에만 적용한다 (5.1절). */
    static final int MAX_REGEX_INPUT = 200;

    /**
     * 정규식 하나가 입력 글자를 읽을 수 있는 횟수. 업로드한 컨트롤러의 정규식과 FE 경로가 함께 백트래킹을 불릴 수 있어 이 횟수를 넘으면 맞지 않은 것으로 본다
     * (확정한 연결만 그린다).
     */
    static final int MAX_REGEX_STEPS = 100_000;

    private static final Comparator<Candidate> MOST_SPECIFIC =
            Comparator.comparingInt(Candidate::literals)
                    .reversed()
                    .thenComparingInt(Candidate::variables)
                    .thenComparing(Candidate::multi);

    /** context-path + API path를 나누고 이어진 {@code **}를 합친 패턴. */
    private record Compiled(ApiEndpoint api, List<ApiSegment> pattern, int fixed) {}

    private final int prefixFixed;
    private final List<Compiled> compiled = new ArrayList<>();

    public ApiPathMatcher(List<ApiEndpoint> apis, String contextPath) {
        // context-path는 업로드한 설정 파일 값이라 길이 상한이 없다. 한 번만 나눈다
        List<ApiSegment> prefix = compact(parse(contextPath == null ? "" : contextPath));
        this.prefixFixed = fixedCount(prefix);
        for (ApiEndpoint api : apis) {
            if (api.fileId() == null) {
                continue;
            }
            List<ApiSegment> pattern = new ArrayList<>(prefix);
            pattern.addAll(parse(api.path()));
            pattern = compact(pattern);
            compiled.add(new Compiled(api, pattern, fixedCount(pattern)));
        }
    }

    public static ApiMatch match(
            List<ApiEndpoint> apis, String contextPath, FeRequest request, WorkBudget budget) {
        return new ApiPathMatcher(apis, contextPath).match(request, budget);
    }

    public ApiMatch match(FeRequest request, WorkBudget budget) {
        // 고정 세그먼트만으로 요청보다 길면 비교하지 않는다
        if (prefixFixed > request.path().size()) {
            return ApiMatch.NONE;
        }
        List<Candidate> candidates = new ArrayList<>();
        for (Compiled c : compiled) {
            budget.spend();
            if (!methodMatches(c.api().method(), request.method())) {
                continue;
            }
            if (matches(c.pattern(), c.fixed(), request.path(), budget)) {
                candidates.add(Candidate.of(c.api().fileId(), c.pattern()));
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

    /** 이어진 {@code **}는 하나와 같다. 그래서 {@code **} 수는 고정 세그먼트 수 + 1 이하가 된다. */
    private static List<ApiSegment> compact(List<ApiSegment> pattern) {
        List<ApiSegment> out = new ArrayList<>(pattern.size());
        for (ApiSegment s : pattern) {
            if (s.type() != Type.MULTI || out.isEmpty() || out.getLast().type() != Type.MULTI) {
                out.add(s);
            }
        }
        return out;
    }

    private static int fixedCount(List<ApiSegment> pattern) {
        int fixed = 0;
        for (ApiSegment s : pattern) {
            if (s.type() != Type.MULTI) {
                fixed++;
            }
        }
        return fixed;
    }

    /**
     * 패턴 세그먼트 i 이후가 경로 세그먼트 j 이후와 맞는지를 뒤에서부터 채운다 (O(패턴 × 경로) 시간, 행 두 개만 둬 O(경로) 메모리). 고정 세그먼트 수로
     * 길이가 맞을 수 없으면 표를 채우지 않는다.
     */
    private static boolean matches(
            List<ApiSegment> pattern, int fixed, List<PathSegment> path, WorkBudget budget) {
        int p = pattern.size();
        int n = path.size();
        if (fixed > n || (fixed == p && p != n)) {
            return false;
        }
        boolean[] next = new boolean[n + 1];
        boolean[] cur = new boolean[n + 1];
        next[n] = true;
        for (int i = p - 1; i >= 0; i--) {
            budget.spend(n + 1);
            ApiSegment segment = pattern.get(i);
            for (int j = n; j >= 0; j--) {
                if (segment.type() == Type.MULTI) {
                    cur[j] = next[j] || (j < n && cur[j + 1]);
                } else {
                    cur[j] = j < n && next[j + 1] && segment.accepts(path.get(j));
                }
            }
            boolean[] swap = next;
            next = cur;
            cur = swap;
        }
        return next[0];
    }

    private static final Pattern NEVER = Pattern.compile("(?!)");

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
            if (s.startsWith("{") && closingBrace(s, 0) == s.length() - 1 && s.indexOf(':') > 0) {
                String regex = s.substring(s.indexOf(':') + 1, s.length() - 1);
                try {
                    return new ApiSegment(Type.VARIABLE, null, Pattern.compile(regex));
                } catch (PatternSyntaxException e) {
                    return new ApiSegment(Type.VARIABLE, null, null);
                }
            }
            if (s.equals("*") || (s.startsWith("{") && closingBrace(s, 0) == s.length() - 1)) {
                return new ApiSegment(Type.VARIABLE, null, null);
            }
            if (s.contains("{") || s.contains("*")) {
                return new ApiSegment(Type.VARIABLE, null, mixed(s));
            }
            return new ApiSegment(Type.LITERAL, s, null);
        }

        /**
         * {@code {name}.json}·{@code *.json}·{@code v{n:\d+}}처럼 변수와 글자가 섞인 세그먼트를 세그먼트 전체 정규식으로 바꾼다.
         * 글자 부분도 맞아야 한다. 정규식이 잘못됐으면 아무것과도 맞지 않는다 (확정한 연결만 그린다).
         */
        private static Pattern mixed(String s) {
            StringBuilder regex = new StringBuilder();
            StringBuilder literal = new StringBuilder();
            int i = 0;
            while (i < s.length()) {
                char c = s.charAt(i);
                int close = c == '{' ? closingBrace(s, i) : -1;
                if (c == '*' || close > 0) {
                    if (!literal.isEmpty()) {
                        regex.append(Pattern.quote(literal.toString()));
                        literal.setLength(0);
                    }
                    if (c == '*') {
                        regex.append(".*");
                        i++;
                    } else {
                        String inner = s.substring(i + 1, close);
                        int colon = inner.indexOf(':');
                        regex.append(colon < 0 ? ".+" : "(?:" + inner.substring(colon + 1) + ")");
                        i = close + 1;
                    }
                } else {
                    literal.append(c);
                    i++;
                }
            }
            if (!literal.isEmpty()) {
                regex.append(Pattern.quote(literal.toString()));
            }
            try {
                return Pattern.compile(regex.toString());
            } catch (PatternSyntaxException e) {
                return NEVER;
            }
        }

        /** 짝이 맞는 닫는 중괄호 위치. 없으면 -1. */
        private static int closingBrace(String s, int open) {
            int depth = 0;
            for (int i = open; i < s.length(); i++) {
                if (s.charAt(i) == '{') {
                    depth++;
                } else if (s.charAt(i) == '}' && --depth == 0) {
                    return i;
                }
            }
            return -1;
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
                                        && regexMatches(fe.literal()));
                case MULTI -> true;
            };
        }

        private boolean regexMatches(String input) {
            try {
                return regex.matcher(new BudgetedChars(input, new int[] {MAX_REGEX_STEPS}))
                        .matches();
            } catch (RegexBudgetExceeded e) {
                return false;
            }
        }
    }

    /** 글자를 읽을 때마다 예산을 줄이고, 다 쓰면 {@link RegexBudgetExceeded}로 정규식 매칭을 끝낸다. */
    private record BudgetedChars(String text, int[] budget) implements CharSequence {

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public char charAt(int index) {
            if (--budget[0] < 0) {
                throw new RegexBudgetExceeded();
            }
            return text.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new BudgetedChars(text.substring(start, end), budget);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private static final class RegexBudgetExceeded extends RuntimeException {
        RegexBudgetExceeded() {
            super(null, null, false, false);
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
