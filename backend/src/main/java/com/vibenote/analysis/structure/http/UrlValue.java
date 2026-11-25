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

    /** 스킴 자리의 동적 조각은 스킴 안에 머문다고 본다 ({@code 'http' + s + '://…'}). */
    private static final Pattern SCHEME = Pattern.compile("^[A-Za-z][A-Za-z0-9+.\\x00-]*://");

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
     * 동적 조각은 놓인 URL 구성 요소(스킴·userinfo·호스트 이름·포트·경로 세그먼트) 안에 머문다고 본다 (5.3절 5).
     */
    public UrlPath toPath() {
        String s = text;
        int cut = firstIndexOf(s, '?', '#');
        if (cut >= 0) {
            s = s.substring(0, cut);
        }
        String host = null;
        Matcher scheme = SCHEME.matcher(s);
        boolean hasScheme = scheme.lookingAt();
        if (hasScheme || s.startsWith("//")) {
            String rest = s.substring(hasScheme ? scheme.end() : 2);
            int slash = rest.indexOf('/');
            host = hostName(slash < 0 ? rest : rest.substring(0, slash));
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
        return new UrlPath(List.copyOf(segments), host);
    }

    /** {@code [userinfo@]호스트[:포트]}에서 호스트 이름. IPv6 {@code [::1]}은 괄호까지. */
    private static String hostName(String authority) {
        String host = authority.substring(authority.lastIndexOf('@') + 1);
        if (host.startsWith("[")) {
            int close = host.indexOf(']');
            return close < 0 ? host : host.substring(0, close + 1);
        }
        int colon = host.indexOf(':');
        return colon < 0 ? host : host.substring(0, colon);
    }

    private static int firstIndexOf(String s, char a, char b) {
        int i = s.indexOf(a);
        int j = s.indexOf(b);
        return i < 0 ? j : (j < 0 ? i : Math.min(i, j));
    }

    /**
     * @param host 절대 URL의 호스트 이름(userinfo·포트 제외). 상대 경로면 null
     */
    public record UrlPath(List<PathSegment> segments, String host) {

        /** 호스트가 있던 절대 URL이면 true (axios는 이때 baseURL을 붙이지 않는다). */
        public boolean absolute() {
            return host != null;
        }

        /** 호스트 이름 자리에 동적 조각이 있어 어느 서버인지 모른다. */
        public boolean hostUnknown() {
            return host != null && host.indexOf(DYNAMIC) >= 0;
        }
    }
}
