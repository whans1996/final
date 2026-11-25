package com.vibenote.analysis.pipeline;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.ApiDetector;
import com.vibenote.analysis.structure.AnalysisStructureResponse;
import com.vibenote.analysis.structure.StructureAnalyzer;
import com.vibenote.analysis.structure.StructureEdge;
import com.vibenote.analysis.structure.WorkBudget;
import com.vibenote.analysis.structure.http.ContextPathReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 인덱스에서 설명할 파일을 중요도로 고른다 (F-26a 5.4절).
 *
 * <p>importance = layer 가중치 + 연결 수 + 진입점 보너스. 연결 수는 구조 그래프(F-21a {@link StructureAnalyzer})에서 서로 다른
 * 파일 기준 {@code 2 × 들어오는 연결 + 나가는 연결}이다. FE·BE가 둘 다 있으면 한쪽이 자리를 다 가져가지 않게 몫을 나눈다.
 */
@Component
public class ImportanceRanker {

    private static final Logger log = LoggerFactory.getLogger(ImportanceRanker.class);

    private static final int MAX_DEGREE = 20;
    private static final int ENTRY_BONUS = 5;
    private static final Map<Layer, Integer> WEIGHTS = new EnumMap<>(Layer.class);

    static {
        WEIGHTS.put(Layer.CONTROLLER, 10);
        WEIGHTS.put(Layer.SERVICE, 8);
        WEIGHTS.put(Layer.MAPPER, 6);
        WEIGHTS.put(Layer.VUE, 6);
        WEIGHTS.put(Layer.CONFIG, 5);
        WEIGHTS.put(Layer.JS, 4);
        WEIGHTS.put(Layer.DOMAIN, 3);
        WEIGHTS.put(Layer.OTHER, 1);
    }

    private static final Pattern SPRING_BOOT_APPLICATION =
            Pattern.compile("@(?:\\w+\\.)*SpringBootApplication\\b");

    /** {@code App.vue}, src 바로 아래 {@code main.{js,ts}}, {@code router/index.{js,ts}}. */
    private static final Pattern ENTRY_PATH =
            Pattern.compile("(?:.*/)?(?:App\\.vue|src/main\\.(?:js|ts)|router/index\\.(?:js|ts))");

    private static final Pattern JS_FILE = Pattern.compile(".+\\.(?:js|mjs|cjs|ts)");

    /** JS 토크나이저가 아직 읽지 못해 그래프에 넣지 않는다 (연결 수 0, F-26b). */
    private static final Set<String> GRAPH_EXCLUDED = Set.of("ts", "tsx", "jsx");

    private static final Comparator<Scored> BY_SCORE =
            Comparator.comparingInt(Scored::importance).reversed().thenComparing(Scored::path);

    private final SelectionProperties properties;

    public ImportanceRanker(SelectionProperties properties) {
        this.properties = properties;
    }

    /**
     * 인덱스 전체에 선별 여부와 점수를 붙여 저장 순서로 돌려준다: 선별 파일을 (layer 선언 순서, 경로) 순으로 먼저, 이어서 선별되지 않은 파일을 경로 순으로
     * (5.4절 7).
     */
    public List<RankedFile> rank(List<SourceFile> index, int limit) {
        List<SourceFile> sorted =
                index.stream().sorted(Comparator.comparing(SourceFile::path)).toList();
        List<Scored> candidates = new ArrayList<>();
        List<RankedFile> indexOnly = new ArrayList<>();
        for (SourceFile file : sorted) {
            Layer layer = FileSelector.layer(file.path(), file.content());
            if (layer.indexOnly()) {
                indexOnly.add(new RankedFile(file.path(), layer, file.content(), false, null));
            } else {
                candidates.add(new Scored(file, layer, 0));
            }
        }

        Map<String, Integer> degrees = degrees(candidates);
        List<Scored> scored =
                candidates.stream()
                        .map(
                                c ->
                                        new Scored(
                                                c.file(),
                                                c.layer(),
                                                WEIGHTS.get(c.layer())
                                                        + degrees.getOrDefault(c.path(), 0)
                                                        + (isEntry(c) ? ENTRY_BONUS : 0)))
                        .toList();

        Set<String> selected = choose(scored, limit);
        List<RankedFile> ranked = new ArrayList<>();
        scored.stream()
                .filter(s -> selected.contains(s.path()))
                .sorted(Comparator.comparing(Scored::layer).thenComparing(Scored::path))
                .forEach(s -> ranked.add(s.toRanked(true)));
        List<RankedFile> rest = new ArrayList<>(indexOnly);
        scored.stream()
                .filter(s -> !selected.contains(s.path()))
                .forEach(s -> rest.add(s.toRanked(false)));
        rest.sort(Comparator.comparing(RankedFile::path));
        ranked.addAll(rest);
        return ranked;
    }

