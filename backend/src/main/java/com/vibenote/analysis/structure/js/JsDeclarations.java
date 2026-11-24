package com.vibenote.analysis.structure.js;

import com.vibenote.analysis.structure.WorkBudget;
import com.vibenote.analysis.structure.js.JsDecl.Kind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 토큰 위에서 선언·함수·내보내기·호출을 모은다 (F-21a AC7~AC10). 문법 전체를 해석하지 않고, URL 계산과 래퍼 인라인에 필요한 모양만 본다.
 *
 * <ul>
 *   <li>{@code const|let|var 이름 = 식}(구조 분해 제외): 가장 안쪽 {@code {}} 블록이 범위다. 식 끝은 최상위 {@code ;}·{@code
 *       ,}·닫는 괄호, 또는 줄바꿈(앞 토큰이 연산자이거나 다음 토큰이 {@code .}·이항 연산자면 이어짐)이다
 *   <li>함수: {@code function}, 화살표 함수, 객체·클래스 메서드. 이름은 {@code function f}·{@code const f = …}에서만 붙인다
 *   <li>호출: 멤버 이름 자리가 아닌 {@code f(…)}, {@code r.m(…)}·{@code r?.m(…)}
 * </ul>
 *
 * <p>계산량 (5.3절 3): 이름 조회는 선언을 모두 읽은 뒤에만 한다(모듈 색인을 한 번 만든다). 둘러싼 함수는 스택으로, 식 끝·class 본문 위치는 한 번 훑은
 * 자리마다 메모해 같은 구간을 다시 훑지 않는다.
 */
final class JsDeclarations {

    private static final Set<String> KEYWORDS =
            Set.of(
                    "if",
                    "for",
                    "while",
                    "switch",
                    "catch",
                    "with",
                    "function",
                    "return",
                    "typeof",
                    "new",
                    "await",
                    "yield",
                    "do",
                    "else",
                    "in",
                    "of",
                    "instanceof",
                    "void",
                    "delete",
                    "throw",
                    "case",
                    "super",
                    "import",
                    "export",
                    "const",
                    "let",
                    "var",
                    "class");

    /** 뒤에 식이 오는 키워드. 그 뒤의 function은 함수 식이다. */
    private static final Set<String> EXPRESSION_BEFORE =
            Set.of(
                    "return",
                    "typeof",
                    "new",
                    "await",
                    "yield",
                    "in",
                    "of",
                    "instanceof",
                    "case",
                    "void",
                    "delete",
                    "throw");

    private static final Set<String> NOT_CONTINUING =
            Set.of("(", "[", "{", "!", "~", "++", "--", "...", ")", "]", "}", ";", ",");

    private final JsModule module;
    private final WorkBudget budget;
    private final List<JsToken> t;
    private final int[] match;

    /** 토큰 위치마다 가장 안쪽 {@code {}}의 여는 위치. 없으면 -1. */
    private final int[] block;

    /** 템플릿 {@code ${}} 안의 토큰이면 true. 호출·화살표 함수만 모은다 (선언은 범위를 알 수 없어 이름을 잘못 가린다). */
    private final boolean inTemplate;

    /** {@code X.defaults.baseURL =}의 X 위치 (X는 axios 이름). 템플릿 안 것까지 바깥 읽기가 모아 마지막에 본다. */
    private final List<Candidate> defaultsCandidates;

    private record Candidate(JsDeclarations reader, int at) {}

    /** 지금 읽는 위치를 둘러쌀 수 있는 함수들 (등록 순). 끝난 함수는 위에서부터 뺀다. */
    private final Deque<JsFunction> openFunctions = new ArrayDeque<>();

    /** 위치별 {@link #expressionEnd} 결과. 아직 모르면 -1. */
    private final int[] endMemo;

    /** 위치별 {@link #nextBrace} 결과. 아직 모르면 -1. */
    private final int[] braceMemo;

    /** 메모를 채울 때 지나온 위치를 담는 버퍼. */
    private int[] visited = new int[16];

    private JsDeclarations(JsModule module) {
        this(module, module.tokens(), false, new ArrayList<>());
    }

