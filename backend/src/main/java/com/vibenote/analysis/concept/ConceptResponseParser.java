package com.vibenote.analysis.concept;

import static com.vibenote.analysis.explain.LlmJsonCall.cut;

import com.vibenote.analysis.concept.ExtractedConcept.NewConcept;
import com.vibenote.analysis.explain.LlmJsonCall.InvalidResponseException;
import com.vibenote.common.Level;
import com.vibenote.concept.ConceptStatus;
import com.vibenote.concept.domain.Concept;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

/**
 * 개념 응답(5.3절)을 검증하고 저장할 항목으로 정리한다 (F-08 AC4·AC5·AC6, K-1 AC7~AC10). 형식이 틀리면({@code concepts}가 배열이
 * 아님) {@link InvalidResponseException}을 던져 재시도하게 하고, 항목 단위의 오류는 그 항목만 버린다. 버린 카테고리를 WARN 로그로 남기는 것
 * 말고는 순수 함수다.
 */
public final class ConceptResponseParser {

    private static final Logger log = LoggerFactory.getLogger(ConceptResponseParser.class);

    static final int MAX_NAME = 100;
    static final int MAX_SUMMARY = 500;
    static final int MAX_BODY = 2000;
    static final int MAX_CONTEXT_NOTE = 500;

    /** {@code concepts.summary_* VARCHAR(300)} (K-1 AC7). */
    static final int MAX_LEVEL_SUMMARY = 300;

    /** 로그에 남기는 원래 카테고리 길이. */
    private static final int MAX_LOGGED_CATEGORY = 50;

    /** 괄호 안 키의 길이 (정규화한 뒤, K-1 5.4절 개정). */
    private static final int MIN_INNER_KEY = 2;

    private static final int MAX_INNER_KEY = 30;

    /** 괄호 안에 이 글자가 있으면 여러 개념을 나열한 것이라 안 키를 만들지 않는다. */
    private static final Pattern LIST_SEPARATOR = Pattern.compile("[·,/]");

    /** {@code analysis_concepts.line_hint VARCHAR(50)}. */
    static final int MAX_LINE_HINT = 50;

    private static final Pattern LINE_HINT = Pattern.compile("^L\\d+(-L\\d+)?$");

    /** 끝의 {@code (…)} 부분. 앞부분 키와 괄호 안 키를 만든다 (5.4절). */
    private static final Pattern TRAILING_PAREN = Pattern.compile("^(.*?)\\s*\\(([^()]*)\\)\\s*$");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private ConceptResponseParser() {}

    /**
     * @param input 이번 프롬프트의 입력 (카드 id, 파일 번호 → id, 코드가 들어간 파일 번호)
     * @param existing 이름 비교에 쓸 기존 카드 전체 (HIDDEN 포함)
     * @param level 분석 수준. 새 초안의 {@code difficulty}가 틀리면 이 값을 쓴다
     * @param analysisId 버린 초안을 로그에 남길 때 쓴다 (평가 러너처럼 저장하지 않은 분석이면 null)
     */
    public static List<ExtractedConcept> parse(
            JsonNode root,
            ConceptPrompts.Input input,
            List<Concept> existing,
            Level level,
            int maxLinks,
            int maxDrafts,
            Long analysisId) {
        JsonNode items = root.get("concepts");
        if (!root.isObject() || items == null || !items.isArray()) {
            throw new InvalidResponseException("concepts가 배열이 아님");
        }
        NameIndex names = new NameIndex(existing);
        Set<String> draftKeys = new HashSet<>();
        Set<Long> linked = new HashSet<>();
        List<ExtractedConcept> result = new ArrayList<>();
        int drafts = 0;
        for (JsonNode item : items) {
            if (result.size() >= maxLinks) {
                break;
            }
            if (!item.isObject()) {
                continue;
            }
            Long fileId = fileId(item, input);
            String lineHint = fileId == null ? null : lineHint(item, input);
            String note = optionalText(item, "contextNote", MAX_CONTEXT_NOTE);

            JsonNode conceptId = item.get("conceptId");
            if (conceptId != null && conceptId.isIntegralNumber()) {
                // long 범위를 넘는 정수는 asLong()이 예외를 던져 응답 전체가 형식 오류가 되므로 그 항목만 버린다
                long id = conceptId.canConvertToLong() ? conceptId.asLong() : -1;
                if (input.catalogIds().contains(id) && linked.add(id)) {
                    result.add(new ExtractedConcept(id, null, fileId, lineHint, note));
                }
                continue;
            }
            NewConcept draft = draft(item.get("new"), level, analysisId);
            if (draft == null) {
                continue;
            }
            NameKeys keys = NameKeys.of(draft.name());
            Concept same = names.find(keys);
            if (same != null) {
                // 맞는 카드가 숨긴 카드뿐이면 분석·퀴즈·복습에 다시 들어오지 않게 연결도 새 초안도 만들지 않는다 (K-1 AC10)
                if (same.getStatus() != ConceptStatus.HIDDEN && linked.add(same.getId())) {
                    result.add(new ExtractedConcept(same.getId(), null, fileId, lineHint, note));
                }
                continue;
            }
            // 초안끼리는 전체 이름·앞부분만 비교한다 ("(Java 21)"처럼 괄호 안만 같은 서로 다른 개념을 합치지 않게)
            List<String> ownKeys = keys.fullAndFront();
            if (ownKeys.stream().anyMatch(draftKeys::contains) || drafts >= maxDrafts) {
                continue;
            }
            draftKeys.addAll(ownKeys);
            drafts++;
            result.add(new ExtractedConcept(null, draft, fileId, lineHint, note));
        }
        return result;
    }

