package com.vibenote.analysis.structure.js;

import com.vibenote.analysis.structure.js.JsToken.Kind;
import java.util.List;

/** 토큰 목록 위의 작은 도우미. 범위 밖 위치는 false·null이다. */
public final class JsTokens {

    private JsTokens() {}

    static boolean is(List<JsToken> t, int i, String text) {
        return i >= 0 && i < t.size() && t.get(i).is(text);
    }

    static Kind kind(List<JsToken> t, int i) {
        return i >= 0 && i < t.size() ? t.get(i).kind() : null;
    }

    /**
     * {@code a.b}·{@code a.#b}의 b처럼 멤버 이름 자리인가 (앞이 {@code .}·{@code ?.}, 또는 그 뒤의 {@code #}, #125).
     */
    static boolean isMemberName(List<JsToken> t, int i) {
        return is(t, i - 1, ".")
                || is(t, i - 1, "?.")
                || (is(t, i - 1, "#") && (is(t, i - 2, ".") || is(t, i - 2, "?.")));
    }

    /**
     * 변수 이름이 아닌 자리인가: 멤버 이름이거나 클래스 본문의 {@code #비공개} 이름({@code #fetch = …}). 비공개 이름은 같은 이름의 변수를 가리거나
     * 바꾸지 않는다 (#125). 선언 읽기는 {@code #load() {…}}를 메서드로 읽어야 하므로 {@link #isMemberName}을 쓴다.
     */
    public static boolean isPropertyName(List<JsToken> t, int i) {
        return isMemberName(t, i) || is(t, i - 1, "#");
    }
}