    private JsDeclarations(
            JsModule module,
            List<JsToken> tokens,
            boolean inTemplate,
            List<Candidate> defaultsCandidates) {
        this.module = module;
        this.defaultsCandidates = defaultsCandidates;
        this.budget = module.budget();
        this.inTemplate = inTemplate;
        this.t = tokens;
        this.match = new int[t.size()];
        this.block = new int[t.size()];
        this.endMemo = new int[t.size()];
        this.braceMemo = new int[t.size()];
        Arrays.fill(endMemo, -1);
        Arrays.fill(braceMemo, -1);
        Deque<Integer> open = new ArrayDeque<>();
        Deque<Integer> braces = new ArrayDeque<>();
        budget.spend(t.size());
        for (int i = 0; i < t.size(); i++) {
            match[i] = -1;
            block[i] = braces.isEmpty() ? -1 : braces.peek();
            JsToken token = t.get(i);
            if (token.is("(") || token.is("[") || token.is("{")) {
                open.push(i);
                if (token.is("{")) {
                    braces.push(i);
                }
            } else if ((token.is(")") || token.is("]") || token.is("}")) && !open.isEmpty()) {
                int o = open.pop();
                match[o] = i;
                match[i] = o;
                if (t.get(o).is("{") && !braces.isEmpty()) {
                    braces.pop();
                }
            }
        }
    }

    static void read(JsModule module) {
        JsDeclarations d = new JsDeclarations(module);
        d.readImportsOfAxios();
        d.readAll();
        // 선언을 다 모은 뒤에야(템플릿 안 포함) 그 자리의 axios 이름이 가려졌는지 안다
        for (Candidate c : d.defaultsCandidates) {
            List<JsToken> ct = c.reader().t;
            int i = c.at();
            if (module.axiosAt(ct.get(i).text(), ct.get(i).start())) {
                module.defaultsBaseUrls().add(ct.subList(i + 6, c.reader().expressionEnd(i + 6)));
            }
        }
    }

    private void readAll() {
        for (int i = 0; i < t.size(); i++) {
            budget.spend();
            readAt(i);
        }
        // 템플릿 ${} 안의 식도 실행된다 (`${fetch('/x')}`). 토큰은 원본 offset을 그대로 가진다
        for (JsToken token : t) {
            if (token.kind() == JsToken.Kind.TEMPLATE) {
                for (List<JsToken> part : token.parts()) {
                    new JsDeclarations(module, part, true, defaultsCandidates).readAll();
                }
            }
        }
    }

    private void readImportsOfAxios() {
        for (JsImport imp : module.imports()) {
            if (imp.specifier().equals("axios")) {
                imp.bindings()
                        .forEach(
                                (local, imported) -> {
                                    if (imported.equals("default")) {
                                        module.axiosNames().add(local);
                                    }
                                });
            }
        }
    }

    private void readAt(int i) {
        JsToken token = t.get(i);
        if (token.kind() != JsToken.Kind.IDENT && !token.is("=>")) {
            return;
        }
        if (token.is("=>")) {
            arrow(i);
            return;
        }
        if (JsTokens.isMemberName(t, i)) {
            return;
        }
        if (inTemplate && KEYWORDS.contains(token.text())) {
            return;
        }
        switch (token.text()) {
            case "const", "let", "var" -> declarators(i);
            case "function" -> function(i);
            case "catch" -> catchParam(i);
            case "class" -> classDecl(i);
            case "export" -> export(i);
            default -> {
                if (!KEYWORDS.contains(token.text())) {
                    callOrMethod(i);
                    defaultsBaseUrl(i);
                }
            }
        }
    }

    // ---- 선언 ----

    private void declarators(int i) {
        Kind kind = t.get(i).is("const") ? Kind.CONST : Kind.VARIABLE;
        // var는 블록이 아니라 둘러싼 함수 전체(없으면 파일)가, for 머리의 let·const는 그 루프가 범위다
        int[] scope =
                t.get(i).is("var")
                        ? functionScopeOf(i)
                        : JsTokens.is(t, i - 1, "(") && JsTokens.is(t, i - 2, "for")
                                ? loopScopeOf(i - 1)
                                : scopeOf(i);
        declarators(i, kind, scope);
    }

