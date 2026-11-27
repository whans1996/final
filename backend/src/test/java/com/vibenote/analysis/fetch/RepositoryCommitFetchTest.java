package com.vibenote.analysis.fetch;

import static com.vibenote.analysis.fetch.FakeRepositoryServer.body;
import static com.vibenote.analysis.fetch.FakeRepositoryServer.status;
import static com.vibenote.analysis.fetch.FakeRepositoryServer.zip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vibenote.analysis.fetch.FakeRepositoryServer.Handler;
import com.vibenote.analysis.fetch.FakeRepositoryServer.Recorded;
import com.vibenote.analysis.pipeline.AnalysisFailedException;
import com.vibenote.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

/**
 * P-1a 최신 커밋 조회와 커밋 지정 아카이브 받기 (docs/specs/P-1a-project-versions.md AC2·AC3·AC11, 3절 외부 API). F-05b
 * 수집기 테스트(RepositoryArchiveFetcherTest)와 AC 번호가 섞이지 않게 나눴다. 가짜 GitHub·GitLab 서버를 쓴다.
 */
class RepositoryCommitFetchTest {

    private static final String GITHUB_REPO = "/repos/octo/todo-app";
    private static final String GITHUB_COMMITS = "/repos/octo/todo-app/commits";
    private static final String GITLAB_PROJECT = "/api/v4/projects/group%2Fsub%2Fmy-app";
    private static final String GITLAB_COMMITS = GITLAB_PROJECT + "/repository/commits";
    private static final String GITLAB_ARCHIVE = GITLAB_PROJECT + "/repository/archive.zip";
    private static final String SHA = "9f2c4e1a7b3d5f60812c9e4a1b2c3d4e5f607182";
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

    private FetchProperties properties(String token, Duration commitTimeout) {
        return new FetchProperties(
                server.base(),
                server.base() + "/api/v4",
                List.of("localhost"),
                token,
                DataSize.ofKilobytes(64),
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                3,
                commitTimeout);
    }

    private RepositoryArchiveFetcher fetcher() {
        return new RepositoryArchiveFetcher(properties(null, Duration.ofSeconds(5)));
    }

    private RepositoryArchiveFetcher fetcherWithToken() {
        return new RepositoryArchiveFetcher(properties(TOKEN, Duration.ofSeconds(5)));
    }

    private static RepositoryUrl github() {
        return RepositoryUrl.parse("https://github.com/octo/todo-app");
    }

    private static RepositoryUrl gitlab() {
        return RepositoryUrl.parse("https://gitlab.com/group/sub/my-app");
    }

    private static Handler json(String json) {
        return body(200, json.getBytes(StandardCharsets.UTF_8));
    }

    /** GitHub {@code GET /repos/{owner}/{repo}/commits?per_page=1} 응답. */
    private static Handler githubCommits(String sha, String message, String date) {
        return json(
                "[{\"sha\":\"%s\",\"commit\":{\"message\":%s,\"committer\":{\"date\":\"%s\"}}}]"
                        .formatted(sha, quote(message), date));
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static Handler repo(String visibility, boolean isPrivate) {
        return json("{\"private\":%s,\"visibility\":\"%s\"}".formatted(isPrivate, visibility));
    }

    private static byte[] archive(int size) {
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, (byte) 'a');
        bytes[0] = 'P';
        bytes[1] = 'K';
        bytes[2] = 3;
        bytes[3] = 4;
        return bytes;
    }

    private List<String> paths() {
        return server.requests().stream().map(Recorded::rawPath).toList();
    }

    // ───────────── AC2 최신 커밋 조회 ─────────────

    @Test
    void AC2_GitHub_커밋_목록_첫_항목의_SHA_메시지_첫_줄_KST_시각을_읽는다() {
        server.on(
                GITHUB_COMMITS,
                githubCommits(SHA, "feat: JWT 로그인\n\n- 토큰 발급", "2026-11-28T05:02:11Z"));

        Optional<RepositoryCommit> commit = fetcher().latestCommit(github());

        assertThat(commit)
                .contains(
                        new RepositoryCommit(
                                SHA, "feat: JWT 로그인", LocalDateTime.of(2026, 11, 28, 14, 2, 11)));
        Recorded request = server.requests().get(0);
        assertThat(request.rawPath()).isEqualTo(GITHUB_COMMITS);
        assertThat(request.rawQuery()).isEqualTo("per_page=1");
        assertThat(request.headers().getFirst("User-Agent")).isEqualTo("VibeNote");
        assertThat(request.headers().getFirst("Accept")).isEqualTo("application/vnd.github+json");
        assertThat(request.headers().containsKey("Authorization")).isFalse();
    }

