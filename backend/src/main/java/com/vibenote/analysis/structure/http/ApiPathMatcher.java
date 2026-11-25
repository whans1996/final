package com.vibenote.analysis.structure.http;

import com.vibenote.analysis.explain.ApiEndpoint;
import com.vibenote.analysis.structure.WorkBudget;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * FE 요청 ↔ 탐지한 API (F-21a AC11).
 *
 * <p>서버 경로 = context-path + API path를 세그먼트 단위로 비교한다. API의 {@code {name}}·{@code *}은 1세그먼트({@code
 * {name:regex}}는 FE 세그먼트가 리터럴이면 정규식까지), {@code **}·{@code {*name}}은 0개 이상이고 FE 와일드카드는 API 리터럴과도
 * 맞는다. 후보 중 {@code **} 없는 것 → 리터럴 세그먼트 많은 것 → 변수 적은 것({@code *}는 {@code {v}}보다 무겁다, Spring 순서)을
 * 고르고, 남은 최상위 후보의 파일이 하나면 그 파일이다.
 *
 * <p>FE 와일드카드가 API의 리터럴·정규식 세그먼트와 맞서야만 맞는 후보가 있거나 메서드를 모르면, 실제 요청이 어느 후보로 갈지 모르므로 후보가 모두 한 파일일 때만 그
 * 파일이다 (5.3절 5).
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

    /** 변수 무게. {@code *}는 {@code {v}}보다 덜 구체적이다 (Spring PathPattern 점수와 같은 비율). */
    private static final int VARIABLE_WEIGHT = 1;

    private static final int STAR_WEIGHT = 100;

    private static final Comparator<Candidate> MOST_SPECIFIC =
            Comparator.comparing(Candidate::multi)
                    .thenComparing(Comparator.comparingInt(Candidate::literals).reversed())
                    .thenComparingInt(Candidate::variables);

    /** context-path + API path를 나누고 이어진 {@code **}를 합친 패턴. */
    private record Compiled(ApiEndpoint api, List<ApiSegment> pattern, int fixed) {}

    private final int prefixFixed;
    private final List<Compiled> compiled = new ArrayList<>();

    /** API 목록이 불완전하다. 목록에 없는 더 구체적인 API가 받을 수 있으므로 변수·와일드카드 없는 일치만 확정한다. */
    private final boolean literalOnly;

    public ApiPathMatcher(List<ApiEndpoint> apis, String contextPath) {
        this(apis, contextPath, false);
    }

    public ApiPathMatcher(List<ApiEndpoint> apis, String contextPath, boolean literalOnly) {
        this.literalOnly = literalOnly;
        // context-path는 업로드한 설정 파일 값이라 길이 상한이 없다. 한 번만 나누고 API마다 복사하지 않고 함께 본다
        List<ApiSegment> prefix = compact(parse(contextPath == null ? "" : contextPath));
        this.prefixFixed = fixedCount(prefix);
        boolean prefixEndsMulti = !prefix.isEmpty() && prefix.getLast().type() == Type.MULTI;
        for (ApiEndpoint api : apis) {
            if (api.fileId() == null) {
                continue;
            }
            List<ApiSegment> own = compact(parse(api.path()));
            // 이어진 ** 는 하나와 같다: context-path 끝과 API 앞의 ** 를 합친다
            if (prefixEndsMulti && !own.isEmpty() && own.getFirst().type() == Type.MULTI) {
                own = own.subList(1, own.size());
            }
            compiled.add(new Compiled(api, joined(prefix, own), prefixFixed + fixedCount(own)));
        }
    }

    /** 앞 목록 뒤에 뒤 목록을 이어 보는 읽기 전용 보기. 복사하지 않는다. */
    private static List<ApiSegment> joined(List<ApiSegment> head, List<ApiSegment> tail) {
        if (head.isEmpty()) {
            return tail;
        }
        return new JoinedList(head, tail);
    }

    private static final class JoinedList extends AbstractList<ApiSegment> implements RandomAccess {
        private final List<ApiSegment> head;
        private final List<ApiSegment> tail;

        JoinedList(List<ApiSegment> head, List<ApiSegment> tail) {
            this.head = head;
            this.tail = tail;
        }

        @Override
        public ApiSegment get(int index) {
            return index < head.size() ? head.get(index) : tail.get(index - head.size());
        }

        @Override
        public int size() {
            return head.size() + tail.size();
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
        boolean wildcards = request.path().stream().anyMatch(PathSegment::wildcard);
        List<Candidate> candidates = new ArrayList<>();
        for (Compiled c : compiled) {
            budget.spend();
            if (!methodMatches(c.api().method(), request.method())) {
                continue;
            }
            boolean[] undecided = {false};
            if (matches(c.pattern(), c.fixed(), request.path(), false, budget, undecided)) {
                // 와일드카드가 변수·이중 별표와만 맞서도 맞으면 그 값이 무엇이든 이 후보와 맞는다
                boolean certain =
                        !wildcards
                                || matches(
                                        c.pattern(),
                                        c.fixed(),
                                        request.path(),
                                        true,
                                        budget,
                                        undecided);
                candidates.add(Candidate.of(c.api().fileId(), c.pattern(), certain, undecided[0]));
            }
        }
        if (candidates.isEmpty()) {
            return ApiMatch.NONE;
        }
        // 정규식을 끝까지 보지 못한 후보는 맞는지도, 더 구체적인 후보를 이기는지도 모른다
        if (candidates.stream().anyMatch(Candidate::undecided)) {
            return ApiMatch.AMBIGUOUS;
        }
        if (literalOnly) {
            candidates =
                    wildcards
                            ? List.of()
                            : candidates.stream()
                                    .filter(c -> c.variables() == 0 && !c.multi())
                                    .toList();
            if (candidates.isEmpty()) {
                return ApiMatch.AMBIGUOUS;
            }
        }
        if (request.method() == null || candidates.stream().anyMatch(c -> !c.certain())) {
            List<Long> files = candidates.stream().map(Candidate::fileId).distinct().toList();
            return files.size() == 1 ? ApiMatch.matched(files.get(0)) : ApiMatch.AMBIGUOUS;
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
     * 길이가 맞을 수 없으면 표를 채우지 않는다. strict면 FE 와일드카드는 정규식 없는 변수·이중 별표와만 맞는다. 정규식을 끝까지 보지 못한 세그먼트는 맞는 것으로
     * 채우고 undecided[0]을 켠다.
     */
    private static boolean matches(
            List<ApiSegment> pattern,
            int fixed,
            List<PathSegment> path,
            boolean strict,
            WorkBudget budget,
            boolean[] undecided) {
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
                    cur[j] =
                            j < n && next[j + 1] && segment.accepts(path.get(j), strict, undecided);
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

    /**
     * API 경로 세그먼트. regex는 {@code {name:regex}}·섞인 세그먼트에서 컴파일된 것만 있다. weight는 변수 무게({@code *}가 들어가면
     * 더 무겁다).
     */
    private record ApiSegment(Type type, String literal, Pattern regex, int weight) {

        static ApiSegment of(String s) {
            if (s.equals("**") || (s.startsWith("{*") && s.endsWith("}"))) {
                return new ApiSegment(Type.MULTI, null, null, 0);
            }
            if (s.startsWith("{") && closingBrace(s, 0) == s.length() - 1 && s.indexOf(':') > 0) {
                String regex = s.substring(s.indexOf(':') + 1, s.length() - 1);
                try {
                    return new ApiSegment(
                            Type.VARIABLE, null, Pattern.compile(regex), VARIABLE_WEIGHT);
                } catch (PatternSyntaxException e) {
                    return new ApiSegment(Type.VARIABLE, null, null, VARIABLE_WEIGHT);
                }
            }
            if (s.equals("*")) {
                return new ApiSegment(Type.VARIABLE, null, null, STAR_WEIGHT);
            }
            if (s.startsWith("{") && closingBrace(s, 0) == s.length() - 1) {
                return new ApiSegment(Type.VARIABLE, null, null, VARIABLE_WEIGHT);
            }
            if (s.contains("{") || s.contains("*")) {
                return new ApiSegment(
                        Type.VARIABLE, null, mixed(s), bareStar(s) ? STAR_WEIGHT : VARIABLE_WEIGHT);
            }
            return new ApiSegment(Type.LITERAL, s, null, 0);
        }

        /** 중괄호 밖에 {@code *}가 있다. */
        private static boolean bareStar(String s) {
            for (int i = 0; i < s.length(); i++) {
                int close = s.charAt(i) == '{' ? closingBrace(s, i) : -1;
                if (close > 0) {
                    i = close;
                } else if (s.charAt(i) == '*') {
                    return true;
                }
            }
            return false;
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

        /**
         * strict면 FE 와일드카드는 어떤 값이어도 맞는 세그먼트(정규식 없는 변수·이중 별표)와만 맞는다. 입력이 너무 길거나 읽기 예산을 넘어 정규식을 끝까지
         * 보지 못하면 맞는 것으로 보고 undecided[0]을 켠다 (확정하지 않는다).
         */
        boolean accepts(PathSegment fe, boolean strict, boolean[] undecided) {
            if (fe.wildcard()) {
                return !strict || type == Type.MULTI || (type == Type.VARIABLE && regex == null);
            }
            return switch (type) {
                case LITERAL -> literal.equals(fe.literal());
                case VARIABLE -> regex == null || regexMatches(fe.literal(), undecided);
                case MULTI -> true;
            };
        }

        private boolean regexMatches(String input, boolean[] undecided) {
            if (input.length() > MAX_REGEX_INPUT) {
                undecided[0] = true;
                return true;
            }
            try {
                return regex.matcher(new BudgetedChars(input, new int[] {MAX_REGEX_STEPS}))
                        .matches();
            } catch (RegexBudgetExceeded e) {
                undecided[0] = true;
                return true;
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

    /**
     * @param variables 변수 무게의 합
     * @param certain FE 와일드카드가 어떤 값이어도 이 후보와 맞는다
     * @param undecided 정규식을 끝까지 보지 못해 맞는지 모른다
     */
    private record Candidate(
            long fileId,
            int literals,
            int variables,
            boolean multi,
            boolean certain,
            boolean undecided) {

        static Candidate of(
                long fileId, List<ApiSegment> pattern, boolean certain, boolean undecided) {
            int literals = 0;
            int variables = 0;
            boolean multi = false;
            for (ApiSegment s : pattern) {
                switch (s.type()) {
                    case LITERAL -> literals++;
                    case VARIABLE -> variables += s.weight();
                    case MULTI -> multi = true;
                }
            }
            return new Candidate(fileId, literals, variables, multi, certain, undecided);
        }
    }
}
