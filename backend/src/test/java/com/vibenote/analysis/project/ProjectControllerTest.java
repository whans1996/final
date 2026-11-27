package com.vibenote.analysis.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.analysis.fetch.FakeRepositoryServer;
import com.vibenote.analysis.fetch.RepositoryArchiveFetcher;
import com.vibenote.analysis.fetch.RepositoryUrl;
import com.vibenote.analysis.pipeline.AnalysisFailedException;
import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.TestProjects;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.common.security.JwtTokenProvider;
import com.vibenote.user.UserService;
import com.vibenote.user.dto.SignupRequest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 프로젝트 API (docs/specs/P-1a-project-versions.md AC6~AC10, AC11 배지). 데이터는 SQL로 직접 넣고
 * {@code @p1a.test} 회원을 지워 정리한다 (프로젝트·버전·분석은 users FK CASCADE). 최신 커밋은 가짜 GitHub 서버가, 5분 캐시는 바꿀 수
 * 있는 시계가 맡는다.
 */
class ProjectControllerTest extends IntegrationTestBase {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    static final LocalDateTime START = LocalDateTime.of(2026, 11, 29, 10, 0, 0);
    static final String START_TEXT = "2026-11-29T10:00:00+09:00";
    static final String SHA_A = "aaaa4e1a7b3d5f60812c9e4a1b2c3d4e5f607182";
    static final String SHA_B = "bbbb4e1a7b3d5f60812c9e4a1b2c3d4e5f607182";

    /** 테스트가 시각을 옮길 수 있는 시계 (AC10 캐시). */
    static final class MovableClock extends Clock {
        // 컨텍스트를 띄울 때도 시계를 읽으므로 처음부터 START다
        private final AtomicReference<Instant> now =
                new AtomicReference<>(START.atZone(KST).toInstant());

        void set(LocalDateTime time) {
            now.set(time.atZone(KST).toInstant());
        }

        void advance(Duration duration) {
            now.updateAndGet(i -> i.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return KST;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }

    static final MovableClock CLOCK = new MovableClock();
    static final FakeRepositoryServer REPOSITORY = new FakeRepositoryServer();

    @TestBean Clock clock;

    @MockitoSpyBean RepositoryArchiveFetcher fetcher;

    static Clock clock() {
        return CLOCK;
    }

    @DynamicPropertySource
    static void repositoryServer(DynamicPropertyRegistry registry) {
        registry.add("analysis.fetch.github-api-base", REPOSITORY::base);
        registry.add("analysis.fetch.gitlab-api-base", () -> REPOSITORY.base() + "/api/v4");
        registry.add("analysis.fetch.allowed-hosts", () -> "localhost");
    }

    @AfterAll
    static void closeRepositoryServer() {
        REPOSITORY.close();
    }

    @Autowired UserService userService;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired JdbcTemplate jdbc;

    long me;
    long other;

    @BeforeEach
    void setUp() {
        cleanUp();
        CLOCK.set(START);
        REPOSITORY.reset();
        me = createUser("p1ame");
        other = createUser("p1aother");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM users WHERE email LIKE '%@p1a.test'");
    }

    // ───── 데이터 ─────

    long createUser(String name) {
        return userService.signup(
                new SignupRequest(name + "@p1a.test", "vibe1234", name, "JUNIOR"));
    }

    String bearer(long userId) {
        return "Bearer " + tokenProvider.createAccessToken(userId, "USER");
    }

    long urlProject(long userId, String repo) {
        return TestProjects.url(jdbc, userId, "https://github.com/octo/" + repo);
    }

    long zipProject(long userId) {
        return TestProjects.zip(jdbc, userId);
    }

    long version(long projectId, int number, String sha, String message) {
        jdbc.update(
                "INSERT INTO project_versions (project_id, number, commit_sha, commit_message,"
                        + " committed_at, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                projectId,
                number,
                sha,
                message,
                sha == null ? null : LocalDateTime.of(2026, 11, 28, 14, 2, 11),
                START.minusDays(1).plusMinutes(number));
        return jdbc.queryForObject(
                "SELECT id FROM project_versions WHERE project_id = ? AND number = ?",
                Long.class,
                projectId,
                number);
    }

    /** 분석 행. {@code versionId}가 null이면 아직 버전이 없는 분석(FETCHING·FAILED)이다. */
    long analysis(
            long projectId,
            Long versionId,
            String level,
            String status,
            LocalDateTime createdAt,
            boolean deleted) {
        var project =
                jdbc.queryForMap(
                        "SELECT user_id, source_type, source_url, name FROM projects WHERE id = ?",
                        projectId);
        jdbc.update(
                "INSERT INTO analyses (user_id, project_id, project_version_id, source_type,"
                    + " source_url, project_name, title, level, status, quiz_status, created_at,"
                    + " deleted_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                project.get("user_id"),
                projectId,
                versionId,
                project.get("source_type"),
                project.get("source_url"),
                project.get("name"),
                project.get("name"),
                level,
                status,
                "DONE".equals(status) ? "READY" : null,
                createdAt,
                deleted ? createdAt.plusHours(1) : null);
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM analyses WHERE project_id = ?", Long.class, projectId);
    }

    long analysis(long projectId, Long versionId, String level, LocalDateTime createdAt) {
        return analysis(projectId, versionId, level, "DONE", createdAt, false);
    }

    /** 가짜 GitHub의 기본 브랜치 최신 커밋. */
    void latestCommitIs(String repo, String sha, String message) {
        REPOSITORY.on(
                "/repos/octo/" + repo + "/commits",
                FakeRepositoryServer.body(
                        200,
                        ("[{\"sha\":\"%s\",\"commit\":{\"message\":\"%s\","
                                        + "\"committer\":{\"date\":\"2026-11-29T00:12:40Z\"}}}]")
                                .formatted(sha, message)
                                .getBytes(StandardCharsets.UTF_8)));
    }

    long commitRequests(String repo) {
        return REPOSITORY.requests().stream()
                .filter(r -> r.rawPath().equals("/repos/octo/" + repo + "/commits"))
                .count();
    }

    ResultActions getAs(long userId, String path) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", bearer(userId)));
    }

