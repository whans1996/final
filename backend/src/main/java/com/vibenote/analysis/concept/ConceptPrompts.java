package com.vibenote.analysis.concept;

import static com.vibenote.analysis.explain.ExplainPrompts.escapeDelimiters;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.ExplainPrompts;
import com.vibenote.concept.ConceptStatus;
import com.vibenote.concept.domain.Concept;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * EXTRACTING 프롬프트의 입력 조각 (F-08 AC2·AC3, 5.2절). 카드 목록 → 파일 목록 → 줄 번호를 붙인 코드 순서로 채우고, 응답 검증에 쓸 카드
 * id·파일 id·코드가 들어간 파일 번호를 함께 돌려준다.
 */
public final class ConceptPrompts {

    private ConceptPrompts() {}

    /**
     * @param catalog {@code id | name | category} 줄
     * @param fileList {@code no. path [layer] role} 줄
     * @param files 줄 번호를 붙여 구분자로 감싼 코드 (상한 안에서 채운 만큼)
     * @param catalogIds 카드 목록에 실제로 넣은 카드 id. 응답의 {@code conceptId}는 이 안에 있어야 한다
     * @param fileIds 파일 번호(1..n) 순서의 {@code analysis_files.id}
     * @param codeFileNos 코드가 프롬프트에 들어간 파일 번호. {@code lineHint}는 이 파일에만 저장한다
     */
    public record Input(
            String catalog,
            String fileList,
            String files,
            Set<Long> catalogIds,
            List<Long> fileIds,
            Set<Integer> codeFileNos) {

        int totalLength() {
            return catalog.length() + fileList.length() + files.length();
        }
    }

    /**
     * @param concepts 기존 카드 전체 (상태 무관). HIDDEN은 목록에서 뺀다
     * @param files 선별 파일 전체 ({@code analysis_files.id} 오름차순)
     */
    public static Input input(
            List<Concept> concepts, List<AnalysisFile> files, ConceptExtractProperties props) {
        List<Concept> listed =
                concepts.stream()
                        .filter(c -> c.getStatus() != ConceptStatus.HIDDEN)
                        .sorted(
                                Comparator.comparing(
                                                (Concept c) ->
                                                        c.getStatus() != ConceptStatus.PUBLISHED)
                                        .thenComparing(Concept::getId))
                        .toList();
        String catalog =
                ExplainPrompts.limitLines(
                        listed.stream()
                                .map(
                                        c ->
                                                escapeDelimiters(
                                                        c.getId()
                                                                + " | "
                                                                + c.getName().replaceAll("\\R", " ")
                                                                + " | "
                                                                + c.getCategory()))
                                .toList(),
                        props.catalogMaxChars(),
                        "개");
        Set<Long> catalogIds = new HashSet<>();
        int catalogLines = (int) catalog.lines().filter(l -> !l.startsWith("... (이하 ")).count();
        listed.stream().limit(catalogLines).forEach(c -> catalogIds.add(c.getId()));

        List<String> fileLines = new ArrayList<>();
        List<Long> fileIds = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            AnalysisFile f = files.get(i);
            fileIds.add(f.getId());
            fileLines.add(
                    escapeDelimiters(
                            (i + 1)
                                    + ". "
                                    + f.getPath()
                                    + " ["
                                    + f.getLayer()
                                    + "]"
                                    + (f.getRole() == null ? "" : " " + f.getRole())));
        }
        String fileList = ExplainPrompts.limitLines(fileLines, props.fileListMaxChars(), "줄");

        int budget = props.maxChars() - catalog.length() - fileList.length();
        StringBuilder code = new StringBuilder();
        Set<Integer> codeFileNos = new HashSet<>();
        for (int i = 0; i < files.size(); i++) {
            AnalysisFile f = files.get(i);
            String block =
                    (code.isEmpty() ? "" : "\n\n")
                            + ExplainPrompts.fileBlock(
                                    i + 1,
                                    f.getPath(),
                                    f.getLayer(),
                                    numberLines(f.getContent()),
                                    props.maxFileChars());
            if (code.length() + block.length() > budget) {
                break;
            }
            code.append(block);
            codeFileNos.add(i + 1);
        }
        return new Input(
                catalog,
                fileList,
                code.toString(),
                Set.copyOf(catalogIds),
                List.copyOf(fileIds),
                Set.copyOf(codeFileNos));
    }

    /** 줄마다 1부터 {@code 12: } 형태의 번호를 붙인다. */
    static String numberLines(String content) {
        StringBuilder out = new StringBuilder(content.length() + 64);
        int no = 1;
        for (String line : (Iterable<String>) content.lines()::iterator) {
            if (no > 1) {
                out.append('\n');
            }
            out.append(no++).append(": ").append(line);
        }
        return out.toString();
    }
}
