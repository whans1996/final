package com.vibenote.analysis.fetch;

import static com.vibenote.analysis.fetch.FakeRepositoryServer.body;
import static com.vibenote.analysis.fetch.FakeRepositoryServer.chunked;
import static com.vibenote.analysis.fetch.FakeRepositoryServer.redirect;
import static com.vibenote.analysis.fetch.FakeRepositoryServer.status;
import static com.vibenote.analysis.fetch.FakeRepositoryServer.zip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vibenote.analysis.fetch.FakeRepositoryServer.Handler;
import com.vibenote.analysis.fetch.FakeRepositoryServer.Recorded;
import com.vibenote.analysis.pipeline.AnalysisFailedException;
import com.vibenote.common.exception.ErrorCode;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

/**
 * F-05b 수집기 (docs/specs/F-05b-analysis-request-url.md AC4~AC10, 5절). 가짜 GitHub·GitLab 서버를 JDK
 * HttpServer로 띄운다 (외부 네트워크 없음).
 */
class RepositoryArchiveFetcherTest {

    private static final String GITHUB_REPO = "/repos/octo/todo-app";
    private static final String GITHUB_ZIPBALL = "/repos/octo/todo-app/zipball";
    private static final String CODELOAD = "/codeload/octo/todo-app/zip/main";
    private static final String GITLAB_ARCHIVE =
            "/api/v4/projects/group%2Fsub%2Fmy-app/repository/archive.zip";
    private static final DataSize MAX = DataSize.ofKilobytes(64);
    private static final String TOKEN = "ghp_test-token-1234";

    @TempDir Path dir;

    private FakeRepositoryServer server;
    private Path target;

    @BeforeEach
    void setUp() {
        server = new FakeRepositoryServer();
        target = dir.resolve("analysis-tmp").resolve("1.zip");
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private FetchProperties properties(String token, Duration timeout) {
        return new FetchProperties(
                server.base(),
                server.base() + "/api/v4",
                List.of("localhost"),
                token,
                MAX,
                Duration.ofSeconds(2),
                timeout,
                3);
    }

    private RepositoryArchiveFetcher fetcher() {
        return new RepositoryArchiveFetcher(properties(null, Duration.ofSeconds(5)));
    }

    private static RepositoryUrl github() {
        return RepositoryUrl.parse("https://github.com/octo/todo-app");
    }

    private static RepositoryUrl gitlab() {
        return RepositoryUrl.parse("https://gitlab.com/group/sub/my-app");
    }

    /** ZIP 시그니처(PK\3\4)로 시작하는 size바이트. */
    private static byte[] archive(int size) {
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, (byte) 'a');
        bytes[0] = 'P';
        bytes[1] = 'K';
        bytes[2] = 3;
        bytes[3] = 4;
        return bytes;
    }

    private void assertFails(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(AnalysisFailedException.class)
                .satisfies(e -> assertThat(((AnalysisFailedException) e).code()).isEqualTo(code))
                .hasMessage(
                        code == ErrorCode.ANALYSIS_413
                                ? RepositoryArchiveFetcher.tooLargeMessage(MAX)
                                : code.message());
        assertThat(target).doesNotExist();
    }

    private List<String> paths() {
        return server.requests().stream().map(Recorded::rawPath).toList();
    }

    // ───────────── AC4 ─────────────

    @Test
    void AC4_GitHub_zipball의_302를_따라가_아카이브를_받는다() throws IOException {
        byte[] bytes = archive(1000);
        server.on(GITHUB_ZIPBALL, redirect(302, server.base() + CODELOAD)).on(CODELOAD, zip(bytes));

        fetcher().download(github(), target);

        assertThat(target).hasBinaryContent(bytes);
        assertThat(paths()).containsExactly(GITHUB_ZIPBALL, CODELOAD);
        Recorded api = server.requests().get(0);
        assertThat(api.method()).isEqualTo("GET");
        assertThat(api.headers().getFirst("User-Agent")).isEqualTo("VibeNote");
        assertThat(api.headers().getFirst("Accept")).isEqualTo("application/vnd.github+json");
        assertThat(api.headers().containsKey("Authorization")).isFalse();
        assertThat(server.requests().get(1).headers().getFirst("User-Agent")).isEqualTo("VibeNote");
    }

