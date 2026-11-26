package com.vibenote.prompteval;

import static com.vibenote.prompteval.GradeFixtures.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** E-1 AC5·AC6·AC7·AC9: 합성 보고서(dev DS-02-01)를 채점한다. 판정 LLM은 가짜 클라이언트만 쓴다. */
class EvalGraderTest {

    @TempDir Path dir;

    static final double EPS = 1e-3;

    JsonNode scores(ObjectNode report, JsonNode label) {
        Map<String, Object> grade = new EvalGrader(Optional.empty(), 42).grade(report, label);
        return JSON.valueToTree(grade).path("scores");
    }

    @Test
    void AC5_없는_경로와_심볼을_환각으로_센다() {
        JsonNode scores = scores(GradeFixtures.report(GradeFixtures.zip(dir)), null);

        // A: 단계별 첫 시도 통과율·최종 통과율 (file은 junior가 재시도, quiz는 senior가 실패)
        assertThat(scores.path("A").path("overview").path("firstPass").asDouble()).isEqualTo(1.0);
        assertThat(scores.path("A").path("file").path("firstPass").asDouble())
                .isCloseTo(2.0 / 3, within(EPS));
        assertThat(scores.path("A").path("file").path("ok").asDouble()).isEqualTo(1.0);
        assertThat(scores.path("A").path("quiz").path("ok").asDouble())
                .isCloseTo(2.0 / 3, within(EPS));
        // B: 수준마다 가리킨 경로 9개(읽는 순서 2 + 흐름 단계 4 + 개념 3) 중 index에 없는 것 2개
        assertThat(scores.path("B").path("unknownPath").asDouble()).isCloseTo(2.0 / 9, within(EPS));
        // 경로가 있는 흐름 단계 심볼 3개 중 ArticleMapper.insertArticle은 본문에 없다
        assertThat(scores.path("B").path("unknownSymbol").asDouble())
                .isCloseTo(1.0 / 3, within(EPS));
        // 근거: 개념 3개(L25는 20줄 파일 밖) + 흐름 단계 4개(심볼 없음 1, 경로 없음 1) → 7개 중 4개 유효
        assertThat(scores.path("evidence").path("valid").asDouble())
                .isCloseTo(4.0 / 7, within(EPS));
        assertThat(scores.path("evidence").path("checked").asInt()).isEqualTo(21);
    }

    @Test
    void AC5_lineHint가_줄_수를_넘으면_근거_무효() {
        ObjectNode report = GradeFixtures.report(GradeFixtures.zip(dir));
        // 15줄 파일(ArticleCommandService)을 가리키는 개념 하나씩만 남긴다
        String[] hints = {"L1-L15", "L14-L16", "L0"};
        String[] levels = {"beginner", "junior", "senior"};
        for (int i = 0; i < levels.length; i++) {
            ObjectNode result = (ObjectNode) report.path("results").path(levels[i]);
            result.putArray("flows");
            ArrayNode concepts = result.putArray("concepts");
            ObjectNode concept = concepts.addObject();
            concept.put("name", "@Transactional");
            concept.put("path", GradeFixtures.SVC);
            concept.put("lineHint", hints[i]);
        }

        JsonNode scores = scores(report, null);

        assertThat(scores.path("evidence").path("checked").asInt()).isEqualTo(3);
        assertThat(scores.path("evidence").path("valid").asDouble())
                .isCloseTo(1.0 / 3, within(EPS));
    }

    @Test
    void AC6_must_재현율과_정밀도를_계산한다() {
        JsonNode scores =
                scores(GradeFixtures.report(GradeFixtures.zip(dir)), GradeFixtures.label());

        // 수준마다 출력 3개(@Transactional·의존성 주입 (DI)·JPA 엔티티), 라벨 must 3개 중 2개 재현.
        // "의존성 주입 (DI)"와 "의존성 주입(DI)"는 공백을 무시해 같은 개념이다
        JsonNode c = scores.path("C");
        assertThat(c.path("mustRecall").asDouble()).isCloseTo(2.0 / 3, within(EPS));
        assertThat(c.path("precision").asDouble()).isCloseTo(2.0 / 3, within(EPS));
        assertThat(c.path("wrongRate").asDouble()).isCloseTo(1.0 / 3, within(EPS));
        // 재현한 must 2개 중 근거 파일이 맞는 것은 @Transactional 하나 (DI는 ArticlesApi를 가리킴)
        assertThat(c.path("mustEvidenceMatch").asDouble()).isCloseTo(0.5, within(EPS));

        // 커버리지: 라벨 파일 4개 중 선별 2개, FE(api layer) 1개 중 0개. API 라벨 2개·탐지 2개 중 1개 일치
        JsonNode coverage = scores.path("coverage");
        assertThat(coverage.path("labelFiles").asDouble()).isCloseTo(0.5, within(EPS));
        assertThat(coverage.path("labelFeFiles").asDouble()).isEqualTo(0.0);
        assertThat(coverage.path("apiRecall").asDouble()).isCloseTo(0.5, within(EPS));
        assertThat(coverage.path("apiPrecision").asDouble()).isCloseTo(0.5, within(EPS));
        // 금지 사실 문장은 판정 모델로만 센다 (ForbiddenJudgeTest). 판정이 꺼져 있으면 건너뛴다
        assertThat(scores.path("B").path("forbiddenHits").asString())
                .isEqualTo(EvalGrader.JUDGE_SKIPPED);
    }