    /** i 다음 토큰부터 쉼표로 이어진 선언자들 (이름과 구조 분해가 섞여도 반복문으로 읽는다). */
    private void declarators(int i, Kind kind, int[] scope) {
        int j = i + 1;
        while (true) {
            budget.spend();
            int next;
            if ((JsTokens.is(t, j, "{") || JsTokens.is(t, j, "[")) && match[j] > j) {
                // 구조 분해(const {axios} = …): 값은 모르지만 이 이름들이 바깥 이름을 가린다
                addPattern(t.subList(j, match[j] + 1), scope[0], scope[1]);
                next = match[j] + 1;
                if (JsTokens.is(t, next, "=")) {
                    next = expressionEnd(next + 1);
                }
            } else if (JsTokens.kind(t, j) == JsToken.Kind.IDENT) {
                String name = t.get(j).text();
                List<JsToken> init = List.of();
                next = j + 1;
                if (JsTokens.is(t, j + 1, "=")) {
                    int end = expressionEnd(j + 2);
                    init = t.subList(j + 2, end);
                    next = end;
                }
                module.decls().add(new JsDecl(name, kind, scope[0], scope[1], init, null));
                if (isRequireAxios(init)) {
                    module.axiosNames().add(name);
                }
            } else {
                return;
            }
            if (!JsTokens.is(t, next, ",")) {
                return;
            }
            j = next + 1;
        }
    }

    static boolean isRequireAxios(List<JsToken> init) {
        return init.size() == 4
                && init.get(0).is("require")
                && init.get(1).is("(")
                && init.get(2).kind() == JsToken.Kind.STRING
                && init.get(2).value().equals("axios")
                && init.get(3).is(")");
    }

    /** 위치 i를 감싼 가장 안쪽 블록 범위. 없으면 파일 전체. */
    private int[] scopeOf(int i) {
        int open = block[i];
        if (open < 0) {
            return new int[] {-1, Integer.MAX_VALUE};
        }
        int close = match[open];
        return new int[] {t.get(open).start(), close < 0 ? Integer.MAX_VALUE : t.get(close).end()};
    }

    /**
     * class 이름. 문장 자리의 {@code class X}는 둘러싼 블록에서, 식 자리({@code = class X {}})의 X는 그 클래스 안에서만 보이고 바깥
     * 이름을 가린다. 값은 모른다.
     */
    private void classDecl(int i) {
        if (JsTokens.kind(t, i + 1) != JsToken.Kind.IDENT || JsTokens.is(t, i + 1, "extends")) {
            return;
        }
        int body = nextBrace(i + 2);
        if (body >= t.size() || match[body] < 0) {
            return;
        }
        int[] scope =
                statementStart(i)
                        ? scopeOf(i)
                        : new int[] {t.get(i + 1).start(), t.get(match[body]).end()};
        module.decls()
                .add(
                        new JsDecl(
                                t.get(i + 1).text(),
                                Kind.VARIABLE,
                                scope[0],
                                scope[1],
                                List.of(),
                                null));
    }

    /** {@code catch (e) {…}}의 e. 그 블록 안에서만 보이고 바깥 이름을 가린다. */
    private void catchParam(int i) {
        if (JsTokens.is(t, i + 1, "(")
                && JsTokens.kind(t, i + 2) == JsToken.Kind.IDENT
                && JsTokens.is(t, i + 3, ")")
                && JsTokens.is(t, i + 4, "{")
                && match[i + 4] > 0) {
            module.decls()
                    .add(
                            new JsDecl(
                                    t.get(i + 2).text(),
                                    Kind.VARIABLE,
                                    t.get(i + 4).start(),
                                    t.get(match[i + 4]).end(),
                                    List.of(),
                                    null));
        }
    }

    /** for 여는 괄호부터 루프 본문 끝까지. */
    private int[] loopScopeOf(int open) {
        int close = match[open];
        if (close < 0) {
            return scopeOf(open);
        }
        int end;
        if (JsTokens.is(t, close + 1, "{") && match[close + 1] > 0) {
            end = t.get(match[close + 1]).end();
        } else {
            int last = expressionEnd(close + 1);
            end = last > close + 1 ? t.get(last - 1).end() : t.get(close).end();
        }
        return new int[] {t.get(open).start(), end};
    }