    /**
     * (importance 내림차순, 경로) 순으로 FE·BE 몫 안에서 고르고, 한쪽이 몫을 다 못 채우면 남은 자리를 다른 쪽이 같은 순서로 채운다. config는
     * 실제로 고른 것만 세어 FE·BE 합쳐 {@code max-config}개까지 둔다 (#159: 몫이 찬 쪽의 config가 상한 자리를 먼저 차지하지 않게).
     */
    private Set<String> choose(List<Scored> scored, int limit) {
        List<Scored> ordered = scored.stream().sorted(BY_SCORE).toList();
        long feCandidates = scored.stream().filter(ImportanceRanker::isFrontend).count();
        int feShare = frontendShare(limit, feCandidates, scored.size());

        Set<String> chosen = new HashSet<>();
        int[] taken = new int[2]; // [FE, BE]
        int configs = 0;
        for (boolean withinShare : new boolean[] {true, false}) {
            for (Scored s : ordered) {
                if (chosen.size() >= limit) {
                    return chosen;
                }
                boolean config = s.layer() == Layer.CONFIG;
                if (chosen.contains(s.path()) || (config && configs >= properties.maxConfig())) {
                    continue;
                }
                int side = isFrontend(s) ? 0 : 1;
                if (withinShare && taken[side] >= (side == 0 ? feShare : limit - feShare)) {
                    continue;
                }
                chosen.add(s.path());
                taken[side]++;
                if (config) {
                    configs++;
                }
            }
        }
        return chosen;
    }

    /** FE 몫 = round(N × FE / 전체)를 [⌈N/3⌉, N − ⌈N/3⌉]로 자른다. 한쪽만 있으면 그쪽이 다 쓴다 (AC6). */
    static int frontendShare(int limit, long frontend, long total) {
        if (frontend == 0) {
            return 0;
        }
        if (frontend == total) {
            return limit;
        }
        int min = (limit + 2) / 3;
        int proportional = (int) Math.round((double) limit * frontend / total);
        return Math.min(limit - min, Math.max(min, proportional));
    }

    /** 경로 → 연결 수. 예산을 다 쓰면 그때까지 찾은 간선만 센다 (AC11). */
    private Map<String, Integer> degrees(List<Scored> candidates) {
        List<AnalysisFile> graph = new ArrayList<>();
        for (Scored c : candidates) {
            if (GRAPH_EXCLUDED.contains(extension(c.path()))) {
                continue;
            }
            AnalysisFile file = new AnalysisFile();
            file.setId((long) graph.size() + 1);
            file.setPath(c.path());
            file.setLayer(c.layer().value());
            file.setContent(c.file().content());
            graph.add(file);
        }
        if (graph.isEmpty()) {
            return Map.of();
        }

        ApiDetector.Detection detection = ApiDetector.scan(graph);
        StructureAnalyzer analyzer =
                new StructureAnalyzer(
                        Integer.MAX_VALUE, 0, properties.maxWorkSteps(), properties.maxWorkTime());
        WorkBudget budget = analyzer.newBudget();
        AnalysisStructureResponse structure =
                analyzer.analyze(
                        graph,
                        detection.apis(),
                        ContextPathReader.read(graph),
                        detection.incomplete(),
                        budget);
        if (budget.exhausted()) {
            log.warn(
                    "선별 그래프 계산 예산을 다 써서 그때까지 찾은 연결만 셉니다 (후보 {}개, 간선 {}개)",
                    graph.size(),
                    structure.edges().size());
        }

        Map<Long, Set<Long>> incoming = new HashMap<>();
        Map<Long, Set<Long>> outgoing = new HashMap<>();
        for (StructureEdge edge : structure.edges()) {
            if (edge.from() != edge.to()) {
                outgoing.computeIfAbsent(edge.from(), k -> new HashSet<>()).add(edge.to());
                incoming.computeIfAbsent(edge.to(), k -> new HashSet<>()).add(edge.from());
            }
        }
        Map<String, Integer> degrees = new HashMap<>();
        for (AnalysisFile file : graph) {
            int in = incoming.getOrDefault(file.getId(), Set.of()).size();
            int out = outgoing.getOrDefault(file.getId(), Set.of()).size();
            degrees.put(file.getPath(), Math.min(MAX_DEGREE, 2 * in + out));
        }
        return degrees;
    }

    private static boolean isEntry(Scored s) {
        if (ENTRY_PATH.matcher(s.path()).matches()) {
            return true;
        }
        return s.path().toLowerCase(Locale.ROOT).endsWith(".java")
                && SPRING_BOOT_APPLICATION.matcher(JavaComments.strip(s.file().content())).find();
    }

    /** FE = vue·js layer, {@code package.json}, JS 계열 설정 파일 (5.4절 5). */
    private static boolean isFrontend(Scored s) {
        if (s.layer() == Layer.VUE || s.layer() == Layer.JS) {
            return true;
        }
        String name = s.path().substring(s.path().lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        return s.layer() == Layer.CONFIG
                && (name.equals("package.json") || JS_FILE.matcher(name).matches());
    }

    private static String extension(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private record Scored(SourceFile file, Layer layer, int importance) {
        String path() {
            return file.path();
        }

        RankedFile toRanked(boolean selected) {
            return new RankedFile(file.path(), layer, file.content(), selected, importance);
        }
    }
}
