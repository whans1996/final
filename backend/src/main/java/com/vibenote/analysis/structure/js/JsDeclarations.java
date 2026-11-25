package com.vibenote.analysis.structure.js;

import com.vibenote.analysis.structure.WorkBudget;
import com.vibenote.analysis.structure.js.JsDecl.Kind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
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
 *   <li>안전하게 실패 (5.3절 1): 단순 이름·구조 분해가 아닌 매개변수·catch 이름({@code url: string})은 UNKNOWN 선언, {@code
 *       with} 본문은 그 안의 바깥 이름을 모두 UNKNOWN으로 본다. 구조 분해 대입·for 머리의 대상은 다시 대입으로 센다
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

    /** 뒤에 {@code (…) {}}가 오는 문장 키워드. 이 밖의 키워드가 그 모양이면 메서드 이름이다. */
    private static final Set<String> STATEMENT_HEADS =
            Set.of("if", "for", "while", "switch", "catch", "with", "function");

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

    /** 이미 이름을 모은 대입 패턴의 끝. 안쪽 패턴을 다시 훑지 않는다. */
    private int patternsReadUntil = -1;

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

    /** 템플릿 {@code ${}} 안에서 이름을 새로 만드는 토큰. */
    private static final Set<String> TEMPLATE_DECLARING =
            Set.of("const", "let", "var", "function", "class", "catch", "=>");

    /**
     * 템플릿 {@code ${}} 안에서 이름을 선언하면(함수 본문·화살표 매개변수 등) 그 범위를 따로 모델링하지 않고, 그 식 안의 이름을 모두 식 전체에서
     * UNKNOWN으로 둔다 (5.3절 1 안전하게 실패). 바깥 HTTP 이름을 가렸으면 URL_UNKNOWN이 된다.
     */
    private void shadowTemplateNames() {
        if (t.isEmpty()
                || t.stream().noneMatch(token -> TEMPLATE_DECLARING.contains(token.text()))) {
            return;
        }
        int from = t.getFirst().start();
        int to = t.getLast().end();
        Set<String> names = new HashSet<>();
        for (int i = 0; i < t.size(); i++) {
            budget.spend();
            JsToken token = t.get(i);
            if (token.kind() == JsToken.Kind.IDENT
                    && !JsTokens.isMemberName(t, i)
                    && !KEYWORDS.contains(token.text())
                    && names.add(token.text())) {
                module.decls()
                        .add(new JsDecl(token.text(), Kind.UNKNOWN, from, to, List.of(), null));
            }
        }
    }

    private void readAll() {
        if (inTemplate) {
            shadowTemplateNames();
        }
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
                                        module.importedAxiosNames().add(local);
                                    }
                                });
            }
        }
    }

    private void readAt(int i) {
        JsToken token = t.get(i);
        if (token.is("=>")) {
            arrow(i);
            return;
        }
        if (token.is("(")) {
            literalNamedMethod(i);
            return;
        }
        if (token.is("[") || token.is("{")) {
            assignmentPattern(i);
            return;
        }
        if (token.kind() != JsToken.Kind.IDENT || JsTokens.isMemberName(t, i)) {
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
            case "for" -> forHead(i);
            case "with" -> withBody(i);
            default -> {
                if (!KEYWORDS.contains(token.text())) {
                    callOrMethod(i);
                    defaultsBaseUrl(i);
                } else if (!STATEMENT_HEADS.contains(token.text()) && JsTokens.is(t, i + 1, "(")) {
                    // 키워드 이름 메서드 ({ delete(id) {…} })
                    method(i);
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
                module.declaratorNames().add(t.get(j).start());
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

    /**
     * {@code catch (e) {…}}의 e와 구조 분해 catch의 이름. 그 블록 안에서만 보이고 바깥 이름을 가린다. 그 밖의 모양({@code e:
     * unknown})은 첫 이름을 UNKNOWN으로 둔다.
     */
    private void catchParam(int i) {
        if (!JsTokens.is(t, i + 1, "(") || match[i + 1] < 0) {
            return;
        }
        int close = match[i + 1];
        if (close <= i + 2 || !JsTokens.is(t, close + 1, "{") || match[close + 1] < 0) {
            return;
        }
        int from = t.get(close + 1).start();
        int to = t.get(match[close + 1]).end();
        JsToken first = t.get(i + 2);
        if (first.is("{") || first.is("[")) {
            addPattern(t.subList(i + 2, close), from, to);
        } else if (first.kind() == JsToken.Kind.IDENT) {
            Kind kind = close == i + 3 ? Kind.VARIABLE : Kind.UNKNOWN;
            module.decls().add(new JsDecl(first.text(), kind, from, to, List.of(), null));
        }
    }

    /**
     * {@code with (o) 본문}의 본문 범위. 그 안의 이름은 o의 속성일 수 있다 ({@link JsModule#lookup}). 본문이 블록이 아니면 문장
     * 하나다.
     */
    private void withBody(int i) {
        if (!JsTokens.is(t, i + 1, "(") || match[i + 1] < 0) {
            return;
        }
        int body = match[i + 1] + 1;
        if (body >= t.size()) {
            return;
        }
        int end;
        if (t.get(body).is("{") && match[body] > 0) {
            end = t.get(match[body]).end();
        } else {
            int last = expressionEnd(body);
            end = last > body ? t.get(last - 1).end() : t.get(body).end();
        }
        module.withScopes()
                .add(new JsDecl("with", Kind.UNKNOWN, t.get(body).start(), end, List.of(), null));
    }

    /** {@code for (x of …)}·{@code for ([a, b] in …)}처럼 선언 없이 for 머리에서 대입하는 이름. */
    private void forHead(int i) {
        int open = JsTokens.is(t, i + 1, "await") ? i + 2 : i + 1;
        if (!JsTokens.is(t, open, "(")) {
            return;
        }
        int j = open + 1;
        int after;
        List<JsToken> names;
        if ((JsTokens.is(t, j, "[") || JsTokens.is(t, j, "{")) && match[j] > j) {
            after = match[j] + 1;
            names = patternNames(t.subList(j, after));
        } else if (JsTokens.kind(t, j) == JsToken.Kind.IDENT
                && !JsTokens.is(t, j, "const")
                && !JsTokens.is(t, j, "let")
                && !JsTokens.is(t, j, "var")) {
            after = j + 1;
            names = List.of(t.get(j));
        } else {
            return;
        }
        if (JsTokens.is(t, after, "of") || JsTokens.is(t, after, "in")) {
            module.patternWrites().addAll(names);
        }
    }

    /**
     * {@code [a, b] = …}·{@code ({a} = …)} 구조 분해 대입의 대상 이름. 선언({@code const [a] = …})의 패턴도 여기에 걸리지만
     * 그 이름은 이미 값을 모르는 변수라 결과가 같다.
     */
    private void assignmentPattern(int i) {
        if (i < patternsReadUntil || match[i] <= i || !JsTokens.is(t, match[i] + 1, "=")) {
            return;
        }
        patternsReadUntil = match[i];
        module.patternWrites().addAll(patternNames(t.subList(i, match[i] + 1)));
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
        Set<String> defaulted;
        if (JsTokens.is(t, a - 1, ")") && match[a - 1] >= 0) {
            paramsStart = match[a - 1];
            params = params(paramsStart);
            defaulted = defaulted(paramsStart);
        } else if (JsTokens.kind(t, a - 1) == JsToken.Kind.IDENT) {
            paramsStart = a - 1;
            params = List.of(t.get(a - 1).text());
            defaulted = Set.of();
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
        JsFunction fn = new JsFunction(name, params, defaulted, t.get(paramsStart).start(), end);
        addFunctionToModule(fn);
        addParams(fn);
        if (t.get(paramsStart).is("(")) {
            addBindings(paramsStart, fn);
        }
    }

    /** {@code name(…) {…}} 객체·클래스 메서드. 이름은 붙이지 않는다(래퍼로 인라인하지 않음). */
    private boolean method(int i) {
        int close = match[i + 1];
        if (close < 0 || !JsTokens.is(t, close + 1, "{")) {
            return false;
        }
        if (memberStart(i)) {
            addFunction(null, i + 1, bodyEnd(close + 1));
        }
        return true;
    }

    /**
     * {@code ['x'](…) {…}}·{@code 'x'(…) {…}}·{@code 0(…) {…}}처럼 이름이 식별자가 아닌 메서드. open은 매개변수 여는
     * 괄호다.
     */
    private void literalNamedMethod(int open) {
        int close = match[open];
        if (open == 0 || close < 0 || !JsTokens.is(t, close + 1, "{") || match[close + 1] < 0) {
            return;
        }
        JsToken name = t.get(open - 1);
        int nameStart;
        if (name.is("]") && match[open - 1] >= 0) {
            nameStart = match[open - 1];
        } else if (name.kind() == JsToken.Kind.STRING || name.kind() == JsToken.Kind.NUMBER) {
            nameStart = open - 1;
        } else {
            return;
        }
        if (memberStart(nameStart)) {
            addFunction(null, open, bodyEnd(close + 1));
        }
    }

    /** k가 객체·클래스 멤버 이름이 시작하는 자리인지 (앞 토큰이 멤버 경계나 수식어). */
    private boolean memberStart(int k) {
        JsToken prev = k > 0 ? t.get(k - 1) : null;
        return prev == null
                || prev.is("{")
                || prev.is(",")
                || prev.is(";")
                || prev.is("}")
                || prev.is("async")
                || prev.is("get")
                || prev.is("set")
                || prev.is("static")
                || prev.is("*")
                || prev.is("#");
    }

    private void addFunction(String name, int paramsOpen, int end) {
        JsFunction fn =
                new JsFunction(
                        name,
                        params(paramsOpen),
                        defaulted(paramsOpen),
                        t.get(paramsOpen).start(),
                        end);
        addFunctionToModule(fn);
        addParams(fn);
        addBindings(paramsOpen, fn);
    }

    /**
     * 단순 이름이 아닌 매개변수가 만드는 이름. 구조 분해({@code {a, b: c, d = 1}}·{@code [x, ...y]})는 값을 모르는 변수로, 그 밖의
     * 모양({@code url: string})은 첫 이름을 UNKNOWN으로 둔다. 인라인하지 않지만 바깥 이름을 가린다. 키({@code b:})와 기본값 식 안의
     * 이름은 바인딩이 아니다.
     */
    private void addBindings(int open, JsFunction fn) {
        int close = match[open];
        if (close < 0) {
            return;
        }
        for (List<JsToken> param : split(open + 1, close)) {
            int k = !param.isEmpty() && param.get(0).is("...") ? 1 : 0;
            if (k >= param.size()) {
                continue;
            }
            JsToken first = param.get(k);
            if (first.is("{") || first.is("[")) {
                addPattern(param.subList(k, param.size()), fn.start(), fn.end());
            } else if (first.kind() == JsToken.Kind.IDENT
                    && k + 1 < param.size()
                    && !param.get(k + 1).is("=")) {
                module.decls()
                        .add(
                                new JsDecl(
                                        first.text(),
                                        Kind.UNKNOWN,
                                        fn.start(),
                                        fn.end(),
                                        List.of(),
                                        null));
            }
        }
    }

    /** 구조 분해 패턴이 만드는 이름을 [from, to] 범위의 변수로 등록한다. */
    private void addPattern(List<JsToken> param, int from, int to) {
        for (JsToken name : patternNames(param)) {
            module.decls().add(new JsDecl(name.text(), Kind.VARIABLE, from, to, List.of(), null));
        }
    }

    /** 구조 분해 패턴의 이름 토큰. 키({@code b:}), 기본값 식 안의 이름, 멤버 이름({@code [this.a] = …}의 a)은 바인딩이 아니다. */
    private List<JsToken> patternNames(List<JsToken> param) {
        List<JsToken> names = new ArrayList<>();
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
                    && !(k + 1 < param.size() && param.get(k + 1).is(":"))
                    && !(k > 0 && (param.get(k - 1).is(".") || param.get(k - 1).is("?.")))) {
                names.add(token);
            }
        }
        return names;
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

    /** 여는 괄호 위치의 매개변수 중 기본값({@code 이름 = 값})이 있는 단순 이름. */
    private Set<String> defaulted(int open) {
        Set<String> names = new HashSet<>();
        int close = match[open];
        if (close < 0) {
            return names;
        }
        for (List<JsToken> param : split(open + 1, close)) {
            if (param.size() > 1
                    && param.get(0).kind() == JsToken.Kind.IDENT
                    && param.get(1).is("=")) {
                names.add(param.get(0).text());
            }
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
