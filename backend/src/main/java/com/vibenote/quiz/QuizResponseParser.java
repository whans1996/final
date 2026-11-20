package com.vibenote.quiz;

import com.vibenote.analysis.explain.LlmJsonCall.InvalidResponseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * QUIZ 응답(6.3절)을 검증하고 저장할 문항으로 정리한다 (F-14 AC3·AC4). 형식이 틀리면({@code questions}가 배열이 아니거나 유효 문항이 최소
 * 개수 미만) {@link InvalidResponseException}을 던져 재시도하게 하고, 문항 단위의 오류는 그 문항만 버린다. 순수 함수다.
 */
public final class QuizResponseParser {

    static final int MAX_QUESTION = 1000;
    static final int MAX_CHOICE = 300;
    static final int MAX_EXPLANATION = 1000;
    static final int CHOICES = 4;

    private QuizResponseParser() {}

    /**
     * @param conceptIds 이번 개념 목록에 넣은 카드 id. 밖의 {@code conceptId}는 null로 바꾼다
     * @param analysisId 선택지 셔플 seed ({@code analysisId * 31 + k})
     * @param min 유효 문항이 이보다 적으면 형식 오류
     * @param max 앞에서부터 이만큼만 쓴다
     */
    public static List<GeneratedQuestion> parse(
            JsonNode root, Set<Long> conceptIds, long analysisId, int min, int max) {
        JsonNode items = root.get("questions");
        if (!root.isObject() || items == null || !items.isArray()) {
            throw new InvalidResponseException("questions가 배열이 아님");
        }
        List<GeneratedQuestion> result = new ArrayList<>();
        for (JsonNode item : items) {
            if (result.size() >= max) {
                break;
            }
            GeneratedQuestion q = question(item, conceptIds, analysisId, result.size() + 1);
            if (q != null) {
                result.add(q);
            }
        }
        if (result.size() < min) {
            throw new InvalidResponseException("유효 문항 " + result.size() + "개");
        }
        return result;
    }

    /** 규칙을 어기면 null (그 문항을 버린다). {@code k}는 저장될 문항 순번(1..n). */
    private static GeneratedQuestion question(
            JsonNode item, Set<Long> conceptIds, long analysisId, int k) {
        if (!item.isObject()) {
            return null;
        }
        String question = text(item.get("question"), MAX_QUESTION);
        String explanation = text(item.get("explanation"), MAX_EXPLANATION);
        List<String> choices = choices(item.get("choices"));
        JsonNode answer = item.get("answerIndex");
        if (question == null
                || explanation == null
                || choices == null
                || answer == null
                || !answer.isIntegralNumber()
                || !answer.canConvertToInt()
                || answer.asInt() < 0
                || answer.asInt() >= CHOICES) {
            return null;
        }
        String correct = choices.get(answer.asInt());
        // LLM이 정답을 한 위치에 몰아 두는 경향을 없애고, 같은 입력이면 같은 결과가 나오게 한다
        Collections.shuffle(choices, new Random(analysisId * 31 + k));
        return new GeneratedQuestion(
                conceptId(item.get("conceptId"), conceptIds),
                question,
                List.copyOf(choices),
                choices.indexOf(correct),
                explanation);
    }

    /** 문자열이면 앞뒤 공백을 뗀 값. 없거나, 비거나, {@code max}를 넘으면 null. */
    private static String text(JsonNode value, int max) {
        if (value == null || !value.isString()) {
            return null;
        }
        String text = value.asString().strip();
        return text.isEmpty() || text.length() > max ? null : text;
    }

    /** 정확히 4개, 각각 유효한 문자열이고 서로 다르면 변경 가능한 목록. 아니면 null. */
    private static List<String> choices(JsonNode node) {
        if (node == null || !node.isArray() || node.size() != CHOICES) {
            return null;
        }
        List<String> choices = new ArrayList<>(CHOICES);
        Set<String> seen = new HashSet<>();
        for (JsonNode c : node) {
            String text = text(c, MAX_CHOICE);
            if (text == null || !seen.add(text)) {
                return null;
            }
            choices.add(text);
        }
        return choices;
    }

    private static Long conceptId(JsonNode value, Set<Long> conceptIds) {
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            return null;
        }
        long id = value.asLong();
        return conceptIds.contains(id) ? id : null;
    }
}
