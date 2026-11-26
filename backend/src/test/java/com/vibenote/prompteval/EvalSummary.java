package com.vibenote.prompteval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.function.Function;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 채점 결과 여러 개의 요약 Markdown (E-1 AC10). 공급자 × 분할 × 오염 그룹(팀 생성 {@code none} / {@code low} / {@code
 * high})으로 묶어 저장소 평균(금지 사실은 합계)과 v2 절대 하한 대비 ✅/❌를 낸다. <b>점수·개수만</b> 쓰고 fixture id·라벨 내용(개념 이름·파일
 * 경로·금지 사실)은 쓰지 않는다. 세부는 각 {@code *-grade.json}의 {@code details}에만 있다.
 */
final class EvalSummary {

    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final List<String> CONTAMINATION = List.of("none", "low", "high");
    private static final List<String> SPLIT = List.of("eval", "dev", "demo");

    /** 지표 열: 머리글 → 저장소 하나의 값 (없으면 null). */
    private record Column(String header, Function<JsonNode, Double> value, boolean sum) {}

    private static final List<Column> COLUMNS =
            List.of(
                    new Column("A 첫 시도(최저 단계)", s -> worstStage(s, "firstPass"), false),
                    new Column("A 최종(최저 단계)", s -> worstStage(s, "ok"), false),
                    new Column("B 없는 경로", s -> number(s, "B", "unknownPath"), false),
                    new Column("B 없는 심볼", s -> number(s, "B", "unknownSymbol"), false),
                    new Column("B 금지 사실(합)", s -> number(s, "B", "forbiddenHits"), true),
                    new Column("C must 재현율", s -> number(s, "C", "mustRecall"), false),
                    new Column("C 정밀도", s -> number(s, "C", "precision"), false),
                    new Column("C wrong", s -> number(s, "C", "wrongRate"), false),
                    new Column("C 근거 일치", s -> number(s, "C", "mustEvidenceMatch"), false),
                    new Column("D 수준 구분", s -> number(s, "D", "accuracy"), false),
                    new Column("E 기계 검사", s -> number(s, "E", "mechanical"), false),
                    new Column("E 정답 유일", s -> number(s, "E", "uniqueAnswer"), false),
                    new Column("파일 선별", s -> number(s, "coverage", "labelFiles"), false),
                    new Column("FE 파일 선별", s -> number(s, "coverage", "labelFeFiles"), false),
                    new Column("API 재현율", s -> number(s, "coverage", "apiRecall"), false),
                    new Column("API 정밀도", s -> number(s, "coverage", "apiPrecision"), false),
                    new Column("근거 유효율", s -> number(s, "evidence", "valid"), false));

    /** v2 절대 하한 (기획서 6.1절): 머리글 → 저장소 평균에 대한 하한. */
    private record Floor(String header, Function<JsonNode, Double> value, double min) {}

    private static final List<Floor> FLOORS =
            List.of(
                    new Floor("스키마 최종 100%", s -> worstStage(s, "ok"), 1.0),
                    new Floor("근거 유효율 90%", s -> number(s, "evidence", "valid"), 0.9),
                    new Floor("수준 구분 70%", s -> number(s, "D", "accuracy"), 0.7),
                    new Floor("정답 유일 100%", s -> number(s, "E", "uniqueAnswer"), 1.0));

    private EvalSummary() {}

