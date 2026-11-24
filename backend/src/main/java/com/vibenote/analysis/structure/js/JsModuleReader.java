package com.vibenote.analysis.structure.js;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.structure.EdgeType;
import com.vibenote.analysis.structure.StructureLinks;
import com.vibenote.analysis.structure.UnresolvedKind;
import com.vibenote.analysis.structure.UnresolvedReason;
import com.vibenote.analysis.structure.js.JsToken.Kind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * JS·Vue 모듈 import (F-21a AC6).
 *
 * <p>{@code import … from 'm'}, {@code import 'm'}, {@code export … from 'm'}, {@code import('m')},
 * {@code require('m')}에서 m이 문자열 리터럴이면 경로를 푼다. {@code ./}·{@code ../}는 그 파일 폴더, {@code @/}는 가장 가까운
 * 상위 {@code src/} 기준이고 후보는 그대로 → .js → .mjs → .vue → /index.js 순이다. 패키지 이름은 무시한다. 풀리지 않은
 * 상대·{@code @/} 경로는 NOT_SELECTED로 남기되, .css·이미지처럼 코드가 아닌 확장자는 선별 대상이 될 수 없어 남기지 않는다.
 */
public final class JsModuleReader {

    private static final List<String> SUFFIXES = List.of("", ".js", ".mjs", ".vue", "/index.js");
    private static final Set<String> CODE_EXTENSIONS = Set.of("js", "mjs", "vue");

    private JsModuleReader() {}

    /** 파일마다 토큰과 import를 읽고 import 간선을 남긴다. 토큰으로 나눌 수 없는 파일은 PARSE_ERROR이고 결과에서 빠진다. */
    public static Map<Long, JsModule> read(List<AnalysisFile> files, StructureLinks out) {
        Map<String, Long> paths = new HashMap<>();
        files.forEach(f -> paths.putIfAbsent(f.getPath(), f.getId()));

        Map<Long, JsModule> modules = new LinkedHashMap<>();
        for (AnalysisFile file :
                files.stream().sorted(Comparator.comparing(AnalysisFile::getId)).toList()) {
            List<JsToken> tokens;
            try {
                tokens = JsTokenizer.tokenizeFile(file.getPath(), file.getContent());
            } catch (JsSyntaxException e) {
                out.unresolved(
                        file.getId(),
                        e.getLine(),
                        UnresolvedKind.PARSE,
                        UnresolvedReason.PARSE_ERROR,
                        e.getMessage());
                continue;
            }
            JsModule module =
                    new JsModule(
                            file.getId(),
                            file.getPath(),
                            file.getContent() == null ? "" : file.getContent(),
                            tokens);
            readImports(module);
            JsDeclarations.read(module);
            linkImports(module, paths, out);
            modules.put(file.getId(), module);
        }
        return modules;
    }

    private static void linkImports(JsModule module, Map<String, Long> paths, StructureLinks out) {
        Map<Long, Integer> edgeLines = new TreeMap<>();
        Map<String, Integer> missing = new LinkedHashMap<>();
        for (JsImport imp : module.imports()) {
            Optional<String> base = resolve(module.path(), imp.specifier());
            if (base.isEmpty()) {
                continue;
            }
            Optional<Long> target =
                    SUFFIXES.stream()
                            .map(suffix -> paths.get(base.get() + suffix))
                            .filter(id -> id != null)
                            .findFirst();
            if (target.isPresent()) {
                imp.setTarget(target.get());
                edgeLines.merge(target.get(), imp.line(), Math::min);
            } else if (isCodePath(base.get())) {
                missing.merge(base.get(), imp.line(), Math::min);
            }
        }
        edgeLines.forEach((to, line) -> out.edge(module.fileId(), to, EdgeType.IMPORT, line));
        missing.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(
                        e ->
                                out.unresolved(
                                        module.fileId(),
                                        e.getValue(),
                                        UnresolvedKind.IMPORT,
                                        UnresolvedReason.NOT_SELECTED,
                                        e.getKey()));
    }