    @Test
    void AC6_라벨이_없으면_C와_커버리지는_라벨_없음() {
        JsonNode scores = scores(GradeFixtures.report(GradeFixtures.zip(dir)), null);

        assertThat(scores.path("C").asString()).isEqualTo(EvalGrader.NO_LABEL);
        assertThat(scores.path("coverage").asString()).isEqualTo(EvalGrader.NO_LABEL);
        assertThat(scores.path("B").path("forbiddenHits").asString())
                .isEqualTo(EvalGrader.NO_LABEL);
        // 라벨 없이도 기계 지표는 계산된다
        assertThat(scores.path("B").path("unknownPath").isNumber()).isTrue();
    }

    @Test
    void AC9_판정을_끄면_판정_지표만_건너뛴다() {
        Map<String, Object> grade =
                new EvalGrader(Optional.empty(), 42)
                        .grade(GradeFixtures.report(GradeFixtures.zip(dir)), GradeFixtures.label());
        JsonNode node = JSON.valueToTree(grade);
        JsonNode scores = node.path("scores");

        assertThat(scores.path("D").asString()).isEqualTo(EvalGrader.JUDGE_SKIPPED);
        assertThat(scores.path("B").path("forbiddenHits").asString())
                .isEqualTo(EvalGrader.JUDGE_SKIPPED);
        assertThat(scores.path("E").path("uniqueAnswer").asString())
                .isEqualTo(EvalGrader.JUDGE_SKIPPED);
        // 기계 검사는 그대로: 수준마다 2문항 중 해설에 "2번 보기"가 있는 1문항이 실패
        assertThat(scores.path("E").path("mechanical").asDouble()).isCloseTo(0.5, within(EPS));
        assertThat(scores.path("E").path("questions").asInt()).isEqualTo(6);
        assertThat(scores.path("C").path("mustRecall").isNumber()).isTrue();
        assertThat(node.path("fixture").asString()).isEqualTo(GradeFixtures.FIXTURE);
        assertThat(node.path("split").asString()).isEqualTo("dev");
        assertThat(node.path("contamination").asString()).isEqualTo("high");
    }

    @Test
    void AC5_fixtureZip이_없으면_러너_캐시에서_같은_이름을_찾는다() throws IOException {
        Path repos = Files.createDirectories(dir.resolve("repos"));
        Path cached = Files.writeString(repos.resolve("DS-02-01.zip"), "");
        String elsewhere = dir.resolve("other-pc/repos/DS-02-01.zip").toString();

        assertThat(EvalPaths.locateZip(cached.toString(), repos)).isEqualTo(cached);
        assertThat(EvalPaths.locateZip(elsewhere, repos)).isEqualTo(cached);
        assertThat(EvalPaths.locateZip(dir.resolve("x/DS-02-02.zip").toString(), repos)).isNull();
        assertThat(EvalPaths.locateZip("", repos)).isNull();
    }

    @Test
    void AC7_판정을_켜면_D와_E_정답_유일률과_금지_사실이_채워진다() {
        // 판정 프롬프트 첫 줄로 세 판정을 구분해 답한다 (수준은 맞히고, 정답은 answerIndex와 같은 하나, 금지 사실은 1번 주장)
        LlmClient judge =
                request -> {
                    String p = request.prompt();
                    String content;
                    if (p.startsWith("너는 코드 설명의 대상 수준")) {
                        content = LevelJudgeTest.correctAnswer(p);
                    } else if (p.startsWith("너는 객관식 퀴즈 문항")) {
                        content = p.contains("예외가") ? "{\"correct\":[1]}" : "{\"correct\":[0]}";
                    } else {
                        content = "{\"made\":[1]}";
                    }
                    return new LlmResponse(content, "gemini-test", 1, 1);
                };

        JsonNode scores =
                JSON.valueToTree(
                                new EvalGrader(Optional.of(judge), 42)
                                        .grade(
                                                GradeFixtures.report(GradeFixtures.zip(dir)),
                                                GradeFixtures.label()))
                        .path("scores");

        assertThat(scores.path("D").path("accuracy").asDouble()).isEqualTo(1.0);
        assertThat(scores.path("D").path("judged").asInt()).isEqualTo(12);
        assertThat(scores.path("E").path("uniqueAnswer").asDouble()).isEqualTo(1.0);
        // 수준 3개 × 1건, 판정 수는 수준 3개 × 라벨 금지 문장 2개
        assertThat(scores.path("B").path("forbiddenHits").asInt()).isEqualTo(3);
        assertThat(scores.path("B").path("forbiddenChecked").asInt()).isEqualTo(6);
        // 기계 지표는 판정과 무관하게 그대로
        assertThat(scores.path("B").path("unknownPath").asDouble()).isCloseTo(2.0 / 9, within(EPS));
    }
}