    /** {@code fileNo}가 1..n이면 그 파일의 id. */
    private static Long fileId(JsonNode item, ConceptPrompts.Input input) {
        JsonNode no = item.get("fileNo");
        if (no == null || !no.isIntegralNumber() || !no.canConvertToLong()) {
            return null;
        }
        long n = no.asLong();
        return n >= 1 && n <= input.fileIds().size() ? input.fileIds().get((int) n - 1) : null;
    }

    /** 형식이 맞고 그 파일의 코드가 프롬프트에 들어갔을 때만. */
    private static String lineHint(JsonNode item, ConceptPrompts.Input input) {
        JsonNode value = item.get("lineHint");
        if (value == null || !value.isString()) {
            return null;
        }
        String hint = value.asString().strip();
        int no = item.get("fileNo").asInt();
        return hint.length() <= MAX_LINE_HINT
                        && LINE_HINT.matcher(hint).matches()
                        && input.codeFileNos().contains(no)
                ? hint
                : null;
    }

    /** 문자열이면 앞뒤 공백을 떼고 {@code max}에서 자른다. 없거나 비면 null. */
    private static String optionalText(JsonNode node, String field, int max) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString()) {
            return null;
        }
        String text = value.asString().strip();
        return text.isEmpty() ? null : cut(text, max).strip();
    }

    /**
     * 새 개념. 이름·요약·카테고리가 틀리면 null (그 항목을 버린다). 카테고리는 별칭을 12종으로 바꾸고, 그래도 밖이면 WARN 로그를 남긴다 (K-1 AC8.
     * 코드 본문은 남기지 않는다).
     */
    private static NewConcept draft(JsonNode node, Level level, Long analysisId) {
        if (node == null || !node.isObject()) {
            return null;
        }
        String name = optionalText(node, "name", Integer.MAX_VALUE);
        String summary = optionalText(node, "summary", MAX_SUMMARY);
        JsonNode category = node.get("category");
        if (name == null
                || name.length() > MAX_NAME
                || summary == null
                || category == null
                || !category.isString()) {
            return null;
        }
        Optional<String> normalized = ConceptCategories.normalize(category.asString());
        if (normalized.isEmpty()) {
            log.warn(
                    "분석 {} 개념 초안 '{}'을 버렸다: 카테고리 '{}'가 12종 밖",
                    analysisId,
                    name,
                    cut(category.asString(), MAX_LOGGED_CATEGORY));
            return null;
        }
        JsonNode levels = node.get("levelSummaries");
        boolean hasLevels = levels != null && levels.isObject();
        return new NewConcept(
                name,
                normalized.get(),
                difficulty(node.get("difficulty"), level),
                summary,
                optionalText(node, "body", MAX_BODY),
                hasLevels ? optionalText(levels, "beginner", MAX_LEVEL_SUMMARY) : null,
                hasLevels ? optionalText(levels, "junior", MAX_LEVEL_SUMMARY) : null,
                hasLevels ? optionalText(levels, "senior", MAX_LEVEL_SUMMARY) : null);
    }

    private static Level difficulty(JsonNode value, Level fallback) {
        if (value == null || !value.isString()) {
            return fallback;
        }
        try {
            return Level.valueOf(value.asString());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static String normalize(String name) {
        return WHITESPACE.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    /**
     * 이름 하나의 비교 키: 소문자로 바꾸고 공백을 모두 뗀 ① 전체 이름 ② 끝의 괄호를 뗀 앞부분 ③ 끝 괄호 안 (2~30자, {@code ·}·{@code
     * ,}·{@code /}가 없을 때만). 없는 키는 null.
     */
    record NameKeys(String full, String front, String inner) {

        static NameKeys of(String name) {
            String full = normalize(name);
            Matcher m = TRAILING_PAREN.matcher(name);
            if (!m.matches()) {
                return new NameKeys(full, null, null);
            }
            String front = normalize(m.group(1));
            String inside = m.group(2);
            String inner = normalize(inside);
            boolean innerOk =
                    inner.length() >= MIN_INNER_KEY
                            && inner.length() <= MAX_INNER_KEY
                            && !LIST_SEPARATOR.matcher(inside).find();
            return new NameKeys(
                    full,
                    front.isEmpty() || front.equals(full) ? null : front,
                    innerOk ? inner : null);
        }

        List<String> fullAndFront() {
            return front == null ? List.of(full) : List.of(full, front);
        }
    }

    /**
     * 기존 카드의 이름 키 → 카드 (키 종류마다 따로). 같은 키를 가진 카드가 여럿이면 숨기지 않은 카드, 그다음 먼저 나온 카드. 괄호 안 키가 다른 카드의 괄호 안
     * 키와 겹치면 둘 다 쓰지 않는다 (K-1 AC9).
     */
    private static final class NameIndex {
        private final Map<String, Concept> byFull = new HashMap<>();
        private final Map<String, Concept> byFront = new HashMap<>();
        private final Map<String, Concept> byInner = new HashMap<>();

        NameIndex(List<Concept> concepts) {
            Map<String, Integer> innerCount = new HashMap<>();
            List<NameKeys> keys = concepts.stream().map(c -> NameKeys.of(c.getName())).toList();
            keys.stream()
                    .map(NameKeys::inner)
                    .filter(k -> k != null)
                    .forEach(k -> innerCount.merge(k, 1, Integer::sum));
            for (int i = 0; i < concepts.size(); i++) {
                Concept c = concepts.get(i);
                NameKeys k = keys.get(i);
                put(byFull, k.full(), c);
                put(byFront, k.front(), c);
                if (k.inner() != null && innerCount.get(k.inner()) == 1) {
                    put(byInner, k.inner(), c);
                }
            }
        }

        /** 먼저 나온 카드를 두되, 숨긴 카드는 나중에 온 숨기지 않은 카드에 자리를 내준다. */
        private static void put(Map<String, Concept> map, String key, Concept c) {
            if (key == null) {
                return;
            }
            Concept old = map.get(key);
            if (old == null
                    || (old.getStatus() == ConceptStatus.HIDDEN
                            && c.getStatus() != ConceptStatus.HIDDEN)) {
                map.put(key, c);
            }
        }

        /**
         * 초안의 키를 전체 이름 → 앞부분 → 괄호 안 순서로, 각 키를 카드의 전체 이름 → 앞부분 → 괄호 안 순서로 찾는다. 괄호 안 키끼리는 비교하지 않는다
         * ("파일 크기 제한 (MultipartFile)"이 "파일 업로드 (MultipartFile)"에 붙지 않게). 숨기지 않은 카드를 먼저 돌려주고, 맞는 카드가
         * 숨긴 카드뿐이면 그 카드를 돌려준다 (호출자가 버린다).
         */
        Concept find(NameKeys keys) {
            Concept hidden = null;
            String[] draftKeys = {keys.full(), keys.front(), keys.inner()};
            for (int i = 0; i < draftKeys.length; i++) {
                String k = draftKeys[i];
                if (k == null) {
                    continue;
                }
                List<Map<String, Concept>> maps =
                        i == 2 ? List.of(byFull, byFront) : List.of(byFull, byFront, byInner);
                for (Map<String, Concept> map : maps) {
                    Concept c = map.get(k);
                    if (c == null) {
                        continue;
                    }
                    if (c.getStatus() != ConceptStatus.HIDDEN) {
                        return c;
                    }
                    if (hidden == null) {
                        hidden = c;
                    }
                }
            }
            return hidden;
        }
    }
}
