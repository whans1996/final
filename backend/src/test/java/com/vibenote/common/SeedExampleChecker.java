package com.vibenote.common;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * 시드 카드 ②예제 실행 검사 BE 쪽 (K-3a AC7·AC8). java는 javac로 컴파일, sql은 MySQL 빈 스키마에서 실행, xml은 well-formed,
 * yaml은 SnakeYAML 파싱. FE 언어(js·ts·vue·json)는 {@code scripts/lib/seed/examples.mjs}가 맡는다.
 *
 * <p>심각도: 맥락이 없어서 나는 진단(심볼·패키지·테이블·컬럼 없음)은 {@code warn}, 그 밖은 {@code error}(CI 실패).
 */
final class SeedExampleChecker {

    static final String STAGE = "②";

    record Block(String lang, String code, int line) {}

    record Card(String file, List<Block> blocks) {}

    record Issue(
            String file, String stage, String severity, String item, int line, String message) {}

    /** java 조각을 감싸는 형태. 문법 오류가 없는 첫 형태를 쓴다 */
    enum Form {
        UNIT,
        CLASS_BODY,
        METHOD_BODY
    }

    static final String IMPORTS =
            "import java.util.*; import java.util.function.*; import java.util.stream.*;"
                    + " import java.time.*; import java.util.concurrent.*;";

    private static final Pattern SECTION = Pattern.compile("^## ([a-z][a-z0-9-]*)\\s*$");
    private static final Pattern FENCE = Pattern.compile("^ {0,3}(`{3,}|~{3,})(.*)$");
    private static final Pattern PACKAGE =
            Pattern.compile("^\\s*package\\s+[\\w.]+\\s*;", Pattern.MULTILINE);
    private static final Pattern PUBLIC_TYPE =
            Pattern.compile(
                    "^public\\s+(?:(?:final|abstract|sealed|non-sealed|static|strictfp)\\s+)*"
                            + "(?:class|interface|enum|record|@interface)\\s+(\\w+)",
                    Pattern.MULTILINE);

    /** 맥락 없음(심볼·패키지를 못 찾음) → 경고 */
    private static final Pattern JAVA_CONTEXT =
            Pattern.compile("compiler\\.err\\.(cant\\.resolve.*|doesnt\\.exist)");

    /** 테이블·컬럼 없음 → 경고 */
    private static final List<Integer> SQL_CONTEXT = List.of(1146, 1054);

    private SeedExampleChecker() {}

    // ───────────── 카드 읽기 (형식은 scripts/lib/seed/card.mjs와 같다. 형식 검사는 거기서 한다) ─────────────

    /** {@code src} 아래 카드 파일을 경로순으로 읽는다. file은 {@code root} 기준 상대 경로(/ 구분) */
    static List<Card> readCards(Path root, Path src) throws IOException {
        if (!Files.isDirectory(src)) return List.of();
        List<Path> files;
        try (Stream<Path> s = Files.walk(src)) {
            files =
                    s.filter(p -> p.toString().endsWith(".md"))
                            .sorted((a, b) -> relative(root, a).compareTo(relative(root, b)))
                            .toList();
        }
        List<Card> cards = new ArrayList<>();
        for (Path p : files) {
            cards.add(parse(relative(root, p), Files.readString(p, StandardCharsets.UTF_8)));
        }
        return cards;
    }

    private static String relative(Path root, Path p) {
        return root.toAbsolutePath()
                .normalize()
                .relativize(p.toAbsolutePath().normalize())
                .toString()
                .replace('\\', '/');
    }

    /** example 섹션의 코드 블록만 읽는다 (line은 파일 기준 1부터, 블록 첫 코드 줄) */
    static Card parse(String file, String text) {
        String[] lines = text.replace("\r\n", "\n").split("\n", -1);
        List<Block> blocks = new ArrayList<>();
        int i = 0;
        if (lines.length > 0 && lines[0].equals("---")) {
            for (i = 1; i < lines.length && !lines[i].equals("---"); i++) {}
            i++;
        }
        String section = null;
        String fence = null;
        String lang = null;
        int start = 0;
        StringBuilder code = new StringBuilder();
        for (; i < lines.length; i++) {
            String line = lines[i];
            if (fence != null) {
                char fenceChar = fence.charAt(0);
                if (line.strip().startsWith(fence)
                        && line.strip().chars().allMatch(c -> c == fenceChar)) {
                    if ("example".equals(section)) {
                        String s = code.toString();
                        blocks.add(new Block(lang, s.isEmpty() ? s : s.substring(1), start));
                    }
                    fence = null;
                } else {
                    code.append('\n').append(line);
                }
                continue;
            }
            Matcher f = FENCE.matcher(line);
            if (f.matches() && !(f.group(1).charAt(0) == '`' && f.group(2).contains("`"))) {
                fence = f.group(1);
                String info = f.group(2).strip();
                lang = info.isEmpty() ? "" : info.split("\\s+")[0];
                start = i + 2;
                code.setLength(0);
                continue;
            }
            Matcher m = SECTION.matcher(line);
            if (m.matches()) section = m.group(1);
        }
        return new Card(file, blocks);
    }

