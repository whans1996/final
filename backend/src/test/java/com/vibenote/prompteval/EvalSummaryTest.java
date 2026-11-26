package com.vibenote.prompteval;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.node.ObjectNode;

/** E-1 AC10: 채점 결과 여러 개를 공급자 × 오염 그룹으로 묶은 요약. 요약에는 점수·개수만 쓴다. */
class EvalSummaryTest {

    @TempDir Path dir;

    Map<String, Object> graded(String provider, String contamination) {
        return graded(provider, "eval", contamination, GradeFixtures.FIXTURE);
    }

    /** 합성 보고서를 채점한 결과. 그룹 키(분할·오염)와 중복 판단용 fixture id만 바꾼다. */
    Map<String, Object> graded(String provider, String split, String contamination, String id) {
        ObjectNode report = GradeFixtures.report(GradeFixtures.zip(dir));
        report.put("fixture", id);
        report.put("provider", provider);
        report.put("split", split);
        report.put("contamination", contamination);
        return new EvalGrader(Optional.empty(), 42).grade(report, GradeFixtures.label());
    }

    /** v2 하한을 모두 넘는 채점 결과 (손으로 만든 값). */
    static Map<String, Object> passing(String provider, String contamination) {
        Map<String, Object> scores = new LinkedHashMap<>();
        scores.put("A", Map.of("overview", Map.of("firstPass", 1.0, "ok", 1.0)));
        scores.put("B", Map.of("unknownPath", 0.0, "unknownSymbol", 0.0, "forbiddenHits", 0));
        scores.put("D", Map.of("accuracy", 0.8, "judged", 9, "excluded", 0, "judge", "gemini"));
        scores.put("E", Map.of("mechanical", 1.0, "uniqueAnswer", 1.0, "questions", 6));
        scores.put("evidence", Map.of("valid", 0.95, "checked", 20));
        Map<String, Object> grade = new LinkedHashMap<>();
        grade.put("fixture", "DS-02-01");
        grade.put("provider", provider);
        grade.put("split", "eval");
        grade.put("contamination", contamination);
        grade.put("scores", scores);
        return grade;
    }

    @Test
    void AC10_공급자와_오염_그룹으로_묶는다() {
        String md =
                EvalSummary.markdown(
                        List.of(
                                graded("gms", "eval", "high", "DS-02-01"),
                                graded("ollama", "low"),
                                graded("gms", "eval", "high", "DS-02-02"),
                                passing("gms", "none")));

        int none = md.indexOf("| gms | eval | none | 1 |");
        int high = md.indexOf("| gms | eval | high | 2 |");
        int ollama = md.indexOf("| ollama | eval | low | 1 |");
        assertThat(none).as(md).isPositive();
        // 공급자 이름순, 같은 공급자 안에서는 none → low → high
        assertThat(high).isGreaterThan(none);
        assertThat(ollama).isGreaterThan(high);

        // v2 하한: 통과 그룹은 ✅, 합성 보고서 그룹(퀴즈 최종 통과 2/3, 근거 57%)은 ❌, 판정 건너뜀은 -
        String passLine = line(md, "| gms | eval | none |", 1);
        assertThat(passLine).doesNotContain("❌").contains("✅");
        String failLine = line(md, "| gms | eval | high |", 1);
        assertThat(failLine).contains("❌").contains("-");
    }

    @Test
    void AC10_dev_결과는_eval_그룹에_섞이지_않고_다시_채점한_것은_최신만_센다() {
        String md =
                EvalSummary.markdown(
                        List.of(
                                passing("gms", "high"),
                                graded("gms", "dev", "high", "DS-02-01"),
                                graded("gms", "dev", "high", "DS-02-01")));

        // 같은 오염 high라도 dev는 eval과 다른 줄이고, eval 줄이 먼저 나온다
        int eval = md.indexOf("| gms | eval | high | 1 |");
        int dev = md.indexOf("| gms | dev | high | 1 |");
        assertThat(eval).as(md).isPositive();
        assertThat(dev).as(md).isGreaterThan(eval);
        // eval 줄의 하한은 dev 합성 결과(❌)에 끌려 내려가지 않는다
        assertThat(line(md, "| gms | eval | high |", 1)).doesNotContain("❌");
        assertThat(md).contains("최신 것으로 바꿔 셌다");
    }

    @Test
    void AC10_요약에_라벨_내용이_나오지_않는다() {
        String md = EvalSummary.markdown(List.of(graded("gms", "high"), graded("gms", "low")));

        for (String secret :
                List.of(
                        "@Transactional",
                        "의존성",
                        "MyBatis 매퍼",
                        "JPA",
                        "Redis",
                        "ArticlesApi",
                        "io/spring",
                        "/articles",
                        GradeFixtures.FIXTURE)) {
            assertThat(md).as(secret).doesNotContain(secret);
        }
    }

    /** {@code prefix}로 시작하는 {@code nth}번째(0부터) 줄. 점수 표와 하한 표에 같은 그룹 줄이 하나씩 있다. */
    static String line(String md, String prefix, int nth) {
        return md.lines().filter(l -> l.startsWith(prefix)).toList().get(nth);
    }
}
