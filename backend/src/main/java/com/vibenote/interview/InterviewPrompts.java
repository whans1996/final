package com.vibenote.interview;

import static com.vibenote.analysis.explain.ExplainPrompts.escapeDelimiters;

import com.vibenote.analysis.concept.LinkedConcept;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.AnalysisOverview;
import com.vibenote.analysis.explain.ExplainPrompts;
import com.vibenote.concept.ConceptStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 면접 질문 프롬프트의 입력 조각 (I-1a AC2, 5.2절). 개요 → 파일 목록 → 개념 목록 → 줄 번호를 붙인 코드 순서로 채우고, 응답 검증에 쓸 파일 순서·코드를
 * 넣은 파일별 줄 수·개념 id를 함께 돌려준다.
 */
public final class InterviewPrompts {

    static final String NONE = "(없음)";

    /** 잘린 코드의 마지막 줄이 번호 접두사까지 보이는지 ({@code 12: }). */
    private static final Pattern NUMBERED_LINE = Pattern.compile("^\\d+: .*");

    private InterviewPrompts() {}

    /**
     * @param overview 개요 요약·기술 스택·레이어 역할·API
     * @param fileList {@code no. path [layer] role} 줄
     * @param concepts {@code id | name | contextNote} 줄
     * @param files 줄 번호를 붙여 구분자로 감싼 코드
     * @param orderedFiles 파일 번호(1..n) 순서의 선별 파일
     * @param codeLines 코드를 넣은 파일 번호 → 넣은 줄 수. 응답의 근거는 이 안에서만 받는다
     * @param conceptIds 개념 목록에 넣은 카드 id
     */
    public record Input(
            String overview,
            String fileList,
            String concepts,
            String files,
            List<AnalysisFile> orderedFiles,
            Map<Integer, Integer> codeLines,
            Set<Long> conceptIds) {}

    /**
     * @param overview 분석 개요 (없으면 null)
     * @param files 선별 파일 전체 (content·importance 포함)
     * @param concepts 분석에 연결된 개념 (HIDDEN은 뺀다)
     */
    public static Input input(
            AnalysisOverview overview,
            List<AnalysisFile> files,
            List<LinkedConcept> concepts,
            InterviewProperties props) {
        List<AnalysisFile> ordered = order(overview, files);

        List<String> fileLines = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            AnalysisFile f = ordered.get(i);
            fileLines.add(
                    escapeDelimiters(
                            (i + 1)
                                    + ". "
                                    + f.getPath()
                                    + " ["
                                    + f.getLayer()
                                    + "]"
                                    + (f.getRole() == null ? "" : " " + oneLine(f.getRole()))));
        }
        String fileList =
                fileLines.isEmpty()
                        ? NONE
                        : ExplainPrompts.limitLines(fileLines, props.fileListMaxChars(), "줄");

        List<LinkedConcept> listed =
                concepts.stream()
                        .filter(c -> !ConceptStatus.HIDDEN.name().equals(c.status()))
                        .toList();
        String conceptText =
                ExplainPrompts.limitLines(
                        listed.stream()
                                .map(
                                        c ->
                                                escapeDelimiters(
                                                        c.conceptId()
                                                                + " | "
                                                                + oneLine(c.name())
                                                                + (c.contextNote() == null
                                                                        ? ""
                                                                        : " | "
                                                                                + oneLine(
                                                                                        c
                                                                                                .contextNote()))))
                                .toList(),
                        props.conceptListMaxChars(),
                        "개");
        Set<Long> conceptIds = new HashSet<>();
        int conceptLines = (int) conceptText.lines().filter(l -> !l.startsWith("... (이하 ")).count();
        listed.stream().limit(conceptLines).forEach(c -> conceptIds.add(c.conceptId()));