    ResultActions latestCommit(long projectId) throws Exception {
        return getAs(me, "/api/projects/" + projectId + "/latest-commit");
    }

    // ───────────── AC6 목록 ─────────────

    @Test
    void AC6_분석이_남은_내_프로젝트만_마지막_요청_최신순으로_버전_수_최신_버전_수준과_함께_준다() throws Exception {
        long todo = urlProject(me, "todo-app");
        long v1 = version(todo, 1, null, null);
        long v2 = version(todo, 2, SHA_A, "feat: JWT 로그인");
        analysis(todo, v1, "JUNIOR", START.minusDays(2));
        analysis(todo, v2, "BEGINNER", START.minusHours(3));
        analysis(todo, v2, "JUNIOR", START.minusHours(2));
        long zip = zipProject(me);
        analysis(zip, version(zip, 1, null, null), "SENIOR", START.minusDays(1));
        long gone = urlProject(me, "gone-app");
        analysis(gone, version(gone, 1, null, null), "JUNIOR", "DONE", START.minusHours(1), true);
        long othersProject = urlProject(other, "todo-app");
        analysis(othersProject, version(othersProject, 1, null, null), "JUNIOR", START);

        getAs(me, "/api/projects")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items[*].id").value(contains((int) todo, (int) zip)))
                .andExpect(jsonPath("$.items[0].name").value("todo-app"))
                .andExpect(jsonPath("$.items[0].sourceType").value("URL"))
                .andExpect(
                        jsonPath("$.items[0].sourceUrl").value("https://github.com/octo/todo-app"))
                .andExpect(jsonPath("$.items[0].versionCount").value(2))
                .andExpect(jsonPath("$.items[0].latestVersion.id").value(v2))
                .andExpect(jsonPath("$.items[0].latestVersion.number").value(2))
                .andExpect(jsonPath("$.items[0].latestVersion.commitSha").value(SHA_A))
                .andExpect(
                        jsonPath("$.items[0].latestVersion.commitMessage").value("feat: JWT 로그인"))
                .andExpect(
                        jsonPath("$.items[0].latestVersion.committedAt")
                                .value("2026-11-28T14:02:11+09:00"))
                .andExpect(jsonPath("$.items[0].levels").value(contains("BEGINNER", "JUNIOR")))
                .andExpect(jsonPath("$.items[0].lastAnalyzedAt").value("2026-11-29T08:00:00+09:00"))
                .andExpect(jsonPath("$.items[1].sourceType").value("ZIP"))
                .andExpect(jsonPath("$.items[1].sourceUrl").value(nullValue()))
                .andExpect(jsonPath("$.items[1].levels").value(contains("SENIOR")));
    }