    // ───────────── 전체 ─────────────

    record Result(List<Issue> issues, Map<String, Integer> checked) {}

    /** 카드들의 java·sql·xml·yaml 블록을 검사한다. sql은 {@code root} 권한 연결이 필요하다(카드마다 스키마를 만든다) */
    static Result checkAll(List<Card> cards, Connection conn) throws SQLException {
        List<Issue> issues = new ArrayList<>();
        Map<String, Integer> checked = new TreeMap<>();
        int n = 0;
        for (Card card : cards) {
            n++;
            for (Block b : card.blocks()) {
                if (List.of("java", "sql", "xml", "yaml").contains(b.lang())) {
                    checked.merge(b.lang(), 1, Integer::sum);
                }
            }
            issues.addAll(checkJava(card));
            issues.addAll(checkSql(card, conn, "seed_check_" + n));
            issues.addAll(checkXmlYaml(card));
        }
        return new Result(issues, checked);
    }

    // ───────────── java (AC7) ─────────────

    /** 감싼 소스. prefixLines는 원래 조각 앞에 붙은 줄 수 */
    record Wrapped(Form form, String name, String source, int prefixLines) {}

    /** 생략 표기 {@code { ... }}·{@code ...} 줄 (가변 인자 {@code String... args}는 앞이 이름이라 제외) */
    private static final Pattern ELISION = Pattern.compile("(?<![\\w\\]>)])\\.\\.\\.(?!\\w)");

    /**
     * 본문 전체를 생략한 {@code { ... }} (한 줄 안). 흐름 분석이 missing return으로 보지 않게 {@code throw null;}로 바꾼다
     */
    private static final Pattern ELIDED_BODY = Pattern.compile("\\{[ \\t]*\\.\\.\\.[ \\t]*\\}");

    /** 값을 돌려주는 return (메서드 본문 조각이면 반환형을 Object로 감싼다) */
    private static final Pattern RETURN_VALUE = Pattern.compile("\\breturn\\s+[^;\\s]");

    /**
     * 조각을 컴파일할 수 있게 감싼다. 줄 번호를 지키려고 앞에 붙이는 것은 모두 첫 줄 하나에 둔다.
     *
     * <ul>
     *   <li>생략 표기: 본문 전체 {@code { ... }}는 {@code { throw null; }}, 그 밖의 {@code ...}는 공백으로 바꾼다 (본문을
     *       줄인 예제의 관례. 주석으로 바꾸면 블록 주석 안에서 겹친다)
     *   <li>그대로: 여러 파일을 이어 붙인 조각이 많아 최상위 타입의 {@code public}을 지운다 (파일 이름 규칙 오류 방지)
     *   <li>메서드 본문: 값을 돌려주는 return이 있으면 {@code Object run() { if (true) { 조각 } return null; }}
     * </ul>
     */
    static Wrapped wrap(String original, Form form, int n) {
        String code =
                ELISION.matcher(ELIDED_BODY.matcher(original).replaceAll("{ throw null; }"))
                        .replaceAll("   ");
        String cls = "Snippet" + n;
        return switch (form) {
            case UNIT -> {
                code =
                        PUBLIC_TYPE
                                .matcher(code)
                                .replaceAll(m -> Matcher.quoteReplacement(m.group().substring(7)));
                Matcher pkg = PACKAGE.matcher(code);
                // package 문이 있으면 그 줄 끝에 import를 붙인다
                yield pkg.find()
                        ? new Wrapped(
                                form,
                                cls,
                                code.substring(0, pkg.end())
                                        + " "
                                        + IMPORTS
                                        + code.substring(pkg.end()),
                                0)
                        : new Wrapped(form, cls, IMPORTS + "\n" + code, 1);
            }
            case CLASS_BODY ->
                    new Wrapped(form, cls, IMPORTS + " class " + cls + " {\n" + code + "\n}\n", 1);
            case METHOD_BODY -> {
                boolean value = RETURN_VALUE.matcher(code).find();
                String open =
                        value
                                ? " { Object run() throws Exception { if (true) {\n"
                                : " { void run() throws Exception {\n";
                String close = value ? "\n} return null; }}\n" : "\n}}\n";
                yield new Wrapped(form, cls, IMPORTS + " class " + cls + open + code + close, 1);
            }
        };
    }

