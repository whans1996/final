package com.vibenote.analysis.pipeline;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 저장·LLM 전송 전에 키로 보이는 값을 {@code [REDACTED]}로 바꾼다 (F-05a 5.2절, AC6).
 *
 * <p>모든 파일: 알려진 키 형식. 설정 파일(.yml·.yaml·.properties): 키 이름에 password·secret 등이 들어간 항목의 값. 값이 환경변수
 * 참조({@code ${...}})이거나 비어 있으면 그대로 둔다.
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

    /** group 1: 들여쓰기·키·구분자, group 2: 값. 키에 '#'이 올 수 없으므로 주석 줄은 걸리지 않는다. */
    private static final Pattern CONFIG_SECRET_ENTRY =
            Pattern.compile(
                    "^([ \\t]*(?:-[ \\t]+)?[^\\s:=#]*"
                            + "(?:password|secret|api-key|apikey|api_key|token|credential)"
                            + "[^\\s:=#]*[ \\t]*[:=][ \\t]*)(.*)$",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

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
        return CONFIG_SECRET_ENTRY
                .matcher(content)
                .replaceAll(
                        m -> {
                            String value = m.group(2);
                            String unquoted = value.strip().replaceFirst("^['\"]", "");
                            String kept =
                                    value.isBlank() || unquoted.startsWith("${") ? value : REDACTED;
                            return Matcher.quoteReplacement(m.group(1) + kept);
                        });
    }
}
