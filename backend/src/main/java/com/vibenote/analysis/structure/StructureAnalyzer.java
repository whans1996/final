package com.vibenote.analysis.structure;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.ApiEndpoint;
import com.vibenote.analysis.pipeline.Layer;
import com.vibenote.analysis.structure.http.HttpLinker;
import com.vibenote.analysis.structure.java.JavaStructureReader;
import com.vibenote.analysis.structure.java.JavaTypeIndex;
import com.vibenote.analysis.structure.js.JsModule;
import com.vibenote.analysis.structure.js.JsModuleReader;
import com.vibenote.analysis.structure.xml.MyBatisXmlReader;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 선별 파일로 구조 그래프를 계산한다 (F-21a AC1~AC12). 같은 입력이면 같은 결과를 낸다(순수 함수, LLM·DB 없음).
 *
 * <p>Java·MyBatis XML·JS/Vue를 읽고 개요 API가 있으면 FE → API를 잇는다. 간선은 {@code (from, to, type)} 중복을 없애고(가장
 * 작은 줄) from → to → type 순으로, 확인하지 못한 연결은 fileId → line 순으로 정렬해 상한까지 자른다.
 *
 * <p>요청 하나에 {@link WorkBudget} 하나를 둔다. 다 쓰면 남은 파일은 PARSE_ERROR, 남은 HTTP 호출은 URL_UNKNOWN이고
 * edgesTruncated를 켠다 (찾지 못한 간선이 있을 수 있다).
 */
public final class StructureAnalyzer {

    private static final Comparator<StructureEdge> EDGE_ORDER =
            Comparator.comparingLong(StructureEdge::from)
                    .thenComparingLong(StructureEdge::to)
                    .thenComparing(StructureEdge::type);

    /** 설정이 없을 때의 작업 예산 ({@link StructureProperties}와 같은 값). */
    static final long DEFAULT_MAX_WORK_STEPS = 20_000_000;

    static final Duration DEFAULT_MAX_WORK_TIME = Duration.ofSeconds(3);

    private final int maxEdges;
    private final int maxUnresolved;
    private final long maxWorkSteps;
    private final Duration maxWorkTime;

    public StructureAnalyzer(int maxEdges, int maxUnresolved) {
        this(maxEdges, maxUnresolved, DEFAULT_MAX_WORK_STEPS, DEFAULT_MAX_WORK_TIME);
    }

    public StructureAnalyzer(
            int maxEdges, int maxUnresolved, long maxWorkSteps, Duration maxWorkTime) {
        this.maxEdges = maxEdges;
        this.maxUnresolved = maxUnresolved;
        this.maxWorkSteps = maxWorkSteps;
        this.maxWorkTime = maxWorkTime;
    }

    /**
     * @param apis 개요의 API 목록. 개요가 없으면 null이고 HTTP 판정을 건너뛴다
     * @param contextPath 선별 설정 파일의 context-path. 없으면 null 또는 빈 문자열
     */
    public AnalysisStructureResponse analyze(
            List<AnalysisFile> files, List<ApiEndpoint> apis, String contextPath) {
        List<AnalysisFile> sorted =
                files.stream().sorted(Comparator.comparing(AnalysisFile::getId)).toList();
        StructureLinks links = new StructureLinks(maxUnresolved);
        WorkBudget budget = new WorkBudget(maxWorkSteps, maxWorkTime);

        JavaTypeIndex index =
                JavaStructureReader.read(
                        sorted.stream().filter(f -> extension(f).equals("java")).toList(),
                        links,
                        budget);
        MyBatisXmlReader.read(
                sorted.stream()
                        .filter(
                                f ->
                                        Layer.MAPPER.value().equals(f.getLayer())
                                                && extension(f).equals("xml"))
                        .toList(),
                index,
                links,
                budget);
        Map<Long, JsModule> modules =
                JsModuleReader.read(
                        sorted.stream()
                                .filter(f -> List.of("js", "mjs", "vue").contains(extension(f)))
                                .toList(),
                        links,
                        budget);
        if (apis != null) {
            HttpLinker.link(modules, apis, contextPath, links, budget);
        }

        Map<String, StructureEdge> unique = new LinkedHashMap<>();
        for (StructureEdge edge : links.edges()) {
            unique.merge(
                    edge.from() + ":" + edge.to() + ":" + edge.type(),
                    edge,
                    (a, b) -> a.line() <= b.line() ? a : b);
        }
        List<StructureEdge> edges = unique.values().stream().sorted(EDGE_ORDER).toList();
        List<UnresolvedLink> unresolved = links.unresolved();

        return new AnalysisStructureResponse(
                sorted.stream().map(StructureAnalyzer::node).toList(),
                edges.subList(0, Math.min(edges.size(), maxEdges)),
                unresolved.subList(0, Math.min(unresolved.size(), maxUnresolved)),
                edges.size() > maxEdges || budget.exhausted(),
                unresolved.size() > maxUnresolved);
    }

    private static StructureNode node(AnalysisFile file) {
        String path = file.getPath();
        return new StructureNode(
                file.getId(),
                path,
                path.substring(path.lastIndexOf('/') + 1),
                file.getLayer(),
                file.getRole());
    }

    private static String extension(AnalysisFile file) {
        String name = file.getPath().substring(file.getPath().lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
