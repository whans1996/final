package com.vibenote.prompteval;

import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmStage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 지표 E 퀴즈 품질 (E-1 AC8). 기계 검사(보기 4개 서로 다름, {@code answerIndex} 0~3, 해설에 선지 번호·위치 언급 없음)를 모두 통과한 문항
 * 비율과, 판정 LLM이 정답으로 볼 수 있는 보기를 모두 골랐을 때 정확히 하나이고 {@code answerIndex}와 같은 문항 비율(정답 유일률)을 낸다. 보기를 섞어
 * 보여 주므로(F-14) 해설이 위치를 말하면 틀린 설명이 된다.
 */
final class QuizCheck {

    private static final ObjectMapper JSON = JsonMapper.builder().build();

    /** 해설의 선지 번호·위치 언급 ("2번", "첫 번째 보기", "A)", "위의 보기", "보기 3", "②", "마지막 보기"). */
    static final Pattern POSITION =
            Pattern.compile(
                    "(?<![0-9])[1-4]\\s*번"
                            + "|(첫|두|세|네)\\s*번?\\s*째"
                            + "|(?<![\\w(])[A-Da-d]\\)"
                            + "|위의?\\s*(보기|선지|선택지)"
                            + "|(보기|선지|선택지)\\s*[1-4A-D①-④]"
                            + "|[①②③④]"
                            + "|(마지막|맨\\s*(위|아래))\\s*(보기|선지|선택지)");

    private QuizCheck() {}

    static boolean mentionsPosition(String text) {
        return text != null && POSITION.matcher(text).find();
    }

    static boolean mechanical(JsonNode question) {
        JsonNode choices = question.path("choices");
        if (!choices.isArray() || choices.size() != 4) {
            return false;
        }
        Set<String> distinct = new HashSet<>();
        for (JsonNode choice : choices) {
            String text = choice.asString("").strip();
            if (text.isEmpty() || !distinct.add(text)) {
                return false;
            }
        }
        JsonNode answer = question.path("answerIndex");
        return answer.isInt()
                && answer.asInt() >= 0
                && answer.asInt() <= 3
                && !mentionsPosition(question.path("explanation").asString(null));
    }

    /**
     * @param judge empty면 정답 유일률은 {@value EvalGrader#JUDGE_SKIPPED} (AC9)
     */
    static Map<String, Object> grade(JsonNode report, Optional<LlmClient> judge) {
        List<JsonNode> questions = new ArrayList<>();
        report.path("results").forEach(level -> level.path("quiz").forEach(questions::add));
        int passed = (int) questions.stream().filter(QuizCheck::mechanical).count();

        Map<String, Object> e = new LinkedHashMap<>();
        e.put("mechanical", EvalGrader.ratio(passed, questions.size()));
        if (judge.isEmpty()) {
            e.put("uniqueAnswer", EvalGrader.JUDGE_SKIPPED);
        } else {
            String template = EvalGrader.resource("judge/answer.md");
            int judged = 0;
            int unique = 0;
            int excluded = 0;
            for (JsonNode q : questions) {
                List<Integer> correct =
                        ask(judge.get(), prompt(template, q), q.path("choices").size());
                if (correct == null) {
                    excluded++;
                    continue;
                }
                judged++;
                unique +=
                        correct.size() == 1 && correct.get(0) == q.path("answerIndex").asInt(-1)
                                ? 1
                                : 0;
            }
            e.put("uniqueAnswer", EvalGrader.ratio(unique, judged));
            e.put("excluded", excluded);
        }
        e.put("questions", questions.size());
        return e;
    }

    private static String prompt(String template, JsonNode q) {
        StringBuilder choices = new StringBuilder();
        int i = 0;
        for (JsonNode choice : q.path("choices")) {
            choices.append(i++).append(") ").append(choice.asString("")).append('\n');
        }
        return template.replace("{{concept}}", q.path("conceptName").asString("(없음)"))
                .replace("{{question}}", q.path("question").asString(""))
                .replace("{{choices}}", choices.toString().strip())
                .replace("{{explanation}}", q.path("explanation").asString(""));
    }

    /** 형식이 틀리면 한 번 더 묻는다. 두 번 다 틀리면 null (그 문항을 빼고 센다). */
    private static List<Integer> ask(LlmClient judge, String prompt, int choices) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                List<Integer> correct =
                        parse(
                                judge.complete(new LlmRequest(LlmStage.QUIZ, null, prompt))
                                        .content(),
                                choices);
                if (correct != null) {
                    return correct;
                }
            } catch (RuntimeException e) {
                System.out.printf("[prompt-grade] 정답 판정 실패: %s%n", e.getMessage());
            }
        }
        return null;
    }

    /** {@code {"correct":[0]}}: 0~(보기 수-1)의 서로 다른 정수 목록. 아니면 null. */
    private static List<Integer> parse(String content, int choices) {
        JsonNode root;
        try {
            root = JSON.readTree(LevelJudge.stripFence(content));
        } catch (JacksonException e) {
            return null;
        }
        JsonNode list = root.path("correct");
        if (!list.isArray()) {
            return null;
        }
        List<Integer> correct = new ArrayList<>();
        for (JsonNode n : list) {
            if (!n.isInt()
                    || n.asInt() < 0
                    || n.asInt() >= choices
                    || correct.contains(n.asInt())) {
                return null;
            }
            correct.add(n.asInt());
        }
        return correct;
    }
}