    private static JavaFileObject source(String name, String text) {
        return new SimpleJavaFileObject(
                URI.create("string:///" + name + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return text;
            }
        };
    }

    private static final JavaCompiler JAVAC = ToolProvider.getSystemJavaCompiler();

    private static List<String> options() {
        return List.of(
                "-proc:none",
                "-Xlint:none",
                "-implicit:none",
                // 기본값은 속성 단계(ATTR)에 오류가 있으면 흐름 분석을 건너뛴다. 심볼 없음(경고)만 있어도 missing return·
                // checked 예외 같은 오류가 사라지므로 흐름 분석까지 간다
                "-XDshould-stop.ifError=FLOW",
                "-classpath",
                System.getProperty("java.class.path"));
    }

    /** 문법(파싱) 단계 오류. 없으면 빈 목록 */
    static List<Diagnostic<? extends JavaFileObject>> syntaxErrors(Wrapped w) {
        DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
        JavacTask task =
                (JavacTask)
                        JAVAC.getTask(
                                null,
                                null,
                                diags,
                                options(),
                                null,
                                List.of(source(w.name(), w.source())));
        try {
            task.parse();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return diags.getDiagnostics().stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                .toList();
    }

    static List<Issue> checkJava(Card card) {
        List<Issue> issues = new ArrayList<>();
        List<Block> blocks = card.blocks().stream().filter(b -> b.lang().equals("java")).toList();
        if (blocks.isEmpty()) return issues;
        Map<JavaFileObject, Map.Entry<Block, Wrapped>> units = new LinkedHashMap<>();
        int n = 0;
        for (Block b : blocks) {
            n++;
            Wrapped chosen = null;
            Diagnostic<? extends JavaFileObject> furthest = null;
            int furthestLine = -1;
            for (Form form : Form.values()) {
                Wrapped w = wrap(b.code(), form, n);
                List<Diagnostic<? extends JavaFileObject>> errs = syntaxErrors(w);
                if (errs.isEmpty()) {
                    chosen = w;
                    break;
                }
                // 셋 다 실패하면 가장 멀리 읽은 형태의 오류를 알린다 (조각에 가장 가까운 해석)
                int line = (int) errs.get(0).getLineNumber() - w.prefixLines();
                if (line > furthestLine) {
                    furthestLine = line;
                    furthest = errs.get(0);
                }
            }
            if (chosen == null) {
                issues.add(
                        issue(
                                card,
                                "error",
                                "java",
                                b.line() + Math.max(furthestLine, 1) - 1,
                                "문법 오류 (그대로·클래스 본문·메서드 본문 모두): "
                                        + furthest.getMessage(Locale.ROOT)));
                continue;
            }
            units.put(source(chosen.name(), chosen.source()), Map.entry(b, chosen));
        }
        if (units.isEmpty()) return issues;

        DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
        List<ClassSpan> classes = new ArrayList<>();
        try (StandardJavaFileManager fm =
                JAVAC.getStandardFileManager(diags, Locale.ROOT, StandardCharsets.UTF_8)) {
            JavacTask task =
                    (JavacTask) JAVAC.getTask(null, fm, diags, options(), null, units.keySet());
            Iterable<? extends CompilationUnitTree> trees = task.parse();
            task.analyze();
            classes = classSpans(task, trees);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
            if (d.getKind() != Diagnostic.Kind.ERROR) continue;
            Map.Entry<Block, Wrapped> unit = units.get(d.getSource());
            int line = 1;
            if (unit != null) {
                line =
                        unit.getKey().line()
                                + Math.max(
                                        (int) d.getLineNumber() - unit.getValue().prefixLines(), 1)
                                - 1;
            }
            // 맥락 없음에서 이어진 진단 → 경고: 상위 타입을 못 찾은 클래스 안의 @Override 오류, 생략한 생성자 때문에 초기화되지
            // 않은 final 필드, 타입을 못 찾은 예외(<any>)의 처리 누락
            String code = d.getCode();
            boolean contextual =
                    JAVA_CONTEXT.matcher(code).matches()
                            || (code.equals("compiler.err.method.does.not.override.superclass")
                                    && unresolvedSuper(classes, d))
                            || code.equals(
                                    "compiler.err.var.not.initialized.in.default.constructor")
                            || (code.equals(
                                            "compiler.err.unreported.exception.need.to.catch.or.throw")
                                    && d.getMessage(Locale.ROOT).contains("<any>"));
            issues.add(
                    issue(
                            card,
                            contextual ? "warn" : "error",
                            "java",
                            line,
                            d.getCode().replace("compiler.err.", "")
                                    + ": "
                                    + d.getMessage(Locale.ROOT).replace('\n', ' ')));
        }
        return issues;
    }

    /** 클래스(익명 포함) 하나의 소스 범위와, 상위 클래스·인터페이스 중 해석되지 않은 것이 있는지 */
    record ClassSpan(JavaFileObject file, long start, long end, boolean unresolvedSuper) {}

    private static List<ClassSpan> classSpans(
            JavacTask task, Iterable<? extends CompilationUnitTree> units) {
        Trees trees = Trees.instance(task);
        SourcePositions pos = trees.getSourcePositions();
        List<ClassSpan> spans = new ArrayList<>();
        for (CompilationUnitTree unit : units) {
            new TreePathScanner<Void, Void>() {
                @Override
                public Void visitClass(ClassTree node, Void v) {
                    Element e = trees.getElement(getCurrentPath());
                    boolean unresolved = false;
                    if (e instanceof TypeElement t) {
                        unresolved = t.getSuperclass().getKind() == TypeKind.ERROR;
                        for (TypeMirror i : t.getInterfaces()) {
                            unresolved |= i.getKind() == TypeKind.ERROR;
                        }
                    }
                    spans.add(
                            new ClassSpan(
                                    unit.getSourceFile(),
                                    pos.getStartPosition(unit, node),
                                    pos.getEndPosition(unit, node),
                                    unresolved));
                    return super.visitClass(node, v);
                }
            }.scan(unit, null);
        }
        return spans;
    }

    /** 진단 위치를 감싸는 가장 안쪽 클래스의 상위 타입이 해석되지 않았는가 */
    private static boolean unresolvedSuper(
            List<ClassSpan> classes, Diagnostic<? extends JavaFileObject> d) {
        return classes.stream()
                .filter(
                        c ->
                                d.getSource() != null
                                        && c.file().toUri().equals(d.getSource().toUri())
                                        && c.start() <= d.getPosition()
                                        && d.getPosition() < c.end())
                .min(Comparator.comparingLong(c -> c.end() - c.start()))
                .map(ClassSpan::unresolvedSuper)
                .orElse(false);
    }

    // ───────────── sql (AC8) ─────────────

    record SqlStatement(String sql, int line) {}

    /** 세미콜론으로 나눈다 (따옴표·주석 안은 제외). 내용이 주석뿐인 문장은 버린다 */
    static List<SqlStatement> splitSql(String code) {
        List<SqlStatement> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean meaningful = false;
        int line = 1;
        int startLine = 1;
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (!meaningful && !Character.isWhitespace(c)) startLine = line;
            if ((c == '-' && code.startsWith("-- ", i)) || c == '#') {
                int end = code.indexOf('\n', i);
                end = end < 0 ? code.length() : end;
                cur.append(code, i, end);
                i = end - 1;
                continue;
            }
            if (c == '/' && code.startsWith("/*", i)) {
                int end = code.indexOf("*/", i + 2);
                end = end < 0 ? code.length() : end + 2;
                String part = code.substring(i, end);
                line += (int) part.chars().filter(ch -> ch == '\n').count();
                cur.append(part);
                i = end - 1;
                continue;
            }
            if (c == '\'' || c == '"' || c == '`') {
                int j = i + 1;
                while (j < code.length() && code.charAt(j) != c) {
                    if (code.charAt(j) == '\\' && c != '`') j++;
                    j++;
                }
                String part = code.substring(i, Math.min(j + 1, code.length()));
                line += (int) part.chars().filter(ch -> ch == '\n').count();
                cur.append(part);
                meaningful = true;
                i = j;
                continue;
            }
            if (c == ';') {
                if (meaningful) out.add(new SqlStatement(cur.toString().strip(), startLine));
                cur.setLength(0);
                meaningful = false;
                continue;
            }
            if (c == '\n') line++;
            if (!Character.isWhitespace(c)) meaningful = true;
            cur.append(c);
        }
        if (meaningful) out.add(new SqlStatement(cur.toString().strip(), startLine));
        return out;
    }

