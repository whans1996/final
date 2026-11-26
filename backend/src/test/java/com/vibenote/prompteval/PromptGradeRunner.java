package com.vibenote.prompteval;

import static com.vibenote.prompteval.PromptEvalRunner.option;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.vibenote.llm.LiveLlmClients;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 평가 보고서 채점 (E-1 AC5~AC10). <b>사람이 요청할 때만</b> {@code ./gradlew promptGrade}로 실행한다 (판정은 Gemini 무료
 * 한도를 쓴다). {@code test}에서는 제외된다.
 *
 * <p>옵션 (Gradle {@code -P}, 경로는 backend 폴더 기준):
 *
 * <ul>
 *   <li>{@code report}: 러너 보고서 파일 하나, 또는 보고서가 든 폴더 (폴더면 {@code summary.md}도 쓴다)
 *   <li>{@code labels}: DS-04 라벨 폴더 (파일 이름 {@code <fixture id>.json}). 없으면 C·커버리지는 "라벨 없음"
 *   <li>{@code judge}: {@code gemini}(기본) 또는 {@code none}. Gemini 키가 없으면 none과 같다 (AC9)
 *   <li>{@code seed}: 수준 판정에서 섞는 순서 (기본 42)
 *   <li>{@code pauseMs}: 판정 호출 사이 대기 (기본 4000)
 * </ul>
 *
 * <p>결과: 보고서 옆 {@code <보고서>-grade.json}(세부 포함, gitignore된 {@code .harness/prompt-eval/}). 콘솔과
 * {@code summary.md}에는 점수·개수만 쓴다.
 */
@Tag("prompt-eval")
class PromptGradeRunner {

    private static final ObjectMapper JSON = JsonMapper.builder().build();

    @Test
    void grade() throws IOException {
        String reportOption = option("report", null);
        assumeTrue(reportOption != null, "-Preport=<보고서 파일|폴더>가 없어 건너뜀");
        Path target = Path.of(reportOption).toAbsolutePath().normalize();
        String labelsOption = option("labels", null);
        Path labels = labelsOption == null ? null : Path.of(labelsOption).toAbsolutePath();
        long seed = Long.parseLong(option("seed", "42"));
        Optional<LlmClient> judge =
                judge(option("judge", "gemini"), Long.parseLong(option("pauseMs", "4000")));

        List<Path> reports;
        if (Files.isDirectory(target)) {
            try (Stream<Path> list = Files.list(target)) {
                reports =
                        list.filter(p -> p.toString().endsWith(".json"))
                                .filter(p -> !p.toString().endsWith("-grade.json"))
                                .sorted()
                                .toList();
            }
        } else {
            reports = List.of(target);
        }

        EvalGrader grader = new EvalGrader(judge, seed);
        List<Map<String, Object>> grades = new ArrayList<>();
        for (Path report : reports) {
            JsonNode node = JSON.readTree(report.toFile());
            if (!node.has("units")) {
                System.out.printf("[prompt-grade] 보고서가 아니어서 건너뜀: %s%n", report.getFileName());
                continue;
            }
            JsonNode label = label(labels, node.path("fixture").asString(""));
            Map<String, Object> grade = grader.grade(node, label);
            grades.add(grade);
            Path out =
                    report.resolveSibling(
                            report.getFileName().toString().replaceFirst("\\.json$", "")
                                    + "-grade.json");
            Files.writeString(out, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(grade));
            System.out.printf(
                    "[prompt-grade] %s split=%s 라벨=%s scores=%s%n",
                    report.getFileName(),
                    grade.get("split"),
                    label != null,
                    JSON.writeValueAsString(grade.get("scores")));
        }
        if (Files.isDirectory(target) && !grades.isEmpty()) {
            String summary = EvalSummary.markdown(grades);
            Files.writeString(target.resolve("summary.md"), summary);
            System.out.println(summary);
        }
    }

    /** 판정 LLM. {@code none}이거나 Gemini 키가 없으면 empty (D·정답 유일률을 건너뛴다). */
    private static Optional<LlmClient> judge(String option, long pauseMs) {
        switch (option.toLowerCase(Locale.ROOT)) {
            case "none" -> {
                return Optional.empty();
            }
            case "gemini" -> {
                LlmProperties.Gemini props =
                        PromptEvalRunner.binder().bindOrCreate("llm", LlmProperties.class).gemini();
                if (props.apiKey() == null || props.apiKey().isBlank()) {
                    System.out.println("[prompt-grade] GEMINI_API_KEY가 없어 판정을 건너뜀");
                    return Optional.empty();
                }
                LlmClient gemini = LiveLlmClients.gemini(props);
                AtomicBoolean first = new AtomicBoolean(true);
                return Optional.of(
                        request -> {
                            if (!first.getAndSet(false)) {
                                pause(pauseMs);
                            }
                            return gemini.complete(request);
                        });
            }
            default -> throw new IllegalArgumentException("judge는 gemini 또는 none: " + option);
        }
    }

    private static JsonNode label(Path labels, String fixture) {
        if (labels == null) {
            return null;
        }
        Path file = labels.resolve(fixture + ".json");
        return Files.exists(file) ? JSON.readTree(file.toFile()) : null;
    }

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