    /**
     * i를 둘러싼 가장 안쪽 함수의 범위. 없으면 파일 전체. 함수는 읽는 순서로 열리고 안쪽이 먼저 끝나므로 스택 위에서 끝난 것을 빼고, 아직 시작하지 않은 것(이름 뒤
     * 괄호에서 시작)은 건너뛴다.
     */
    private int[] functionScopeOf(int i) {
        int offset = t.get(i).start();
        while (!openFunctions.isEmpty() && openFunctions.peek().end() <= offset) {
            openFunctions.pop();
        }
        for (JsFunction fn : openFunctions) {
            budget.spend();
            if (fn.contains(offset)) {
                return new int[] {fn.start(), fn.end()};
            }
        }
        return new int[] {-1, Integer.MAX_VALUE};
    }

    private void addFunctionToModule(JsFunction fn) {
        module.functions().add(fn);
        openFunctions.push(fn);
    }

    // ---- 함수 ----

    private void function(int i) {
        int j = i + 1;
        if (JsTokens.is(t, j, "*")) {
            j++;
        }
        int start = JsTokens.is(t, i - 1, "async") ? i - 1 : i;
        // 문장 자리의 function f는 선언(둘러싼 블록에 f), 식 자리(= function f, (function f)는 f가 그 함수 안에서만 보인다
        boolean declaration = statementStart(start);
        String own = null;
        if (JsTokens.kind(t, j) == JsToken.Kind.IDENT && JsTokens.is(t, j + 1, "(")) {
            own = t.get(j).text();
            j++;
        }
        String name = own != null && declaration ? own : assignedName(start);
        if (!JsTokens.is(t, j, "(") || match[j] < 0 || !JsTokens.is(t, match[j] + 1, "{")) {
            return;
        }
        int bodyOpen = match[j] + 1;
        int end = bodyEnd(bodyOpen);
        addFunction(name, j, end);
        if (own != null) {
            int[] scope = declaration ? scopeOf(i) : new int[] {t.get(j).start(), end};
            module.decls().add(new JsDecl(own, Kind.FUNCTION, scope[0], scope[1], List.of(), null));
        }
    }

    /**
     * k가 문장 시작인지: 앞 토큰이 없거나 문장·블록 경계, export default 뒤, 또는 앞 토큰이 이전 줄에 있고 식을 잇는 토큰(연산자, return 등)이
     * 아닐 때 (세미콜론 없이 줄바꿈으로 끝난 문장).
     */
    private boolean statementStart(int k) {
        if (k == 0) {
            return true;
        }
        JsToken prev = t.get(k - 1);
        if (prev.is(";")
                || prev.is("{")
                || prev.is("}")
                || prev.is(")")
                || prev.is("export")
                || prev.is("default")
                || prev.is("else")) {
            return true;
        }
        boolean continues =
                (prev.kind() == JsToken.Kind.PUNCT && !prev.is("]"))
                        || EXPRESSION_BEFORE.contains(prev.text());
        return prev.line() < t.get(k).line() && !continues;
    }

    private void arrow(int a) {
        int paramsStart;
        List<String> params;
        if (JsTokens.is(t, a - 1, ")") && match[a - 1] >= 0) {
            paramsStart = match[a - 1];
            params = params(paramsStart);
        } else if (JsTokens.kind(t, a - 1) == JsToken.Kind.IDENT) {
            paramsStart = a - 1;
            params = List.of(t.get(a - 1).text());
        } else {
            return;
        }
        int nameAt = JsTokens.is(t, paramsStart - 1, "async") ? paramsStart - 1 : paramsStart;
        String name = assignedName(nameAt);
        int end;
        if (JsTokens.is(t, a + 1, "{")) {
            end = bodyEnd(a + 1);
        } else {
            int last = expressionEnd(a + 1);
            end = last > a + 1 ? t.get(last - 1).end() : t.get(a).end();
        }
        JsFunction fn = new JsFunction(name, params, t.get(paramsStart).start(), end);
        addFunctionToModule(fn);
        addParams(fn);
        if (t.get(paramsStart).is("(")) {
            addDestructured(paramsStart, fn);
        }
    }

