package com.vibenote.analysis.structure.js;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.vibenote.analysis.structure.WorkBudget;
import com.vibenote.analysis.structure.js.JsToken.Kind;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** F-21a AC5 JS 토크나이저 (docs/specs/F-21a-structure-graph.md). */
class JsTokenizerTest {

    private static List<String> texts(List<JsToken> tokens) {
        return tokens.stream().map(JsToken::text).toList();
    }

    @Test
    void AC5_주석과_문자열_안의_import와_HTTP_호출은_코드_토큰이_아니다() {
        List<JsToken> tokens =
                JsTokenizer.tokenize(
                        """
                        // import x from 'a'
                        /* axios.get('/x') */
                        const s = "import y from 'b'";
                        const t = 'it\\'s';
                        """);

        assertThat(texts(tokens))
                .containsExactly(
                        "const",
                        "s",
                        "=",
                        "\"import y from 'b'\"",
                        ";",
                        "const",
                        "t",
                        "=",
                        "'it\\'s'",
                        ";");
        assertThat(tokens.get(3).kind()).isEqualTo(Kind.STRING);
        assertThat(tokens.get(3).value()).isEqualTo("import y from 'b'");
        assertThat(tokens.get(8).value()).isEqualTo("it's");
        assertThat(tokens.get(0).line()).isEqualTo(3);
        assertThat(tokens.get(5).line()).isEqualTo(4);
    }

    @Test
    void AC5_템플릿_리터럴은_중첩된_표현식을_토큰으로_가진다() {
        List<JsToken> tokens = JsTokenizer.tokenize("const u = `a${b + `c${d}`}e`;");

        JsToken template = tokens.get(3);
        assertThat(template.kind()).isEqualTo(Kind.TEMPLATE);
        assertThat(template.quasis()).containsExactly("a", "e");
        assertThat(template.parts()).hasSize(1);
        List<JsToken> expr = template.parts().get(0);
        assertThat(texts(expr.subList(0, 2))).containsExactly("b", "+");
        JsToken inner = expr.get(2);
        assertThat(inner.kind()).isEqualTo(Kind.TEMPLATE);
        assertThat(inner.quasis()).containsExactly("c", "");
        assertThat(texts(inner.parts().get(0))).containsExactly("d");
        assertThat(texts(tokens.subList(4, tokens.size()))).containsExactly(";");
    }

    @Test
    void AC5_정규식_리터럴과_나눗셈을_구분한다() {
        List<JsToken> tokens =
                JsTokenizer.tokenize(
                        """
                        const r = /ab+c\\/[/]'/gi;
                        const x = a / b / (c) / 2;
                        if (ok) { return /x/.test(s) }
                        """);

        assertThat(tokens.get(3).kind()).isEqualTo(Kind.REGEX);
        assertThat(tokens.get(3).text()).isEqualTo("/ab+c\\/[/]'/gi");
        assertThat(texts(tokens.subList(5, 17)))
                .containsExactly("const", "x", "=", "a", "/", "b", "/", "(", "c", ")", "/", "2");
        assertThat(tokens.stream().filter(t -> t.kind() == Kind.REGEX).map(JsToken::text))
                .containsExactly("/ab+c\\/[/]'/gi", "/x/");
    }

    @Test
    void AC5_여러_글자_연산자를_하나의_토큰으로_읽는다() {
        assertThat(texts(JsTokenizer.tokenize("a === b => c?.d ... e != f")))
                .containsExactly("a", "===", "b", "=>", "c", "?.", "d", "...", "e", "!=", "f");
    }

    @Test
    void AC5_vue는_script_블록만_원본_줄_번호로_읽는다() {
        List<JsToken> tokens =
                JsTokenizer.tokenizeFile(
                        "src/views/TodoView.vue",
                        """
                        <template>
                          <div @click="axios.get('/x')">{{ 'import' }}</div>
                        </template>
                        <script setup>
                        import api from './api'
                        </script>
                        <style>.a { color: red }</style>
                        """,
                        WorkBudget.unlimited(),
                        Integer.MAX_VALUE);

        assertThat(texts(tokens)).containsExactly("import", "api", "from", "'./api'");
        assertThat(tokens.get(0).line()).isEqualTo(5);
    }

