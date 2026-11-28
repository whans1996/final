package com.vibenote.analysis.explain;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.pipeline.Layer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * EXPLAINING 프롬프트의 입력 조각 (F-06 AC3, 5.2절). 파일은 {@code <<<FILE no=.. path=.. layer=..>>>} ~ {@code
 * <<<END FILE>>>}로 감싸고, 긴 내용은 프롬프트에서만 자른다 (DB의 content는 그대로).
 */
public final class ExplainPrompts {

    /** 개요 입력에서 파일 하나에 넣는 최대 글자 수. */
    static final int OVERVIEW_FILE_CHARS = 3000;

    static final String NO_APIS = "(탐지된 API 없음)";

    static final String NO_README = "(README 없음)";

    /** README·파일 목록·API 목록이 각각 개요 입력 상한에서 차지할 수 있는 몫(1/4). 나머지는 코드에 쓴다. */
    private static final int META_SHARE = 4;

    private static final String README = "README.md";

    /** 파일 목록 줄에서 빼는 layer (test·migration·doc, F-26a 5.4절 1). */
    private static final Set<String> INDEX_ONLY_LAYERS =
            Arrays.stream(Layer.values())
                    .filter(Layer::indexOnly)
                    .map(Layer::value)
                    .collect(Collectors.toSet());

    /** 개요 입력에 코드를 넣는 순서 (5.2절). */
    private static final List<String> OVERVIEW_LAYERS =
            List.of(Layer.CONFIG.value(), Layer.CONTROLLER.value(), Layer.SERVICE.value());

    private ExplainPrompts() {}

    /**
     * @param readme 루트 README 앞부분 (없으면 {@link #NO_README})
     * @param fileTree 인덱스 후보 파일(test·migration·doc 제외)의 {@code path [layer]} 줄
     * @param apis 탐지한 API 줄 (없으면 {@link #NO_APIS})
     * @param sources 구분자로 감싼 코드 (상한 안에서 채운 만큼)
     */
    record OverviewInput(String readme, String fileTree, String apis, String sources) {
        int totalLength() {
            return readme.length() + fileTree.length() + apis.length() + sources.length();
        }
    }

    public static String truncate(String content, int maxChars) {
        if (content.length() <= maxChars) {
            return content;
        }
        return content.substring(0, maxChars)
                + "\n// ... (이하 "
                + (content.length() - maxChars)
                + "자 생략)";
    }

    /**
     * 값 안의 {@code <<<}를 {@code ‹‹‹}로 바꾼다. 코드·경로에 든 {@code <<<END FILE>>>} 같은 문자열이 우리가 만든 구분자처럼 보이지
     * 않게 한다 (F-08 AC3, #23). 프롬프트에 넣는 값에만 쓰고 DB의 내용은 바꾸지 않는다.
     */
    public static String escapeDelimiters(String value) {
        return value.replace("<<<", "‹‹‹");
    }

    /** 줄마다 1부터 {@code 12: } 형태의 번호를 붙인다 (F-08 AC2, I-1a AC2). 응답의 {@code L12-L30} 근거를 검증하는 기준이다. */
    public static String numberLines(String content) {
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

    public static String fileBlock(int no, AnalysisFile f, int maxChars) {
        return fileBlock(no, f.getPath(), f.getLayer(), f.getContent(), maxChars);
    }

    /** {@code content}를 {@code maxChars}에서 잘라 구분자로 감싼다. 경로·내용의 구분자 문자열은 바꿔 넣는다. */
    public static String fileBlock(
            int no, String path, String layer, String content, int maxChars) {
        return "<<<FILE no=%d path=%s layer=%s>>>\n%s\n<<<END FILE>>>"
                .formatted(
                        no,
                        escapeDelimiters(path),
                        layer,
                        escapeDelimiters(truncate(content, maxChars)));
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
        return limitLines(lines, maxChars, "줄");
    }

    /** {@link #limitLines(List, int)}와 같고 생략 줄의 단위를 고른다 (예: 카드 목록은 {@code 개}). */
    public static String limitLines(List<String> lines, int maxChars, String unit) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String line = (i == 0 ? "" : "\n") + lines.get(i);
            int rest = lines.size() - i - 1;
            String marker = rest == 0 ? "" : omitted(false, rest, unit);
            if (out.length() + line.length() + marker.length() > maxChars) {
                String tail = omitted(out.isEmpty(), lines.size() - i, unit);
                return out.length() + tail.length() <= maxChars ? out + tail : out.toString();
            }
            out.append(line);
        }
        return out.toString();
    }

    private static String omitted(boolean first, int count, String unit) {
        return (first ? "" : "\n") + "... (이하 " + count + unit + " 생략)";
    }

    /**
     * 개요 입력. 루트 README·파일 목록·API 목록을 먼저 두고(각각 상한의 1/4까지, README는 {@code readmeMaxChars}까지 앞부분, 목록은
     * 줄 단위로 자름), 남은 자리에 선별 파일 중 config → controller → service 파일(파일당 3,000자)을 순서대로 넣다가 다음 파일이 들어가지
     * 않으면 멈춘다. 합계는 {@code maxChars}를 넘지 않는다 (F-06 5.2절, F-26a 5.6절).
     *
     * @param files 선별 파일 (코드 블록)
     * @param index 인덱스 전체 (README·파일 목록)
     */
    static OverviewInput overviewInput(
            List<AnalysisFile> files,
            List<AnalysisFile> index,
            List<ApiEndpoint> apis,
            int maxChars,
            int readmeMaxChars) {
        int metaChars = maxChars / META_SHARE;
        String readme = readme(index, Math.min(readmeMaxChars, metaChars));
        String fileTree =
                limitLines(
                        index.stream()
                                .filter(f -> !indexOnly(f))
                                .map(f -> escapeDelimiters(f.getPath()) + " [" + f.getLayer() + "]")
                                .toList(),
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
                                                        escapeDelimiters(
                                                                a.method()
                                                                        + " "
                                                                        + a.path()
                                                                        + " ("
                                                                        + paths.getOrDefault(
                                                                                a.fileId(), "?")
                                                                        + ")"))
                                        .toList(),
                                metaChars);

        int budget = maxChars - readme.length() - fileTree.length() - apiLines.length();
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
        return new OverviewInput(readme, fileTree, apiLines, sources.toString());
    }

    /** 경로가 정확히 {@code README.md}(대소문자 무시)인 루트 파일의 앞 {@code maxChars}자. 하위 폴더 README는 보지 않는다. */
    private static String readme(List<AnalysisFile> index, int maxChars) {
        return index.stream()
                .filter(f -> README.equalsIgnoreCase(f.getPath()))
                .findFirst()
                .map(
                        f ->
                                escapeDelimiters(
                                        f.getContent()
                                                .substring(
                                                        0,
                                                        Math.min(
                                                                maxChars,
                                                                f.getContent().length()))))
                .orElse(NO_README);
    }

    private static boolean indexOnly(AnalysisFile f) {
        return INDEX_ONLY_LAYERS.contains(f.getLayer());
    }
}
