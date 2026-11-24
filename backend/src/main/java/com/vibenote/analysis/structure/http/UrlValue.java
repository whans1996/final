package com.vibenote.analysis.structure.http;

import com.vibenote.analysis.structure.js.JsFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 계산한 URL 식 값 (F-21a AC8). 값을 모르는 조각은 {@link #DYNAMIC} 문자로 둔다.
 *
 * @param paramOf 계산 중 값을 모르는 매개변수를 만났다면 그 함수 (AC10 래퍼 인라인 판단)
 */
public record UrlValue(String text, JsFunction paramOf) {

    static final char DYNAMIC = '\0';

    private static final Pattern SCHEME = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*://");

    public static UrlValue literal(String text) {
        return new UrlValue(text, null);
    }

    public static UrlValue dynamic() {
        return new UrlValue(String.valueOf(DYNAMIC), null);
    }

    public static UrlValue dynamic(JsFunction paramOf) {
        return new UrlValue(String.valueOf(DYNAMIC), paramOf);
    }

    public UrlValue plus(UrlValue other) {
        return new UrlValue(text + other.text, paramOf != null ? paramOf : other.paramOf);
    }

    /** 맨 앞이 동적 조각이면 경로를 확정할 수 없다. */
    public boolean startsDynamic() {
        return !text.isEmpty() && text.charAt(0) == DYNAMIC;
    }

    public boolean known() {
        return text.indexOf(DYNAMIC) < 0;
    }

    /**
     * {@code ?}·{@code #} 뒤를 버리고 {@code scheme://호스트[:포트]}를 떼어 세그먼트로 나눈다. 동적 조각이 들어간 세그먼트는 와일드카드다.
     */
    public UrlPath toPath() {
        String s = text;
        int cut = firstIndexOf(s, '?', '#');
        if (cut >= 0) {
            s = s.substring(0, cut);
        }
        boolean absolute = false;
        Matcher scheme = SCHEME.matcher(s);
        boolean hasScheme = scheme.lookingAt();
        if (hasScheme || s.startsWith("//")) {
            absolute = true;
            String rest = s.substring(hasScheme ? scheme.end() : 2);
            int slash = rest.indexOf('/');
            s = slash < 0 ? "" : rest.substring(slash);
        }
        List<PathSegment> segments = new ArrayList<>();
        for (String part : s.split("/")) {
            if (!part.isEmpty()) {
                segments.add(
                        part.indexOf(DYNAMIC) >= 0
                                ? PathSegment.WILDCARD
                                : PathSegment.literal(part));
            }
        }
        return new UrlPath(absolute, List.copyOf(segments));
    }

    private static int firstIndexOf(String s, char a, char b) {
        int i = s.indexOf(a);
        int j = s.indexOf(b);
        return i < 0 ? j : (j < 0 ? i : Math.min(i, j));
    }

    /**
     * @param absolute 호스트가 있던 절대 URL이면 true (axios는 이때 baseURL을 붙이지 않는다)
     */
    public record UrlPath(boolean absolute, List<PathSegment> segments) {}
}
