package com.vibenote.analysis.structure.js;

import com.vibenote.analysis.structure.WorkBudget;
import com.vibenote.analysis.structure.js.JsToken.Kind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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

    /** 뒤의 괄호가 문의 조건인 키워드. 그 괄호를 닫은 뒤에는 값이 아니라 문이 온다. */
    private static final Set<String> CONDITION_WORDS = Set.of("if", "while", "for", "with");

    /** 여는 태그의 이름까지. 속성과 닫는 {@code >}는 {@link #tagEnd}가 따옴표를 보며 찾는다. */
    private static final Pattern SCRIPT_OPEN =
            Pattern.compile("<script(?=[\\s>/])", Pattern.CASE_INSENSITIVE);

    private static final int SCRIPT_OPEN_LENGTH = "<script".length();

    /** {@code .vue}의 {@code <script lang="ts|tsx">}는 읽지 않는다 (spec 5.3절 2·8절). */
    private static final Set<String> TS_LANGS = Set.of("ts", "tsx");

    private static final Pattern SCRIPT_CLOSE =
            Pattern.compile("</script\\s*>", Pattern.CASE_INSENSITIVE);

    /** 템플릿 리터럴 중첩 상한. 넘으면 PARSE_ERROR (재귀가 스택을 넘지 않게). */
    static final int MAX_TEMPLATE_DEPTH = 64;

    /** 정규식으로 끝나지 않은 후보를 훑은 글자 수의 파일당 상한. 같은 줄을 거듭 훑어 제곱 시간이 되지 않게 한다. */
    static final int MAX_FAILED_REGEX_SCAN = 10_000_000;

    /** 코드 포인트 이스케이프(백슬래시 u 중괄호)에서 닫는 중괄호를 찾는 범위. */
    private static final int MAX_CODE_POINT_ESCAPE = 16;

    /** 토큰 수가 상한을 넘은 파일의 오류 메시지 (그 파일은 PARSE_ERROR, 5.3절 3). */
    public static final String TOO_MANY_TOKENS = "코드가 너무 길어 해석하지 않았습니다.";

    /** 템플릿 토큰의 text·value. 원문은 start·end 위치로 알 수 있으므로 복사하지 않는다 (중첩된 템플릿마다 바깥 원문 전체를 다시 복사하지 않게). */
    private static final String TEMPLATE_TEXT = "`";

    /** 한 글자 구두점 문자열. 토큰마다 새 문자열을 만들지 않고 공유한다. */
    private static final String[] SINGLE_CHARS = new String[128];

    static {
        for (char c = 0; c < SINGLE_CHARS.length; c++) {
            SINGLE_CHARS[c] = String.valueOf(c);
        }
    }

    private final String src;
    private final WorkBudget budget;
    private final int maxTokens;
    private int produced;
    private int pos;
    private int line = 1;
    private int templateDepth;
    private long failedRegexScan;

    private JsTokenizer(String src, WorkBudget budget, int maxTokens) {
        this.src = src;
        this.budget = budget;
        this.maxTokens = maxTokens;
    }

    public static List<JsToken> tokenize(String code) {
        return tokenize(code, WorkBudget.unlimited(), Integer.MAX_VALUE);
    }

    /** 토큰 하나마다 예산을 쓴다. 템플릿 안 토큰까지 maxTokens개를 넘으면 {@link #TOO_MANY_TOKENS}. */
    public static List<JsToken> tokenize(String code, WorkBudget budget, int maxTokens) {
        return new JsTokenizer(code == null ? "" : code, budget, maxTokens).lex(false);
    }

    /**
     * {@code .vue}는 {@code <script>} 블록만 읽는다. 블록 밖은 줄 끝만 남기고 공백으로 바꿔 줄 번호·위치를 원본과 같게 둔다. TS 블록이 있으면
     * 그 여는 태그 줄의 {@link JsSyntaxException}이다.
     */
    public static List<JsToken> tokenizeFile(
            String path, String content, WorkBudget budget, int maxTokens) {
        String code = content == null ? "" : content;
        if (path.toLowerCase(Locale.ROOT).endsWith(".vue")) {
            code = scriptBlocks(code);
        }
        return tokenize(code, budget, maxTokens);
    }

    /** 이 목록에 새로 붙은 토큰만큼 예산을 쓰고 개수 상한을 본다. 센 개수를 돌려준다. */
    private int account(List<JsToken> tokens, int counted) {
        int added = tokens.size() - counted;
        if (added > 0) {
            budget.spend(added);
            produced += added;
            if (produced > maxTokens) {
                throw new JsSyntaxException(TOO_MANY_TOKENS, line);
            }
        }
        return tokens.size();
    }

    /** 한 번 훑는다: 찾은 여는 태그·주석·닫는 태그 뒤로만 나아가고, 닫히지 않은 여는 태그를 만나면 멈춘다 (5.3절 6). */
    static String scriptBlocks(String vue) {
        char[] out = vue.toCharArray();
        boolean[] keep = new boolean[out.length];
        Matcher open = SCRIPT_OPEN.matcher(vue);
        Matcher close = SCRIPT_CLOSE.matcher(vue);
        int from = 0;
        // from 이후 첫 여는 태그·HTML 주석 시작. from이 지나친 때만 다시 찾는다 (파일 끝까지 거듭 찾지 않게)
        int openAt = -1;
        int comment = vue.indexOf("<!--");
        while (from < vue.length()) {
            if (openAt < from) {
                if (!open.find(from)) {
                    break;
                }
                openAt = open.start();
            }
            if (comment >= 0 && comment < from) {
                comment = vue.indexOf("<!--", from);
            }
            // 블록 밖 HTML 주석 안의 <script>는 실행되지 않는다
            if (comment >= 0 && comment < openAt) {
                int commentEnd = vue.indexOf("-->", comment + 4);
                if (commentEnd < 0) {
                    break;
                }
                from = commentEnd + 3;
                continue;
            }
            int attrs = openAt + SCRIPT_OPEN_LENGTH;
            int gt = tagEnd(vue, attrs);
            if (gt < 0) {
                break;
            }
            String lang = langOf(vue.substring(attrs, gt));
            if (lang != null && TS_LANGS.contains(lang.trim().toLowerCase(Locale.ROOT))) {
                throw new JsSyntaxException(
                        "TypeScript 스크립트(lang=\"" + lang + "\")는 지원하지 않습니다.",
                        1 + lineBreaks(vue, 0, openAt));
            }
            int end = close.find(gt + 1) ? close.start() : vue.length();
            for (int i = gt + 1; i < end; i++) {
                keep[i] = true;
            }
            from = end >= vue.length() ? vue.length() : close.end();
        }
        for (int i = 0; i < out.length; i++) {
            if (!keep[i] && !isLineTerminator(out[i])) {
                out[i] = ' ';
            }
        }
        return new String(out);
    }

    /** 여는 태그를 닫는 {@code >}의 위치. 속성 값 따옴표 안의 {@code >}는 건너뛴다. 없으면 -1. */
    private static int tagEnd(String s, int from) {
        char quote = 0;
        boolean valueStart = false;
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '>') {
                return i;
            } else if (valueStart && (c == '"' || c == '\'')) {
                quote = c;
                valueStart = false;
            } else if (c == '=') {
                valueStart = true;
            } else if (!Character.isWhitespace(c)) {
                valueStart = false;
            }
        }
        return -1;
    }

    /** 여는 태그 속성 문자열에서 {@code lang} 값 (이름은 대소문자 무시). 없으면 null. */
    private static String langOf(String attrs) {
        int i = 0;
        int n = attrs.length();
        while (i < n) {
            char c = attrs.charAt(i);
            if (Character.isWhitespace(c) || c == '/') {
                i++;
                continue;
            }
            int nameStart = i;
            while (i < n
                    && !Character.isWhitespace(attrs.charAt(i))
                    && attrs.charAt(i) != '='
                    && attrs.charAt(i) != '/') {
                i++;
            }
            String name = attrs.substring(nameStart, i);
            while (i < n && Character.isWhitespace(attrs.charAt(i))) {
                i++;
            }
            String value = "";
            if (i < n && attrs.charAt(i) == '=') {
                i++;
                while (i < n && Character.isWhitespace(attrs.charAt(i))) {
                    i++;
                }
                if (i < n && (attrs.charAt(i) == '"' || attrs.charAt(i) == '\'')) {
                    int close = attrs.indexOf(attrs.charAt(i), i + 1);
                    int valueEnd = close < 0 ? n : close;
                    value = attrs.substring(i + 1, valueEnd);
                    i = valueEnd + 1;
                } else {
                    int valueStart = i;
                    while (i < n && !Character.isWhitespace(attrs.charAt(i))) {
                        i++;
                    }
                    value = attrs.substring(valueStart, i);
                }
            }
            if (name.equalsIgnoreCase("lang")) {
                return value;
            }
        }
        return null;
    }

    /** JS 줄 끝 문자 (LF·CR·U+2028·U+2029). */
    private static boolean isLineTerminator(char c) {
        return c == '\n' || c == '\r' || c == ' ' || c == ' ';
    }

    /** i의 문자가 줄을 끝내는지. CRLF는 LF에서 한 번만 센다. */
    private static boolean endsLine(String s, int i) {
        char c = s.charAt(i);
        return c == '\r'
                ? i + 1 >= s.length() || s.charAt(i + 1) != '\n'
                : c == '\n' || c == ' ' || c == ' ';
    }

    /** [from, to)의 줄 끝 수. */
    private static int lineBreaks(String s, int from, int to) {
        int count = 0;
        for (int i = from; i < to; i++) {
            if (endsLine(s, i)) {
                count++;
            }
        }
        return count;
    }

    /** 토큰을 읽는다. inTemplate이면 짝이 맞는 {@code }}에서 멈추고 그 {@code }}를 소비한다. */
    private List<JsToken> lex(boolean inTemplate) {
        List<JsToken> tokens = new ArrayList<>();
        int depth = 0;
        // 여는 괄호마다 if·while·for·with의 조건 괄호인지. 조건 괄호를 닫은 ) 뒤의 /는 정규식을 시작할 수 있다
        Deque<Boolean> parens = new ArrayDeque<>();
        boolean closedCondition = false;
        // 여는 중괄호마다 식 자리(객체 리터럴)인지. 객체를 닫은 } 뒤의 /는 나눗셈이다
        Deque<Boolean> braces = new ArrayDeque<>();
        boolean closedObject = false;
        // 마지막 ! 가 같은 줄 값 바로 뒤(TS non-null 단언)인지. 그 뒤의 /는 나눗셈이다
        boolean postfixBang = false;
        int counted = 0;
        while (pos < src.length()) {
            counted = account(tokens, counted);
            char c = src.charAt(pos);
            if (isLineTerminator(c)) {
                if (endsLine(src, pos)) {
                    line++;
                }
                pos++;
            } else if (Character.isWhitespace(c) || c == ' ' || c == '﻿') {
                pos++;
            } else if (c == '/' && peek(1) == '/') {
                while (pos < src.length() && !isLineTerminator(src.charAt(pos))) {
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
            } else if (c == '/'
                    && regexAllowed(tokens, closedCondition, closedObject, postfixBang)) {
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
                account(tokens, counted);
                return tokens;
            } else {
                if (c == '{') {
                    depth++;
                    braces.push(objectBrace(tokens));
                } else if (c == '}') {
                    depth--;
                    closedObject = !braces.isEmpty() && braces.pop();
                } else if (c == '(') {
                    parens.push(
                            !tokens.isEmpty()
                                    && CONDITION_WORDS.contains(tokens.getLast().text())
                                    && tokens.getLast().kind() == Kind.IDENT);
                } else if (c == ')') {
                    closedCondition = !parens.isEmpty() && parens.pop();
                }
                String op = operatorAt();
                if (op.equals("!")) {
                    postfixBang =
                            !tokens.isEmpty()
                                    && tokens.getLast().line() == line
                                    && !regexAllowed(
                                            tokens, closedCondition, closedObject, postfixBang);
                }
                tokens.add(punct(op));
            }
        }
        if (inTemplate) {
            throw new JsSyntaxException("템플릿 리터럴이 끝나지 않았습니다.", line);
        }
        account(tokens, counted);
        return tokens;
    }

    /**
     * 지금 여는 {@code {}가 식 자리(객체 리터럴)인지. 연산자·{@code (}·{@code ,}·{@code :}·{@code return} 같은 값 앞 자리면 객체이고,
     * 문 끝·블록 경계·{@code )}·{@code =>}·이름 뒤면 블록이다.
     */
    private static boolean objectBrace(List<JsToken> tokens) {
        if (tokens.isEmpty()) {
            return false;
        }
        JsToken prev = tokens.getLast();
        return switch (prev.kind()) {
            case PUNCT -> !BLOCK_BEFORE.contains(prev.text());
            case IDENT -> OBJECT_AFTER_WORDS.contains(prev.text());
            default -> false;
        };
    }

    /** 이 토큰 뒤의 { 는 블록이다. */
    private static final Set<String> BLOCK_BEFORE =
            Set.of(")", ";", "{", "}", "=>", "]", "++", "--");

    /** 이 낱말 뒤의 { 는 객체 리터럴이다. */
    private static final Set<String> OBJECT_AFTER_WORDS =
            Set.of(
                    "return",
                    "typeof",
                    "instanceof",
                    "in",
                    "of",
                    "new",
                    "delete",
                    "void",
                    "throw",
                    "yield",
                    "await",
                    "case");

    /**
     * closedCondition: 직전 토큰 )가 if·while·for·with의 조건 괄호를 닫았는지. closedObject: 직전 토큰 }가 객체 리터럴을
     * 닫았는지. postfixBang: 직전 토큰 !가 TS non-null 단언(같은 줄 값 바로 뒤)인지.
     */
    private static boolean regexAllowed(
            List<JsToken> tokens,
            boolean closedCondition,
            boolean closedObject,
            boolean postfixBang) {
        if (tokens.isEmpty()) {
            return true;
        }
        JsToken prev = tokens.get(tokens.size() - 1);
        return switch (prev.kind()) {
            case PUNCT ->
                    switch (prev.text()) {
                        case ")" -> closedCondition;
                        case "}" -> !closedObject;
                        case "!" -> !postfixBang;
                        case "]", "++", "--" -> false;
                        default -> true;
                    };
            case IDENT -> REGEX_AFTER_WORDS.contains(prev.text());
            default -> false;
        };
    }

    private String operatorAt() {
        for (String op : OPERATORS) {
            // a?.5:1 은 조건 연산자 ? 와 소수 .5 다
            if (src.startsWith(op, pos) && !(op.equals("?.") && Character.isDigit(peek(2)))) {
                return op;
            }
        }
        char c = src.charAt(pos);
        return c < SINGLE_CHARS.length ? SINGLE_CHARS[c] : String.valueOf(c);
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
            if (pos >= src.length() || src.charAt(pos) == '\n' || src.charAt(pos) == '\r') {
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
                // U+2028·U+2029는 문자열 안에 그대로 올 수 있다 (ES2019)
                if (isLineTerminator(c)) {
                    line++;
                }
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
        if (++templateDepth > MAX_TEMPLATE_DEPTH) {
            throw new JsSyntaxException("템플릿 리터럴이 너무 깊게 중첩되었습니다.", startLine);
        }
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
                if (endsLine(src, pos)) {
                    line++;
                }
                quasi.append(c);
                pos++;
            }
        }
        quasis.add(quasi.toString());
        templateDepth--;
        return new JsToken(
                Kind.TEMPLATE,
                TEMPLATE_TEXT,
                TEMPLATE_TEXT,
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
            // 줄 이음: 백슬래시 뒤 줄 끝은 값에 넣지 않는다
            case '\n', ' ', ' ' -> line++;
            case '\r' -> {
                if (peek(0) == '\n') {
                    pos++;
                }
                line++;
            }
            case 'x' -> out.append(hex(2));
            case 'u' -> {
                if (peek(0) == '{') {
                    int close = src.indexOf('}', pos);
                    if (close > pos && close - pos <= MAX_CODE_POINT_ESCAPE) {
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
            if (isLineTerminator(c)) {
                return failedRegex(i - start);
            }
            if (c == '\\') {
                // 정규식은 줄을 잇지 못한다: 백슬래시 다음 줄 끝도 실패
                if (i + 1 < src.length() && isLineTerminator(src.charAt(i + 1))) {
                    return failedRegex(i + 1 - start);
                }
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
        return failedRegex(i - start);
    }

    private JsToken failedRegex(int scanned) {
        failedRegexScan += scanned;
        if (failedRegexScan > MAX_FAILED_REGEX_SCAN) {
            throw new JsSyntaxException("정규식인지 나눗셈인지 가릴 수 없는 '/'가 너무 많습니다.", line);
        }
        return null;
    }

    private char peek(int offset) {
        int i = pos + offset;
        return i < src.length() ? src.charAt(i) : '\0';
    }

    private void advanceTo(int end) {
        line += lineBreaks(src, pos, end);
        pos = end;
    }
}
