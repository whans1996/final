package com.vibenote.analysis.pipeline;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 저장·LLM 전송 전에 키로 보이는 값을 {@code [REDACTED]}로 바꾼다 (F-05a 5.2절, AC6).
 *
 * <p>모든 파일: 알려진 키 형식. 설정 파일(.yml·.yaml·.properties): 키 이름에 password·secret 등이 들어가거나 마지막 단어가 key인
 * 항목의 값. 값이 비어 있거나 기본값 없는 환경변수 참조({@code ${NAME}})면 그대로 두고, {@code ${NAME:기본값}}이면 기본값만 가린다.
 *
 * <p>설정 파일은 두 번 가린다. 1차는 줄 단위(형식이 깨져도 동작), 2차는 실제 파서로 비밀 키 아래 값을 모아 본문 전체에서 가린다({@link
 * ConfigSecretValues}).
 */
public final class SecretRedactor {

    static final String REDACTED = "[REDACTED]";

    /** 2차 마스킹 토큰 상한. 넘으면 비정상 설정으로 보고 파일 전체를 가린다 (토큰 수 × 본문 길이 비용 제한). */
    static final int MAX_SECRET_TOKENS = 200;

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
     * 한 줄 전체. group 1: 들여쓰기·키·구분자, group 2: 키, group 3: 값. 키에 '#'·'{'가 올 수 없으므로 주석 줄과 흐름 매핑 줄(- {a:
     * b})은 걸리지 않는다. 키를 소유 수량자로 읽어 구분자 없는 긴 줄에서도 백트래킹하지 않는다 (ReDoS 방지). 비밀 키워드가 들어 있는지는 매칭 뒤에 검사한다.
     */
    private static final Pattern CONFIG_ENTRY =
            Pattern.compile("([ \\t]*+(?:-[ \\t]++)?([^\\s:=#{]++)[ \\t]*+[:=][ \\t]*+)(.*)");

    /** .properties 한 줄. {@link #CONFIG_ENTRY}와 같되 키와 값을 공백만으로도 나눌 수 있다 ({@code key value}). */
    private static final Pattern PROPERTIES_ENTRY =
            Pattern.compile("([ \\t]*+([^\\s:=#!]++)(?:[ \\t]*+[:=][ \\t]*+|[ \\t]++))(.*)");

    /**
     * YAML 흐름 매핑 안의 항목 하나({@code {key: value, ...}}). group 1: 여는 기호·키·구분자, group 2: 키, group 3: 값.
     * 따옴표 문자열과 {@code ${...}} 안의 쉼표·중괄호는 값에 포함하고, 길이를 제한해 긴 줄에서도 선형 시간에 끝난다. 값에 '{'를 넣지 않아 중첩 매핑은
     * 다음 매칭에서 따로 검사한다.
     */
    private static final Pattern YAML_FLOW_ENTRY =
            Pattern.compile(
                    "([{,][ \\t]*+['\"]?([^\\s:,{}\\[\\]'\"]++)['\"]?[ \\t]*+:[ \\t]*+)"
                            + "((?:\"[^\"\\n]{0,4096}+\"|'[^'\\n]{0,4096}+'"
                            + "|\\$\\{[^}\\n]{0,256}+}|[^,{}\\n])*+)");

    /** YAML 블록 스칼라 지시자({@code |}, {@code >-} 등). 값은 다음 줄부터 더 깊은 들여쓰기로 이어진다. */
    private static final Pattern YAML_BLOCK_SCALAR =
            Pattern.compile("[|>][-+0-9]{0,2}[ \\t]*(?:#.*)?");

    private static final List<String> SECRET_KEYWORDS =
            List.of(
                    "password",
                    "secret",
                    "api-key",
                    "apikey",
                    "api_key",
                    "token",
                    "credential",
                    "privatekey",
                    "accesskey");

    /**
     * 마지막 단어가 key인 키 이름 ({@code key}, {@code private-key}, {@code ACCESS_KEY}, {@code aws.key},
     * {@code signingKey}). 단어 경계를 보므로 monkey·hockey는 걸리지 않는다 (#16).
     */
    private static final Pattern KEY_SUFFIX = Pattern.compile("(?:^|[-_.])(?i:key)$|[a-z0-9]Key$");

