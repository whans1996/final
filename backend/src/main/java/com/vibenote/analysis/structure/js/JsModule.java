package com.vibenote.analysis.structure.js;

import com.vibenote.analysis.structure.WorkBudget;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 토큰으로 읽은 JS·Vue 파일 하나와 그 선언·내보내기·호출 (F-21a AC6~AC10). */
public final class JsModule {

    private final long fileId;
    private final String path;
    private final String source;
    private final List<JsToken> tokens;
    private final WorkBudget budget;
    private final List<JsImport> imports = new ArrayList<>();
    private final List<JsDecl> decls = new ArrayList<>();
    private final Map<String, List<JsDecl>> declsByName = new HashMap<>();
    private int indexed;
    private Set<JsDecl> reassigned;

    /** 대입 연산자. {@code ==}·{@code =>}는 토크나이저가 따로 나눈다. */
    public static final Set<String> ASSIGN_OPS =
            Set.of(
                    "=", "+=", "-=", "*=", "/=", "%=", "**=", "<<=", ">>=", ">>>=", "&=", "|=",
                    "^=", "&&=", "||=", "??=");

    private final List<JsFunction> functions = new ArrayList<>();
    private final Map<String, JsExport> exports = new HashMap<>();
    private final Set<String> axiosNames = new HashSet<>();
    private final List<JsCall> calls = new ArrayList<>();
    private final List<List<JsToken>> defaultsBaseUrls = new ArrayList<>();

    public JsModule(
            long fileId, String path, String source, List<JsToken> tokens, WorkBudget budget) {
        this.fileId = fileId;
        this.path = path;
        this.source = source;
        this.tokens = tokens;
        this.budget = budget;
    }

    /** 이 모듈을 읽고 계산하는 요청의 작업 예산. */
    public WorkBudget budget() {
        return budget;
    }

    public long fileId() {
        return fileId;
    }

    public String path() {
        return path;
    }

    public List<JsToken> tokens() {
        return tokens;
    }

    public List<JsImport> imports() {
        return imports;
    }

    public List<JsDecl> decls() {
        return decls;
    }

    public List<JsFunction> functions() {
        return functions;
    }

    /** 내보낸 이름({@code default} 포함) → 내보낸 것. */
    public Map<String, JsExport> exports() {
        return exports;
    }

    /** {@code import X from 'axios'}·{@code const X = require('axios')}의 X. */
    public Set<String> axiosNames() {
        return axiosNames;
    }

    public List<JsCall> calls() {
        return calls;
    }

    /** {@code X.defaults.baseURL = 식}의 식 (X는 axios 이름). */
    public List<List<JsToken>> defaultsBaseUrls() {
        return defaultsBaseUrls;
    }

    /** offset에서 보이는 name 선언 중 가장 안쪽 것. */
    public Optional<JsDecl> lookup(String name, int offset) {
        JsDecl best = null;
        for (JsDecl decl : declsNamed(name)) {
            if (decl.visibleAt(offset) && (best == null || decl.scopeStart() > best.scopeStart())) {
                best = decl;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * offset에서 name이 axios인지: import·require로 얻은 axios 이름이고, 그 자리에서 매개변수·지역 변수가 가리지 않았거나 보이는 선언이
     * {@code require('axios')}다.
     */
    public boolean axiosAt(String name, int offset) {
        if (!axiosNames.contains(name)) {
            return false;
        }
        Optional<JsDecl> decl = lookup(name, offset);
        return decl.isEmpty()
                || (decl.get().kind() == JsDecl.Kind.CONST
                        && JsDeclarations.isRequireAxios(decl.get().init()));
    }

    /**
     * 이 선언이 파일 어딘가에서 다시 대입되는지 ({@code x = …}·{@code x += …}·{@code x++}). 매개변수 목록 안의 기본값({@code (x
     * = 1) =>})은 대입이 아니다. 선언을 다 읽은 뒤 처음 물을 때 한 번 훑는다.
     */
    public boolean reassigned(JsDecl decl) {
        if (reassigned == null) {
            reassigned = findReassigned();
        }
        return reassigned.contains(decl);
    }

    private Set<JsDecl> findReassigned() {
        Set<Integer> paramNames = new HashSet<>();
        Map<Integer, Integer> indexByStart = new HashMap<>();
        budget.spend(tokens.size());
        for (int i = 0; i < tokens.size(); i++) {
            indexByStart.put(tokens.get(i).start(), i);
        }
        for (JsFunction fn : functions) {
            Integer at = indexByStart.get(fn.start());
            if (at == null) {
                continue;
            }
            if (!tokens.get(at).is("(")) {
                paramNames.add(fn.start());
                continue;
            }
            int depth = 0;
            for (int i = at; i < tokens.size(); i++) {
                budget.spend();
                JsToken token = tokens.get(i);
                if (token.is("(")) {
                    depth++;
                } else if (token.is(")") && --depth == 0) {
                    break;
                } else if (token.kind() == JsToken.Kind.IDENT) {
                    paramNames.add(token.start());
                }
            }
        }
        Set<JsDecl> out = new HashSet<>();
        for (int i = 0; i < tokens.size(); i++) {
            budget.spend();
            JsToken token = tokens.get(i);
            if (token.kind() != JsToken.Kind.IDENT
                    || paramNames.contains(token.start())
                    || (i > 0 && (tokens.get(i - 1).is(".") || tokens.get(i - 1).is("?.")))) {
                continue;
            }
            boolean write =
                    (i + 1 < tokens.size()
                                    && (ASSIGN_OPS.contains(tokens.get(i + 1).text())
                                            || tokens.get(i + 1).is("++")
                                            || tokens.get(i + 1).is("--")))
                            || (i > 0
                                    && (tokens.get(i - 1).is("++") || tokens.get(i - 1).is("--")));
            if (write) {
                lookup(token.text(), token.start()).ifPresent(out::add);
            }
        }
        return out;
    }

    /** 파일 최상위 선언. */
    public Optional<JsDecl> lookupTopLevel(String name) {
        return declsNamed(name).stream().filter(d -> d.scopeStart() < 0).findFirst();
    }

    /** 이름별 선언. 조회 때 새로 붙은 선언만 묶어 넣는다 (decls는 덧붙이기만 한다). 호출마다 모든 선언을 훑지 않게 한다. */
    private List<JsDecl> declsNamed(String name) {
        for (; indexed < decls.size(); indexed++) {
            JsDecl decl = decls.get(indexed);
            declsByName.computeIfAbsent(decl.name(), k -> new ArrayList<>()).add(decl);
        }
        return declsByName.getOrDefault(name, List.of());
    }

    /** 이 지역 이름을 가져온 import와 그 가져온 이름. */
    public Optional<Map.Entry<JsImport, String>> importOf(String local) {
        for (JsImport imp : imports) {
            String imported = imp.bindings().get(local);
            if (imported != null) {
                return Optional.of(Map.entry(imp, imported));
            }
        }
        return Optional.empty();
    }

    /** 원문 [start, end)를 공백 하나로 접어 max자까지. */
    public String excerpt(int start, int end, int max) {
        String text =
                source.substring(Math.max(0, start), Math.min(source.length(), end))
                        .replaceAll("\\s+", " ")
                        .strip();
        return text.length() <= max ? text : text.substring(0, max);
    }
}