    @Test
    void AC6_페이지로_나눠_주고_버전이_아직_없으면_latestVersion은_null이다() throws Exception {
        long newer = urlProject(me, "fetching-app");
        analysis(newer, null, "JUNIOR", "FETCHING", START.minusMinutes(1), false);
        long older = zipProject(me);
        analysis(older, version(older, 1, null, null), "JUNIOR", START.minusDays(1));

        getAs(me, "/api/projects?size=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(newer))
                .andExpect(jsonPath("$.items[0].versionCount").value(0))
                .andExpect(jsonPath("$.items[0].latestVersion").value(nullValue()));
        getAs(me, "/api/projects?page=2&size=1")
                .andExpect(jsonPath("$.items[*].id").value(contains((int) older)));
    }

    @Test
    void AC6_토큰이_없으면_401() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    // ───────────── AC7 상세 ─────────────

    @Test
    void AC7_버전은_번호_내림차순_분석은_요청_최신순이고_분석이_모두_삭제된_버전은_뺀다() throws Exception {
        long todo = urlProject(me, "todo-app");
        long v1 = version(todo, 1, null, null);
        long v2 = version(todo, 2, SHA_A, "feat: 지운 버전");
        long v3 = version(todo, 3, SHA_B, "feat: 최신");
        long a1 = analysis(todo, v1, "JUNIOR", START.minusDays(3));
        analysis(todo, v2, "JUNIOR", "DONE", START.minusDays(2), true);
        long a3 = analysis(todo, v3, "BEGINNER", START.minusDays(1));
        long fetching = analysis(todo, null, "SENIOR", "FETCHING", START.minusMinutes(1), false);

        getAs(me, "/api/projects/" + todo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(todo))
                .andExpect(jsonPath("$.name").value("todo-app"))
                .andExpect(jsonPath("$.sourceType").value("URL"))
                .andExpect(jsonPath("$.sourceUrl").value("https://github.com/octo/todo-app"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.versions[*].id").value(contains((int) v3, (int) v1)))
                .andExpect(jsonPath("$.versions[0].number").value(3))
                .andExpect(jsonPath("$.versions[0].commitSha").value(SHA_B))
                .andExpect(jsonPath("$.versions[0].commitMessage").value("feat: 최신"))
                .andExpect(jsonPath("$.versions[1].commitSha").value(nullValue()))
                .andExpect(jsonPath("$.versions[1].committedAt").value(nullValue()))
                .andExpect(
                        jsonPath("$.analyses[*].id")
                                .value(contains((int) fetching, (int) a3, (int) a1)))
                .andExpect(jsonPath("$.analyses[0].versionId").value(nullValue()))
                .andExpect(jsonPath("$.analyses[0].status").value("FETCHING"))
                .andExpect(jsonPath("$.analyses[1].versionId").value(v3))
                .andExpect(jsonPath("$.analyses[1].level").value("BEGINNER"))
                .andExpect(jsonPath("$.analyses[1].status").value("DONE"))
                .andExpect(jsonPath("$.analyses[1].quizStatus").value("READY"))
                .andExpect(jsonPath("$.analyses[1].title").value("todo-app"))
                .andExpect(jsonPath("$.analyses[1].createdAt").value("2026-11-28T10:00:00+09:00"));
    }

    @Test
    void AC7_없거나_남의_프로젝트거나_분석을_모두_삭제했으면_404_PROJECT_404() throws Exception {
        long others = urlProject(other, "todo-app");
        analysis(others, version(others, 1, null, null), "JUNIOR", START);
        long gone = zipProject(me);
        analysis(gone, version(gone, 1, null, null), "JUNIOR", "DONE", START, true);

        for (long id : new long[] {others, gone, 999_999_999L}) {
            getAs(me, "/api/projects/" + id)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PROJECT_404"))
                    .andExpect(jsonPath("$.message").value("프로젝트를 찾을 수 없습니다."));
        }
    }

    // ───────────── AC8 이름 바꾸기 ─────────────

    ResultActions rename(long userId, long projectId, String json) throws Exception {
        return mockMvc.perform(
                patch("/api/projects/" + projectId)
                        .header("Authorization", bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    @Test
    void AC8_앞뒤_공백을_뗀_이름으로_바꾸고_분석_제목은_그대로다() throws Exception {
        long todo = urlProject(me, "todo-app");
        long a = analysis(todo, version(todo, 1, null, null), "JUNIOR", START);

        rename(me, todo, "{\"name\":\"  할 일 앱  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(todo))
                .andExpect(jsonPath("$.name").value("할 일 앱"));

        assertThat(
                        jdbc.queryForObject(
                                "SELECT name FROM projects WHERE id = ?", String.class, todo))
                .isEqualTo("할 일 앱");
        assertThat(jdbc.queryForObject("SELECT title FROM analyses WHERE id = ?", String.class, a))
                .isEqualTo("todo-app");
        rename(me, todo, "{\"name\":\"" + "가".repeat(100) + "\"}").andExpect(status().isOk());
    }

    @Test
    void AC8_비었거나_100자를_넘으면_400_details_name() throws Exception {
        long todo = urlProject(me, "todo-app");
        analysis(todo, version(todo, 1, null, null), "JUNIOR", START);

        for (String json :
                new String[] {
                    "{\"name\":\"   \"}", "{}", "{\"name\":\"" + "가".repeat(101) + "\"}"
                }) {
            rename(me, todo, json)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_400"))
                    .andExpect(jsonPath("$.details.name").value("프로젝트 이름은 1~100자로 입력하세요."));
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT name FROM projects WHERE id = ?", String.class, todo))
                .isEqualTo("todo-app");
    }

    @Test
    void AC8_남의_프로젝트면_404이고_바뀌지_않는다() throws Exception {
        long others = urlProject(other, "todo-app");
        analysis(others, version(others, 1, null, null), "JUNIOR", START);

        rename(me, others, "{\"name\":\"뺏기\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_404"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT name FROM projects WHERE id = ?", String.class, others))
                .isEqualTo("todo-app");
    }

    // ───────────── AC9 새 커밋 배지 ─────────────

    @Test
    void AC9_최신_커밋이_분석한_최신_커밋과_다르면_NEW_COMMIT() throws Exception {
        long p = urlProject(me, "new-app");
        analysis(p, version(p, 1, SHA_A, "feat: 처음"), "JUNIOR", START.minusDays(1));
        latestCommitIs("new-app", SHA_B, "feat: 비밀번호 재설정");

        latestCommit(p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW_COMMIT"))
                .andExpect(jsonPath("$.latest.sha").value(SHA_B))
                .andExpect(jsonPath("$.latest.message").value("feat: 비밀번호 재설정"))
                .andExpect(jsonPath("$.latest.committedAt").value("2026-11-29T09:12:40+09:00"))
                .andExpect(jsonPath("$.analyzedSha").value(SHA_A))
                .andExpect(jsonPath("$.checkedAt").value(START_TEXT));
    }

    @Test
    void AC9_같으면_UP_TO_DATE이고_분석을_모두_지운_커밋_버전은_분석한_커밋으로_보지_않는다() throws Exception {
        long p = urlProject(me, "same-app");
        analysis(p, version(p, 1, SHA_A, "feat: 처음"), "JUNIOR", START.minusDays(2));
        analysis(p, version(p, 2, SHA_B, "feat: 지움"), "JUNIOR", "DONE", START.minusDays(1), true);
        latestCommitIs("same-app", SHA_A, "feat: 처음");

        latestCommit(p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP_TO_DATE"))
                .andExpect(jsonPath("$.latest.sha").value(SHA_A))
                .andExpect(jsonPath("$.analyzedSha").value(SHA_A));
    }

    @Test
    void AC9_분석한_커밋이_없으면_UNKNOWN이고_조회한_최신_커밋은_준다() throws Exception {
        long p = urlProject(me, "backfill-app");
        analysis(p, version(p, 1, null, null), "JUNIOR", START.minusDays(1));
        latestCommitIs("backfill-app", SHA_B, "feat: 최신");

        latestCommit(p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNKNOWN"))
                .andExpect(jsonPath("$.latest.sha").value(SHA_B))
                .andExpect(jsonPath("$.analyzedSha").value(nullValue()))
                .andExpect(jsonPath("$.checkedAt").value(START_TEXT));
    }

    @Test
    void AC9_조회에_실패하면_UNKNOWN이고_latest는_null이다() throws Exception {
        long p = urlProject(me, "down-app");
        analysis(p, version(p, 1, SHA_A, "feat: 처음"), "JUNIOR", START.minusDays(1));
        REPOSITORY.on("/repos/octo/down-app/commits", FakeRepositoryServer.status(502));

        latestCommit(p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNKNOWN"))
                .andExpect(jsonPath("$.latest").value(nullValue()))
                .andExpect(jsonPath("$.analyzedSha").value(SHA_A))
                .andExpect(jsonPath("$.checkedAt").value(START_TEXT));
    }

    @Test
    void AC9_ZIP_프로젝트는_외부_요청_없이_NOT_SUPPORTED() throws Exception {
        long zip = zipProject(me);
        analysis(zip, version(zip, 1, null, null), "JUNIOR", START);

        latestCommit(zip)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_SUPPORTED"))
                .andExpect(jsonPath("$.latest").value(nullValue()))
                .andExpect(jsonPath("$.analyzedSha").value(nullValue()))
                .andExpect(jsonPath("$.checkedAt").value(nullValue()));
        assertThat(REPOSITORY.requests()).isEmpty();
    }

    @Test
    void AC9_남의_프로젝트면_404이고_외부_요청을_하지_않는다() throws Exception {
        long others = urlProject(other, "others-app");
        analysis(others, version(others, 1, SHA_A, "feat: 남"), "JUNIOR", START);
        latestCommitIs("others-app", SHA_B, "feat: 새");

        latestCommit(others)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_404"));
        assertThat(REPOSITORY.requests()).isEmpty();
    }

    // ───────────── AC10 5분 캐시 ─────────────

    @Test
    void AC10_5분_안에는_외부_요청_없이_저장한_결과를_주고_지나면_다시_조회한다() throws Exception {
        long p = urlProject(me, "cache-app");
        analysis(p, version(p, 1, SHA_A, "feat: 처음"), "JUNIOR", START.minusDays(1));
        latestCommitIs("cache-app", SHA_A, "feat: 처음");
        latestCommit(p).andExpect(jsonPath("$.status").value("UP_TO_DATE"));

        latestCommitIs("cache-app", SHA_B, "feat: 새 커밋");
        CLOCK.advance(Duration.ofMinutes(4).plusSeconds(59));
        latestCommit(p)
                .andExpect(jsonPath("$.status").value("UP_TO_DATE"))
                .andExpect(jsonPath("$.latest.sha").value(SHA_A))
                .andExpect(jsonPath("$.checkedAt").value(START_TEXT));
        assertThat(commitRequests("cache-app")).isEqualTo(1);

        CLOCK.advance(Duration.ofSeconds(2));
        latestCommit(p)
                .andExpect(jsonPath("$.status").value("NEW_COMMIT"))
                .andExpect(jsonPath("$.latest.sha").value(SHA_B))
                .andExpect(jsonPath("$.checkedAt").value("2026-11-29T10:05:01+09:00"));
        assertThat(commitRequests("cache-app")).isEqualTo(2);
    }

    @Test
    void AC10_조회_실패도_5분_동안_저장해_다시_요청하지_않는다() throws Exception {
        long p = urlProject(me, "flaky-app");
        analysis(p, version(p, 1, SHA_A, "feat: 처음"), "JUNIOR", START.minusDays(1));
        REPOSITORY.on("/repos/octo/flaky-app/commits", FakeRepositoryServer.status(503));
        latestCommit(p).andExpect(jsonPath("$.status").value("UNKNOWN"));

        latestCommitIs("flaky-app", SHA_B, "feat: 복구");
        CLOCK.advance(Duration.ofMinutes(4));
        latestCommit(p)
                .andExpect(jsonPath("$.status").value("UNKNOWN"))
                .andExpect(jsonPath("$.latest").value(nullValue()))
                .andExpect(jsonPath("$.checkedAt").value(START_TEXT));
        assertThat(commitRequests("flaky-app")).isEqualTo(1);
    }

    // ───────────── AC11 서버 토큰으로 비공개 판정 ─────────────

    @Test
    void AC11_비공개로_바뀌었으면_커밋_정보_없이_UNKNOWN() throws Exception {
        long p = urlProject(me, "private-app");
        analysis(p, version(p, 1, SHA_A, "feat: 처음"), "JUNIOR", START.minusDays(1));
        doThrow(new AnalysisFailedException(ErrorCode.ANALYSIS_004))
                .when(fetcher)
                .latestCommit(any(RepositoryUrl.class));

        latestCommit(p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNKNOWN"))
                .andExpect(jsonPath("$.latest").value(nullValue()))
                .andExpect(jsonPath("$.analyzedSha").value(SHA_A));
    }
}
