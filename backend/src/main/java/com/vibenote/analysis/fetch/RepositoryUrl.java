package com.vibenote.analysis.fetch;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 공개 GitHub·GitLab(gitlab.com) 저장소 주소 (docs/specs/F-05b-analysis-request-url.md 4.1절). 요청 때
 * 검증·정규화하고, 정규화한 주소가 {@code analyses.source_url}이 된다. 다운로드는 이 주소로 하지 않고 조각(owner·repo, 프로젝트 경로)으로
 * 고정 API 주소를 조립한다.
 *
 * <p>{@link java.net.URI}로 파싱하지 않고 직접 나눈다. 조각은 영문·숫자·{@code . _ -}만 허용하므로 퍼센트 인코딩·역슬래시 등 해석 차이를 노리는
 * 입력은 형식 위반이 된다.
 *
 * @param host 저장소 서비스
 * @param segments 경로 조각. GitHub는 [owner, repo], GitLab은 [namespace…, project]
 */
public record RepositoryUrl(Host host, List<String> segments) {

    public static final String EMPTY = "저장소 주소를 입력하세요.";
    public static final String TOO_LONG = "저장소 주소는 500자 이하로 입력하세요.";
    public static final String SCHEME = "https://로 시작하는 GitHub·GitLab 저장소 주소를 입력하세요.";
    public static final String HOST = "GitHub(github.com)나 GitLab(gitlab.com)의 공개 저장소만 분석할 수 있습니다.";
    public static final String PATH_EXTRA =
            "저장소 주소만 입력하세요. 브랜치·파일 경로(/tree/…, /blob/…)는 지원하지 않습니다.";
    public static final String FORMAT = "저장소 주소 형식이 올바르지 않습니다. 예: https://github.com/owner/repo";

    /** {@code analyses.source_url VARCHAR(500)}. */
    static final int MAX_LENGTH = 500;

    /** {@code analyses.title} 길이 (F-05a {@code AnalysisService.MAX_NAME_LENGTH}와 같다). */
    static final int MAX_NAME_LENGTH = 100;

    static final int GITLAB_MAX_SEGMENTS = 20;

    private static final Pattern SCHEME_PREFIX = Pattern.compile("^(?i)(https?)://");
    private static final Pattern GITHUB_OWNER = Pattern.compile("[A-Za-z0-9-]{1,39}");
    private static final Pattern GITHUB_REPO = Pattern.compile("[A-Za-z0-9._-]{1,100}");
    private static final Pattern GITLAB_SEGMENT = Pattern.compile("[A-Za-z0-9._-]{1,255}");
    private static final Set<String> DOT_SEGMENTS = Set.of(".", "..");

    public enum Host {
        GITHUB("github.com"),
        GITLAB("gitlab.com");

        private final String domain;

        Host(String domain) {
            this.domain = domain;
        }
    }

    public RepositoryUrl {
        segments = List.copyOf(segments);
    }

    /**
     * 4.1절 규칙으로 검증하고 정규화한다.
     *
     * @throws InvalidRepositoryUrlException 규칙 위반. 메시지가 {@code details.sourceUrl} 문구다
     */
    public static RepositoryUrl parse(String input) {
        String url = input == null ? "" : input.strip();
        if (url.isEmpty()) {
            throw new InvalidRepositoryUrlException(EMPTY);
        }
        if (url.length() > MAX_LENGTH) {
            throw new InvalidRepositoryUrlException(TOO_LONG);
        }
        Matcher scheme = SCHEME_PREFIX.matcher(url);
        if (!scheme.find()) {
            throw new InvalidRepositoryUrlException(SCHEME);
        }

        String rest = url.substring(scheme.end());
        int authorityEnd = indexOfAny(rest, "/?#");
        String authority = authorityEnd < 0 ? rest : rest.substring(0, authorityEnd);
        String remainder = authorityEnd < 0 ? "" : rest.substring(authorityEnd);
        Host host = host(authority);

        if (indexOfAny(remainder, "?#") >= 0) {
            throw new InvalidRepositoryUrlException(PATH_EXTRA);
        }
        String path = remainder;
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.endsWith(".git")) {
            path = path.substring(0, path.length() - ".git".length());
        }
        List<String> segments =
                List.of(path.isEmpty() ? new String[0] : path.substring(1).split("/", -1));
        if (segments.isEmpty() || segments.contains("")) {
            throw new InvalidRepositoryUrlException(FORMAT);
        }
        return host == Host.GITHUB ? github(segments) : gitlab(segments);
    }

    /** 정규화한 주소. 호스트는 소문자·{@code www.} 없음, 경로 대소문자는 입력 그대로. */
    public String normalized() {
        return "https://" + host.domain + "/" + path();
    }

    /** 조각을 {@code /}로 이은 저장소 경로 ({@code octo/todo-app}, {@code group/sub/my-app}). */
    public String path() {
        return String.join("/", segments);
    }

    /** project_name·title. 마지막 조각을 100자로 자른다. */
    public String name() {
        String last = segments.getLast();
        return last.length() > MAX_NAME_LENGTH ? last.substring(0, MAX_NAME_LENGTH) : last;
    }

    /** 사용자 정보·포트가 있거나 허용 호스트가 아니면 위반. */
    private static Host host(String authority) {
        return switch (authority.toLowerCase(Locale.ROOT)) {
            case "github.com", "www.github.com" -> Host.GITHUB;
            case "gitlab.com" -> Host.GITLAB;
            default -> throw new InvalidRepositoryUrlException(HOST);
        };
    }

    private static RepositoryUrl github(List<String> segments) {
        if (segments.size() > 2) {
            throw new InvalidRepositoryUrlException(PATH_EXTRA);
        }
        if (segments.size() < 2
                || !GITHUB_OWNER.matcher(segments.get(0)).matches()
                || !GITHUB_REPO.matcher(segments.get(1)).matches()
                || DOT_SEGMENTS.contains(segments.get(1))) {
            throw new InvalidRepositoryUrlException(FORMAT);
        }
        return new RepositoryUrl(Host.GITHUB, segments);
    }

    /** {@code /-/tree/…}처럼 {@code -} 조각이 있으면 GitLab의 브랜치·파일 경로다. */
    private static RepositoryUrl gitlab(List<String> segments) {
        if (segments.size() > GITLAB_MAX_SEGMENTS || segments.contains("-")) {
            throw new InvalidRepositoryUrlException(PATH_EXTRA);
        }
        if (segments.size() < 2
                || segments.stream()
                        .anyMatch(
                                s ->
                                        !GITLAB_SEGMENT.matcher(s).matches()
                                                || DOT_SEGMENTS.contains(s))) {
            throw new InvalidRepositoryUrlException(FORMAT);
        }
        return new RepositoryUrl(Host.GITLAB, segments);
    }

    private static int indexOfAny(String s, String chars) {
        for (int i = 0; i < s.length(); i++) {
            if (chars.indexOf(s.charAt(i)) >= 0) {
                return i;
            }
        }
        return -1;
    }
}
