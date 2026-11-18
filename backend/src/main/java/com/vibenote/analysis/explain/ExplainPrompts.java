package com.vibenote.analysis.explain;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.pipeline.Layer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EXPLAINING 프롬프트의 입력 조각 (F-06 AC3, 5.2절). 파일은 {@code <<<FILE no=.. path=.. layer=..>>>} ~ {@code
 * <<<END FILE>>>}로 감싸고, 긴 내용은 프롬프트에서만 자른다 (DB의 content는 그대로).
 */
final class ExplainPrompts {

    /** 개요 입력에서 파일 하나에 넣는 최대 글자 수. */
    static final int OVERVIEW_FILE_CHARS = 3000;

    static final String NO_APIS = "(탐지된 API 없음)";

    /** 파일 목록·API 목록이 각각 개요 입력 상한에서 차지할 수 있는 몫(1/4). 나머지는 코드에 쓴다. */
    private static final int META_SHARE = 4;

    /** 개요 입력에 코드를 넣는 순서 (5.2절). */
    private static final List<String> OVERVIEW_LAYERS =
            List.of(Layer.CONFIG.value(), Layer.CONTROLLER.value(), Layer.SERVICE.value());

    private ExplainPrompts() {}

    /**
     * @param fileTree 선별 파일 전체의 {@code path [layer]} 줄
     * @param apis 탐지한 API 줄 (없으면 {@link #NO_APIS})
     * @param sources 구분자로 감싼 코드 (상한 안에서 채운 만큼)
     */
    record OverviewInput(String fileTree, String apis, String sources) {
        int totalLength() {
            return fileTree.length() + apis.length() + sources.length();
        }
    }

    static String truncate(String content, int maxChars) {
        if (content.length() <= maxChars) {
            return content;
        }
        return content.substring(0, maxChars)
                + "\n// ... (이하 "
                + (content.length() - maxChars)
                + "자 생략)";
    }

    static String fileBlock(int no, AnalysisFile f, int maxChars) {
        return "<<<FILE no=%d path=%s layer=%s>>>\n%s\n<<<END FILE>>>"
                .formatted(no, f.getPath(), f.getLayer(), truncate(f.getContent(), maxChars));
    }

    /** 파일 묶음. 번호는 1부터 묶음 안 순서다. */
    static String files(List<AnalysisFile> batch, int maxChars) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < batch.size(); i++) {
            if (i > 0) {
                out.append("\n\n");
            }
            out.append(fileBlock(i + 1, batch.get(i), maxChars));
        }
        return out.toString();
    }

    /**
     * 줄을 앞에서부터 {@code maxChars} 안에 들어가는 만큼 잇는다. 남은 줄이 있으면 생략 줄을 붙이며, 생략 줄을 포함해도 {@code maxChars}를
     * 넘지 않는다.
     */
    static String limitLines(List<String> lines, int maxChars) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String line = (i == 0 ? "" : "\n") + lines.get(i);
            int rest = lines.size() - i - 1;
            String marker = rest == 0 ? "" : omitted(false, rest);
            if (out.length() + line.length() + marker.length() > maxChars) {
                String tail = omitted(out.isEmpty(), lines.size() - i);
                return out.length() + tail.length() <= maxChars ? out + tail : out.toString();
            }
            out.append(line);
        }
        return out.toString();
    }

    private static String omitted(boolean first, int count) {
        return (first ? "" : "\n") + "... (이하 " + count + "줄 생략)";
    }

    /**
     * 개요 입력. 파일 목록과 API 목록을 먼저 두고(각각 상한의 1/4까지, 넘으면 줄 단위로 자름), 남은 자리에 config → controller → service
     * 파일(파일당 3,000자)을 순서대로 넣다가 다음 파일이 들어가지 않으면 멈춘다. 합계는 {@code maxChars}를 넘지 않는다.
     */
    static OverviewInput overviewInput(
            List<AnalysisFile> files, List<ApiEndpoint> apis, int maxChars) {
        int metaChars = maxChars / META_SHARE;
        String fileTree =
                limitLines(
                        files.stream().map(f -> f.getPath() + " [" + f.getLayer() + "]").toList(),
                        metaChars);
        Map<Long, String> paths = new HashMap<>();
        files.forEach(f -> paths.put(f.getId(), f.getPath()));
        String apiLines =
                apis.isEmpty()
                        ? NO_APIS
                        : limitLines(
                                apis.stream()
                                        .map(
                                                a ->
                                                        a.method()
                                                                + " "
                                                                + a.path()
                                                                + " ("
                                                                + paths.getOrDefault(
                                                                        a.fileId(), "?")
                                                                + ")")
                                        .toList(),
                                metaChars);

        int budget = maxChars - fileTree.length() - apiLines.length();
        StringBuilder sources = new StringBuilder();
        int no = 0;
        fill:
        for (String layer : OVERVIEW_LAYERS) {
            for (AnalysisFile f : files) {
                if (!layer.equals(f.getLayer())) {
                    continue;
                }
                String block =
                        (sources.isEmpty() ? "" : "\n\n")
                                + fileBlock(no + 1, f, OVERVIEW_FILE_CHARS);
                if (sources.length() + block.length() > budget) {
                    break fill;
                }
                sources.append(block);
                no++;
            }
        }
        return new OverviewInput(fileTree, apiLines, sources.toString());
    }
}