    @Test
    void AC5_끝나지_않은_문자열_주석_템플릿은_줄_번호와_함께_오류() {
        assertThatThrownBy(() -> JsTokenizer.tokenize("const a = 1;\nconst s = 'abc\nfoo"))
                .isInstanceOf(JsSyntaxException.class)
                .extracting("line")
                .isEqualTo(2);
        assertThatThrownBy(() -> JsTokenizer.tokenize("/* never closed"))
                .isInstanceOf(JsSyntaxException.class);
        assertThatThrownBy(() -> JsTokenizer.tokenize("const t = `a${b}"))
                .isInstanceOf(JsSyntaxException.class);
    }

    @Test
    void AC5_끝나지_않는_정규식_후보가_한_줄에_반복되면_제한_시간_안에_PARSE_ERROR다() {
        String code = "x" + "=/[".repeat(100_000);

        assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () ->
                        assertThatThrownBy(() -> JsTokenizer.tokenize(code))
                                .isInstanceOf(JsSyntaxException.class));
    }

    @Test
    void AC5_닫히지_않은_유니코드_이스케이프가_반복돼도_제한_시간_안에_나눈다() {
        String code = "'\\u{'".repeat(100_000);

        List<JsToken> tokens =
                assertTimeoutPreemptively(Duration.ofSeconds(5), () -> JsTokenizer.tokenize(code));

        assertThat(tokens).hasSize(100_000).allMatch(t -> t.kind() == Kind.STRING);
        assertThat(JsTokenizer.tokenize("'\\u{41}b'").get(0).value()).isEqualTo("Ab");
    }

    @Test
    void AC5_조건_괄호_뒤의_슬래시는_정규식이고_값_괄호_뒤는_나눗셈이다() {
        assertThat(JsTokenizer.tokenize("if (ready) /['\"]/.test(value);"))
                .extracting(JsToken::kind, JsToken::text)
                .contains(org.assertj.core.groups.Tuple.tuple(Kind.REGEX, "/['\"]/"));
        assertThat(JsTokenizer.tokenize("while (x) /a/g.test(y)"))
                .extracting(JsToken::kind)
                .contains(Kind.REGEX);
        assertThat(JsTokenizer.tokenize("const r = (a) / b / c"))
                .extracting(JsToken::kind)
                .doesNotContain(Kind.REGEX);
        assertThat(JsTokenizer.tokenize("if ((a) / b / c) {}"))
                .extracting(JsToken::kind)
                .doesNotContain(Kind.REGEX);
    }

    @Test
    void AC5_객체_리터럴을_닫은_중괄호_뒤의_슬래시는_나눗셈이고_블록_뒤는_정규식이다() {
        List<JsToken> tokens = JsTokenizer.tokenize("const n = {} / 2; fetch('/todos');");
        assertThat(tokens).extracting(JsToken::kind).doesNotContain(Kind.REGEX);
        assertThat(tokens).extracting(JsToken::value).contains("/todos");
        assertThat(JsTokenizer.tokenize("const m = x ? {a: 1} / 2 : {}; f('/a')"))
                .extracting(JsToken::kind)
                .doesNotContain(Kind.REGEX);
        assertThat(JsTokenizer.tokenize("function f() {}\n/a/g.test(y)"))
                .extracting(JsToken::kind)
                .contains(Kind.REGEX);
        assertThat(JsTokenizer.tokenize("if (x) {} /a/.test(y)"))
                .extracting(JsToken::kind)
                .contains(Kind.REGEX);
    }

    // ---- 5.3절 3: 메모리 ----

    @Test
    void AC5_토큰이_상한을_넘는_파일은_오류다() {
        assertThatThrownBy(() -> JsTokenizer.tokenize("a;".repeat(10), WorkBudget.unlimited(), 5))
                .isInstanceOf(JsSyntaxException.class)
                .hasMessage(JsTokenizer.TOO_MANY_TOKENS);
        assertThat(JsTokenizer.tokenize("a;".repeat(10), WorkBudget.unlimited(), 20)).hasSize(20);
    }

    @Test
    void AC5_템플릿_토큰은_원문을_복사하지_않고_한_글자_구두점은_문자열을_공유한다() {
        List<JsToken> tokens = JsTokenizer.tokenize("f(`a${`b${c}`}d`)(x)");

        JsToken template = tokens.get(2);
        assertThat(template.kind()).isEqualTo(Kind.TEMPLATE);
        assertThat(template.start()).isEqualTo(2);
        assertThat(template.end()).isEqualTo(16);
        assertThat(template.text()).isEqualTo("`");
        assertThat(template.quasis()).containsExactly("a", "d");
        assertThat(tokens.get(1).text()).isSameAs(tokens.get(4).text());
        assertThat(tokens.get(3).text()).isSameAs(tokens.get(6).text());
    }
}