        StringBuilder code = new StringBuilder();
        Map<Integer, Integer> codeLines = new LinkedHashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            AnalysisFile f = ordered.get(i);
            String numbered =
                    ExplainPrompts.numberLines(Objects.requireNonNullElse(f.getContent(), ""));
            String block =
                    (code.isEmpty() ? "" : "\n\n")
                            + ExplainPrompts.fileBlock(
                                    i + 1,
                                    f.getPath(),
                                    f.getLayer(),
                                    numbered,
                                    props.maxFileChars());
            if (!code.isEmpty() && code.length() + block.length() > props.maxChars()) {
                break;
            }
            code.append(block);
            codeLines.put(i + 1, visibleLines(numbered, props.maxFileChars()));
        }

        return new Input(
                overview(overview, props.maxApis()),
                fileList,
                conceptText.isEmpty() ? NONE : conceptText,
                code.toString(),
                List.copyOf(ordered),
                Map.copyOf(codeLines),
                Set.copyOf(conceptIds));
    }

    /** 개요 {@code readingOrder}의 선별 파일 먼저(그 순서, 중복 제외), 그다음 나머지를 중요도 내림차순·id 오름차순. */
    static List<AnalysisFile> order(AnalysisOverview overview, List<AnalysisFile> files) {
        Map<Long, AnalysisFile> byId = new HashMap<>();
        files.forEach(f -> byId.put(f.getId(), f));
        Set<AnalysisFile> ordered = new LinkedHashSet<>();
        if (overview != null && overview.readingOrder() != null) {
            for (AnalysisOverview.ReadingStep step : overview.readingOrder()) {
                AnalysisFile f = step.fileId() == null ? null : byId.get(step.fileId());
                if (f != null) {
                    ordered.add(f);
                }
            }
        }
        files.stream()
                .sorted(
                        Comparator.comparing(
                                        (AnalysisFile f) ->
                                                Objects.requireNonNullElse(f.getImportance(), 0),
                                        Comparator.reverseOrder())
                                .thenComparing(AnalysisFile::getId))
                .forEach(ordered::add);
        return new ArrayList<>(ordered);
    }

    /** {@code maxChars}에서 자른 뒤에도 번호 접두사까지 보이는 줄 수. */
    private static int visibleLines(String numbered, int maxChars) {
        if (numbered.isEmpty()) {
            return 0;
        }
        if (numbered.length() <= maxChars) {
            return (int) numbered.lines().count();
        }
        List<String> lines = numbered.substring(0, maxChars).lines().toList();
        int count = lines.size();
        return NUMBERED_LINE.matcher(lines.get(count - 1)).matches() ? count : count - 1;
    }

    private static String overview(AnalysisOverview overview, int maxApis) {
        if (overview == null) {
            return NONE;
        }
        StringBuilder out = new StringBuilder();
        out.append("요약: ").append(oneLine(Objects.requireNonNullElse(overview.summary(), "")));
        out.append("\n기술 스택: ")
                .append(
                        overview.techStack() == null || overview.techStack().isEmpty()
                                ? NONE
                                : String.join(", ", overview.techStack()));
        out.append("\n레이어 역할:");
        if (overview.layers() == null || overview.layers().isEmpty()) {
            out.append(' ').append(NONE);
        } else {
            overview.layers()
                    .forEach(
                            l ->
                                    out.append("\n- ")
                                            .append(l.layer())
                                            .append(": ")
                                            .append(oneLine(l.role())));
        }
        out.append("\nAPI:");
        if (overview.apis() == null || overview.apis().isEmpty()) {
            out.append(' ').append(NONE);
        } else {
            overview.apis().stream()
                    .limit(maxApis)
                    .forEach(
                            a ->
                                    out.append("\n- ")
                                            .append(a.method())
                                            .append(' ')
                                            .append(a.path()));
        }
        return escapeDelimiters(out.toString());
    }

    private static String oneLine(String value) {
        return value == null ? "" : value.replaceAll("\\R", " ");
    }
}
