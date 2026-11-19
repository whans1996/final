package com.vibenote.analysis.flow;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.ExplainPrompts;
import com.vibenote.analysis.pipeline.JavaComments;
import com.vibenote.analysis.pipeline.Layer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 실행 흐름 프롬프트의 입력 파일을 고르고 이어 붙인다 (F-07 AC2·AC3). LLM 없이 규칙으로 추적하는 순수 함수다.
 *
 * <p>controller 파일에서 시작해, 주석 밖의 대문자 식별자와 파일 기본 이름이 같은(또는 {@code 이름Impl}) service·mapper 파일을 1단계로
 * 넣고, 넣은 service 파일에서 같은 규칙으로 1단계 더 넣는다.
 */
public final class FlowContextBuilder {

    private static final Pattern TYPE_NAME = Pattern.compile("\\b[A-Z]\\w*");
    private static final List<String> LAYERS =
            List.of(Layer.CONTROLLER.value(), Layer.SERVICE.value(), Layer.MAPPER.value());

    private FlowContextBuilder() {}

    /**
     * @param controllerFileId API 매핑이 있는 파일 (개요 {@code apis[].fileId})
     * @return controller → service → mapper 순, 같은 layer 안은 id 오름차순, 최대 {@code maxFiles}개.
     *     controller 파일이 없으면 빈 목록
     */
    public static List<AnalysisFile> inputFiles(
            List<AnalysisFile> files, long controllerFileId, int maxFiles) {
        AnalysisFile controller =
                files.stream()
                        .filter(f -> f.getId() != null && f.getId() == controllerFileId)
                        .findFirst()
                        .orElse(null);
        if (controller == null) {
            return List.of();
        }
        List<AnalysisFile> candidates =
                files.stream()
                        .filter(
                                f ->
                                        Layer.SERVICE.value().equals(f.getLayer())
                                                || Layer.MAPPER.value().equals(f.getLayer()))
                        .toList();

        Set<AnalysisFile> picked = new LinkedHashSet<>();
        picked.add(controller);
        List<AnalysisFile> first = referenced(controller, candidates);
        picked.addAll(first);
        for (AnalysisFile f : first) {
            if (Layer.SERVICE.value().equals(f.getLayer())) {
                picked.addAll(referenced(f, candidates));
            }
        }
        return picked.stream()
                .sorted(
                        Comparator.comparingInt((AnalysisFile f) -> LAYERS.indexOf(f.getLayer()))
                                .thenComparing(AnalysisFile::getId))
                .limit(maxFiles)
                .toList();
    }

    /**
     * 파일들을 {@code <<<FILE no=.. path=.. layer=..>>>}로 감싸 순서대로 잇는다. 파일은 {@code maxFileChars}에서 자르고,
     * 다음 파일이 합계 {@code maxChars}를 넘기면 멈춘다. 첫 파일(controller)은 항상 넣는다.
     */
    public static String render(List<AnalysisFile> inputFiles, int maxFileChars, int maxChars) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < inputFiles.size(); i++) {
            String block =
                    (i == 0 ? "" : "\n\n")
                            + ExplainPrompts.fileBlock(i + 1, inputFiles.get(i), maxFileChars);
            if (i > 0 && out.length() + block.length() > maxChars) {
                break;
            }
            out.append(block);
        }
        return out.toString();
    }

    /** {@code from}의 주석 밖 대문자 식별자가 가리키는 후보 파일. */
    private static List<AnalysisFile> referenced(AnalysisFile from, List<AnalysisFile> candidates) {
        Set<String> names = new HashSet<>();
        Matcher m = TYPE_NAME.matcher(JavaComments.strip(from.getContent()));
        while (m.find()) {
            names.add(m.group());
            names.add(m.group() + "Impl");
        }
        List<AnalysisFile> result = new ArrayList<>();
        for (AnalysisFile f : candidates) {
            if (f != from && names.contains(baseName(f.getPath()))) {
                result.add(f);
            }
        }
        return result;
    }

    private static String baseName(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }
}
