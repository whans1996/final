package com.vibenote.analysis.fetch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vibenote.analysis.fetch.RepositoryUrl.Host;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** F-05b 저장소 주소 규칙 (docs/specs/F-05b-analysis-request-url.md 4.1절, AC2 형식). */
class RepositoryUrlTest {

    static Stream<Arguments> allowed() {
        return Stream.of(
                Arguments.of(
                        "https://github.com/octo/todo-app",
                        Host.GITHUB,
                        "https://github.com/octo/todo-app",
                        "todo-app"),
                Arguments.of(
                        "https://github.com/octo/todo-app.git/",
                        Host.GITHUB,
                        "https://github.com/octo/todo-app",
                        "todo-app"),
                Arguments.of(
                        "  HTTP://WWW.GitHub.com/Octo-Cat/My.Repo_1/  ",
                        Host.GITHUB,
                        "https://github.com/Octo-Cat/My.Repo_1",
                        "My.Repo_1"),
                Arguments.of(
                        "https://github.com/octo/todo-app/",
                        Host.GITHUB,
                        "https://github.com/octo/todo-app",
                        "todo-app"),
                Arguments.of(
                        "https://gitlab.com/group/sub/my-app",
                        Host.GITLAB,
                        "https://gitlab.com/group/sub/my-app",
                        "my-app"),
                Arguments.of(
                        "https://GITLAB.com/group/my-app.git",
                        Host.GITLAB,
                        "https://gitlab.com/group/my-app",
                        "my-app"));
    }

    @ParameterizedTest
    @MethodSource("allowed")
    void AC2_허용되는_주소는_정규화하고_마지막_조각을_이름으로_쓴다(
            String input, Host host, String normalized, String name) {
        RepositoryUrl url = RepositoryUrl.parse(input);

        assertThat(url.host()).isEqualTo(host);
        assertThat(url.normalized()).isEqualTo(normalized);
        assertThat(url.name()).isEqualTo(name);
    }

    @Test
    void AC2_GitHub는_owner와_repo를_GitLab은_전체_경로를_준다() {
        assertThat(RepositoryUrl.parse("https://github.com/octo/todo-app").path())
                .isEqualTo("octo/todo-app");
        assertThat(RepositoryUrl.parse("https://gitlab.com/group/sub/my-app").path())
                .isEqualTo("group/sub/my-app");
    }

    @Test
    void AC2_이름은_100자로_자른다() {
        String repo = "r".repeat(100);
        String project = "p".repeat(150);

        assertThat(RepositoryUrl.parse("https://github.com/octo/" + repo).name()).isEqualTo(repo);
        assertThat(RepositoryUrl.parse("https://gitlab.com/group/" + project).name())
                .isEqualTo("p".repeat(100));
    }

    @Test
    void AC2_GitLab은_20조각까지_허용한다() {
        String twenty = "/a".repeat(20);

        assertThat(RepositoryUrl.parse("https://gitlab.com" + twenty).normalized())
                .isEqualTo("https://gitlab.com" + twenty);
        assertThatThrownBy(() -> RepositoryUrl.parse("https://gitlab.com" + twenty + "/a"))
                .isInstanceOf(InvalidRepositoryUrlException.class)
                .hasMessage(RepositoryUrl.PATH_EXTRA);
    }

