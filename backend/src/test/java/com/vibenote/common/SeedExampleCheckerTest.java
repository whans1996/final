package com.vibenote.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.SeedExampleChecker.Block;
import com.vibenote.common.SeedExampleChecker.Card;
import com.vibenote.common.SeedExampleChecker.Form;
import com.vibenote.common.SeedExampleChecker.Issue;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** K-3a AC7·AC8: 시드 카드 예제 검사의 감싸기·진단 분류 (문자열 픽스처). 실제 카드 전체는 {@link SeedExampleTest}. */
class SeedExampleCheckerTest {

    static Connection conn;

    @BeforeAll
    static void connect() throws Exception {
        conn =
                DriverManager.getConnection(
                        IntegrationTestBase.MYSQL.getJdbcUrl(),
                        "root",
                        IntegrationTestBase.MYSQL.getPassword());
    }

    @AfterAll
    static void close() throws Exception {
        conn.close();
    }

    private static Card card(String lang, String... codes) {
        List<Block> blocks =
                java.util.stream.IntStream.range(0, codes.length)
                        .mapToObj(i -> new Block(lang, codes[i], 10 + i * 20))
                        .toList();
        return new Card("data/seed/concepts/java/x.md", blocks);
    }

    private static List<String> severities(List<Issue> issues) {
        return issues.stream().map(Issue::severity).sorted().toList();
    }

    private static Form formOf(String code) {
        for (Form f : Form.values()) {
            if (SeedExampleChecker.syntaxErrors(SeedExampleChecker.wrap(code, f, 1)).isEmpty()) {
                return f;
            }
        }
        return null;
    }

    @Test
    void AC7_조각은_그대로_클래스_본문_메서드_본문_순으로_감싸_본다() {
        assertThat(formOf("public class A { int x; }")).isEqualTo(Form.UNIT);
        assertThat(formOf("package com.example;\n\npublic record A(int x) {}"))
                .isEqualTo(Form.UNIT);
        // 최상위 메서드는 Java 21에서 컴파일 단위가 아니다 → 클래스 본문
        assertThat(formOf("class A {}\n\npublic int twice(int x) {\n    return x * 2;\n}"))
                .isEqualTo(Form.CLASS_BODY);
        assertThat(formOf("@Autowired MockMvc mockMvc;")).isEqualTo(Form.CLASS_BODY);
        assertThat(formOf("int a = 1;\nint twice() { return a * 2; }")).isEqualTo(Form.CLASS_BODY);
        assertThat(formOf("int a = 1;\nSystem.out.println(a);")).isEqualTo(Form.METHOD_BODY);
        assertThat(formOf("System.out.println(1);\nfor (int i = 0; i < 3; i++) {}"))
                .isEqualTo(Form.METHOD_BODY);
        assertThat(formOf("int a = ;")).isNull();
        // 생략 표기 { ... }·... 줄은 주석으로 본다 (가변 인자는 그대로)
        assertThat(formOf("@GetMapping(\"/a\")\npublic String a(@PathVariable long id) { ... }"))
                .isEqualTo(Form.CLASS_BODY);
        assertThat(formOf("try {\n    ...\n} catch (Exception e) {\n    throw e;\n}"))
                .isEqualTo(Form.METHOD_BODY);
        assertThat(formOf("void log(String... args) {}")).isEqualTo(Form.CLASS_BODY);
    }

    @Test
    void AC7_여러_파일을_이은_조각과_값을_돌려주는_문장_조각도_컴파일한다() {
        // 최상위 public 타입이 여럿이어도 파일 이름 규칙 오류가 나지 않는다
        assertThat(
                        SeedExampleChecker.checkJava(
                                card(
                                        "java",
                                        "public record A(int x) {}\n\npublic record B(int y) {}")))
                .isEmpty();
        assertThat(
                        SeedExampleChecker.checkJava(
                                card(
                                        "java",
                                        "Object o = 1;\nif (o instanceof Integer i) {\n"
                                                + "    return i + 1;\n}")))
                .isEmpty();
        // 상위 타입을 못 찾아서 이어진 @Override 오류는 경고 (상위 타입이 있으면 오류, 아래 AC7 테스트)
        assertThat(
                        severities(
                                SeedExampleChecker.checkJava(
                                        card(
                                                "java",
                                                "class I implements HandlerInterceptor {\n"
                                                        + "    @Override\n"
                                                        + "    public boolean preHandle() { return"
                                                        + " true; }\n}"))))
                .isNotEmpty()
                .containsOnly("warn");
    }

    @Test
    void AC7_셋_다_문법_오류면_오류() {
        List<Issue> issues = SeedExampleChecker.checkJava(card("java", "int a = ;"));
        assertThat(severities(issues)).containsExactly("error");
        assertThat(issues.get(0).message()).contains("문법 오류");
        assertThat(issues.get(0).line()).isEqualTo(10);
    }

