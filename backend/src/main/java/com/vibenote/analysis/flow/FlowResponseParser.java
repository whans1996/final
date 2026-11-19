package com.vibenote.analysis.flow;

import static com.vibenote.analysis.explain.LlmJsonCall.cut;
import static com.vibenote.analysis.explain.LlmJsonCall.text;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.LlmJsonCall.InvalidResponseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/** 실행 흐름 응답(5.3절)을 검증하고 저장 형태로 정리한다 (F-07 AC4·AC5). 형식이 틀리면 {@link InvalidResponseException}. */
public final class FlowResponseParser {

    static final int MAX_SUMMARY = 500;
    static final int MAX_TITLE = 60;
    static final int MAX_SYMBOL = 100;
    static final int MAX_DESCRIPTION = 600;

    private FlowResponseParser() {}

    /**
     * @param files 분석의 파일 전체. {@code steps[].path}가 이 중 하나면 fileId·layer를 붙인다
     * @param maxSteps 앞에서 이만큼만 남긴다. 버리는 항목은 검증하지 않는다
     */
    public static FlowResult parse(JsonNode root, List<AnalysisFile> files, int maxSteps) {
        if (!root.isObject()) {
            throw new InvalidResponseException("객체가 아님");
        }
        String summary = text(root, "summary");
        if (summary.isBlank()) {
            throw new InvalidResponseException("summary가 비었음");
        }
        JsonNode items = root.get("steps");
        if (items == null || !items.isArray() || items.isEmpty()) {
            throw new InvalidResponseException("steps가 1개 이상인 배열이 아님");
        }
        Map<String, AnalysisFile> byPath = new HashMap<>();
        files.forEach(f -> byPath.putIfAbsent(f.getPath(), f));

        List<FlowStep> steps = new ArrayList<>();
        for (JsonNode item : items) {
            if (steps.size() == maxSteps) {
                break;
            }
            if (!item.isObject()) {
                throw new InvalidResponseException("steps[]가 객체가 아님");
            }
            String path = text(item, "path");
            AnalysisFile file = byPath.get(path);
            steps.add(
                    new FlowStep(
                            steps.size() + 1,
                            file == null ? null : file.getLayer(),
                            file == null ? null : file.getId(),
                            path,
                            symbol(item),
                            cut(text(item, "title"), MAX_TITLE),
                            cut(text(item, "description"), MAX_DESCRIPTION)));
        }
        return new FlowResult(cut(summary, MAX_SUMMARY), steps);
    }

    private static String symbol(JsonNode item) {
        JsonNode value = item.get("symbol");
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isString()) {
            throw new InvalidResponseException("symbol이 문자열이 아님");
        }
        return value.asString().isBlank() ? null : cut(value.asString(), MAX_SYMBOL);
    }
}