    static Stream<Arguments> rejected() {
        return Stream.of(
                // 전처리
                Arguments.of(null, RepositoryUrl.EMPTY),
                Arguments.of("   ", RepositoryUrl.EMPTY),
                Arguments.of("https://github.com/octo/" + "r".repeat(477), RepositoryUrl.TOO_LONG),
                // 스킴
                Arguments.of("ftp://github.com/octo/todo-app", RepositoryUrl.SCHEME),
                Arguments.of("github.com/octo/todo-app", RepositoryUrl.SCHEME),
                Arguments.of("git@github.com:octo/todo-app.git", RepositoryUrl.SCHEME),
                // 호스트
                Arguments.of("https://lab.ssafy.com/octo/todo-app", RepositoryUrl.HOST),
                Arguments.of("https://octo.github.io/todo-app", RepositoryUrl.HOST),
                Arguments.of("https://api.github.com/octo/todo-app", RepositoryUrl.HOST),
                Arguments.of("https://github.com.evil.example/octo/todo-app", RepositoryUrl.HOST),
                Arguments.of("https://user@github.com/octo/todo-app", RepositoryUrl.HOST),
                Arguments.of("https://github.com@evil.example/octo/todo-app", RepositoryUrl.HOST),
                Arguments.of("https://github.com:443/octo/todo-app", RepositoryUrl.HOST),
                Arguments.of("https:///octo/todo-app", RepositoryUrl.HOST),
                // 브랜치·하위 경로, 쿼리·프래그먼트
                Arguments.of("https://github.com/octo/todo-app/tree/dev", RepositoryUrl.PATH_EXTRA),
                Arguments.of(
                        "https://github.com/octo/todo-app/blob/main/a.java",
                        RepositoryUrl.PATH_EXTRA),
                Arguments.of(
                        "https://gitlab.com/group/my-app/-/tree/dev", RepositoryUrl.PATH_EXTRA),
                Arguments.of(
                        "https://github.com/octo/todo-app?tab=readme", RepositoryUrl.PATH_EXTRA),
                Arguments.of("https://github.com/octo/todo-app#readme", RepositoryUrl.PATH_EXTRA),
                // 형식
                Arguments.of("https://github.com", RepositoryUrl.FORMAT),
                Arguments.of("https://github.com/", RepositoryUrl.FORMAT),
                Arguments.of("https://github.com/octo", RepositoryUrl.FORMAT),
                Arguments.of("https://gitlab.com/my-app", RepositoryUrl.FORMAT),
                Arguments.of("https://github.com/octo//todo-app", RepositoryUrl.FORMAT),
                Arguments.of("https://github.com/octo/..", RepositoryUrl.FORMAT),
                Arguments.of("https://github.com/octo/.", RepositoryUrl.FORMAT),
                Arguments.of("https://github.com/octo_cat/todo-app", RepositoryUrl.FORMAT),
                Arguments.of("https://github.com/" + "o".repeat(40) + "/r", RepositoryUrl.FORMAT),
                Arguments.of("https://github.com/octo/" + "r".repeat(101), RepositoryUrl.FORMAT),
                Arguments.of("https://github.com/octo/todo%2Fapp", RepositoryUrl.FORMAT),
                Arguments.of("https://github.com/octo/todo app", RepositoryUrl.FORMAT),
                Arguments.of("https://gitlab.com/group/../my-app", RepositoryUrl.FORMAT),
                Arguments.of("https://gitlab.com/group/" + "p".repeat(256), RepositoryUrl.FORMAT));
    }

    @ParameterizedTest
    @MethodSource("rejected")
    void AC2_규칙을_어기면_4_1절_문구로_거부한다(String input, String message) {
        assertThatThrownBy(() -> RepositoryUrl.parse(input))
                .isInstanceOf(InvalidRepositoryUrlException.class)
                .hasMessage(message);
    }

    @Test
    void AC2_500자까지는_허용한다() {
        String prefix = "https://gitlab.com/group/";
        String url = prefix + "p".repeat(255) + "/" + "q".repeat(500 - prefix.length() - 256);

        assertThat(url).hasSize(500);
        assertThat(RepositoryUrl.parse(url).normalized()).isEqualTo(url);
        assertThatThrownBy(() -> RepositoryUrl.parse(url + "q")).hasMessage(RepositoryUrl.TOO_LONG);
    }

    @Test
    void AC2_정규화하면_500자를_넘는_주소도_거부한다() {
        // http:// → https://로 한 글자 늘어 analyses.source_url VARCHAR(500)을 넘는다 (Codex 리뷰)
        String prefix = "http://gitlab.com/group/";
        String url = prefix + "p".repeat(255) + "/" + "q".repeat(500 - prefix.length() - 256);

        assertThat(url).hasSize(500);
        assertThatThrownBy(() -> RepositoryUrl.parse(url))
                .isInstanceOf(InvalidRepositoryUrlException.class)
                .hasMessage(RepositoryUrl.TOO_LONG);
    }
}
