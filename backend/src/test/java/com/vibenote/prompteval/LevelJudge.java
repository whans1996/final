package com.vibenote.prompteval;

import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmResponse;
import com.vibenote.llm.LlmStage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 지표 D 수준 구분도 (E-1 AC7). 같은 key(개요, 파일 경로, 흐름 {@code METHOD path})의 세 수준 출력을 seed로 섞어 판정 LLM에 어느
 * 수준인지 묻고, 맞힌 출력 수 / 판정한 출력 수를 낸다 (우연 33%). 판정 응답이 형식에 맞지 않으면 한 번 더 묻고, 또 틀리면 그 묶음을 빼고 뺀 수를 센다.
 */
final class LevelJudge {

    static final List<String> LEVELS = List.of("beginner", "junior", "senior");
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    /** 판정 묶음 하나: 수준 → 판정에 보여 줄 설명. */
    private record Group(LlmStage stage, String key, Map<String, String> texts) {}

    private final LlmClient client;
    private final long seed;
    private final String template = EvalGrader.resource("judge/level.md");

    LevelJudge(LlmClient client, long seed) {
        this.client = client;
        this.seed = seed;
    }

    Map<String, Object> judge(JsonNode report) {
        Random random = new Random(seed);
        int judged = 0;
        int correct = 0;
        int excluded = 0;
        String model = null;
        List<Group> groups = groups(report.path("results"));
        for (Group group : groups) {
            List<String> order = new ArrayList<>(LEVELS);
            Collections.shuffle(order, random);
            Map<String, String> ids = new LinkedHashMap<>();
            StringBuilder items = new StringBuilder();
            for (int i = 0; i < order.size(); i++) {
                String id = String.valueOf((char) ('a' + i));
                ids.put(id, order.get(i));
                items.append("### ")
                        .append(id)
                        .append('\n')
                        .append(group.texts().get(order.get(i)).strip())
                        .append("\n\n");
            }
            String prompt = template.replace("{{items}}", items.toString().strip());
            Map<String, String> answer = null;
            for (int attempt = 0; attempt < 2 && answer == null; attempt++) {
                try {
                    LlmResponse response =
                            client.complete(new LlmRequest(group.stage(), null, prompt));
                    model = model == null ? response.model() : model;
                    answer = parse(response.content(), ids.keySet());
                } catch (RuntimeException e) {
                    // 한도 초과·타임아웃도 형식 오류처럼 한 번 더 묻고, 또 실패하면 이 묶음만 뺀다
                    System.out.printf(
                            "[prompt-grade] 수준 판정 실패 (%s): %s%n", group.key(), e.getMessage());
                }
            }
            if (answer == null) {
                excluded++;
                continue;
            }
            for (Map.Entry<String, String> id : ids.entrySet()) {
                judged++;
                correct += id.getValue().equals(answer.get(id.getKey())) ? 1 : 0;
            }
        }
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("accuracy", EvalGrader.ratio(correct, judged));
        d.put("judged", judged);
        d.put("excluded", excluded);
        d.put("groups", groups.size());
        d.put("judge", model);
        return d;
    }

    /** 세 수준이 모두 있는 key만 묶는다. 순서는 개요 → 파일(입문 수준 목록 순) → 흐름. */
    private static List<Group> groups(JsonNode results) {
        List<Group> groups = new ArrayList<>();
        if (!LEVELS.stream().allMatch(results::has)) {
            return groups;
        }
        add(groups, LlmStage.OVERVIEW, "overview", results, r -> overviewText(r.path("overview")));
        for (JsonNode file : results.path("beginner").path("files")) {
            String path = file.path("path").asString();
            add(
                    groups,
                    LlmStage.FILE,
                    path,
                    results,
                    r -> fileText(find(r.path("files"), "path", path)));
        }
        for (JsonNode flow : results.path("beginner").path("flows")) {
            String endpoint = flow.path("endpoint").asString();
            add(
                    groups,
                    LlmStage.FLOW,
                    endpoint,
                    results,
                    r -> flowText(find(r.path("flows"), "endpoint", endpoint)));
        }
        return groups;
    }

    private static void add(
            List<Group> groups,
            LlmStage stage,
            String key,
            JsonNode results,
            Function<JsonNode, String> text) {
        Map<String, String> texts = new LinkedHashMap<>();
        for (String level : LEVELS) {
            String t = text.apply(results.path(level));
            if (t == null || t.isBlank()) {
                return;
            }
            texts.put(level, t);
        }
        groups.add(new Group(stage, key, texts));
    }

    private static JsonNode find(JsonNode list, String field, String value) {
        for (JsonNode row : list) {
            if (value.equals(row.path(field).asString(null))) {
                return row;
            }
        }
        return null;
    }

    private static String overviewText(JsonNode overview) {
        if (!overview.isObject()) {
            return null;
        }
        StringBuilder text = new StringBuilder(overview.path("summary").asString(""));
        for (JsonNode layer : overview.path("layers")) {
            text.append("\n- ")
                    .append(layer.path("layer").asString(""))
                    .append(": ")
                    .append(layer.path("role").asString(""));
        }
        for (JsonNode step : overview.path("readingOrder")) {
            text.append("\n- 읽는 순서 ")
                    .append(step.path("path").asString(""))
                    .append(": ")
                    .append(step.path("reason").asString(""));
        }
        return text.toString();
    }

    private static String fileText(JsonNode file) {
        if (file == null || file.path("explanation").isNull()) {
            return null;
        }
        return file.path("role").asString("") + "\n" + file.path("explanation").asString("");
    }

    private static String flowText(JsonNode flow) {
        if (flow == null || flow.path("summary").isNull() || flow.path("steps").isEmpty()) {
            return null;
        }
        StringBuilder text = new StringBuilder(flow.path("summary").asString(""));
        for (JsonNode step : flow.path("steps")) {
            text.append("\n")
                    .append(step.path("no").asInt())
                    .append(". ")
                    .append(step.path("title").asString(""))
                    .append(": ")
                    .append(step.path("description").asString(""));
        }
        return text.toString();
    }

    /** 응답 {@code {"items":[{"id","level"}]}}이 id마다 한 번, 수준마다 한 번이면 id → 수준. 아니면 null. */
    private static Map<String, String> parse(String content, Set<String> ids) {
        JsonNode root;
        try {
            root = JSON.readTree(stripFence(content));
        } catch (JacksonException e) {
            return null;
        }
        Map<String, String> answer = new LinkedHashMap<>();
        Set<String> levels = new HashSet<>();
        for (JsonNode item : root.path("items")) {
            String id = item.path("id").asString("");
            String level = item.path("level").asString("");
            if (!ids.contains(id)
                    || !LEVELS.contains(level)
                    || answer.put(id, level) != null
                    || !levels.add(level)) {
                return null;
            }
        }
        return answer.size() == ids.size() ? answer : null;
    }

    /** 판정 모델이 붙이는 코드 펜스(```json)를 뗀다. */
    static String stripFence(String content) {
        String text = content == null ? "" : content.strip();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        return text;
    }
}
