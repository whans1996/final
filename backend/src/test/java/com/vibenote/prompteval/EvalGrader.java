package com.vibenote.prompteval;

import com.vibenote.analysis.pipeline.SourceFile;
import com.vibenote.analysis.pipeline.ZipArchiveReader;
import com.vibenote.llm.LlmClient;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/**
 * 평가 보고서 채점기 (E-1 AC5·AC6·AC9). 러너 보고서 하나와 (있으면) DS-04 라벨로 지표 A~E·커버리지·근거 유효율을 계산한다.
 *
 * <ul>
 *   <li>A 스키마 적합률: 단계별 단위의 첫 시도 통과율·최종 통과율
 *   <li>B 환각률: 출력이 가리킨 경로가 {@code index}에 없는 비율, 흐름 심볼의 메서드 이름이 그 파일 본문에 없는 비율, 라벨 금지 사실이 나온 (수준,
 *       사실) 수
 *   <li>C 개념: must 재현율, (must+ok) 정밀도, wrong 비율, must 근거 파일 일치율 (라벨 필요)
 *   <li>D 수준 구분도: {@link LevelJudge} (판정 LLM 필요) · E 퀴즈: {@link QuizCheck}
 *   <li>커버리지: 라벨 파일 선별률(전체·FE), API 탐지 재현율·정밀도 (라벨 필요)
 *   <li>근거 유효율: 개념 {@code lineHint}가 파일 줄 수 안, 흐름 단계 경로·심볼이 실제로 있는 비율
 * </ul>
 *
 * <p>비율은 수준을 모두 합쳐(분자·분모를 더해) 계산하고, 분모가 0이면 null이다. 라벨 내용(개념 이름·경로·금지 사실)은 {@code details}에만 남고
 * {@code scores}에는 점수·개수만 들어간다 (AC10).
 */
final class EvalGrader {

    static final String NO_LABEL = "라벨 없음";
    static final String JUDGE_SKIPPED = "판정 건너뜀";

    private static final List<String> STAGES =
            List.of("overview", "file", "flow", "concept", "quiz");
    private static final Set<String> FE_LAYERS = Set.of("vue", "js");

    /** 출력 텍스트로 보지 않는 필드 (경로·메타·호출 원문). 금지 사실 패턴을 찾을 때 뺀다. */
    private static final Set<String> NOT_TEXT =
            Set.of("path", "layer", "fileId", "endpoint", "calls", "skipped", "errors");

    private static final Pattern LINE_HINT = Pattern.compile("L(\\d+)(?:\\s*-\\s*L?(\\d+))?");

    private final Optional<LlmClient> judge;
    private final long seed;

    /**
     * @param judge 판정 LLM (Gemini). empty면 D와 E 정답 유일률을 건너뛴다 (AC9)
     * @param seed 수준 판정에서 세 출력을 섞는 순서 (AC7)
     */
    EvalGrader(Optional<LlmClient> judge, long seed) {
        this.judge = judge;
        this.seed = seed;
    }

    /**
     * @param label DS-04 라벨. 없으면 null (C·커버리지·금지 사실은 {@value #NO_LABEL})
     */
    Map<String, Object> grade(JsonNode report, JsonNode label) {
        Map<String, Integer> lines = new HashMap<>();
        report.path("index")
                .forEach(f -> lines.put(f.path("path").asString(), f.path("lines").asInt()));
        Map<String, String> sources = sources(report);
        Map<String, Object> details = new LinkedHashMap<>();

        Map<String, Object> scores = new LinkedHashMap<>();
        scores.put("A", schema(report));
        Map<String, Object> b = new LinkedHashMap<>();
        Map<String, Object> evidence = new LinkedHashMap<>();
        grounding(report, lines, sources, b, evidence, details);
        scores.put("B", b);
        if (label == null) {
            b.put("forbiddenHits", NO_LABEL);
            scores.put("C", NO_LABEL);
        } else {
            b.put("forbiddenHits", forbidden(report, label, details));
            scores.put("C", concepts(report, label, details));
        }
        scores.put(
                "D",
                judge.<Object>map(j -> new LevelJudge(j, seed).judge(report))
                        .orElse(JUDGE_SKIPPED));
        scores.put("E", QuizCheck.grade(report, judge));
        scores.put("coverage", label == null ? NO_LABEL : coverage(report, label, details));
        scores.put("evidence", evidence);

        Map<String, Object> grade = new LinkedHashMap<>();
        for (String key : List.of("fixture", "provider", "model", "split", "contamination")) {
            grade.put(key, report.path(key).asString(null));
        }
        grade.put("stage", report.path("stage").asString(null));
        grade.put("labeled", label != null);
        grade.put("scores", scores);
        grade.put("details", details);
        return grade;
    }