    @Test
    void AC4_토큰은_api_호스트_요청에만_붙이고_리다이렉트_요청에는_붙이지_않는다() throws IOException {
        server.on(GITHUB_REPO, repo("public", false))
                .on(GITHUB_ZIPBALL, redirect(302, server.base() + CODELOAD))
                .on(CODELOAD, zip(archive(100)));

        new RepositoryArchiveFetcher(properties(TOKEN, Duration.ofSeconds(5)))
                .download(github(), target);

        assertThat(paths()).containsExactly(GITHUB_REPO, GITHUB_ZIPBALL, CODELOAD);
        assertThat(server.requests().get(0).headers().getFirst("Authorization"))
                .isEqualTo("Bearer " + TOKEN);
        assertThat(server.requests().get(1).headers().getFirst("Authorization"))
                .isEqualTo("Bearer " + TOKEN);
        assertThat(server.requests().get(2).headers().containsKey("Authorization")).isFalse();
    }

    @Test
    void AC4_토큰이_없으면_공개_여부_확인_요청을_하지_않는다() throws IOException {
        server.on(GITHUB_ZIPBALL, zip(archive(100)));

        fetcher().download(github(), target);

        assertThat(paths()).containsExactly(GITHUB_ZIPBALL);
    }

    @Test
    void AC6_토큰이_있으면_비공개_저장소는_받지_않고_ANALYSIS_004() {
        for (Handler repo :
                List.of(
                        repo("private", true),
                        repo("internal", true),
                        repo(null, true),
                        FakeRepositoryServer.body(200, "{}".getBytes(StandardCharsets.UTF_8)))) {
            server.reset();
            server.on(GITHUB_REPO, repo).on(GITHUB_ZIPBALL, zip(archive(100)));

            assertFails(
                    () ->
                            new RepositoryArchiveFetcher(properties(TOKEN, Duration.ofSeconds(5)))
                                    .download(github(), target),
                    ErrorCode.ANALYSIS_004);
            assertThat(paths()).containsExactly(GITHUB_REPO);
        }
    }

    @Test
    void AC6_공개_여부_확인이_실패하면_아카이브를_받지_않는다() {
        Map<Handler, ErrorCode> cases =
                Map.of(
                        status(404), ErrorCode.ANALYSIS_004,
                        status(403, Map.of("x-ratelimit-remaining", "0")), ErrorCode.ANALYSIS_503,
                        status(500), ErrorCode.ANALYSIS_502,
                        FakeRepositoryServer.body(200, "not json".getBytes(StandardCharsets.UTF_8)),
                                ErrorCode.ANALYSIS_502);
        cases.forEach(
                (repo, code) -> {
                    server.reset();
                    server.on(GITHUB_REPO, repo).on(GITHUB_ZIPBALL, zip(archive(100)));

                    assertFails(
                            () ->
                                    new RepositoryArchiveFetcher(
                                                    properties(TOKEN, Duration.ofSeconds(5)))
                                            .download(github(), target),
                            code);
                    assertThat(paths()).containsExactly(GITHUB_REPO);
                });
    }

