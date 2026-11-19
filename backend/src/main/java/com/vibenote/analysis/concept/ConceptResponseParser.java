package com.vibenote.analysis.concept;

import static com.vibenote.analysis.explain.LlmJsonCall.cut;

import com.vibenote.analysis.concept.ExtractedConcept.NewConcept;
import com.vibenote.analysis.explain.LlmJsonCall.InvalidResponseException;
import com.vibenote.common.Level;
import com.vibenote.concept.domain.Concept;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/**
 * 개념 응답(5.3절)을 검증하고 저장할 항목으로 정리한다 (F-08 AC4·AC5·AC6). 형식이 틀리면({@code concepts}가 배열이 아님) {@link
 * InvalidResponseException}을 던져 재시도하게 하고, 항목 단위의 오류는 그 항목만 버린다. 순수 함수다.
 */
public final class ConceptResponseParser {

    static final int MAX_NAME = 100;
    static final int MAX_SUMMARY = 500;
    static final int MAX_BODY = 2000;
    static final int MAX_CONTEXT_NOTE = 500;

    /** {@code analysis_concepts.line_hint VARCHAR(50)}. */
    static final int MAX_LINE_HINT = 50;

    private static final Pattern LINE_HINT = Pattern.compile("^L\\d+(-L\\d+)?$");

    /** 끝의 {@code (…)} 부분. 앞부분 이름 키를 만들 때 뗀다 (5.4절). */
    private static final Pattern TRAILING_PAREN = Pattern.compile("^(.*?)\\s*\\([^()]*\\)\\s*$");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private ConceptResponseParser() {}

    /**
     * @param input 이번 프롬프트의 입력 (카드 id, 파일 번호 → id, 코드가 들어간 파일 번호)
     * @param existing 이름 비교에 쓸 기존 카드 전체 (HIDDEN 포함)
     * @param level 분석 수준. 새 초안의 {@code difficulty}가 틀리면 이 값을 쓴다
     */
    public static List<ExtractedConcept> parse(
            JsonNode root,
            ConceptPrompts.Input input,
            List<Concept> existing,
            Level level,
            int maxLinks,
            int maxDrafts) {
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
                long id = conceptId.asLong();
                if (input.catalogIds().contains(id) && linked.add(id)) {
                    result.add(new ExtractedConcept(id, null, fileId, lineHint, note));
                }
                continue;
            }
            NewConcept draft = draft(item.get("new"), level);
            if (draft == null) {
                continue;
            }
            List<String> keys = keys(draft.name());
            Long same = names.find(keys);
            if (same != null) {
                if (linked.add(same)) {
                    result.add(new ExtractedConcept(same, null, fileId, lineHint, note));
                }
                continue;
            }
            if (keys.stream().anyMatch(draftKeys::contains) || drafts >= maxDrafts) {
                continue;
            }
            draftKeys.addAll(keys);
            drafts++;
            result.add(new ExtractedConcept(null, draft, fileId, lineHint, note));
        }
        return result;
    }

    /** {@code fileNo}가 1..n이면 그 파일의 id. */
    private static Long fileId(JsonNode item, ConceptPrompts.Input input) {
        JsonNode no = item.get("fileNo");
        if (no == null || !no.isIntegralNumber()) {
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

    /** 새 개념. 이름·요약·카테고리가 틀리면 null (그 항목을 버린다). */
    private static NewConcept draft(JsonNode node, Level level) {
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
                || !category.isString()
                || !ConceptCategories.ALL.contains(category.asString())) {
            return null;
        }
        return new NewConcept(
                name,
                category.asString(),
                difficulty(node.get("difficulty"), level),
                summary,
                optionalText(node, "body", MAX_BODY));
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

    /** 이름 비교 키 (5.4절): 소문자로 바꾸고 공백을 모두 뗀 전체 이름, 끝의 괄호 부분을 뗀 앞부분. */
    static List<String> keys(String name) {
        List<String> keys = new ArrayList<>(2);
        keys.add(normalize(name));
        Matcher m = TRAILING_PAREN.matcher(name);
        if (m.matches()) {
            String front = normalize(m.group(1));
            if (!front.isEmpty() && !keys.contains(front)) {
                keys.add(front);
            }
        }
        return keys;
    }

    private static String normalize(String name) {
        return WHITESPACE.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    /** 기존 카드의 이름 키 → id. 같은 키를 가진 카드가 여럿이면 먼저 나온 카드. */
    private static final class NameIndex {
        private final Map<String, Long> byKey = new HashMap<>();

        NameIndex(List<Concept> concepts) {
            // 전체 이름 키를 먼저 넣어, 앞부분 키가 다른 카드의 전체 이름과 겹치면 전체 이름이 이긴다
            concepts.forEach(c -> byKey.putIfAbsent(normalize(c.getName()), c.getId()));
            concepts.forEach(c -> keys(c.getName()).forEach(k -> byKey.putIfAbsent(k, c.getId())));
        }

        Long find(List<String> keys) {
            for (String k : keys) {
                Long id = byKey.get(k);
                if (id != null) {
                    return id;
                }
            }
            return null;
        }
    }
}