    static List<Issue> checkSql(Card card, Connection conn, String schema) throws SQLException {
        List<Issue> issues = new ArrayList<>();
        List<Block> blocks = card.blocks().stream().filter(b -> b.lang().equals("sql")).toList();
        if (blocks.isEmpty()) return issues;
        try (Statement st = conn.createStatement()) {
            st.execute("DROP DATABASE IF EXISTS " + schema);
            st.execute("CREATE DATABASE " + schema);
            st.execute("USE " + schema);
            outer:
            for (Block b : blocks) {
                for (SqlStatement s : splitSql(b.code())) {
                    try {
                        st.execute(s.sql());
                    } catch (SQLException e) {
                        boolean contextual = SQL_CONTEXT.contains(e.getErrorCode());
                        issues.add(
                                issue(
                                        card,
                                        contextual ? "warn" : "error",
                                        "sql",
                                        b.line() + s.line() - 1,
                                        "MySQL " + e.getErrorCode() + ": " + e.getMessage()));
                        if (!contextual) break outer; // 뒤 문장은 대개 연쇄 실패
                    }
                }
            }
        } finally {
            try (Statement st = conn.createStatement()) {
                st.execute("DROP DATABASE IF EXISTS " + schema);
            }
        }
        return issues;
    }

    // ───────────── xml·yaml (AC8) ─────────────

