package com.vibenote.analysis.structure.js;

/** 토큰으로 나눌 수 없는 JS 코드 (끝나지 않은 문자열·주석·템플릿, F-21a AC5). */
public class JsSyntaxException extends RuntimeException {

    private final int line;

    public JsSyntaxException(String message, int line) {
        super(message);
        this.line = line;
    }

    public int getLine() {
        return line;
    }
}
