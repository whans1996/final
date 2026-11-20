package com.vibenote.quiz;

import static com.vibenote.analysis.explain.ExplainPrompts.escapeDelimiters;

import com.vibenote.analysis.concept.LinkedConcept;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.ExplainPrompts;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * QUIZ 프롬프트의 입력 조각 (F-14 AC2, 6.2절). 연결 개념 목록 → 코드 순서로 채우고, 응답 검증에 쓸 개념 id를 함께 돌려준다. 코드에는 줄 번호를 붙이지
 * 않는다.
 */
public final class QuizPrompts {

    static final String NO_CONCEPTS = "(없음)";

    private QuizPrompts() {}

    /**
     * @param concepts {@code id | name | contextNote} 줄. 연결이 없으면 {@link #NO_CONCEPTS}
     * @param files 구분자로 감싼 코드 (상한 안에서 채운 만큼)
     * @param conceptIds 개념 목록에 실제로 넣은 카드 id. 응답의 {@code conceptId}는 이 안에 있어야 한다
     */
    public record Input(String concepts, String files, Set<Long> conceptIds) {}

    /**
     * @param concepts 분석의 연결 개념 (HIDDEN 제외, {@code concept_id} 오름차순)
     * @param files 선별 파일 전체 ({@code analysis_files.id} 오름차순)
     */
    public static Input input(
            List<LinkedConcept> concepts, List<AnalysisFile> files, QuizProperties props) {
        String list = NO_CONCEPTS;
        Set<Long> ids = new HashSet<>();
        if (!concepts.isEmpty()) {
            list =
                    ExplainPrompts.limitLines(
                            concepts.stream().map(QuizPrompts::line).toList(),
                            props.conceptListMaxChars(),
                            "개");
            int listed = (int) list.lines().filter(l -> !l.startsWith("... (이하 ")).count();
            concepts.stream().limit(listed).forEach(c -> ids.add(c.conceptId()));
        }

        int budget = props.maxChars() - list.length();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < files.size(); i++) {
            AnalysisFile f = files.get(i);
            String block =
                    (code.isEmpty() ? "" : "\n\n")
                            + ExplainPrompts.fileBlock(i + 1, f, props.maxFileChars());
            if (code.length() + block.length() > budget) {
                break;
            }
            code.append(block);
        }
        return new Input(list, code.toString(), Set.copyOf(ids));
    }

    private static String line(LinkedConcept c) {
        String line = c.conceptId() + " | " + oneLine(c.name());
        if (c.contextNote() != null && !c.contextNote().isBlank()) {
            line += " | " + oneLine(c.contextNote());
        }
        return escapeDelimiters(line);
    }

    private static String oneLine(String value) {
        return value.strip().replaceAll("\\R", " ");
    }
}