    /** {@code name(…) {…}} 객체·클래스 메서드. 이름은 붙이지 않는다(래퍼로 인라인하지 않음). */
    private boolean method(int i) {
        int close = match[i + 1];
        if (close < 0 || !JsTokens.is(t, close + 1, "{")) {
            return false;
        }
        JsToken prev = i > 0 ? t.get(i - 1) : null;
        boolean atMember =
                prev == null
                        || prev.is("{")
                        || prev.is(",")
                        || prev.is(";")
                        || prev.is("}")
                        || prev.is("async")
                        || prev.is("get")
                        || prev.is("set")
                        || prev.is("static")
                        || prev.is("*");
        if (atMember) {
            addFunction(null, i + 1, bodyEnd(close + 1));
        }
        return true;
    }

    private void addFunction(String name, int paramsOpen, int end) {
        JsFunction fn = new JsFunction(name, params(paramsOpen), t.get(paramsOpen).start(), end);
        addFunctionToModule(fn);
        addParams(fn);
        addDestructured(paramsOpen, fn);
    }

    /**
     * 구조 분해 매개변수({@code {a, b: c, d = 1}}·{@code [x, ...y]})가 만드는 이름. 값은 모르므로 인라인하지 않지만 바깥 이름을 가린다.
     * 키({@code b:})와 기본값 식 안의 이름은 바인딩이 아니다.
     */
    private void addDestructured(int open, JsFunction fn) {
        int close = match[open];
        if (close < 0) {
            return;
        }
        for (List<JsToken> param : split(open + 1, close)) {
            int k = !param.isEmpty() && param.get(0).is("...") ? 1 : 0;
            if (k < param.size() && (param.get(k).is("{") || param.get(k).is("["))) {
                addPattern(param.subList(k, param.size()), fn.start(), fn.end());
            }
        }
    }

    /** 구조 분해 패턴이 만드는 이름을 [from, to] 범위의 변수로 등록한다. 키({@code b:})와 기본값 식 안의 이름은 바인딩이 아니다. */
    private void addPattern(List<JsToken> param, int from, int to) {
        int depth = 0;
        int defaultDepth = -1;
        for (int k = 0; k < param.size(); k++) {
            budget.spend();
            JsToken token = param.get(k);
            boolean opens = token.is("{") || token.is("[") || token.is("(");
            boolean closes = token.is("}") || token.is("]") || token.is(")");
            if (defaultDepth >= 0) {
                if (opens) {
                    depth++;
                } else if (closes) {
                    depth--;
                }
                if (depth < defaultDepth || (token.is(",") && depth == defaultDepth)) {
                    defaultDepth = -1;
                }
                continue;
            }
            if (opens) {
                depth++;
            } else if (closes) {
                depth--;
            } else if (token.is("=")) {
                defaultDepth = depth;
            } else if (token.kind() == JsToken.Kind.IDENT
                    && !(k + 1 < param.size() && param.get(k + 1).is(":"))) {
                module.decls()
                        .add(new JsDecl(token.text(), Kind.VARIABLE, from, to, List.of(), null));
            }
        }
    }

    private void addParams(JsFunction fn) {
        for (String param : fn.params()) {
            if (!param.isEmpty()) {
                String name = param.startsWith("...") ? param.substring(3) : param;
                module.decls()
                        .add(new JsDecl(name, Kind.PARAM, fn.start(), fn.end(), List.of(), fn));
            }
        }
    }

    /**
     * 여는 괄호 위치의 매개변수 목록. 단순 이름(기본값 허용)·나머지 매개변수만 이름을 갖고, 나머지 매개변수는 {@code ...이름}이다 (인자 하나가 아니라
     * 배열이므로 래퍼 인라인 때 인자를 넣지 않는다).
     */
    private List<String> params(int open) {
        List<String> names = new ArrayList<>();
        int close = match[open];
        if (close < 0) {
            return names;
        }
        for (List<JsToken> param : split(open + 1, close)) {
            boolean rest = !param.isEmpty() && param.get(0).is("...");
            List<JsToken> p = rest ? param.subList(1, param.size()) : param;
            boolean simple =
                    !p.isEmpty()
                            && p.get(0).kind() == JsToken.Kind.IDENT
                            && (p.size() == 1 || p.get(1).is("="));
            names.add(simple ? (rest ? "..." : "") + p.get(0).text() : "");
        }
        return names;
    }

