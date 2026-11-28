package com.vibenote.analysis.concept;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 개념 카드 카테고리 12종 (K-1 AC5). 관리자 검증 정규식({@code AdminConceptRequest.CATEGORY_REGEX}), concept 스키마
 * {@code enum}, FE {@code utils/conceptCategories.js}, 시드 {@code CATEGORY_ORDER}가 모두 이 목록과 같다 (테스트로
 * 묶는다).
 */
public final class ConceptCategories {

    /** 화면·시드 생성 순서. */
    public static final List<String> ORDERED =
            List.of(
                    "java",
                    "spring",
                    "web",
                    "security",
                    "mybatis",
                    "vue",
                    "database",
                    "javascript",
                    "tooling",
                    "typescript",
                    "react",
                    "cs");

    public static final Set<String> ALL = Set.copyOf(ORDERED);

    /** LLM이 12종 밖으로 쓴 값의 별칭 (K-1 AC8, spec 6절 표). 키는 {@link #normalize}의 정규화 결과다. */
    private static final Map<String, String> ALIASES =
            Map.ofEntries(
                    Map.entry("db", "database"),
                    Map.entry("sql", "database"),
                    Map.entry("mysql", "database"),
                    Map.entry("mariadb", "database"),
                    Map.entry("rdb", "database"),
                    Map.entry("rdbms", "database"),
                    Map.entry("jpa", "database"),
                    Map.entry("jdbc", "database"),
                    Map.entry("js", "javascript"),
                    Map.entry("ecmascript", "javascript"),
                    Map.entry("node", "javascript"),
                    Map.entry("nodejs", "javascript"),
                    Map.entry("ts", "typescript"),
                    Map.entry("reactjs", "react"),
                    Map.entry("vuejs", "vue"),
                    Map.entry("pinia", "vue"),
                    Map.entry("springboot", "spring"),
                    Map.entry("springframework", "spring"),
                    Map.entry("git", "tooling"),
                    Map.entry("docker", "tooling"),
                    Map.entry("gradle", "tooling"),
                    Map.entry("maven", "tooling"),
                    Map.entry("npm", "tooling"),
                    Map.entry("vite", "tooling"),
                    Map.entry("build", "tooling"),
                    Map.entry("tool", "tooling"),
                    Map.entry("tools", "tooling"),
                    Map.entry("devops", "tooling"),
                    Map.entry("jvm", "java"));

    private static final Pattern SEPARATORS = Pattern.compile("[\\s.\\-_]+");

    private ConceptCategories() {}

    /** 소문자로 바꾸고 공백·{@code .}·{@code -}·{@code _}를 뗀 뒤 12종이면 그대로, 별칭이면 바꾼 값. 그 밖이면 빈 값 (K-1 AC8). */
    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String key = SEPARATORS.matcher(raw.toLowerCase(Locale.ROOT)).replaceAll("");
        if (ALL.contains(key)) {
            return Optional.of(key);
        }
        return Optional.ofNullable(ALIASES.get(key));
    }
}
