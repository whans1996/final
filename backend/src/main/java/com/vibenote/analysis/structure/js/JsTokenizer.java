package com.vibenote.analysis.structure.js;

import com.vibenote.analysis.structure.js.JsToken.Kind;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JS 토크나이저 (F-21a AC5). 문자열·템플릿 리터럴(중첩 {@code ${}})·주석·정규식 리터럴·식별자·구두점을 나눈다.
 *
 * <p>정규식과 나눗셈은 앞 토큰으로 구분한다: 값으로 끝나는 토큰({@code )}·{@code ]}·식별자·숫자·문자열) 뒤의 {@code /}는 나눗셈이고, 그
 * 밖(연산자·{@code (}·{@code }}·{@code return} 같은 키워드 뒤)은 정규식이다. 정규식이 줄 안에서 끝나지 않으면 나눗셈으로 되돌린다. 끝나지 않은
 * 문자열·주석·템플릿은 {@link JsSyntaxException}이다.
 */
public final class JsTokenizer {

    private static final List<String> OPERATORS =
            List.of(
                    ">>>=", "...", "===", "!==", "**=", "<<=", ">>=", ">>>", "&&=", "||=", "??=",
                    "=>", "==", "!=", "<=", ">=", "&&", "||", "??", "?.", "++", "--", "+=", "-=",
                    "*=", "/=", "%=", "&=", "|=", "^=", "**", "<<", ">>");

    private static final Set<String> REGEX_AFTER_WORDS =
            Set.of(
                    "return",
                    "typeof",
                    "case",
                    "do",
                    "else",
                    "in",
                    "of",
                    "new",
                    "delete",
                    "void",
                    "throw",
                    "instanceof",
                    "yield",
                    "await");

