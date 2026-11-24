package com.vibenote.analysis.structure.java;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.TypePatternExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeParameters;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.LocalRecordDeclarationStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.structure.EdgeType;
import com.vibenote.analysis.structure.StructureLinks;
import com.vibenote.analysis.structure.UnresolvedKind;
import com.vibenote.analysis.structure.UnresolvedReason;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 선별된 .java 파일 사이 타입 참조 (F-21a AC1~AC3).
 *
 * <p>JavaParser AST에서 타입 이름이 나오는 자리(타입 위치 전부, 어노테이션, 단일 타입·static import, 정적 호출·필드 접근·메서드 참조의 대상)를
 * 모아 Java 이름 규칙으로 푼다: 파일 안 선언 → 단일 타입 import → 같은 패키지 → 와일드카드 import. 소문자로 시작하는 점 이름은 FQN으로 보고 첫
 * 대문자 세그먼트까지를 타입 FQN으로 쓴다. 심볼 해석기는 쓰지 않으므로 import 없는 단순 이름이 선별되지 않은 타입이면 FQN을 몰라 남기지 않는다.
 */
public final class JavaStructureReader {

    static final int MAX_DETAIL = 200;

    private static final Pattern PACKAGE_LINE =
            Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);

    private JavaStructureReader() {}

    public static JavaTypeIndex read(List<AnalysisFile> javaFiles, StructureLinks out) {
        List<AnalysisFile> files =
                javaFiles.stream().sorted(Comparator.comparing(AnalysisFile::getId)).toList();
        List<Parsed> parsed = files.stream().map(JavaStructureReader::parse).toList();

        Map<String, Long> types = new HashMap<>();
        List<String> packages = new ArrayList<>();
        for (Parsed p : parsed) {
            packages.add(p.pkg());
            for (String fqn : p.declaredTypes()) {
                types.putIfAbsent(fqn, p.file().getId());
            }
        }
        JavaTypeIndex index = new JavaTypeIndex(Map.copyOf(types), rootPackage(packages));

        for (Parsed p : parsed) {
            if (p.unit() == null) {
                out.unresolved(
                        p.file().getId(),
                        p.errorLine(),
                        UnresolvedKind.PARSE,
                        UnresolvedReason.PARSE_ERROR,
                        p.error());
            } else {
                new FileReader(p, index).read(out);
            }
        }
        return index;
    }

    private static Parsed parse(AnalysisFile file) {
        String code = file.getContent() == null ? "" : file.getContent();
        String fileType = fileTypeName(file.getPath());
        ParserConfiguration config =
                new ParserConfiguration()
                        .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                        .setAttributeComments(false);
        ParseResult<CompilationUnit> result = new JavaParser(config).parse(code);
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            Matcher m = PACKAGE_LINE.matcher(code);
            String pkg = m.find() ? m.group(1) : "";
            Problem problem = result.getProblems().isEmpty() ? null : result.getProblems().get(0);
            String message = problem == null ? "Java 코드를 해석하지 못했습니다." : problem.getMessage();
            int line =
                    problem == null
                            ? 1
                            : problem.getLocation()
                                    .flatMap(range -> range.toRange())
                                    .map(range -> range.begin.line)
                                    .orElse(1);
            return new Parsed(
                    file,
                    null,
                    pkg,
                    Set.of(qualify(pkg, fileType)),
                    truncate(message.strip()),
                    line);
        }
        CompilationUnit unit = result.getResult().get();
        String pkg =
                unit.getPackageDeclaration().map(PackageDeclaration::getNameAsString).orElse("");
        Set<String> declared = new HashSet<>();
        declared.add(qualify(pkg, fileType));
        for (TypeDeclaration<?> type : unit.findAll(TypeDeclaration.class)) {
            type.getFullyQualifiedName().ifPresent(declared::add);
        }
        return new Parsed(file, unit, pkg, declared, null, 0);
    }

    /** 선별된 모든 Java 파일 패키지의 공통 접두. 2단 미만이면 빈 문자열 (AC2). */
    static String rootPackage(List<String> packages) {
        if (packages.isEmpty()) {
            return "";
        }
        String[] common = packages.get(0).split("\\.");
        int length = common.length;
        for (String pkg : packages) {
            String[] parts = pkg.split("\\.");
            int i = 0;
            while (i < length && i < parts.length && parts[i].equals(common[i])) {
                i++;
            }
            length = i;
        }
        return length < 2 ? "" : String.join(".", List.of(common).subList(0, length));
    }

    private static String fileTypeName(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        return name.endsWith(".java") ? name.substring(0, name.length() - 5) : name;
    }

    private static String qualify(String pkg, String simple) {
        return pkg.isEmpty() ? simple : pkg + "." + simple;
    }

    private static String truncate(String text) {
        return text.length() <= MAX_DETAIL ? text : text.substring(0, MAX_DETAIL);
    }

    private record Parsed(
            AnalysisFile file,
            CompilationUnit unit,
            String pkg,
            Set<String> declaredTypes,
            String error,
            int errorLine) {}

    /** 해석 결과. fileId가 null이면 선별 파일이 아니고, fqn이 null이면 FQN을 모른다. */
    private record Target(Long fileId, String fqn) {}

    /** 파일 하나의 이름 해석. 같은 대상은 가장 작은 줄 하나만 남긴다. */
    private static final class FileReader {

        private final Parsed parsed;
        private final JavaTypeIndex index;

        /** 변수 이름 → 그 변수가 보이는 범위 노드들 (메서드·람다·블록·for·try·타입 등). */
        private final Map<String, List<Node>> variableScopes = new HashMap<>();

        private final Map<String, String> singleImports = new HashMap<>();
        private final List<String> wildcardImports = new ArrayList<>();
        private final Map<Long, Integer> edgeLines = new TreeMap<>();
        private final Map<String, Integer> missingLines = new LinkedHashMap<>();

        FileReader(Parsed parsed, JavaTypeIndex index) {
            this.parsed = parsed;
            this.index = index;
        }

        void read(StructureLinks out) {
            CompilationUnit unit = parsed.unit();
            unit.findAll(VariableDeclarator.class)
                    .forEach(v -> variable(v.getNameAsString(), declaratorScope(v)));
            unit.findAll(Parameter.class)
                    .forEach(p -> variable(p.getNameAsString(), p.getParentNode().orElse(null)));
            unit.findAll(TypePatternExpr.class)
                    .forEach(
                            p ->
                                    variable(
                                            p.getNameAsString(),
                                            p.findAncestor(BlockStmt.class)
                                                    .map(Node.class::cast)
                                                    .orElse(p.getParentNode().orElse(null))));

            for (ImportDeclaration imp : unit.getImports()) {
                String name = imp.getNameAsString();
                if (imp.isAsterisk() && !imp.isStatic()) {
                    wildcardImports.add(name);
                    continue;
                }
                if (!imp.isStatic()) {
                    singleImports.put(name.substring(name.lastIndexOf('.') + 1), name);
                }
                // static import의 마지막 세그먼트(멤버)는 첫 대문자 세그먼트 규칙이 떼어 낸다
                reference(name, imp);
            }

            for (ClassOrInterfaceType type : unit.findAll(ClassOrInterfaceType.class)) {
                boolean isScopeOfOuter =
                        type.getParentNode()
                                .filter(ClassOrInterfaceType.class::isInstance)
                                .map(ClassOrInterfaceType.class::cast)
                                .flatMap(ClassOrInterfaceType::getScope)
                                .filter(scope -> scope == type)
                                .isPresent();
                if (!isScopeOfOuter) {
                    reference(type.getNameWithScope(), type);
                }
            }
            for (AnnotationExpr annotation : unit.findAll(AnnotationExpr.class)) {
                reference(annotation.getNameAsString(), annotation);
            }
            unit.findAll(MethodCallExpr.class).forEach(c -> c.getScope().ifPresent(this::scope));
            unit.findAll(FieldAccessExpr.class).forEach(f -> scope(f.getScope()));
            unit.findAll(MethodReferenceExpr.class).forEach(r -> scope(r.getScope()));

            long self = parsed.file().getId();
            edgeLines.forEach((to, line) -> out.edge(self, to, EdgeType.IMPORT, line));
            missingLines.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .forEach(
                            e ->
                                    out.unresolved(
                                            self,
                                            e.getValue(),
                                            UnresolvedKind.IMPORT,
                                            UnresolvedReason.NOT_SELECTED,
                                            e.getKey()));
        }

        /** 정적 호출·필드 접근·메서드 참조의 대상. 이름으로만 된 식이고 첫 이름이 변수가 아닐 때만 타입 이름으로 본다. */
        private void scope(Expression expr) {
            dottedName(expr)
                    .filter(name -> !variableAt(name.split("\\.")[0], expr))
                    .ifPresent(name -> reference(name, expr));
        }

        private void variable(String name, Node scope) {
            if (scope != null) {
                variableScopes.computeIfAbsent(name, k -> new ArrayList<>()).add(scope);
            }
        }

        /** 필드는 그 타입 전체, 지역 변수는 선언을 담은 블록·for·try가 범위다. */
        private static Node declaratorScope(VariableDeclarator v) {
            Node node = v.getParentNode().orElse(null);
            if (node instanceof FieldDeclaration field) {
                return field.getParentNode().orElse(null);
            }
            while (node != null
                    && (node instanceof VariableDeclarationExpr
                            || node instanceof ExpressionStmt)) {
                node = node.getParentNode().orElse(null);
            }
            return node;
        }

        /** at에서 name 변수가 보이는지 (그 변수의 범위 노드가 at의 조상인지). */
        private boolean variableAt(String name, Node at) {
            List<Node> scopes = variableScopes.get(name);
            if (scopes == null) {
                return false;
            }
            for (Node node = at; node != null; node = node.getParentNode().orElse(null)) {
                for (Node scope : scopes) {
                    if (scope == node) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void reference(String name, Node at) {
            Target target = resolve(name, at);
            if (target == null) {
                return;
            }
            int line = at.getBegin().map(p -> p.line).orElse(1);
            if (target.fileId() != null) {
                edgeLines.merge(target.fileId(), line, Math::min);
            } else if (target.fqn() != null && index.inProject(target.fqn())) {
                missingLines.merge(target.fqn(), line, Math::min);
            }
        }

        private Target resolve(String name, Node at) {
            String[] segments = name.split("\\.");
            if (segments.length > 1 && Character.isLowerCase(segments[0].charAt(0))) {
                for (int k = 0; k < segments.length; k++) {
                    if (Character.isUpperCase(segments[k].charAt(0))) {
                        String fqn = String.join(".", List.of(segments).subList(0, k + 1));
                        return new Target(index.fileOf(fqn).orElse(null), fqn);
                    }
                }
                return null;
            }
            String simple = segments[0];
            if (declaredAround(simple, at)) {
                return new Target(parsed.file().getId(), null);
            }
            String imported = singleImports.get(simple);
            if (imported != null) {
                return new Target(index.fileOf(imported).orElse(null), imported);
            }
            Optional<Long> samePackage = index.fileOf(qualify(parsed.pkg(), simple));
            if (samePackage.isPresent()) {
                return new Target(samePackage.get(), null);
            }
            for (String pkg : wildcardImports) {
                Optional<Long> found = index.fileOf(pkg + "." + simple);
                if (found.isPresent()) {
                    return new Target(found.get(), null);
                }
            }
            return null;
        }

        /**
         * at에서 보이는 이 파일 안의 타입 이름인지: 바깥쪽으로 올라가며 둘러싼 타입 자신과 그 멤버 타입, 타입 매개변수, 블록 안 지역 클래스, 최상위 타입을
         * 본다. 중첩 타입 이름은 그 타입 범위 밖에서는 같은 패키지의 다른 파일을 가리킬 수 있다.
         */
        private static boolean declaredAround(String simple, Node at) {
            for (Node node = at; node != null; node = node.getParentNode().orElse(null)) {
                if (node instanceof TypeDeclaration<?> type) {
                    if (type.getNameAsString().equals(simple)
                            || type.getMembers().stream()
                                    .anyMatch(
                                            m ->
                                                    m instanceof TypeDeclaration<?> inner
                                                            && inner.getNameAsString()
                                                                    .equals(simple))) {
                        return true;
                    }
                }
                if (node instanceof NodeWithTypeParameters<?> generic
                        && generic.getTypeParameters().stream()
                                .anyMatch(p -> p.getNameAsString().equals(simple))) {
                    return true;
                }
                if (node instanceof BlockStmt block
                        && block.getStatements().stream()
                                .anyMatch(stmt -> localTypeName(stmt).equals(simple))) {
                    return true;
                }
                if (node instanceof CompilationUnit cu
                        && cu.getTypes().stream()
                                .anyMatch(type -> type.getNameAsString().equals(simple))) {
                    return true;
                }
            }
            return false;
        }

        private static String localTypeName(Statement stmt) {
            if (stmt instanceof LocalClassDeclarationStmt local) {
                return local.getClassDeclaration().getNameAsString();
            }
            if (stmt instanceof LocalRecordDeclarationStmt local) {
                return local.getRecordDeclaration().getNameAsString();
            }
            return "";
        }

        private static Optional<String> dottedName(Expression expr) {
            if (expr instanceof NameExpr name) {
                return Optional.of(name.getNameAsString());
            }
            if (expr instanceof FieldAccessExpr access) {
                return dottedName(access.getScope()).map(s -> s + "." + access.getNameAsString());
            }
            return Optional.empty();
        }
    }
}
