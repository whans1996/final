package com.vibenote.interview;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.LlmJsonCall;
import com.vibenote.analysis.explain.LlmJsonCall.InvalidResponseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/**
 * 면접 질문 응답 검증·정리 (I-1a AC3·AC4, 5.3절). 질문 단위로 걸러 내고(범주 밖·근거 0개·핵심 포인트 0개는 버림), 남은 질문이 하한보다 적으면 형식
 * 오류로 본다 ({@link LlmJsonCall}이 1회 다시 묻는다).
 */
public final class InterviewResponseParser {

    static final int MAX_QUESTION_CHARS = 300;
    static final int MAX_KEY_POINT_CHARS = 200;
    static final int MAX_KEY_POINTS = 5;
    static final int MAX_EVIDENCE = 3;
    static final int MAX_CONCEPTS = 3;

    private static final Pattern LINES = Pattern.compile("^L(\\d{1,9})(?:-L(\\d{1,9}))?$");

    private InterviewResponseParser() {}

    /**
     * @throws InvalidResponseException 형식이 틀리거나 남은 질문이 {@code minQuestions}보다 적을 때
     */
    public static List<GeneratedQuestion> parse(
            JsonNode root, InterviewPrompts.Input input, InterviewProperties props) {
        if (root == null || !root.isObject() || !root.path("questions").isArray()) {
            throw new InvalidResponseException("questions 배열이 없음");
        }
        List<GeneratedQuestion> result = new ArrayList<>();
        for (JsonNode item : root.path("questions")) {
            if (result.size() >= props.maxQuestions()) {
                break;
            }
            GeneratedQuestion q = question(item, input, result.size() + 1);
            if (q != null) {
                result.add(q);
            }
        }
        if (result.size() < props.minQuestions()) {
            throw new InvalidResponseException(
                    "유효한 질문 " + result.size() + "개 (최소 " + props.minQuestions() + "개)");
        }
        return List.copyOf(result);
    }

    /** 버릴 질문이면 null. */
    private static GeneratedQuestion question(JsonNode item, InterviewPrompts.Input input, int no) {
        if (!item.isObject()) {
            return null;
        }
        InterviewCategory category = category(item.get("category"));
        String question = string(item.get("question"));
        if (category == null || question == null) {
            return null;
        }
        List<String> keyPoints = keyPoints(item.get("keyPoints"));
        List<InterviewEvidence> evidence = evidence(item.get("evidence"), input);
        if (keyPoints.isEmpty() || evidence.isEmpty()) {
            return null;
        }
        return new GeneratedQuestion(
                no,
                category,
                LlmJsonCall.cut(question, MAX_QUESTION_CHARS),
                keyPoints,
                evidence,
                conceptIds(item.get("conceptIds"), input.conceptIds()));
    }

    private static InterviewCategory category(JsonNode node) {
        String value = string(node);
        if (value == null) {
            return null;
        }
        try {
            return InterviewCategory.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 공백을 다듬은 문자열. 문자열이 아니거나 비었으면 null. */
    private static String string(JsonNode node) {
        if (node == null || !node.isString()) {
            return null;
        }
        String value = node.asString().strip();
        return value.isEmpty() ? null : value;
    }

    private static List<String> keyPoints(JsonNode node) {
        List<String> points = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return points;
        }
        for (JsonNode p : node) {
            String value = string(p);
            if (value != null) {
                points.add(LlmJsonCall.cut(value, MAX_KEY_POINT_CHARS));
                if (points.size() == MAX_KEY_POINTS) {
                    break;
                }
            }
        }
        return List.copyOf(points);
    }

    private static List<InterviewEvidence> evidence(JsonNode node, InterviewPrompts.Input input) {
        List<InterviewEvidence> result = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return result;
        }
        for (JsonNode e : node) {
            InterviewEvidence ev = oneEvidence(e, input);
            if (ev != null) {
                result.add(ev);
                if (result.size() == MAX_EVIDENCE) {
                    break;
                }
            }
        }
        return List.copyOf(result);
    }

    private static InterviewEvidence oneEvidence(JsonNode e, InterviewPrompts.Input input) {
        if (!e.isObject()) {
            return null;
        }
        JsonNode fileNo = e.get("fileNo");
        if (fileNo == null || !fileNo.isIntegralNumber() || !fileNo.canConvertToInt()) {
            return null;
        }
        Integer visible = input.codeLines().get(fileNo.asInt());
        String lines = string(e.get("lines"));
        if (visible == null || lines == null) {
            return null;
        }
        Matcher m = LINES.matcher(lines);
        if (!m.matches()) {
            return null;
        }
        int start = Integer.parseInt(m.group(1));
        int end = m.group(2) == null ? start : Integer.parseInt(m.group(2));
        if (start < 1 || start > end || end > visible) {
            return null;
        }
        AnalysisFile file = input.orderedFiles().get(fileNo.asInt() - 1);
        return new InterviewEvidence(file.getId(), file.getPath(), start, end);
    }

    private static List<Long> conceptIds(JsonNode node, Set<Long> allowed) {
        Set<Long> ids = new LinkedHashSet<>();
        if (node == null || !node.isArray()) {
            return List.of();
        }
        for (JsonNode id : node) {
            if (id.isIntegralNumber() && id.canConvertToLong() && allowed.contains(id.asLong())) {
                ids.add(id.asLong());
                if (ids.size() == MAX_CONCEPTS) {
                    break;
                }
            }
        }
        return List.copyOf(ids);
    }
}