    /** GitHub 저장소 정보 응답. visibility가 null이면 필드를 넣지 않는다. */
    private static Handler repo(String visibility, boolean isPrivate) {
        String json =
                visibility == null
                        ? "{\"private\":%s}".formatted(isPrivate)
                        : "{\"private\":%s,\"visibility\":\"%s\"}".formatted(isPrivate, visibility);
        return FakeRepositoryServer.body(200, json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void AC4_빈_토큰은_붙이지_않는다() throws IOException {
        server.on(GITHUB_ZIPBALL, zip(archive(100)));

        new RepositoryArchiveFetcher(properties(" ", Duration.ofSeconds(5)))
                .download(github(), target);

        assertThat(server.requests().get(0).headers().containsKey("Authorization")).isFalse();
    }

    @Test
    void AC4_FetchProperties_문자열에_토큰이_드러나지_않는다() {
        assertThat(properties(TOKEN, Duration.ofSeconds(5)).toString()).doesNotContain(TOKEN);
    }

    // ───────────── AC5 ─────────────

    @Test
    void AC5_GitLab은_인코딩한_프로젝트_경로로_archive_zip을_받고_토큰을_붙이지_않는다() throws IOException {
        byte[] bytes = archive(500);
        server.on(GITLAB_ARCHIVE, zip(bytes));

        new RepositoryArchiveFetcher(properties(TOKEN, Duration.ofSeconds(5)))
                .download(gitlab(), target);

        assertThat(target).hasBinaryContent(bytes);
        assertThat(paths()).containsExactly(GITLAB_ARCHIVE);
        Recorded request = server.requests().get(0);
        assertThat(request.headers().getFirst("User-Agent")).isEqualTo("VibeNote");
        assertThat(request.headers().containsKey("Authorization")).isFalse();
    }

    // ───────────── AC6 ─────────────

    @Test
    void AC6_404_401_403_그_밖의_4xx는_ANALYSIS_004() {
        for (int code : new int[] {404, 401, 403, 400, 410, 451}) {
            server.reset();
            server.on(GITHUB_ZIPBALL, status(code));
            assertFails(() -> fetcher().download(github(), target), ErrorCode.ANALYSIS_004);
        }
        server.reset();
        server.on(GITHUB_ZIPBALL, status(403, Map.of("x-ratelimit-remaining", "12")));
        assertFails(() -> fetcher().download(github(), target), ErrorCode.ANALYSIS_004);
    }

    @Test
    void AC6_리다이렉트된_다운로드의_404도_ANALYSIS_004() {
        server.on(GITHUB_ZIPBALL, redirect(302, server.base() + CODELOAD))
                .on(CODELOAD, status(404));

        assertFails(() -> fetcher().download(github(), target), ErrorCode.ANALYSIS_004);
    }

    // ───────────── AC7 ─────────────

    @Test
    void AC7_429나_남은_한도가_0인_403은_ANALYSIS_503() {
        server.on(GITHUB_ZIPBALL, status(429));
        assertFails(() -> fetcher().download(github(), target), ErrorCode.ANALYSIS_503);

        server.reset();
        server.on(GITHUB_ZIPBALL, status(403, Map.of("x-ratelimit-remaining", "0")));
        assertFails(() -> fetcher().download(github(), target), ErrorCode.ANALYSIS_503);
    }

    // ───────────── AC8 ─────────────

    @Test
    void AC8_5xx는_ANALYSIS_502() {
        for (int code : new int[] {500, 502, 503}) {
            server.reset();
            server.on(GITHUB_ZIPBALL, status(code));
            assertFails(() -> fetcher().download(github(), target), ErrorCode.ANALYSIS_502);
        }
    }

    @Test
    void AC8_연결이_안_되면_ANALYSIS_502() {
        String base = server.base();
        server.close();
        RepositoryArchiveFetcher fetcher =
                new RepositoryArchiveFetcher(
                        new FetchProperties(
                                base,
                                base,
                                List.of("localhost"),
                                null,
                                MAX,
                                Duration.ofSeconds(2),
                                Duration.ofSeconds(5),
                                3));

        assertFails(() -> fetcher.download(github(), target), ErrorCode.ANALYSIS_502);
    }

    @Test
    void AC8_응답_헤더가_시간_안에_오지_않으면_ANALYSIS_502() {
        server.on(
                GITHUB_ZIPBALL,
                exchange -> {
                    sleep(3000);
                    zip(archive(100)).handle(exchange);
                });
        RepositoryArchiveFetcher fetcher =
                new RepositoryArchiveFetcher(properties(null, Duration.ofMillis(500)));

        long start = System.nanoTime();
        assertFails(() -> fetcher.download(github(), target), ErrorCode.ANALYSIS_502);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(2500));
    }

    @Test
    void AC8_본문이_끝까지_오는_데_시간을_넘기면_쓰다_만_파일_없이_ANALYSIS_502() {
        server.on(
                GITHUB_ZIPBALL,
                exchange -> {
                    exchange.sendResponseHeaders(200, 2000);
                    OutputStream out = exchange.getResponseBody();
                    out.write(archive(1000));
                    out.flush();
                    sleep(3000);
                    out.write(new byte[1000]);
                    out.close();
                });
        RepositoryArchiveFetcher fetcher =
                new RepositoryArchiveFetcher(properties(null, Duration.ofMillis(800)));

        long start = System.nanoTime();
        assertFails(() -> fetcher.download(github(), target), ErrorCode.ANALYSIS_502);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(2500));
    }

    @Test
    void AC8_ZIP_시그니처로_시작하지_않는_본문은_ANALYSIS_502() {
        server.on(GITHUB_ZIPBALL, body(200, "<html>login</html>".getBytes(StandardCharsets.UTF_8)));
        assertFails(() -> fetcher().download(github(), target), ErrorCode.ANALYSIS_502);

        server.reset();
        server.on(GITHUB_ZIPBALL, body(200, new byte[] {'P', 'K'}));
        assertFails(() -> fetcher().download(github(), target), ErrorCode.ANALYSIS_502);
    }

    @Test
    void AC8_200이_아닌_2xx와_리다이렉트가_아닌_3xx는_ANALYSIS_502() {
        for (int code : new int[] {204, 304}) {
            server.reset();
            server.on(GITHUB_ZIPBALL, status(code));
            assertFails(() -> fetcher().download(github(), target), ErrorCode.ANALYSIS_502);
        }
    }

    // ───────────── AC9 ─────────────

