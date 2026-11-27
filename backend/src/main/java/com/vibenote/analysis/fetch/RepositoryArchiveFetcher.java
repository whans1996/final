package com.vibenote.analysis.fetch;

import com.vibenote.analysis.pipeline.AnalysisFailedException;
import com.vibenote.common.config.ClockConfig;
import com.vibenote.common.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 공개 저장소의 기본 브랜치 아카이브 ZIP을 임시 파일로 받는다 (docs/specs/F-05b-analysis-request-url.md 5절, AC4~AC10). 기본
 * 브랜치 최신 커밋을 조회하고 그 커밋의 아카이브를 받을 수도 있다 (docs/specs/P-1a-project-versions.md 3절 외부 API,
 * AC2·AC3·AC9·AC11).
 *
 * <p>SSRF 방어: 첫 요청 주소는 사용자 입력이 아니라 설정의 API 주소에 검증된 조각(owner·repo, 프로젝트 경로)을 붙여 조립한다. 리다이렉트는 직접 따라가며
 * 시작 요청과 같은 스킴·포트이고 {@code allowed-hosts}에 있는 호스트일 때만 요청한다. 토큰은 GitHub api 호스트로 보내는 첫 요청에만 붙이고, 토큰을
 * 쓸 때는 먼저 공개 저장소인지 확인한다.
 *
 * <p>본문은 스트리밍으로 쓰며 바이트를 세어 상한을 넘는 순간 멈춘다. 시간 상한은 첫 요청부터 본문 끝까지이며, 넘으면 작업 스레드를 인터럽트해 받기를 끊는다. 실패하면
 * 쓰다 만 파일을 지운 뒤에 돌아온다.
 */
@Component
public class RepositoryArchiveFetcher {

    private static final Logger log = LoggerFactory.getLogger(RepositoryArchiveFetcher.class);

    static final String TOO_LARGE = "저장소 아카이브가 %dMB를 넘습니다. ZIP으로 필요한 코드만 올려 주세요.";
    static final String USER_AGENT = "VibeNote";
    static final String GITHUB_ACCEPT = "application/vnd.github+json";

    private static final byte[] ZIP_SIGNATURE = {0x50, 0x4B, 0x03, 0x04};
    private static final Set<Integer> REDIRECT_STATUSES = Set.of(301, 302, 303, 307, 308);
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_METADATA_BYTES = 1024 * 1024;
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private final FetchProperties properties;
    private final HttpClient client;

    public RepositoryArchiveFetcher(FetchProperties properties) {
        this.properties = properties;
        this.client =
                HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .connectTimeout(properties.connectTimeout())
                        .build();
    }

    static String tooLargeMessage(DataSize max) {
        return TOO_LARGE.formatted(max.toMegabytes());
    }

    /**
     * 아카이브를 {@code target}에 쓴다. 실패하면 {@code target}을 남기지 않는다.
     *
     * @throws AnalysisFailedException 저장소 없음·비공개(ANALYSIS_004), 요청 한도(ANALYSIS_503), 서버 오류·연결 실패·시간
     *     초과·ZIP 아님·허용되지 않은 리다이렉트(ANALYSIS_502), 상한 초과(ANALYSIS_413)
     */
    public void download(RepositoryUrl repository, Path target) {
        download(repository, null, target);
    }

    /**
     * {@code sha} 커밋의 아카이브를 {@code target}에 쓴다. {@code sha}가 null이면 기본 브랜치 아카이브다. 실패하면 {@code
     * target}을 남기지 않는다.
     *
     * @param sha {@link RepositoryCommit#isSha} 형식의 커밋. null이면 기본 브랜치
     * @throws AnalysisFailedException {@link #download(RepositoryUrl, Path)}와 같다
     */
    public void download(RepositoryUrl repository, String sha, Path target) {
        if (sha != null && !RepositoryCommit.isSha(sha)) {
            throw new IllegalArgumentException("커밋 SHA 형식이 아님");
        }
        boolean done = false;
        try {
            // run()은 작업 스레드가 끝난 뒤 돌아오므로, 아래 finally는 더 쓰일 일이 없는 파일을 지운다
            run(
                    properties.timeout(),
                    repository,
                    () -> {
                        transfer(repository, sha, target);
                        return null;
                    });
            done = true;
        } finally {
            // 취소된 작업 스레드는 끊긴 본문을 정상 종료(EOF)로 읽고 파일을 남길 수 있다
            if (!done) {
                deleteQuietly(target);
            }
        }
    }