    /**
     * @param grades 채점 결과. 같은 공급자·모델·fixture·단계가 여럿이면 목록에서 마지막 것(보고서 파일 이름이 시각순이라 최신)만 쓴다
     */
    static String markdown(List<Map<String, Object>> grades) {
        // 다시 돌린 보고서가 평균에 두 번 들어가지 않게 최신 하나만 남긴다
        Map<List<String>, JsonNode> latest = new LinkedHashMap<>();
        for (Map<String, Object> grade : grades) {
            JsonNode node = JSON.valueToTree(grade);
            List<String> run =
                    List.of(
                            text(node, "provider"),
                            text(node, "model"),
                            text(node, "fixture"),
                            text(node, "stage"));
            latest.remove(run);
            latest.put(run, node);
        }
        int replaced = grades.size() - latest.size();

        // dev·demo 결과가 eval 점수에 섞이지 않게 분할도 그룹에 넣는다 (09-데이터셋 5절)
        Map<List<String>, List<JsonNode>> groups =
                new TreeMap<>(
                        Comparator.<List<String>, String>comparing(k -> k.get(0))
                                .thenComparing(k -> order(SPLIT, k.get(1)))
                                .thenComparing(k -> k.get(1))
                                .thenComparing(k -> order(CONTAMINATION, k.get(2)))
                                .thenComparing(k -> k.get(2)));
        for (JsonNode node : latest.values()) {
            List<String> key =
                    List.of(
                            text(node, "provider"),
                            text(node, "split"),
                            text(node, "contamination"));
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(node.path("scores"));
        }

        StringBuilder md = new StringBuilder("# 프롬프트 평가 요약\n\n");
        md.append(
                "> 점수·개수만 쓴다 (E-1 AC10). 저장소가 여럿인 그룹은 평균이고 금지 사실은 합계다. 항목별 세부는 각"
                        + " `*-grade.json`에 있다. `-`는 계산 대상 없음·라벨 없음·판정 건너뜀. 점수는 분할 `eval`만이고"
                        + " `dev`·`demo`는 개발용 참고다.\n");
        if (replaced > 0) {
            md.append("> 같은 공급자·모델·저장소·단계를 다시 채점한 결과 ")
                    .append(replaced)
                    .append("개는 최신 것으로 바꿔 셌다.\n");
        }
        md.append("\n## 지표\n\n| 공급자 | 분할 | 오염 | 저장소 |");
        COLUMNS.forEach(c -> md.append(' ').append(c.header()).append(" |"));
        md.append("\n|---|---|---|---|").append("---|".repeat(COLUMNS.size())).append('\n');
        groups.forEach(
                (key, scores) -> {
                    md.append(row(key, scores));
                    for (Column c : COLUMNS) {
                        md.append(' ')
                                .append(format(aggregate(scores, c.value(), c.sum()), c.sum()))
                                .append(" |");
                    }
                    md.append('\n');
                });

        md.append("\n## v2 절대 하한\n\n| 공급자 | 분할 | 오염 | 저장소 |");
        FLOORS.forEach(f -> md.append(' ').append(f.header()).append(" |"));
        md.append("\n|---|---|---|---|").append("---|".repeat(FLOORS.size())).append('\n');
        groups.forEach(
                (key, scores) -> {
                    md.append(row(key, scores));
                    for (Floor f : FLOORS) {
                        Double value = aggregate(scores, f.value(), false);
                        md.append(' ')
                                .append(
                                        value == null
                                                ? "-"
                                                : (value >= f.min() - 1e-9 ? "✅ " : "❌ ")
                                                        + format(value, false))
                                .append(" |");
                    }
                    md.append('\n');
                });
        return md.toString();
    }

    private static String row(List<String> key, List<JsonNode> scores) {
        return "| " + String.join(" | ", key) + " | " + scores.size() + " |";
    }

    private static String text(JsonNode node, String field) {
        String value = node.path(field).asString(null);
        return value == null || value.isBlank() ? "unknown" : value;
    }

    /** 정해 둔 순서의 위치. 목록에 없으면 맨 뒤. */
    private static int order(List<String> values, String value) {
        int i = values.indexOf(value);
        return i < 0 ? values.size() : i;
    }

    /** 숫자가 있는 저장소만 평균(또는 합). 하나도 없으면 null. */
    private static Double aggregate(
            List<JsonNode> scores, Function<JsonNode, Double> value, boolean sum) {
        List<Double> values = scores.stream().map(value).filter(v -> v != null).toList();
        if (values.isEmpty()) {
            return null;
        }
        if (sum) {
            return values.stream().mapToDouble(Double::doubleValue).sum();
        }
        OptionalDouble mean = values.stream().mapToDouble(Double::doubleValue).average();
        return mean.getAsDouble();
    }

    private static String format(Double value, boolean count) {
        if (value == null) {
            return "-";
        }
        return count
                ? String.valueOf(Math.round(value))
                : String.format(Locale.ROOT, "%.1f%%", value * 100);
    }

    private static Double number(JsonNode scores, String metric, String field) {
        JsonNode value = scores.path(metric).path(field);
        return value.isNumber() ? value.asDouble() : null;
    }

    /** 지표 A에서 가장 낮은 단계의 값 (최종 100% 하한은 모든 단계가 100%여야 한다). */
    private static Double worstStage(JsonNode scores, String field) {
        Double worst = null;
        for (JsonNode stage : scores.path("A")) {
            JsonNode value = stage.path(field);
            if (value.isNumber()) {
                worst = worst == null ? value.asDouble() : Math.min(worst, value.asDouble());
            }
        }
        return worst;
    }
}