    /** {@code const|let|var 이름 = [async] function…}·{@code … = (…) =>}의 이름. */
    private String assignedName(int at) {
        if (JsTokens.is(t, at - 1, "=")
                && JsTokens.kind(t, at - 2) == JsToken.Kind.IDENT
                && (JsTokens.is(t, at - 3, "const")
                        || JsTokens.is(t, at - 3, "let")
                        || JsTokens.is(t, at - 3, "var"))) {
            return t.get(at - 2).text();
        }
        return null;
    }

    private int bodyEnd(int bodyOpen) {
        int close = match[bodyOpen];
        return close < 0 ? Integer.MAX_VALUE : t.get(close).end();
    }

    // ---- 내보내기 ----

    private void export(int i) {
        if (JsTokens.is(t, i + 1, "default")) {
            int j = i + 2;
            if (JsTokens.is(t, j, "async")) {
                j++;
            }
            if (JsTokens.is(t, j, "function")) {
                int k = JsTokens.is(t, j + 1, "*") ? j + 2 : j + 1;
                if (JsTokens.kind(t, k) == JsToken.Kind.IDENT && JsTokens.is(t, k + 1, "(")) {
                    module.exports().put("default", new JsExport(t.get(k).text(), null));
                }
                return;
            }
            int end = expressionEnd(i + 2);
            List<JsToken> expr = t.subList(i + 2, end);
            if (expr.size() == 1 && expr.get(0).kind() == JsToken.Kind.IDENT) {
                module.exports().put("default", new JsExport(expr.get(0).text(), null));
            } else {
                module.exports().put("default", new JsExport(null, expr));
            }
            return;
        }
        int j = JsTokens.is(t, i + 1, "async") ? i + 2 : i + 1;
        if (JsTokens.is(t, j, "function")) {
            int k = JsTokens.is(t, j + 1, "*") ? j + 2 : j + 1;
            if (JsTokens.kind(t, k) == JsToken.Kind.IDENT) {
                module.exports().put(t.get(k).text(), new JsExport(t.get(k).text(), null));
            }
        } else if (JsTokens.is(t, j, "const")
                || JsTokens.is(t, j, "let")
                || JsTokens.is(t, j, "var")) {
            int k = j + 1;
            while (JsTokens.kind(t, k) == JsToken.Kind.IDENT) {
                module.exports().put(t.get(k).text(), new JsExport(t.get(k).text(), null));
                int next = JsTokens.is(t, k + 1, "=") ? expressionEnd(k + 2) : k + 1;
                if (!JsTokens.is(t, next, ",")) {
                    break;
                }
                k = next + 1;
            }
        } else if (JsTokens.is(t, i + 1, "{")) {
            Map<String, String> names = new LinkedHashMap<>();
            int after = JsModuleReader.readSpecifiers(t, i + 2, names);
            if (!JsTokens.is(t, after, "from")) {
                names.forEach(
                        (exported, local) ->
                                module.exports().put(exported, new JsExport(local, null)));
            }
        }
    }

    // ---- 호출 ----

    private void callOrMethod(int i) {
        if (JsTokens.is(t, i + 1, "(") && !JsTokens.is(t, i - 1, "function")) {
            if (method(i)) {
                return;
            }
            int close = match[i + 1];
            if (close < 0 || JsTokens.is(t, close + 1, "=>")) {
                return;
            }
            addCall(i, null, i + 1);
        } else if ((JsTokens.is(t, i + 1, ".") || JsTokens.is(t, i + 1, "?."))
                && JsTokens.kind(t, i + 2) == JsToken.Kind.IDENT
                && JsTokens.is(t, i + 3, "(")) {
            int close = match[i + 3];
            if (close < 0 || JsTokens.is(t, close + 1, "{")) {
                return;
            }
            addCall(i, t.get(i + 2).text(), i + 3);
        }
    }

    private void addCall(int i, String member, int open) {
        int close = match[open];
        module.calls()
                .add(
                        new JsCall(
                                t.get(i).text(),
                                member,
                                split(open + 1, close),
                                t.get(i).line(),
                                t.get(i).start(),
                                t.get(close).end()));
    }