    /**
     * 기본 브랜치 최신 커밋 (P-1a AC2·AC9). 조회가 실패하면(요청 한도·서버 오류·없는 경로·시간 초과·빈 저장소·형식이 다른 응답) 분석을 멈추지 않도록
     * empty다 (AC3). 서버 토큰을 쓸 때는 받기와 같이 먼저 공개 저장소인지 확인하고, 공개가 아니면(없는 저장소 포함) ANALYSIS_004다 (AC11).
     *
     * @throws AnalysisFailedException ANALYSIS_004 (토큰을 쓸 때 공개 저장소가 아님)
     */
    public Optional<RepositoryCommit> latestCommit(RepositoryUrl repository) {
        try {
            return run(properties.commitTimeout(), repository, () -> lookUpCommit(repository));
        } catch (AnalysisFailedException e) {
            // 커밋 목록 요청의 실패는 lookUpCommit이 empty로 바꾸므로 여기 오는 ANALYSIS_004는 공개 확인 결과다
            if (e.code() == ErrorCode.ANALYSIS_004) {
                throw e;
            }
            log.info("최신 커밋 조회 실패({}): {}", e.code(), repository.normalized());
            return Optional.empty();
        }
    }

    private Optional<RepositoryCommit> lookUpCommit(RepositoryUrl repository)
            throws IOException, InterruptedException {
        if (repository.host() == RepositoryUrl.Host.GITHUB && properties.hasGithubToken()) {
            ensurePublic(repository);
        }
        try {
            HttpResponse<InputStream> response = follow(repository, commitsUri(repository));
            try (InputStream body = response.body()) {
                return parseCommit(repository.host(), body.readNBytes(MAX_METADATA_BYTES));
            }
        } catch (AnalysisFailedException e) {
            log.info("최신 커밋 조회 실패({}): {}", e.code(), repository.normalized());
            return Optional.empty();
        }
    }

    /**
     * 커밋 목록 첫 항목. GitHub는 {@code sha}·{@code commit.message}·{@code commit.committer.date}, GitLab은
     * {@code id}·{@code title}·{@code committed_date}. 빈 목록이거나 SHA 형식이 아니면 empty, 시각을 읽지 못하면 시각만
     * null이다.
     */
    static Optional<RepositoryCommit> parseCommit(RepositoryUrl.Host host, byte[] json) {
        JsonNode first;
        try {
            JsonNode list = JSON.readTree(json);
            if (list == null || !list.isArray() || list.isEmpty()) {
                return Optional.empty();
            }
            first = list.get(0);
        } catch (JacksonException e) {
            return Optional.empty();
        }
        String sha;
        String message;
        String date;
        if (host == RepositoryUrl.Host.GITHUB) {
            sha = text(first.path("sha"));
            message = text(first.path("commit").path("message"));
            date = text(first.path("commit").path("committer").path("date"));
        } else {
            sha = text(first.path("id"));
            message = text(first.path("title"));
            date = text(first.path("committed_date"));
        }
        if (!RepositoryCommit.isSha(sha)) {
            return Optional.empty();
        }
        return Optional.of(new RepositoryCommit(sha, message, committedAt(date)));
    }

    private static String text(JsonNode node) {
        return node.isString() ? node.asString() : null;
    }