    private static final Pattern SCRIPT_OPEN =
            Pattern.compile("<script\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_CLOSE =
            Pattern.compile("</script\\s*>", Pattern.CASE_INSENSITIVE);

    private final String src;
    private int pos;
    private int line = 1;

    private JsTokenizer(String src) {
        this.src = src;
    }

    public static List<JsToken> tokenize(String code) {
        return new JsTokenizer(code == null ? "" : code).lex(false);
    }

    /** {@code .vue}는 {@code <script>} 블록만 읽는다. 블록 밖은 줄바꿈만 남기고 공백으로 바꿔 줄 번호·위치를 원본과 같게 둔다. */
    public static List<JsToken> tokenizeFile(String path, String content) {
        String code = content == null ? "" : content;
        if (path.toLowerCase(Locale.ROOT).endsWith(".vue")) {
            code = scriptBlocks(code);
        }
        return tokenize(code);
    }

    static String scriptBlocks(String vue) {
        char[] out = vue.toCharArray();
        boolean[] keep = new boolean[out.length];
        Matcher open = SCRIPT_OPEN.matcher(vue);
        int from = 0;
        while (open.find(from)) {
            Matcher close = SCRIPT_CLOSE.matcher(vue);
            int end = close.find(open.end()) ? close.start() : vue.length();
            for (int i = open.end(); i < end; i++) {
                keep[i] = true;
            }
            from = end >= vue.length() ? vue.length() : close.end();
            if (from >= vue.length()) {
                break;
            }
        }
        for (int i = 0; i < out.length; i++) {
            if (!keep[i] && out[i] != '\n' && out[i] != '\r') {
                out[i] = ' ';
            }
        }
        return new String(out);
    }

    /** 토큰을 읽는다. inTemplate이면 짝이 맞는 {@code }}에서 멈추고 그 {@code }}를 소비한다. */
    private List<JsToken> lex(boolean inTemplate) {
        List<JsToken> tokens = new ArrayList<>();
        int depth = 0;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '\n') {
                line++;
                pos++;
            } else if (Character.isWhitespace(c) || c == ' ' || c == '﻿') {
                pos++;
            } else if (c == '/' && peek(1) == '/') {
                while (pos < src.length() && src.charAt(pos) != '\n') {
                    pos++;
                }
            } else if (c == '/' && peek(1) == '*') {
                int startLine = line;
                int close = src.indexOf("*/", pos + 2);
                if (close < 0) {
                    throw new JsSyntaxException("주석이 끝나지 않았습니다.", startLine);
                }
                advanceTo(close + 2);
            } else if (c == '\'' || c == '"') {
                tokens.add(string(c));
            } else if (c == '`') {
                tokens.add(template());
            } else if (c == '/' && regexAllowed(tokens)) {
                JsToken regex = regex();
                tokens.add(regex != null ? regex : punct("/"));
            } else if (Character.isJavaIdentifierStart(c)) {
                int start = pos;
                while (pos < src.length() && Character.isJavaIdentifierPart(src.charAt(pos))) {
                    pos++;
                }
                tokens.add(simple(Kind.IDENT, start));
            } else if (Character.isDigit(c) || (c == '.' && Character.isDigit(peek(1)))) {
                int start = pos;
                while (pos < src.length()
                        && (Character.isLetterOrDigit(src.charAt(pos))
                                || src.charAt(pos) == '.'
                                || src.charAt(pos) == '_')) {
                    pos++;
                }
                tokens.add(simple(Kind.NUMBER, start));
            } else if (inTemplate && c == '}' && depth == 0) {
                pos++;
                return tokens;
            } else {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                }
                tokens.add(punct(operatorAt()));
            }
        }
        if (inTemplate) {
            throw new JsSyntaxException("템플릿 리터럴이 끝나지 않았습니다.", line);
        }
        return tokens;
    }

    private static boolean regexAllowed(List<JsToken> tokens) {
        if (tokens.isEmpty()) {
            return true;
        }
        JsToken prev = tokens.get(tokens.size() - 1);
        return switch (prev.kind()) {
            case PUNCT ->
                    !prev.text().equals(")")
                            && !prev.text().equals("]")
                            && !prev.text().equals("++")
                            && !prev.text().equals("--");
            case IDENT -> REGEX_AFTER_WORDS.contains(prev.text());
            default -> false;
        };
    }

    private String operatorAt() {
        for (String op : OPERATORS) {
            if (src.startsWith(op, pos)) {
                return op;
            }
        }
        return String.valueOf(src.charAt(pos));
    }

    private JsToken punct(String text) {
        int start = pos;
        pos += text.length();
        return new JsToken(Kind.PUNCT, text, text, line, start, pos, List.of(), List.of());
    }

    private JsToken simple(Kind kind, int start) {
        String text = src.substring(start, pos);
        return new JsToken(kind, text, text, line, start, pos, List.of(), List.of());
    }

    private JsToken string(char quote) {
        int start = pos;
        int startLine = line;
        pos++;
        StringBuilder value = new StringBuilder();
        while (true) {
            if (pos >= src.length() || src.charAt(pos) == '\n') {
                throw new JsSyntaxException("문자열이 끝나지 않았습니다.", startLine);
            }
            char c = src.charAt(pos);
            if (c == quote) {
                pos++;
                break;
            }
            if (c == '\\') {
                escape(value);
            } else {
                value.append(c);
                pos++;
            }
        }
        return new JsToken(
                Kind.STRING,
                src.substring(start, pos),
                value.toString(),
                startLine,
                start,
                pos,
                List.of(),
                List.of());
    }

    private JsToken template() {
        int start = pos;
        int startLine = line;
        pos++;
        List<String> quasis = new ArrayList<>();
        List<List<JsToken>> parts = new ArrayList<>();
        StringBuilder quasi = new StringBuilder();
        while (true) {
            if (pos >= src.length()) {
                throw new JsSyntaxException("템플릿 리터럴이 끝나지 않았습니다.", startLine);
            }
            char c = src.charAt(pos);
            if (c == '`') {
                pos++;
                break;
            }
            if (c == '\\') {
                escape(quasi);
            } else if (c == '$' && peek(1) == '{') {
                pos += 2;
                quasis.add(quasi.toString());
                quasi.setLength(0);
                parts.add(lex(true));
            } else {
                if (c == '\n') {
                    line++;
                }
                quasi.append(c);
                pos++;
            }
        }
        quasis.add(quasi.toString());
        String text = src.substring(start, pos);
        return new JsToken(
                Kind.TEMPLATE,
                text,
                text,
                startLine,
                start,
                pos,
                List.copyOf(quasis),
                List.copyOf(parts));
    }

    /** 문자열·템플릿 안 이스케이프 하나를 풀어 붙인다. pos는 백슬래시에 있다. */
    private void escape(StringBuilder out) {
        char next = peek(1);
        pos += 2;
        switch (next) {
            case 'n' -> out.append('\n');
            case 't' -> out.append('\t');
            case 'r' -> out.append('\r');
            case 'b' -> out.append('\b');
            case 'f' -> out.append('\f');
            case 'v' -> out.append('\u000B');
            case '0' -> out.append('\0');
            case '\n' -> line++;
            case '\r' -> {
                if (peek(0) == '\n') {
                    pos++;
                    line++;
                }
            }
            case 'x' -> out.append(hex(2));
            case 'u' -> {
                if (peek(0) == '{') {
                    int close = src.indexOf('}', pos);
                    if (close > pos) {
                        try {
                            out.appendCodePoint(
                                    Integer.parseInt(src.substring(pos + 1, close), 16));
                        } catch (IllegalArgumentException e) {
                            // 잘못된 코드 포인트는 값에서 뺀다
                        }
                        pos = close + 1;
                    }
                } else {
                    out.append(hex(4));
                }
            }
            case '\0' -> throw new JsSyntaxException("문자열이 끝나지 않았습니다.", line);
            default -> out.append(next);
        }
    }

    private String hex(int digits) {
        if (pos + digits > src.length()) {
            return "";
        }
        try {
            String value =
                    String.valueOf((char) Integer.parseInt(src.substring(pos, pos + digits), 16));
            pos += digits;
            return value;
        } catch (NumberFormatException e) {
            return "";
        }
    }

    /** 정규식 리터럴. 줄 안에서 끝나지 않으면 null(나눗셈으로 본다). */
    private JsToken regex() {
        int start = pos;
        int i = pos + 1;
        boolean inClass = false;
        while (i < src.length()) {
            char c = src.charAt(i);
            if (c == '\n') {
                return null;
            }
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '[') {
                inClass = true;
            } else if (c == ']') {
                inClass = false;
            } else if (c == '/' && !inClass) {
                i++;
                while (i < src.length() && Character.isLetter(src.charAt(i))) {
                    i++;
                }
                pos = i;
                return simple(Kind.REGEX, start);
            }
            i++;
        }
        return null;
    }

    private char peek(int offset) {
        int i = pos + offset;
        return i < src.length() ? src.charAt(i) : '\0';
    }

    private void advanceTo(int end) {
        for (int i = pos; i < end; i++) {
            if (src.charAt(i) == '\n') {
                line++;
            }
        }
        pos = end;
    }
}
