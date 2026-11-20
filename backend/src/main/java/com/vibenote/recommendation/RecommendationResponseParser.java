package com.vibenote.recommendation;

import com.vibenote.analysis.explain.LlmJsonCall.InvalidResponseException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/**
 * RECOMMEND 응답(6.3절)을 검증하고 이유를 정리한다 (F-16 AC7). 루트가 객체가 아니거나 {@code reasons}가 배열이 아니면 {@link
 * InvalidResponseException}(재시도). 항목 단위 문제(요청하지 않은 id, 빈 이유, 같은 id의 두 번째 항목)는 그 항목만 버린다. 순수 함수다.
 */
final class RecommendationResponseParser {

    static final int MAX_REASON = 100;

    private static final Pattern SPACES = Pattern.compile("\\s+");

    private RecommendationResponseParser() {}

    /**
     * @param requestedIds 프롬프트에 넣은 카드 id
     * @return 카드 id → 정리한 이유 (응답 순서). 못 받은 카드는 없다
     */
    static Map<Long, String> parse(JsonNode root, Set<Long> requestedIds) {
        JsonNode items = root.get("reasons");
        if (!root.isObject() || items == null || !items.isArray()) {
            throw new InvalidResponseException("reasons가 배열이 아님");
        }
        Map<Long, String> reasons = new LinkedHashMap<>();
        Set<Long> seen = new HashSet<>();
        for (JsonNode item : items) {
            Long id = conceptId(item, requestedIds);
            if (id == null || !seen.add(id)) {
                continue;
            }
            String reason = clean(item.get("reason"));
            if (reason != null) {
                reasons.put(id, reason);
            }
        }
        return reasons;
    }

    private static Long conceptId(JsonNode item, Set<Long> requestedIds) {
        JsonNode value = item.isObject() ? item.get("conceptId") : null;
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            return null;
        }
        long id = value.asLong();
        return requestedIds.contains(id) ? id : null;
    }

    /** 공백 정리 후 100자(코드 포인트)까지. 문자열이 아니거나 비면 null. */
    private static String clean(JsonNode value) {
        if (value == null || !value.isString()) {
            return null;
        }
        String text = SPACES.matcher(value.asString().strip()).replaceAll(" ");
        if (text.isEmpty()) {
            return null;
        }
        if (text.codePointCount(0, text.length()) > MAX_REASON) {
            text = text.substring(0, text.offsetByCodePoints(0, MAX_REASON)).strip();
        }
        return text;
    }
}