    @Test
    void AC9_Content_Length가_상한을_넘으면_ANALYSIS_413() {
        server.on(GITHUB_ZIPBALL, zip(archive((int) MAX.toBytes() + 1)));

        assertFails(() -> fetcher().download(github(), target), ErrorCode.ANALYSIS_413);
    }

    @Test
    void AC9_헤더_없이_받은_바이트가_상한을_넘으면_ANALYSIS_413() {
        server.on(GITHUB_ZIPBALL, chunked(archive((int) MAX.toBytes() + 1)));

        assertFails(() -> fetcher().download(github(), target), ErrorCode.ANALYSIS_413);
    }

    @Test
    void AC9_정확히_상한이면_계속_진행한다() throws IOException {
        byte[] exact = archive((int) MAX.toBytes());
        server.on(GITHUB_ZIPBALL, zip(exact));
        fetcher().download(github(), target);
        assertThat(target).hasBinaryContent(exact);

        server.reset();
        server.on(GITHUB_ZIPBALL, chunked(exact));
        fetcher().download(github(), target);
        assertThat(target).hasBinaryContent(exact);
    }

    @Test
    void AC9_문구는_MB로_알린다() {
        assertThat(RepositoryArchiveFetcher.tooLargeMessage(DataSize.ofMegabytes(30)))
                .isEqualTo("저장소 아카이브가 30MB를 넘습니다. ZIP으로 필요한 코드만 올려 주세요.");
    }

    // ───────────── AC10 ─────────────

    @Test
    void AC10_리다이렉트는_3번까지_따라간다() throws IOException {
        server.on(GITHUB_ZIPBALL, redirect(302, server.base() + "/r1"))
                .on("/r1", redirect(301, "/r2"))
                .on("/r2", redirect(307, server.base() + "/r3"))
                .on("/r3", zip(archive(100)));

        fetcher().download(github(), target);

        assertThat(paths()).containsExactly(GITHUB_ZIPBALL, "/r1", "/r2", "/r3");
        assertThat(target).exists();
    }

    @Test
    void AC10_4번째_리다이렉트는_따라가지_않는다() {
        server.on(GITHUB_ZIPBALL, redirect(302, server.base() + "/r1"))
                .on("/r1", redirect(302, server.base() + "/r2"))
                .on("/r2", redirect(302, server.base() + "/r3"))
                .on("/r3", redirect(302, server.base() + "/r4"))
                .on("/r4", zip(archive(100)));

        assertFails(() -> fetcher().download(github(), target), ErrorCode.ANALYSIS_502);
        assertThat(paths()).containsExactly(GITHUB_ZIPBALL, "/r1", "/r2", "/r3");
    }

    @Test
    void AC10_허용되지_않은_호스트로는_요청하지_않는다() {
        try (FakeRepositoryServer other = new FakeRepositoryServer()) {
            other.on(CODELOAD, zip(archive(100)));
            for (String location :
                    List.of(
                            "http://127.0.0.1:" + other.port() + CODELOAD,
                            "http://169.254.169.254/latest/meta-data/",
                            "http://evil.example" + CODELOAD,
                            "http://user@localhost:" + server.port() + CODELOAD)) {
                server.reset();
                server.on(GITHUB_ZIPBALL, redirect(302, location)).on(CODELOAD, zip(archive(100)));

                assertFails(() -> fetcher().download(github(), target), ErrorCode.ANALYSIS_502);
                assertThat(paths()).containsExactly(GITHUB_ZIPBALL);
            }
            assertThat(other.requests()).isEmpty();
        }
    }

    @Test
    void AC10_스킴이_바뀌거나_Location이_없거나_잘못된_주소면_ANALYSIS_502() {
        for (String location :
                new String[] {
                    "https://localhost:" + server.port() + CODELOAD, null, "http://[bad", ""
                }) {
            server.reset();
            server.on(GITHUB_ZIPBALL, redirect(302, location)).on(CODELOAD, zip(archive(100)));

            assertFails(() -> fetcher().download(github(), target), ErrorCode.ANALYSIS_502);
            assertThat(paths()).containsExactly(GITHUB_ZIPBALL);
        }
    }

    @Test
    void AC10_다른_포트로는_요청하지_않는다() {
        try (FakeRepositoryServer other = new FakeRepositoryServer()) {
            other.on(CODELOAD, zip(archive(100)));
            server.on(GITHUB_ZIPBALL, redirect(302, "http://localhost:" + other.port() + CODELOAD));

            assertFails(() -> fetcher().download(github(), target), ErrorCode.ANALYSIS_502);
            assertThat(other.requests()).isEmpty();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