    /**
     * key로 끝나도 비밀이 아닌 이름. 마지막 점 뒤 부분에서 구분자를 빼고 소문자로 비교한다 (#16). primary-key는 Azure 계정 키 이름이라 넣지
     * 않는다.
     */
    private static final Set<String> NON_SECRET_KEY_NAMES =
            Set.of("cachekey", "routingkey", "partitionkey", "sortkey", "foreignkey", "publickey");

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
        String lower = path.toLowerCase(Locale.ROOT);
        boolean yaml = lower.endsWith(".yml") || lower.endsWith(".yaml");
        if (yaml || lower.endsWith(".properties")) {
            Set<String> secrets =
                    yaml
                            ? ConfigSecretValues.fromYaml(result)
                            : ConfigSecretValues.fromProperties(result);
            // 이스케이프·줄 이음으로 해석한 값이 원문 글자와 다르면 위치를 찾을 수 없으므로 파일 전체를 가린다
            String source = result;
            if (secrets.stream().anyMatch(secret -> !source.contains(secret))) {
                return REDACTED;
            }
            result = redactEverywhere(redactConfigValues(result, yaml), secrets);
        }
        return result;
    }

    /** 파서로 모은 비밀 토큰을 단어 경계 기준으로 본문 전체에서 가린다. 긴 토큰부터 맞춘다. */
    private static String redactEverywhere(String content, Set<String> secrets) {
        if (secrets.isEmpty()) {
            return content;
        }
        if (secrets.size() > MAX_SECRET_TOKENS) {
            return REDACTED;
        }
        String alternatives =
                secrets.stream()
                        .sorted(
                                Comparator.comparingInt(String::length)
                                        .reversed()
                                        .thenComparing(Comparator.naturalOrder()))
                        .map(Pattern::quote)
                        .collect(Collectors.joining("|"));
        return Pattern.compile("(?<![A-Za-z0-9_])(?:" + alternatives + ")(?![A-Za-z0-9_])")
                .matcher(content)
                .replaceAll(Matcher.quoteReplacement(REDACTED));
    }

    /**
     * 줄 단위로 비밀 항목의 값을 가린다. 줄 수와 CRLF는 유지한다.
     *
     * <ul>
     *   <li>YAML 블록 스칼라: 지시자를 남기고 이어지는 더 깊은 줄을 모두 가린다
     *   <li>YAML 흐름 매핑({@code {password: x}}): 안쪽 항목도 검사한다
     *   <li>.properties 줄 이음({@code \}로 끝남): 이어지는 줄까지 가린다
     * </ul>
     */
    private static String redactConfigValues(String content, boolean yaml) {
        String[] lines = content.split("\n", -1);
        // 가리는 중인 블록 스칼라 키의 들여쓰기. 블록 밖이면 -1
        int blockIndent = -1;
        // 비밀 값이 '\'로 다음 줄에 이어지는 중 (.properties)
        boolean continued = false;
        for (int i = 0; i < lines.length; i++) {
            boolean crlf = lines[i].endsWith("\r");
            String line = crlf ? lines[i].substring(0, lines[i].length() - 1) : lines[i];
            String eol = crlf ? "\r" : "";
            if (continued) {
                continued = endsWithContinuation(line);
                lines[i] = line.substring(0, indentOf(line)) + REDACTED + eol;
                continue;
            }
            if (blockIndent >= 0) {
                if (line.isBlank()) {
                    continue;
                }
                int indent = indentOf(line);
                if (indent > blockIndent) {
                    lines[i] = line.substring(0, indent) + REDACTED + eol;
                    continue;
                }
                blockIndent = -1;
            }
            if (yaml && line.indexOf('{') >= 0) {
                line = redactFlowEntries(line);
            }
            Matcher m = (yaml ? CONFIG_ENTRY : PROPERTIES_ENTRY).matcher(line);
            if (m.matches() && isSecretKey(m.group(2))) {
                if (yaml && YAML_BLOCK_SCALAR.matcher(m.group(3)).matches()) {
                    blockIndent = m.start(2);
                } else {
                    continued = !yaml && endsWithContinuation(m.group(3));
                    line = m.group(1) + redactValue(m.group(3));
                }
            }
            lines[i] = line + eol;
        }
        return String.join("\n", lines);
    }

    private static String redactFlowEntries(String line) {
        return YAML_FLOW_ENTRY
                .matcher(line)
                .replaceAll(
                        m ->
                                Matcher.quoteReplacement(
                                        isSecretKey(m.group(2))
                                                ? m.group(1) + redactValue(m.group(3))
                                                : m.group()));
    }

    /** 끝의 '\'가 홀수 개면 다음 줄로 이어진다 ('\\'는 글자 '\'). */
    private static boolean endsWithContinuation(String line) {
        int backslashes = 0;
        for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return backslashes % 2 == 1;
    }

    private static int indentOf(String line) {
        int indent = 0;
        while (indent < line.length()
                && (line.charAt(indent) == ' ' || line.charAt(indent) == '\t')) {
            indent++;
        }
        return indent;
    }

    static boolean isSecretKey(String key) {
        // 줄 단위 1차 마스킹은 YAML 키를 따옴표째 넘긴다
        String name = key.replaceAll("^['\"]|['\"]$", "");
        String lower = name.toLowerCase(Locale.ROOT);
        if (SECRET_KEYWORDS.stream().anyMatch(lower::contains)) {
            return true;
        }
        String lastName = lower.substring(lower.lastIndexOf('.') + 1).replaceAll("[-_]", "");
        return KEY_SUFFIX.matcher(name).find() && !NON_SECRET_KEY_NAMES.contains(lastName);
    }

    /** 값에서 가려야 할 부분. 비었거나 기본값 없는 환경변수 참조면 null, {@code ${NAME:기본값}}이면 기본값(비면 null). */
    static String secretPartOf(String value) {
        if (value.isBlank() || PLAIN_ENV_REF.matcher(value).matches()) {
            return null;
        }
        Matcher withDefault = ENV_REF_WITH_DEFAULT.matcher(value);
        if (withDefault.matches()) {
            return withDefault.group(2).isEmpty() ? null : withDefault.group(2);
        }
        return value;
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
