package com.vibenote.analysis.fetch;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * 저장소 아카이브 수집 설정 ({@code analysis.fetch.*}, F-05b 5.1절). 테스트는 base·allowedHosts를 가짜 서버로 바꾼다.
 *
 * @param githubApiBase GitHub API 주소. 끝에 {@code /}를 붙이지 않는다
 * @param gitlabApiBase GitLab API 주소 ({@code …/api/v4})
 * @param allowedHosts 리다이렉트를 따라갈 수 있는 호스트 (소문자)
 * @param githubToken 선택. 있으면 GitHub api 호스트 요청에만 붙인다. 로그·{@link #toString()}에 남기지 않는다
 * @param maxArchiveBytes 아카이브 상한. 넘으면 ANALYSIS_413
 * @param connectTimeout 연결 시간 상한
 * @param timeout 첫 요청부터 본문 끝까지의 시간 상한. 넘으면 ANALYSIS_502
 * @param maxRedirects 따라가는 리다이렉트 수 상한
 */
@ConfigurationProperties("analysis.fetch")
public record FetchProperties(
        @DefaultValue("https://api.github.com") String githubApiBase,
        @DefaultValue("https://gitlab.com/api/v4") String gitlabApiBase,
        @DefaultValue({"api.github.com", "codeload.github.com", "gitlab.com"})
                List<String> allowedHosts,
        String githubToken,
        @DefaultValue("30MB") DataSize maxArchiveBytes,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("60s") Duration timeout,
        @DefaultValue("3") int maxRedirects) {

    public FetchProperties {
        allowedHosts = List.copyOf(allowedHosts);
    }

    /** 토큰이 설정되어 있는가 (빈 값·공백은 없음). */
    public boolean hasGithubToken() {
        return githubToken != null && !githubToken.isBlank();
    }

    @Override
    public String toString() {
        return "FetchProperties[githubApiBase=%s, gitlabApiBase=%s, allowedHosts=%s, githubToken=%s, maxArchiveBytes=%s, connectTimeout=%s, timeout=%s, maxRedirects=%d]"
                .formatted(
                        githubApiBase,
                        gitlabApiBase,
                        allowedHosts,
                        hasGithubToken() ? "****" : "(none)",
                        maxArchiveBytes,
                        connectTimeout,
                        timeout,
                        maxRedirects);
    }
}