    /** A: 단계별 {@code units[]}의 첫 시도 통과율·최종 통과율. */
    private static Map<String, Object> schema(JsonNode report) {
        Map<String, Object> a = new LinkedHashMap<>();
        for (String stage : STAGES) {
            int units = 0;
            int firstPass = 0;
            int ok = 0;
            for (JsonNode unit : report.path("units")) {
                if (!stage.equals(unit.path("stage").asString())) {
                    continue;
                }
                units++;
                firstPass += unit.path("firstPass").asBoolean() ? 1 : 0;
                ok += unit.path("ok").asBoolean() ? 1 : 0;
            }
            if (units > 0) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("firstPass", ratio(firstPass, units));
                row.put("ok", ratio(ok, units));
                row.put("units", units);
                a.put(stage, row);
            }
        }
        return a;
    }

    /** B의 없는 경로·심볼과 근거 유효율. 둘 다 출력이 가리킨 경로·심볼을 한 번씩 훑는다. */
    private static void grounding(
            JsonNode report,
            Map<String, Integer> lines,
            Map<String, String> sources,
            Map<String, Object> b,
            Map<String, Object> evidence,
            Map<String, Object> details) {
        int paths = 0;
        int unknownPaths = 0;
        int symbols = 0;
        int unknownSymbols = 0;
        int checked = 0;
        int valid = 0;
        Set<String> missingPaths = new LinkedHashSet<>();
        Set<String> missingSymbols = new LinkedHashSet<>();
        Set<String> invalidHints = new LinkedHashSet<>();

        for (Map.Entry<String, JsonNode> level : report.path("results").properties()) {
            JsonNode result = level.getValue();
            List<String> referenced = new ArrayList<>();
            result.path("overview")
                    .path("readingOrder")
                    .forEach(r -> referenced.add(r.path("path").asString(null)));
            for (JsonNode flow : result.path("flows")) {
                for (JsonNode step : flow.path("steps")) {
                    String path = step.path("path").asString(null);
                    String symbol = step.path("symbol").asString(null);
                    referenced.add(path);
                    boolean known = path != null && lines.containsKey(path);
                    boolean symbolOk = true;
                    if (known && symbol != null && !symbol.isBlank()) {
                        symbols++;
                        symbolOk = symbolExists(symbol, sources.getOrDefault(path, ""));
                        if (!symbolOk) {
                            unknownSymbols++;
                            missingSymbols.add(path + "#" + symbol);
                        }
                    }
                    checked++;
                    valid += known && symbolOk ? 1 : 0;
                }
            }
            for (JsonNode concept : result.path("concepts")) {
                String path = concept.path("path").asString(null);
                referenced.add(path);
                String hint = concept.path("lineHint").asString(null);
                if (hint == null || hint.isBlank()) {
                    continue;
                }
                checked++;
                if (path != null && withinLines(hint, lines.getOrDefault(path, -1))) {
                    valid++;
                } else {
                    invalidHints.add(path + ":" + hint);
                }
            }
            for (String path : referenced) {
                if (path == null) {
                    continue;
                }
                paths++;
                if (!lines.containsKey(path)) {
                    unknownPaths++;
                    missingPaths.add(path);
                }
            }
        }

        b.put("unknownPath", ratio(unknownPaths, paths));
        b.put("unknownSymbol", ratio(unknownSymbols, symbols));
        b.put("paths", paths);
        b.put("symbols", symbols);
        evidence.put("valid", ratio(valid, checked));
        evidence.put("checked", checked);
        details.put("unknownPaths", missingPaths);
        details.put("unknownSymbols", missingSymbols);
        details.put("invalidLineHints", invalidHints);
    }

    /** 심볼(클래스.메서드)의 마지막 이름이 본문에 식별자로 있는지. 괄호·인자는 뗀다. */
    static boolean symbolExists(String symbol, String body) {
        String name = symbol.strip();
        int paren = name.indexOf('(');
        if (paren >= 0) {
            name = name.substring(0, paren);
        }
        int dot = Math.max(name.lastIndexOf('.'), name.lastIndexOf('#'));
        name = name.substring(dot + 1).strip();
        if (name.isEmpty()) {
            return false;
        }
        return Pattern.compile("(?<![\\w$])" + Pattern.quote(name) + "(?![\\w$])")
                .matcher(body)
                .find();
    }

    /**
     * {@code L12} 또는 {@code L12-L30}이 1~{@code lines} 안인지. 파일이 index에 없으면({@code lines < 0}) 무효.
     */
    static boolean withinLines(String hint, int lines) {
        Matcher m = LINE_HINT.matcher(hint.strip());
        if (lines < 0 || !m.matches()) {
            return false;
        }
        long start = Long.parseLong(m.group(1));
        long end = m.group(2) == null ? start : Long.parseLong(m.group(2));
        return start >= 1 && start <= end && end <= lines;
    }

    /** C: 수준마다 출력 개념(이름 정규화, 중복 제거)을 라벨 등급과 대조한다. */
    private static Map<String, Object> concepts(
            JsonNode report, JsonNode label, Map<String, Object> details) {
        Map<String, String> grades = new HashMap<>();
        Map<String, Set<String>> evidence = new HashMap<>();
        for (JsonNode c : label.path("concepts")) {
            String name = normalize(c.path("name").asString());
            grades.put(name, c.path("grade").asString().toLowerCase(Locale.ROOT));
            Set<String> files = new HashSet<>();
            c.path("evidence").forEach(e -> files.add(e.asString()));
            evidence.put(name, files);
        }
        long mustTotal = grades.values().stream().filter("must"::equals).count();

        int must = 0;
        int recalled = 0;
        int outputs = 0;
        int good = 0;
        int wrong = 0;
        int evidenceChecked = 0;
        int evidenceMatched = 0;
        Map<String, Object> perLevel = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> level : report.path("results").properties()) {
            if (!level.getValue().has("concepts")) {
                continue;
            }
            Map<String, Set<String>> out = new LinkedHashMap<>();
            for (JsonNode c : level.getValue().path("concepts")) {
                Set<String> paths =
                        out.computeIfAbsent(
                                normalize(c.path("name").asString()), k -> new HashSet<>());
                String path = c.path("path").asString(null);
                if (path != null) {
                    paths.add(path);
                }
            }
            must += (int) mustTotal;
            outputs += out.size();
            List<String> missed = new ArrayList<>();
            List<String> wrongNames = new ArrayList<>();
            List<String> unlabeled = new ArrayList<>();
            for (Map.Entry<String, Set<String>> o : out.entrySet()) {
                String grade = grades.get(o.getKey());
                if (grade == null) {
                    unlabeled.add(o.getKey());
                } else if (grade.equals("wrong")) {
                    wrong++;
                    wrongNames.add(o.getKey());
                } else {
                    good++;
                }
                if ("must".equals(grade)) {
                    recalled++;
                    Set<String> files = evidence.get(o.getKey());
                    if (!files.isEmpty()) {
                        evidenceChecked++;
                        evidenceMatched += o.getValue().stream().anyMatch(files::contains) ? 1 : 0;
                    }
                }
            }
            grades.forEach(
                    (name, grade) -> {
                        if (grade.equals("must") && !out.containsKey(name)) {
                            missed.add(name);
                        }
                    });
            perLevel.put(
                    level.getKey(),
                    Map.of("missedMust", missed, "wrong", wrongNames, "unlabeled", unlabeled));
        }
        details.put("concepts", perLevel);

        Map<String, Object> c = new LinkedHashMap<>();
        c.put("mustRecall", ratio(recalled, must));
        c.put("precision", ratio(good, outputs));
        c.put("wrongRate", ratio(wrong, outputs));
        c.put("mustEvidenceMatch", ratio(evidenceMatched, evidenceChecked));
        c.put("outputs", outputs);
        return c;
    }

    /** 커버리지: 라벨 파일 중 선별된 비율(전체·FE layer)과 API 탐지 재현율·정밀도. */
    private static Map<String, Object> coverage(
            JsonNode report, JsonNode label, Map<String, Object> details) {
        Map<String, JsonNode> index = new HashMap<>();
        report.path("index").forEach(f -> index.put(f.path("path").asString(), f));
        int files = 0;
        int selected = 0;
        int feFiles = 0;
        int feSelected = 0;
        List<String> notSelected = new ArrayList<>();
        for (JsonNode f : label.path("files")) {
            String path = f.path("path").asString();
            JsonNode row = index.get(path);
            String layer =
                    f.path("layer").asString(row == null ? "" : row.path("layer").asString());
            boolean isSelected = row != null && row.path("selected").asBoolean();
            boolean fe = FE_LAYERS.contains(layer);
            files++;
            feFiles += fe ? 1 : 0;
            if (isSelected) {
                selected++;
                feSelected += fe ? 1 : 0;
            } else {
                notSelected.add(path);
            }
        }
        details.put("labelFilesNotSelected", notSelected);

        Map<String, Object> coverage = new LinkedHashMap<>();
        coverage.put("labelFiles", ratio(selected, files));
        coverage.put("labelFeFiles", ratio(feSelected, feFiles));
        if (report.has("apis")) {
            Set<String> detected = apis(report.path("apis"));
            Set<String> expected = apis(label.path("apis"));
            Set<String> hit = new LinkedHashSet<>(detected);
            hit.retainAll(expected);
            coverage.put("apiRecall", ratio(hit.size(), expected.size()));
            coverage.put("apiPrecision", ratio(hit.size(), detected.size()));
            Set<String> missed = new LinkedHashSet<>(expected);
            missed.removeAll(detected);
            Set<String> extra = new LinkedHashSet<>(detected);
            extra.removeAll(expected);
            details.put("apisMissed", missed);
            details.put("apisExtra", extra);
        } else {
            coverage.put("apiRecall", null);
            coverage.put("apiPrecision", null);
        }
        return coverage;
    }

    private static Set<String> apis(JsonNode list) {
        Set<String> apis = new LinkedHashSet<>();
        for (JsonNode api : list) {
            String path = api.path("path").asString().strip();
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            apis.add(api.path("method").asString().strip().toUpperCase(Locale.ROOT) + " " + path);
        }
        return apis;
    }

    /** B 금지 사실: 수준마다 출력 텍스트에 패턴(대소문자 무시)이 하나라도 나온 사실 수의 합. */
    private static int forbidden(JsonNode report, JsonNode label, Map<String, Object> details) {
        int hits = 0;
        List<String> found = new ArrayList<>();
        for (Map.Entry<String, JsonNode> level : report.path("results").properties()) {
            StringBuilder text = new StringBuilder();
            collectText(level.getValue(), text);
            String haystack = text.toString().toLowerCase(Locale.ROOT);
            for (JsonNode fact : label.path("forbidden")) {
                boolean hit = false;
                for (JsonNode pattern : fact.path("patterns")) {
                    String p = pattern.asString().toLowerCase(Locale.ROOT);
                    hit |= !p.isBlank() && haystack.contains(p);
                }
                if (hit) {
                    hits++;
                    found.add(level.getKey() + ": " + fact.path("fact").asString());
                }
            }
        }
        details.put("forbiddenHits", found);
        return hits;
    }

    private static void collectText(JsonNode node, StringBuilder out) {
        if (node.isString()) {
            out.append(node.asString()).append('\n');
        } else if (node.isArray()) {
            node.forEach(child -> collectText(child, out));
        } else if (node.isObject()) {
            for (Map.Entry<String, JsonNode> e : node.properties()) {
                if (!NOT_TEXT.contains(e.getKey())) {
                    collectText(e.getValue(), out);
                }
            }
        }
    }

    /** 개념 이름 비교용: 대소문자·공백·{@code @} 무시 (AC6). */
    static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[\\s@]", "");
    }

    /** {@code fixtureZip}을 앱과 같은 규칙으로 다시 읽은 파일 본문 (경로 → 본문). */
    private static Map<String, String> sources(JsonNode report) {
        String zip = report.path("fixtureZip").asString("");
        if (zip.isBlank() || !Files.exists(Path.of(zip))) {
            throw new IllegalArgumentException("보고서의 fixtureZip이 없습니다: " + zip);
        }
        Map<String, String> sources = new HashMap<>();
        for (SourceFile f :
                new ZipArchiveReader(EvalSettings.defaults().analysis())
                        .read(Path.of(zip))
                        .files()) {
            sources.put(f.path(), f.content());
        }
        return sources;
    }

    /** 소수 넷째 자리까지. 분모가 0이면 null (대상 없음). */
    static Double ratio(long numerator, long denominator) {
        return denominator == 0 ? null : Math.round(numerator * 10000.0 / denominator) / 10000.0;
    }

    /** {@code src/test/resources/prompt-eval/<name>} 텍스트. */
    static String resource(String name) {
        try (InputStream in = EvalGrader.class.getResourceAsStream("/prompt-eval/" + name)) {
            if (in == null) {
                throw new IllegalStateException("리소스 없음: prompt-eval/" + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