    private static LocalDateTime committedAt(String date) {
        if (date == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(date)
                    .atZoneSameInstant(ClockConfig.ZONE)
                    .toLocalDateTime()
                    .truncatedTo(ChronoUnit.SECONDS);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 작업을 가상 스레드에서 {@code timeout}까지 기다린다. 넘으면 작업 스레드를 인터럽트해 끊고 ANALYSIS_502다. 작업이 던진
     * AnalysisFailedException은 그대로, 그 밖의 실패는 ANALYSIS_502다. 돌아올 때는 작업 스레드가 끝나 있다.
     */
    private <T> T run(Duration timeout, RepositoryUrl repository, Callable<T> work) {
        // close()가 작업 스레드가 끝날 때까지 기다린다
        try (ExecutorService worker = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<T> task = worker.submit(work);
            try {
                return task.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                task.cancel(true);
                log.warn("저장소 요청 시간 초과: {}", repository.normalized());
                throw new AnalysisFailedException(ErrorCode.ANALYSIS_502);
            } catch (InterruptedException e) {
                task.cancel(true);
                Thread.currentThread().interrupt();
                throw new AnalysisFailedException(ErrorCode.ANALYSIS_502);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof AnalysisFailedException failed) {
                    throw failed;
                }
                log.warn("저장소 요청 실패: {}", repository.normalized(), e.getCause());
                throw new AnalysisFailedException(ErrorCode.ANALYSIS_502);
            }
        }
    }

    private void transfer(RepositoryUrl repository, String sha, Path target)
            throws IOException, InterruptedException {
        boolean saved = false;
        try {
            if (repository.host() == RepositoryUrl.Host.GITHUB && properties.hasGithubToken()) {
                ensurePublic(repository);
            }
            HttpResponse<InputStream> response = follow(repository, archiveUri(repository, sha));
            try (InputStream body = response.body()) {
                save(body, response.headers().firstValueAsLong("Content-Length"), target);
                saved = true;
            }
        } finally {
            if (!saved) {
                deleteQuietly(target);
            }
        }
    }

    /**
     * 서버 토큰은 비공개 저장소도 읽을 수 있으므로, 토큰을 쓸 때는 받기 전에 공개 저장소인지 확인한다. 공개가 아니면 비공개 저장소와 같은 ANALYSIS_004다
     * (PR #82 Codex 리뷰). 토큰이 없으면 비인증 요청이라 공개 저장소만 받을 수 있다.
     */
    private void ensurePublic(RepositoryUrl repository) throws IOException, InterruptedException {
        URI uri = URI.create(githubRepo(repository));
        HttpResponse<InputStream> response = follow(repository, uri);
        try (InputStream body = response.body()) {
            if (!isPublic(body.readNBytes(MAX_METADATA_BYTES))) {
                log.info("공개 저장소가 아니라 받지 않음: {}", repository.normalized());
                throw new AnalysisFailedException(ErrorCode.ANALYSIS_004);
            }
        }
    }

    /** {@code visibility}가 {@code public}이거나, 그 필드가 없으면 {@code private: false}일 때만 공개다. */
    private static boolean isPublic(byte[] json) {
        JsonNode repo;
        try {
            repo = JSON.readTree(json);
        } catch (JacksonException e) {
            throw new AnalysisFailedException(ErrorCode.ANALYSIS_502);
        }
        JsonNode visibility = repo.get("visibility");
        if (visibility != null) {
            return "public".equals(visibility.asString());
        }
        JsonNode isPrivate = repo.get("private");
        return isPrivate != null && isPrivate.isBoolean() && !isPrivate.booleanValue();
    }

    /** 허용된 리다이렉트를 따라가 200 응답을 준다. 200이 아니면 상태로 분류해 실패한다. 본문은 호출자가 닫는다. */
    private HttpResponse<InputStream> follow(RepositoryUrl repository, URI start)
            throws IOException, InterruptedException {
        URI uri = start;
        for (int redirects = 0; ; redirects++) {
            HttpResponse<InputStream> response =
                    client.send(
                            request(repository, uri, redirects == 0),
                            HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            if (status == 200) {
                return response;
            }
            try (InputStream body = response.body()) {
                if (!REDIRECT_STATUSES.contains(status)) {
                    log.info("저장소 응답 {}: {}", status, repository.normalized());
                    throw new AnalysisFailedException(classify(response));
                }
                if (redirects >= properties.maxRedirects()) {
                    log.warn("저장소 리다이렉트 상한 초과: {}", repository.normalized());
                    throw new AnalysisFailedException(ErrorCode.ANALYSIS_502);
                }
                uri = redirectTarget(start, uri, response);
            }
        }
    }

    /** 고정 API 주소 + 검증된 조각. 사용자가 입력한 주소로는 요청하지 않는다. */
    URI firstUri(RepositoryUrl repository) {
        return archiveUri(repository, null);
    }

    /** {@code sha}는 형식을 확인한 값만 온다. null이면 기본 브랜치. */
    URI archiveUri(RepositoryUrl repository, String sha) {
        return switch (repository.host()) {
            case GITHUB ->
                    URI.create(
                            githubRepo(repository) + "/zipball" + (sha == null ? "" : "/" + sha));
            case GITLAB ->
                    URI.create(
                            gitlabProject(repository)
                                    + "/repository/archive.zip"
                                    + (sha == null ? "" : "?sha=" + sha));
        };
    }

    /** 기본 브랜치 커밋 목록 첫 항목. */
    URI commitsUri(RepositoryUrl repository) {
        return switch (repository.host()) {
            case GITHUB -> URI.create(githubRepo(repository) + "/commits?per_page=1");
            case GITLAB -> URI.create(gitlabProject(repository) + "/repository/commits?per_page=1");
        };
    }

    private String githubRepo(RepositoryUrl repository) {
        return properties.githubApiBase() + "/repos/" + repository.path();
    }

    private String gitlabProject(RepositoryUrl repository) {
        return properties.gitlabApiBase()
                + "/projects/"
                + URLEncoder.encode(repository.path(), StandardCharsets.UTF_8);
    }

    private HttpRequest request(RepositoryUrl repository, URI uri, boolean first) {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(uri).header("User-Agent", USER_AGENT).GET();
        if (first && repository.host() == RepositoryUrl.Host.GITHUB) {
            builder.header("Accept", GITHUB_ACCEPT);
            if (properties.hasGithubToken()) {
                builder.header("Authorization", "Bearer " + properties.githubToken());
            }
        }
        return builder.build();
    }

    private URI redirectTarget(URI start, URI current, HttpResponse<?> response) {
        String location =
                response.headers()
                        .firstValue("Location")
                        .filter(l -> !l.isBlank())
                        .orElseThrow(() -> new AnalysisFailedException(ErrorCode.ANALYSIS_502));
        URI next;
        try {
            next = current.resolve(location);
        } catch (IllegalArgumentException e) {
            throw new AnalysisFailedException(ErrorCode.ANALYSIS_502);
        }
        if (!allowed(start, next)) {
            log.warn("허용되지 않은 리다이렉트를 따라가지 않음: host={}", next.getHost());
            throw new AnalysisFailedException(ErrorCode.ANALYSIS_502);
        }
        return next;
    }

    /** 시작 요청과 같은 스킴·포트, 사용자 정보 없음, 허용 호스트. */
    private boolean allowed(URI start, URI next) {
        return start.getScheme().equalsIgnoreCase(next.getScheme())
                && next.getRawUserInfo() == null
                && next.getHost() != null
                && properties.allowedHosts().contains(next.getHost().toLowerCase(Locale.ROOT))
                && port(next) == port(start);
    }

    private static int port(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    /** 429와 남은 한도가 0인 403은 요청 한도, 그 밖 4xx는 저장소 없음, 나머지는 서버 오류. */
    private static ErrorCode classify(HttpResponse<?> response) {
        int status = response.statusCode();
        boolean rateLimited =
                status == 429
                        || (status == 403
                                && response.headers()
                                        .firstValue("x-ratelimit-remaining")
                                        .map(String::strip)
                                        .filter("0"::equals)
                                        .isPresent());
        if (rateLimited) {
            return ErrorCode.ANALYSIS_503;
        }
        return status >= 400 && status < 500 ? ErrorCode.ANALYSIS_004 : ErrorCode.ANALYSIS_502;
    }

    /** Content-Length를 먼저 보고, 헤더가 없거나 틀려도 실제로 받은 바이트로 상한을 센다. 정확히 상한이면 통과한다. */
    private void save(InputStream body, OptionalLong contentLength, Path target)
            throws IOException {
        long max = properties.maxArchiveBytes().toBytes();
        if (contentLength.isPresent() && contentLength.getAsLong() > max) {
            throw tooLarge();
        }
        byte[] head = body.readNBytes(ZIP_SIGNATURE.length);
        if (!Arrays.equals(head, ZIP_SIGNATURE)) {
            throw new AnalysisFailedException(ErrorCode.ANALYSIS_502);
        }
        Files.createDirectories(target.getParent());
        try (OutputStream out = Files.newOutputStream(target)) {
            out.write(head);
            long total = head.length;
            byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            while ((n = body.read(buffer)) != -1) {
                total += n;
                if (total > max) {
                    throw tooLarge();
                }
                out.write(buffer, 0, n);
            }
        }
    }

    private AnalysisFailedException tooLarge() {
        return new AnalysisFailedException(
                ErrorCode.ANALYSIS_413, tooLargeMessage(properties.maxArchiveBytes()));
    }

    private static void deleteQuietly(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("쓰다 만 저장소 아카이브 삭제 실패: {}", target, e);
        }
    }
}
