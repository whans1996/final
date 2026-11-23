package com.vibenote.analysis.fetch;

import com.vibenote.analysis.pipeline.AnalysisFailedException;
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
import java.util.Arrays;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.Set;
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

/**
 * 공개 저장소의 기본 브랜치 아카이브 ZIP을 임시 파일로 받는다 (docs/specs/F-05b-analysis-request-url.md 5절, AC4~AC10).
 *
 * <p>SSRF 방어: 첫 요청 주소는 사용자 입력이 아니라 설정의 API 주소에 검증된 조각(owner·repo, 프로젝트 경로)을 붙여 조립한다. 리다이렉트는 직접 따라가며
 * 시작 요청과 같은 스킴·포트이고 {@code allowed-hosts}에 있는 호스트일 때만 요청한다. 토큰은 첫 요청(GitHub api 호스트)에만 붙인다.
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
        boolean done = false;
        // close()가 작업 스레드가 끝날 때까지 기다리므로, 아래 finally는 더 쓰일 일이 없는 파일을 지운다
        try (ExecutorService worker = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> task =
                    worker.submit(
                            () -> {
                                transfer(repository, target);
                                return null;
                            });
            try {
                task.get(properties.timeout().toMillis(), TimeUnit.MILLISECONDS);
                done = true;
            } catch (TimeoutException e) {
                task.cancel(true);
                log.warn("저장소 아카이브 시간 초과: {}", repository.normalized());
                throw new AnalysisFailedException(ErrorCode.ANALYSIS_502);
            } catch (InterruptedException e) {
                task.cancel(true);
                Thread.currentThread().interrupt();
                throw new AnalysisFailedException(ErrorCode.ANALYSIS_502);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof AnalysisFailedException failed) {
                    throw failed;
                }
                log.warn("저장소 아카이브 받기 실패: {}", repository.normalized(), e.getCause());
                throw new AnalysisFailedException(ErrorCode.ANALYSIS_502);
            }
        } finally {
            // 취소된 작업 스레드는 끊긴 본문을 정상 종료(EOF)로 읽고 파일을 남길 수 있다
            if (!done) {
                deleteQuietly(target);
            }
        }
    }

    private void transfer(RepositoryUrl repository, Path target)
            throws IOException, InterruptedException {
        boolean saved = false;
        try {
            URI start = firstUri(repository);
            URI uri = start;
            for (int redirects = 0; ; redirects++) {
                HttpResponse<InputStream> response =
                        client.send(
                                request(repository, uri, redirects == 0),
                                HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream body = response.body()) {
                    int status = response.statusCode();
                    if (REDIRECT_STATUSES.contains(status)) {
                        if (redirects >= properties.maxRedirects()) {
                            log.warn("저장소 아카이브 리다이렉트 상한 초과: {}", repository.normalized());
                            throw new AnalysisFailedException(ErrorCode.ANALYSIS_502);
                        }
                        uri = redirectTarget(start, uri, response);
                        continue;
                    }
                    if (status != 200) {
                        log.info("저장소 아카이브 응답 {}: {}", status, repository.normalized());
                        throw new AnalysisFailedException(classify(response));
                    }
                    save(body, response.headers().firstValueAsLong("Content-Length"), target);
                    saved = true;
                    return;
                }
            }
        } finally {
            if (!saved) {
                deleteQuietly(target);
            }
        }
    }

    /** 고정 API 주소 + 검증된 조각. 사용자가 입력한 주소로는 요청하지 않는다. */
    URI firstUri(RepositoryUrl repository) {
        return switch (repository.host()) {
            case GITHUB ->
                    URI.create(
                            properties.githubApiBase()
                                    + "/repos/"
                                    + repository.path()
                                    + "/zipball");
            case GITLAB ->
                    URI.create(
                            properties.gitlabApiBase()
                                    + "/projects/"
                                    + URLEncoder.encode(repository.path(), StandardCharsets.UTF_8)
                                    + "/repository/archive.zip");
        };
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
