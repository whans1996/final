package com.vibenote.analysis.structure.js;

import java.util.List;

/**
 * {@code f(…)}·{@code r.m(…)} 모양의 호출 하나 (F-21a AC7·AC10). 더 긴 멤버 체인({@code this.$http.get})은 모으지
 * 않는다.
 *
 * @param receiver f 또는 r
 * @param member m. {@code f(…)}면 null
 * @param args 최상위 쉼표로 나눈 인자 토큰
 * @param start 호출 원문 시작 (원본 offset)
 * @param end 호출 원문 끝 (닫는 괄호 다음)
 */
public record JsCall(
        String receiver, String member, List<List<JsToken>> args, int line, int start, int end) {

    public List<JsToken> arg(int index) {
        return index < args.size() ? args.get(index) : null;
    }
}