    @Test
    void AC2_GitLab_커밋_목록_첫_항목의_id_title_committed_date를_읽는다() {
        server.on(
                GITLAB_COMMITS,
                json(
                        "[{\"id\":\"%s\",\"title\":\"fix: 로그인 버그\",\"committed_date\":\"2026-11-28T14:02:11.000+09:00\"}]"
                                .formatted(SHA)));

        Optional<RepositoryCommit> commit = fetcher().latestCommit(gitlab());

        assertThat(commit)
                .contains(
                        new RepositoryCommit(
                                SHA, "fix: 로그인 버그", LocalDateTime.of(2026, 11, 28, 14, 2, 11)));
        assertThat(server.requests().get(0).rawPath()).isEqualTo(GITLAB_COMMITS);
        assertThat(server.requests().get(0).rawQuery()).isEqualTo("per_page=1");
    }

    @Test
    void AC2_메시지_첫_줄은_200자까지_자른다() {
        String longLine = "가".repeat(250);
        server.on(
                GITHUB_COMMITS,
                githubCommits(SHA, "  " + longLine + "  \n둘째 줄", "2026-11-28T05:02:11Z"));

        RepositoryCommit commit = fetcher().latestCommit(github()).orElseThrow();

        assertThat(commit.message()).isEqualTo("가".repeat(200));
    }

    @Test
    void AC2_200자_경계의_서로게이트_쌍은_반으로_자르지_않는다() {
        String line = "a".repeat(199) + "😀" + "b";
        server.on(GITHUB_COMMITS, githubCommits(SHA, line, "2026-11-28T05:02:11Z"));

        RepositoryCommit commit = fetcher().latestCommit(github()).orElseThrow();

        assertThat(commit.message()).isEqualTo("a".repeat(199));
    }

    @Test
    void AC2_빈_메시지는_null이고_시각을_읽지_못하면_시각만_null이다() {
        server.on(GITHUB_COMMITS, githubCommits(SHA, "  \n본문만", "어제"));

        RepositoryCommit commit = fetcher().latestCommit(github()).orElseThrow();

        assertThat(commit).isEqualTo(new RepositoryCommit(SHA, null, null));
    }

    // ───────────── AC2 그 커밋으로 받기 ─────────────

    @Test
    void AC2_GitHub는_zipball_sha로_그_커밋_아카이브를_받는다() {
        byte[] bytes = archive(500);
        server.on(GITHUB_REPO + "/zipball/" + SHA, zip(bytes));

        fetcher().download(github(), SHA, target);

        assertThat(target).hasBinaryContent(bytes);
        assertThat(paths()).containsExactly(GITHUB_REPO + "/zipball/" + SHA);
    }

    @Test
    void AC2_GitLab은_archive_zip_sha_쿼리로_그_커밋_아카이브를_받는다() {
        byte[] bytes = archive(500);
        server.on(GITLAB_ARCHIVE, zip(bytes));

        fetcher().download(gitlab(), SHA, target);

        assertThat(target).hasBinaryContent(bytes);
        assertThat(server.requests()).hasSize(1);
        assertThat(server.requests().get(0).rawPath()).isEqualTo(GITLAB_ARCHIVE);
        assertThat(server.requests().get(0).rawQuery()).isEqualTo("sha=" + SHA);
    }

    @Test
    void AC2_SHA_형식이_아니면_요청하지_않는다() {
        assertThatThrownBy(() -> fetcher().download(github(), "../../etc", target))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(server.requests()).isEmpty();
        assertThat(target).doesNotExist();
    }

    // ───────────── AC3 조회 실패는 empty ─────────────

    @Test
    void AC3_요청_한도_429는_empty() {
        server.on(GITHUB_COMMITS, status(429));

        assertThat(fetcher().latestCommit(github())).isEmpty();
    }

    @Test
    void AC3_남은_한도가_0인_403은_empty() {
        server.on(GITHUB_COMMITS, status(403, Map.of("x-ratelimit-remaining", "0")));

        assertThat(fetcher().latestCommit(github())).isEmpty();
    }

    @Test
    void AC3_서버_오류_5xx는_empty() {
        server.on(GITHUB_COMMITS, status(502));

        assertThat(fetcher().latestCommit(github())).isEmpty();
    }

    @Test
    void AC3_없는_경로_404는_empty() {
        assertThat(fetcher().latestCommit(gitlab())).isEmpty();

        assertThat(paths()).containsExactly(GITLAB_COMMITS);
    }

    @Test
    void AC3_빈_저장소_409는_empty() {
        server.on(GITHUB_COMMITS, status(409));

        assertThat(fetcher().latestCommit(github())).isEmpty();
    }