    /** {@code X.defaults.baseURL = 식} (X는 axios 이름). */
    private void defaultsBaseUrl(int i) {
        if (module.axiosNames().contains(t.get(i).text())
                && JsTokens.is(t, i + 1, ".")
                && JsTokens.is(t, i + 2, "defaults")
                && JsTokens.is(t, i + 3, ".")
                && JsTokens.is(t, i + 4, "baseURL")
                && JsTokens.is(t, i + 5, "=")) {
            defaultsCandidates.add(new Candidate(this, i));
        }
    }

    // ---- 도우미 ----

    /** [from, to) 토큰을 최상위 쉼표로 나눈다. 빈 목록이면 인자 없음. */
    private List<List<JsToken>> split(int from, int to) {
        List<List<JsToken>> parts = new ArrayList<>();
        if (from >= to) {
            return parts;
        }
        int start = from;
        int j = from;
        while (j < to) {
            budget.spend();
            JsToken token = t.get(j);
            if ((token.is("(") || token.is("[") || token.is("{")) && match[j] > j) {
                j = match[j] + 1;
                continue;
            }
            if (token.is(",")) {
                parts.add(t.subList(start, j));
                start = j + 1;
            }
            j++;
        }
        if (start < to) {
            parts.add(t.subList(start, to));
        }
        return parts;
    }

    /**
     * from에서 시작한 식이 끝나는 위치(제외). 같은 깊이에서 지나온 위치에서 시작해도 끝은 같으므로(시작 자리의 줄바꿈 규칙만 다른데 지나왔다면 그 규칙이 걸리지
     * 않았다) 지나온 위치마다 메모한다. 화살표 함수 사슬처럼 식 끝을 거듭 묻는 입력이 제곱 시간이 되지 않는다.
     */
    int expressionEnd(int from) {
        if (from < t.size() && endMemo[from] >= 0) {
            return endMemo[from];
        }
        int count = 0;
        int end = t.size();
        int j = from;
        while (j < t.size()) {
            budget.spend();
            JsToken token = t.get(j);
            if (token.is(";") || token.is(",") || token.is(")") || token.is("]") || token.is("}")) {
                end = j;
                break;
            }
            if (j > from
                    && token.line() > t.get(j - 1).line()
                    && !continuesAfter(t.get(j - 1))
                    && !continuesBefore(token)) {
                end = j;
                break;
            }
            if (j > from && endMemo[j] >= 0) {
                end = endMemo[j];
                break;
            }
            count = visit(count, j);
            if ((token.is("(") || token.is("[") || token.is("{")) && match[j] > j) {
                j = match[j] + 1;
                continue;
            }
            j++;
        }
        for (int k = 0; k < count; k++) {
            endMemo[visited[k]] = end;
        }
        return end;
    }

    /**
     * from부터 같은 깊이에서 처음 나오는 {@code {}} 위치 ({@code ()}·{@code []} 묶음은 건너뜀). 없으면 t.size(). {@link
     * #expressionEnd}처럼 지나온 위치마다 메모한다 ({@code class a class a … {}}가 제곱 시간이 되지 않게).
     */
    private int nextBrace(int from) {
        int count = 0;
        int found = t.size();
        int j = from;
        while (j < t.size()) {
            budget.spend();
            if (braceMemo[j] >= 0) {
                found = braceMemo[j];
                break;
            }
            if (t.get(j).is("{")) {
                found = j;
                break;
            }
            count = visit(count, j);
            j = (t.get(j).is("(") || t.get(j).is("[")) && match[j] > j ? match[j] + 1 : j + 1;
        }
        for (int k = 0; k < count; k++) {
            braceMemo[visited[k]] = found;
        }
        return found;
    }

    private int visit(int count, int position) {
        if (count == visited.length) {
            visited = Arrays.copyOf(visited, count * 2);
        }
        visited[count] = position;
        return count + 1;
    }

    private static boolean continuesAfter(JsToken prev) {
        return prev.kind() == JsToken.Kind.PUNCT
                && !prev.is(")")
                && !prev.is("]")
                && !prev.is("}")
                && !prev.is("++")
                && !prev.is("--");
    }

    private static boolean continuesBefore(JsToken token) {
        return token.kind() == JsToken.Kind.PUNCT && !NOT_CONTINUING.contains(token.text());
    }
}
