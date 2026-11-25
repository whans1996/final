package com.vibenote.analysis.structure.js;

import com.vibenote.analysis.structure.js.JsToken.Kind;
import java.util.List;

/** 토큰 목록 위의 작은 도우미. 범위 밖 위치는 false·null이다. */
final class JsTokens {

    private JsTokens() {}

    static boolean is(List<JsToken> t, int i, String text) {
        return i >= 0 && i < t.size() && t.get(i).is(text);
    }

    static Kind kind(List<JsToken> t, int i) {
        return i >= 0 && i < t.size() ? t.get(i).kind() : null;
    }

    /** {@code a.b}의 b처럼 멤버 이름 자리인가 (앞이 {@code .}·{@code ?.}). */
    static boolean isMemberName(List<JsToken> t, int i) {
        return is(t, i - 1, ".") || is(t, i - 1, "?.");
    }
}
