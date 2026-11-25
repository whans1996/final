package com.vibenote.analysis.structure.js;

import java.util.List;

/**
 * JS 토큰 하나 (F-21a AC5). 주석은 토큰이 아니다.
 *
 * @param text 원문
 * @param value STRING이면 이스케이프를 푼 값, 그 밖에는 원문
 * @param line 원본 파일 줄 번호 (1부터, .vue도 파일 기준)
 * @param start 원본에서 시작 위치 (포함)
 * @param end 원본에서 끝 위치 (제외)
 * @param quasis TEMPLATE의 고정 문자열 조각 (표현식 수 + 1개)
 * @param parts TEMPLATE의 {@code ${}} 안 토큰들
 */
public record JsToken(
        Kind kind,
        String text,
        String value,
        int line,
        int start,
        int end,
        List<String> quasis,
        List<List<JsToken>> parts) {

    public enum Kind {
        IDENT,
        NUMBER,
        STRING,
        TEMPLATE,
        REGEX,
        PUNCT
    }

    public boolean is(String punctOrWord) {
        return (kind == Kind.PUNCT || kind == Kind.IDENT) && text.equals(punctOrWord);
    }
}