    private static boolean isCodePath(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return dot < 0
                || CODE_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /** 모듈 지정자를 저장소 기준 경로(확장자 후보 붙이기 전)로 푼다. 패키지 이름이거나 풀 수 없으면 empty. */
    static Optional<String> resolve(String fromPath, String specifier) {
        List<String> base;
        String rest;
        List<String> dir = new ArrayList<>(List.of(fromPath.split("/")));
        dir.remove(dir.size() - 1);
        if (specifier.startsWith("./") || specifier.startsWith("../")) {
            base = dir;
            rest = specifier;
        } else if (specifier.startsWith("@/")) {
            int src = dir.lastIndexOf("src");
            if (src < 0) {
                return Optional.empty();
            }
            base = new ArrayList<>(dir.subList(0, src + 1));
            rest = specifier.substring(2);
        } else {
            return Optional.empty();
        }
        Deque<String> parts = new ArrayDeque<>(base);
        for (String part : rest.split("/")) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                if (parts.isEmpty()) {
                    return Optional.empty();
                }
                parts.removeLast();
            } else {
                parts.addLast(part);
            }
        }
        return parts.isEmpty() ? Optional.empty() : Optional.of(String.join("/", parts));
    }

    /** import·export from·import()·require() 문을 찾는다. */
    static void readImports(JsModule module) {
        List<JsToken> t = module.tokens();
        for (int i = 0; i < t.size(); i++) {
            JsToken token = t.get(i);
            if (token.kind() != Kind.IDENT || JsTokens.isMemberName(t, i)) {
                continue;
            }
            switch (token.text()) {
                case "import" -> i = readImport(module, t, i);
                case "export" -> i = readExportFrom(module, t, i);
                case "require" -> {
                    if (JsTokens.is(t, i + 1, "(")
                            && JsTokens.kind(t, i + 2) == Kind.STRING
                            && JsTokens.is(t, i + 3, ")")) {
                        module.imports()
                                .add(new JsImport(t.get(i + 2).value(), token.line(), Map.of()));
                    }
                }
                default -> {}
            }
        }
    }

    private static int readImport(JsModule module, List<JsToken> t, int i) {
        int line = t.get(i).line();
        if (JsTokens.is(t, i + 1, "(")) {
            if (JsTokens.kind(t, i + 2) == Kind.STRING && JsTokens.is(t, i + 3, ")")) {
                module.imports().add(new JsImport(t.get(i + 2).value(), line, Map.of()));
            }
            return i;
        }
        if (JsTokens.is(t, i + 1, ".")) {
            return i;
        }
        if (JsTokens.kind(t, i + 1) == Kind.STRING) {
            module.imports().add(new JsImport(t.get(i + 1).value(), line, Map.of()));
            return i + 1;
        }
        Map<String, String> bindings = new LinkedHashMap<>();
        int j = i + 1;
        while (j < t.size() && !t.get(j).is("from") && !t.get(j).is(";")) {
            JsToken b = t.get(j);
            if (b.is("*") && JsTokens.is(t, j + 1, "as") && JsTokens.kind(t, j + 2) == Kind.IDENT) {
                bindings.put(t.get(j + 2).text(), "*");
                j += 3;
            } else if (b.is("{")) {
                j = readSpecifiers(t, j + 1, bindings);
            } else {
                if (b.kind() == Kind.IDENT) {
                    bindings.put(b.text(), "default");
                }
                j++;
            }
        }
        if (JsTokens.is(t, j, "from") && JsTokens.kind(t, j + 1) == Kind.STRING) {
            module.imports().add(new JsImport(t.get(j + 1).value(), line, bindings));
            return j + 1;
        }
        return i;
    }

    /**
     * {@code export * from}, {@code export {…} from}, {@code export {…}}(다시 내보내지 않는 목록은 exports로).
     */
    private static int readExportFrom(JsModule module, List<JsToken> t, int i) {
        int line = t.get(i).line();
        Map<String, String> names = new LinkedHashMap<>();
        int j;
        if (JsTokens.is(t, i + 1, "*")) {
            j = i + 2;
            if (JsTokens.is(t, j, "as")) {
                j += 2;
            }
        } else if (JsTokens.is(t, i + 1, "{")) {
            j = readSpecifiers(t, i + 2, names);
        } else {
            return i;
        }
        if (JsTokens.is(t, j, "from") && JsTokens.kind(t, j + 1) == Kind.STRING) {
            module.imports().add(new JsImport(t.get(j + 1).value(), line, Map.of()));
            return j + 1;
        }
        return i;
    }

    /**
     * {@code {a, b as c, default as d}} 안을 읽어 names에 넣는다. import면 지역 이름 → 가져온 이름, export면 내보낸 이름 →
     * 지역 이름. 닫는 중괄호 다음 위치를 돌려준다.
     */
    static int readSpecifiers(List<JsToken> t, int j, Map<String, String> names) {
        while (j < t.size() && !t.get(j).is("}")) {
            JsToken name = t.get(j);
            if (name.kind() == Kind.IDENT || name.kind() == Kind.STRING) {
                String original = name.value();
                String alias = original;
                if (JsTokens.is(t, j + 1, "as") && j + 2 < t.size()) {
                    alias = t.get(j + 2).value();
                    j += 2;
                }
                names.put(alias, original);
            }
            j++;
        }
        return j + 1;
    }
}
