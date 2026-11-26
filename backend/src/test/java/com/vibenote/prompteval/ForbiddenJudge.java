package com.vibenote.prompteval;

import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmStage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 지표 B 금지 사실 (E-1 AC6, 11-26 변경). DS-04 {@code forbidden[]}은 "~라고 설명하면 틀림 (근거)" 꼴 문장이라 부분 일치로 셀 수
 * 없다. 수준마다 그 수준 출력 텍스트(경로·메타 제외)와 금지 문장 목록을 판정 모델에 주고 출력이 실제로 한 틀린 주장의 번호를 받는다. 문장마다 따로 부르지 않고 수준마다
 * 한 번 부른다 (Gemini 무료 한도). 형식이 틀리면 한 번 더 묻고, 또 틀리면 그 수준을 빼고 뺀 수를 센다.
 */
final class ForbiddenJudge {

    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private final LlmClient client;
    private final String template = EvalGrader.resource("judge/forbidden.md");

    ForbiddenJudge(LlmClient client) {
        this.client = client;
    }

    /**
     * @return {@code forbiddenHits}(주장한 문장 수 합)·{@code forbiddenChecked}(판정한 수준 × 문장 수)·{@code
     *     forbiddenExcluded}(뺀 수준 수). 주장한 문장은 {@code details.forbiddenHits}에만 남긴다
     */
    Map<String, Object> judge(JsonNode report, JsonNode label, Map<String, Object> details) {
        List<String> claims = new ArrayList<>();
        label.path("forbidden")
                .forEach(
                        c -> {
                            if (!c.asString("").isBlank()) {
                                claims.add(c.asString().strip());
                            }
                        });
        StringBuilder numbered = new StringBuilder();
        for (int i = 0; i < claims.size(); i++) {
            numbered.append(i + 1).append(". ").append(claims.get(i)).append('\n');
        }

        int hits = 0;
        int checked = 0;
        int excluded = 0;
        List<String> found = new ArrayList<>();
        if (!claims.isEmpty()) {
            for (Map.Entry<String, JsonNode> level : report.path("results").properties()) {
                String prompt =
                        template.replace(
                                        "{{output}}",
                                        EvalGrader.outputText(level.getValue()).strip())
                                .replace("{{claims}}", numbered.toString().strip());
                List<Integer> made = ask(prompt, claims.size(), level.getKey());
                if (made == null) {
                    excluded++;
                    continue;
                }
                checked += claims.size();
                hits += made.size();
                made.forEach(no -> found.add(level.getKey() + ": " + claims.get(no - 1)));
            }
        }
        details.put("forbiddenHits", found);

        Map<String, Object> b = new LinkedHashMap<>();
        b.put("forbiddenHits", hits);
        b.put("forbiddenChecked", checked);
        b.put("forbiddenExcluded", excluded);
        return b;
    }

    private List<Integer> ask(String prompt, int claims, String level) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                List<Integer> made =
                        parse(
                                client.complete(new LlmRequest(LlmStage.OVERVIEW, null, prompt))
                                        .content(),
                                claims);
                if (made != null) {
                    return made;
                }
            } catch (RuntimeException e) {
                System.out.printf("[prompt-grade] 금지 사실 판정 실패 (%s): %s%n", level, e.getMessage());
            }
        }
        return null;
    }

    /** {@code {"made":[1]}}: 1~{@code claims}의 서로 다른 정수 목록. 아니면 null. */
    private static List<Integer> parse(String content, int claims) {
        JsonNode root;
        try {
            root = JSON.readTree(LevelJudge.stripFence(content));
        } catch (JacksonException e) {
            return null;
        }
        JsonNode list = root.path("made");
        if (!list.isArray()) {
            return null;
        }
        List<Integer> made = new ArrayList<>();
        for (JsonNode n : list) {
            if (!n.isInt() || n.asInt() < 1 || n.asInt() > claims || made.contains(n.asInt())) {
                return null;
            }
            made.add(n.asInt());
        }
        return made;
    }
}
