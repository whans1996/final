package com.vibenote.analysis.pipeline;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 저장·LLM 전송 전에 키로 보이는 값을 {@code [REDACTED]}로 바꾼다 (F-05a 5.2절, AC6).
 *
 * <p>모든 파일: 알려진 키 형식. 설정 파일(.yml·.yaml·.properties): 키 이름에 password·secret 등이 들어간 항목의 값. 값이 비어 있거나
 * 기본값 없는 환경변수 참조({@code ${NAME}})면 그대로 두고, {@code ${NAME:기본값}}이면 기본값만 가린다.
 */
public final class SecretRedactor {

    static final String REDACTED = "[REDACTED]";

    private static final List<Pattern> KEY_PATTERNS =
            List.of(
                    // 닫는 줄이 없으면 파일 끝까지 가린다
                    Pattern.compile(
                            "-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?"
                                    + "(?:-----END [A-Z ]*PRIVATE KEY-----|\\z)"),
                    Pattern.compile("AIza[0-9A-Za-z_-]{35}"),
                    // "task-list-..."처럼 단어 중간의 sk-는 키가 아니다
                    Pattern.compile("(?<![A-Za-z0-9])sk-[A-Za-z0-9_-]{20,}"),
                    Pattern.compile("gh[pousr]_[A-Za-z0-9]{36,}"),
                    Pattern.compile("github_pat_[A-Za-z0-9_]{22,}"),
                    Pattern.compile("AKIA[0-9A-Z]{16}"));

    /**
     * group 1: 들여쓰기·키·구분자, group 2: 키, group 3: 값. 키에 '#'이 올 수 없으므로 주석 줄은 걸리지 않는다. 키를 소유 수량자로 읽어
     * 구분자 없는 긴 줄에서도 백트래킹하지 않는다 (ReDoS 방지). 비밀 키워드가 들어 있는지는 매칭 뒤에 검사한다.
     */
    private static final Pattern CONFIG_ENTRY =
            Pattern.compile(
                    "^([ \\t]*+(?:-[ \\t]++)?([^\\s:=#]++)[ \\t]*+[:=][ \\t]*+)(.*)$",
                    Pattern.MULTILINE);

    private static final List<String> SECRET_KEYWORDS =
            List.of("password", "secret", "api-key", "apikey", "api_key", "token", "credential");

    /** 기본값 없는 환경변수 참조 하나: {@code ${NAME}} (따옴표 허용). */
    private static final Pattern PLAIN_ENV_REF =
            Pattern.compile("[ \\t]*['\"]?\\$\\{[^}:]+}['\"]?[ \\t]*");

    /** 기본값 있는 환경변수 참조 하나: group 1 앞부분, group 2 기본값, group 3 뒷부분. */
    private static final Pattern ENV_REF_WITH_DEFAULT =
            Pattern.compile("([ \\t]*['\"]?\\$\\{[^}:]+:)([^}]*)(}['\"]?[ \\t]*)");

    private SecretRedactor() {}

    public static String redact(String path, String content) {
        String result = content;
        for (Pattern pattern : KEY_PATTERNS) {
            result = pattern.matcher(result).replaceAll(Matcher.quoteReplacement(REDACTED));
        }
        if (isConfigFile(path)) {
            result = redactConfigValues(result);
        }
        return result;
    }

    private static boolean isConfigFile(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".yml") || lower.endsWith(".yaml") || lower.endsWith(".properties");
    }

    private static String redactConfigValues(String content) {
        return CONFIG_ENTRY
                .matcher(content)
                .replaceAll(
                        m ->
                                Matcher.quoteReplacement(
                                        isSecretKey(m.group(2))
                                                ? m.group(1) + redactValue(m.group(3))
                                                : m.group()));
    }

    private static boolean isSecretKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return SECRET_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private static String redactValue(String value) {
        if (value.isBlank() || PLAIN_ENV_REF.matcher(value).matches()) {
            return value;
        }
        Matcher withDefault = ENV_REF_WITH_DEFAULT.matcher(value);
        if (withDefault.matches()) {
            return withDefault.group(2).isEmpty()
                    ? value
                    : withDefault.group(1) + REDACTED + withDefault.group(3);
        }
        return REDACTED;
    }
}
