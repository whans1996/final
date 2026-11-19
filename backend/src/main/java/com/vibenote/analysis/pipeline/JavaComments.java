package com.vibenote.analysis.pipeline;

/** Java 주석 제거. layer 판정(F-07 AC12), API 탐지(F-06 AC7), 실행 흐름 입력 파일 추적(F-07 AC2)이 같은 규칙을 쓴다. */
public final class JavaComments {

    private JavaComments() {}

    /** 문자열·문자 리터럴은 두고 {@code //}·{@code /* *\/} 주석을 공백으로 바꾼다. */
    public static String strip(String code) {
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
}