    @Test
    void AC7_심볼_패키지를_못_찾으면_경고_그_밖의_진단은_오류() {
        // 자동 import (java.util·function·stream·time·concurrent)
        assertThat(
                        SeedExampleChecker.checkJava(
                                card(
                                        "java",
                                        "List<String> names = List.of(\"a\");\n"
                                                + "Map<String, Long> m = names.stream()"
                                                + ".collect(Collectors.groupingBy(s -> s,"
                                                + " Collectors.counting()));\n"
                                                + "LocalDate d = LocalDate.now();")))
                .isEmpty();
        // 맥락 없음: 모르는 타입·변수·패키지
        List<Issue> missing =
                SeedExampleChecker.checkJava(
                        card(
                                "java",
                                "import com.example.Foo;\n\nclass A { User u; }",
                                "userMapper.findById(1L);"));
        assertThat(severities(missing)).containsOnly("warn").hasSizeGreaterThanOrEqualTo(3);
        // Spring·MyBatis는 BE 테스트 클래스패스에 있다
        assertThat(
                        SeedExampleChecker.checkJava(
                                card(
                                        "java",
                                        "import org.springframework.stereotype.Service;\n"
                                                + "import org.apache.ibatis.annotations.Mapper;\n"
                                                + "@Service class S {}\n@Mapper interface M {}")))
                .isEmpty();
        // 타입 불일치·잘못된 override는 오류
        List<Issue> wrong =
                SeedExampleChecker.checkJava(
                        card(
                                "java",
                                "int n = \"문자열\";",
                                "class B { @Override public String toStrin() { return \"\"; } }"));
        assertThat(severities(wrong)).containsExactly("error", "error");
        // 줄 번호는 카드 파일 기준 (감싼 줄을 뺀다)
        List<Issue> line = SeedExampleChecker.checkJava(card("java", "int a = 1;\nString s = a;"));
        assertThat(line).singleElement().extracting(Issue::line).isEqualTo(11);
    }

    @Test
    void AC7_같은_카드의_java_블록은_함께_컴파일한다() {
        assertThat(
                        SeedExampleChecker.checkJava(
                                card(
                                        "java",
                                        "public record Point(int x, int y) {}",
                                        "Point p = new Point(1, 2);\nint sum = p.x() + p.y();")))
                .isEmpty();
    }

    @Test
    void AC8_sql은_빈_스키마에서_블록_순서대로_실행하고_문법_오류는_오류_테이블_컬럼_없음은_경고() throws Exception {
        assertThat(
                        SeedExampleChecker.checkSql(
                                card(
                                        "sql",
                                        "-- 테이블\nCREATE TABLE tags (id BIGINT PRIMARY KEY,"
                                                + " name VARCHAR(50));",
                                        "INSERT INTO tags VALUES (1, 'a;b');\n"
                                                + "SELECT name FROM tags WHERE id = 1;"),
                                conn,
                                "seed_check_t1"))
                .isEmpty();
        List<Issue> context =
                SeedExampleChecker.checkSql(
                        card(
                                "sql",
                                "SELECT * FROM posts;",
                                "CREATE TABLE t (id INT);\nSELECT nope FROM t;"),
                        conn,
                        "seed_check_t2");
        assertThat(severities(context)).containsExactly("warn", "warn");
        assertThat(context.get(1).line()).isEqualTo(31);
        List<Issue> syntax =
                SeedExampleChecker.checkSql(
                        card("sql", "SELEC * FROM t;", "SELECT 1;"), conn, "seed_check_t3");
        assertThat(syntax).singleElement().extracting(Issue::severity).isEqualTo("error");
        assertThat(syntax.get(0).message()).contains("1064");
    }

    @Test
    void AC8_xml은_well_formed_검사_매퍼_조각은_mapper로_감싼다() {
        assertThat(
                        SeedExampleChecker.checkXmlYaml(
                                card(
                                        "xml",
                                        "<!-- 조각 -->\n<select id=\"a\">SELECT 1</select>\n"
                                                + "<select id=\"b\">SELECT 2</select>",
                                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                            + "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper"
                                            + " 3.0//EN\""
                                            + " \"https://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n"
                                            + "<mapper namespace=\"a\"></mapper>")))
                .isEmpty();
        List<Issue> bad =
                SeedExampleChecker.checkXmlYaml(
                        card("xml", "<select id=\"a\">\n  WHERE a < b\n</select>"));
        assertThat(bad).singleElement().extracting(Issue::severity).isEqualTo("error");
        assertThat(bad.get(0).line()).isEqualTo(11);
    }

    @Test
    void AC8_yaml은_SnakeYAML로_파싱하고_실패하면_오류() {
        assertThat(
                        SeedExampleChecker.checkXmlYaml(
                                card(
                                        "yaml",
                                        "# 설정\nspring:\n  datasource:\n    url: x\n---\na: 1")))
                .isEmpty();
        List<Issue> bad = SeedExampleChecker.checkXmlYaml(card("yaml", "spring:\n  a: 1\n   b: 2"));
        assertThat(bad).singleElement().extracting(Issue::severity).isEqualTo("error");
    }

    @Test
    void 카드_파일의_example_섹션_코드_블록만_읽는다() {
        String text =
                "---\nname: a\n---\n\n## body\n\n```java\nint notExample;\n```\n\n## example\n\n"
                        + "```java\nint a = 1;\n```\n\n````xml\n<a>```</a>\n````\n";
        Card c = SeedExampleChecker.parse("x.md", text);
        assertThat(c.blocks())
                .containsExactly(
                        new Block("java", "int a = 1;", 14), new Block("xml", "<a>```</a>", 18));
    }
}