    static List<Issue> checkXmlYaml(Card card) {
        List<Issue> issues = new ArrayList<>();
        for (Block b : card.blocks()) {
            if (b.lang().equals("xml")) {
                String err = xmlError(b.code());
                if (err != null) {
                    String[] parts = err.split("\t", 2);
                    issues.add(
                            issue(
                                    card,
                                    "error",
                                    "xml",
                                    b.line() + Integer.parseInt(parts[0]) - 1,
                                    parts[1]));
                }
            } else if (b.lang().equals("yaml")) {
                try {
                    new Yaml(new SafeConstructor(new LoaderOptions()))
                            .loadAll(b.code())
                            .forEach(doc -> {});
                } catch (MarkedYAMLException e) {
                    int line = e.getProblemMark() == null ? 1 : e.getProblemMark().getLine() + 1;
                    issues.add(
                            issue(
                                    card,
                                    "error",
                                    "yaml",
                                    b.line() + line - 1,
                                    "YAML: " + e.getMessage().replace('\n', ' ')));
                } catch (YAMLException e) {
                    issues.add(issue(card, "error", "yaml", b.line(), "YAML: " + e.getMessage()));
                }
            }
        }
        return issues;
    }

    /**
     * well-formed가 아니면 "줄\t메시지". 선언·DOCTYPE이 없으면 {@code <mapper>}로 감싼다 (MyBatis 매퍼 조각, 같은 줄이라 줄 번호는
     * 그대로). 외부 DTD는 읽지 않는다.
     */
    static String xmlError(String code) {
        String trimmed = code.stripLeading();
        boolean whole = trimmed.startsWith("<?xml") || trimmed.startsWith("<!DOCTYPE");
        String text = whole ? code : "<mapper>" + code + "\n</mapper>";
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            DocumentBuilder builder = f.newDocumentBuilder();
            builder.setErrorHandler(
                    new ErrorHandler() {
                        @Override
                        public void warning(SAXParseException e) {}

                        @Override
                        public void error(SAXParseException e) throws SAXParseException {
                            throw e;
                        }

                        @Override
                        public void fatalError(SAXParseException e) throws SAXParseException {
                            throw e;
                        }
                    });
            builder.parse(new InputSource(new StringReader(text)));
            return null;
        } catch (SAXParseException e) {
            return Math.max(e.getLineNumber(), 1) + "\tXML: " + e.getMessage();
        } catch (Exception e) {
            return "1\tXML: " + e.getMessage();
        }
    }

    private static Issue issue(Card card, String severity, String item, int line, String message) {
        return new Issue(card.file(), STAGE, severity, item, line, message);
    }
}