    @Test
    void AC3_빈_목록은_empty() {
        server.on(GITHUB_COMMITS, json("[]"));

        assertThat(fetcher().latestCommit(github())).isEmpty();
    }

    @Test
    void AC3_SHA_형식이_아니거나_JSON이_아니면_empty() {
        server.on(
                GITHUB_COMMITS,
                githubCommits(SHA.toUpperCase(), "feat: 대문자", "2026-11-28T05:02:11Z"));
        assertThat(fetcher().latestCommit(github())).isEmpty();

        server.on(GITHUB_COMMITS, githubCommits("abc123", "feat: 짧음", "2026-11-28T05:02:11Z"));
        assertThat(fetcher().latestCommit(github())).isEmpty();

        server.on(GITHUB_COMMITS, json("<html>rate limited</html>"));
        assertThat(fetcher().latestCommit(github())).isEmpty();

        server.on(GITHUB_COMMITS, json("{\"sha\":\"%s\"}".formatted(SHA)));
        assertThat(fetcher().latestCommit(github())).isEmpty();
    }

    @Test
    void AC3_시간_상한을_넘기면_기다리지_않고_empty() {
        server.on(
                GITHUB_COMMITS,
                exchange -> {
                    sleep(3000);
                    githubCommits(SHA, "feat: 늦음", "2026-11-28T05:02:11Z").handle(exchange);
                });
        RepositoryArchiveFetcher fetcher =
                new RepositoryArchiveFetcher(properties(null, Duration.ofMillis(300)));

        long start = System.nanoTime();
        assertThat(fetcher.latestCommit(github())).isEmpty();
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(2500));
    }

    // ───────────── AC11 서버 토큰 ─────────────

    @Test
    void AC11_토큰이_있으면_공개_저장소인지_먼저_확인하고_커밋을_조회한다() {
        server.on(GITHUB_REPO, repo("public", false))
                .on(GITHUB_COMMITS, githubCommits(SHA, "feat: 공개", "2026-11-28T05:02:11Z"));

        assertThat(fetcherWithToken().latestCommit(github()))
                .map(RepositoryCommit::sha)
                .contains(SHA);

        assertThat(paths()).containsExactly(GITHUB_REPO, GITHUB_COMMITS);
        assertThat(server.requests().get(1).headers().getFirst("Authorization"))
                .isEqualTo("Bearer " + TOKEN);
    }

    @Test
    void AC11_토큰이_있고_비공개로_바뀌었으면_ANALYSIS_004이고_커밋_목록을_요청하지_않는다() {
        server.on(GITHUB_REPO, repo("private", true))
                .on(GITHUB_COMMITS, githubCommits(SHA, "feat: 비공개", "2026-11-28T05:02:11Z"));

        assertThatThrownBy(() -> fetcherWithToken().latestCommit(github()))
                .isInstanceOf(AnalysisFailedException.class)
                .satisfies(
                        e ->
                                assertThat(((AnalysisFailedException) e).code())
                                        .isEqualTo(ErrorCode.ANALYSIS_004));

        assertThat(paths()).containsExactly(GITHUB_REPO);
    }

    @Test
    void AC11_토큰이_있고_저장소가_없어졌으면_ANALYSIS_004() {
        assertThatThrownBy(() -> fetcherWithToken().latestCommit(github()))
                .isInstanceOf(AnalysisFailedException.class)
                .satisfies(
                        e ->
                                assertThat(((AnalysisFailedException) e).code())
                                        .isEqualTo(ErrorCode.ANALYSIS_004));

        assertThat(paths()).containsExactly(GITHUB_REPO);
    }

    @Test
    void AC11_공개_확인이_서버_오류나_요청_한도로_실패하면_분석을_멈추지_않고_empty() {
        server.on(GITHUB_REPO, status(500));
        assertThat(fetcherWithToken().latestCommit(github())).isEmpty();

        server.on(GITHUB_REPO, status(429));
        assertThat(fetcherWithToken().latestCommit(github())).isEmpty();

        assertThat(paths()).containsExactly(GITHUB_REPO, GITHUB_REPO);
    }

    @Test
    void AC11_GitLab은_토큰이_있어도_공개_확인_없이_조회한다() {
        server.on(
                GITLAB_COMMITS,
                json(
                        "[{\"id\":\"%s\",\"title\":\"t\",\"committed_date\":\"2026-11-28T14:02:11+09:00\"}]"
                                .formatted(SHA)));

        assertThat(fetcherWithToken().latestCommit(gitlab())).isPresent();

        assertThat(paths()).containsExactly(GITLAB_COMMITS);
        assertThat(server.requests().get(0).headers().containsKey("Authorization")).isFalse();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
