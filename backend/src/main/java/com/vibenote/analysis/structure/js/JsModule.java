package com.vibenote.analysis.structure.js;

import com.vibenote.analysis.structure.WorkBudget;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
    private Set<JsDecl> reassigned;

    /** 조회 색인. 선언·import·호출을 다 읽은 뒤(첫 조회 때) 만든다. 그 뒤에 목록이 늘면 다시 만든다 (읽기 단계는 조회 전에 모두 끝나므로 한 번이다). */
    private int indexedDecls = -1;

    private Map<String, List<JsDecl>> declsByName;
    private final Map<String, JsScopeIndex> scopes = new HashMap<>();
    private final Map<String, Optional<JsDecl>> topLevel = new HashMap<>();
    private int indexedImports = -1;
    private Map<String, Map.Entry<JsImport, String>> importsByLocal;
    private int indexedCalls = -1;
    private Map<String, List<JsCall>> callsByReceiver;

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

    /** offset에서 보이는 name 선언 중 가장 안쪽 것 (범위 색인, O(log)). */
    public Optional<JsDecl> lookup(String name, int offset) {
        List<JsDecl> named = declsNamed(name);
        if (named.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(
                scopes.computeIfAbsent(name, k -> new JsScopeIndex(named)).lookup(offset));
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
        // 선언은 record라 해시가 초기값 토큰 전체를 훑는다. 같은 선언 객체인지만 본다
        Set<JsDecl> out = Collections.newSetFromMap(new IdentityHashMap<>());
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

    /** 파일 최상위 선언 중 먼저 나온 것. */
    public Optional<JsDecl> lookupTopLevel(String name) {
        List<JsDecl> named = declsNamed(name);
        return topLevel.computeIfAbsent(
                name, k -> named.stream().filter(d -> d.scopeStart() < 0).findFirst());
    }

    /** 이름별 선언 (등록 순서). */
    private List<JsDecl> declsNamed(String name) {
        if (indexedDecls != decls.size()) {
            declsByName = new HashMap<>();
            for (JsDecl decl : decls) {
                declsByName.computeIfAbsent(decl.name(), k -> new ArrayList<>()).add(decl);
            }
            scopes.clear();
            topLevel.clear();
            indexedDecls = decls.size();
        }
        return declsByName.getOrDefault(name, List.of());
    }

    /** 이 지역 이름을 가져온 import(앞의 것)와 그 가져온 이름. */
    public Optional<Map.Entry<JsImport, String>> importOf(String local) {
        if (indexedImports != imports.size()) {
            importsByLocal = new HashMap<>();
            for (JsImport imp : imports) {
                imp.bindings()
                        .forEach(
                                (l, imported) ->
                                        importsByLocal.putIfAbsent(l, Map.entry(imp, imported)));
            }
            indexedImports = imports.size();
        }
        return Optional.ofNullable(importsByLocal.get(local));
    }

    /** {@code receiver(…)}(member가 null) 또는 {@code receiver.member(…)} 호출, 나온 순서. */
    public List<JsCall> callsTo(String receiver, String member) {
        if (indexedCalls != calls.size()) {
            callsByReceiver = new HashMap<>();
            for (JsCall call : calls) {
                callsByReceiver
                        .computeIfAbsent(
                                callKey(call.receiver(), call.member()), k -> new ArrayList<>())
                        .add(call);
            }
            indexedCalls = calls.size();
        }
        return callsByReceiver.getOrDefault(callKey(receiver, member), List.of());
    }

    /** 식별자에 나올 수 없는 글자로 잇는다. */
    private static String callKey(String receiver, String member) {
        return member == null ? receiver + "(" : receiver + "." + member;
    }

    /** 원문 [start, end)를 공백 하나로 접고 앞뒤 공백을 뺀 앞 max자. 필요한 앞부분만 읽는다 (중첩된 긴 호출마다 원문 전체를 복사하지 않게). */
    public String excerpt(int start, int end, int max) {
        StringBuilder out = new StringBuilder(max);
        boolean space = false;
        for (int i = Math.max(0, start); i < Math.min(source.length(), end); i++) {
            char c = source.charAt(i);
            if (Character.isWhitespace(c)) {
                space = !out.isEmpty();
                continue;
            }
            if (space) {
                if (out.length() == max) {
                    break;
                }
                out.append(' ');
                space = false;
            }
            if (out.length() == max) {
                break;
            }
            out.append(c);
        }
        return out.toString();
    }
}
