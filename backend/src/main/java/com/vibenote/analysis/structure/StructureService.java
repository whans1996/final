package com.vibenote.analysis.structure;

import com.vibenote.analysis.AnalysisService;
import com.vibenote.analysis.AnalysisStatus;
import com.vibenote.analysis.domain.AnalysisDetail;
import com.vibenote.analysis.explain.AnalysisOverview;
import com.vibenote.analysis.explain.ApiDetector;
import com.vibenote.analysis.explain.ApiEndpoint;
import com.vibenote.analysis.structure.StructureCache.Computed;
import com.vibenote.analysis.structure.http.ContextPathReader;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 분석 구조 그래프 조회 (F-21a AC12·AC13). 결과는 DB에 저장하지 않고 요청 때 계산한다. DONE 분석은 {@link StructureCache}에 두고 다시
 * 쓴다 (5.3절 4).
 */
@Service
public class StructureService {

    private final AnalysisService analysisService;
    private final StructureAnalyzer analyzer;
    private final StructureCache cache;

    public StructureService(
            AnalysisService analysisService, StructureProperties properties, StructureCache cache) {
        this.analysisService = analysisService;
        this.cache = cache;
        this.analyzer =
                new StructureAnalyzer(
                        properties.maxEdges(),
                        properties.maxUnresolved(),
                        properties.maxWorkSteps(),
                        properties.maxWorkTime());
    }

    /**
     * 볼 수 없으면 ANALYSIS_404 (캐시에 있어도 매 요청 확인한다). 개요가 없으면(진행 중) HTTP 판정을 건너뛴다. 동시 계산 자리를 기다려도 나지 않으면
     * ANALYSIS_007. 계산은 CPU만 쓰므로 트랜잭션으로 감싸지 않는다 (계산하는 동안 DB 커넥션을 잡지 않게). 확인과 파일 조회 사이에 분석이 지워지면 파일이
     * 없는 것과 같다.
     */
    public AnalysisStructureResponse get(long userId, long analysisId) {
        AnalysisDetail analysis = analysisService.checkViewableDetail(userId, analysisId);
        AnalysisOverview overview = analysis.getOverview();
        if (analysis.getStatus() == AnalysisStatus.DONE) {
            return cache.get(analysisId, () -> compute(analysisId, overview));
        }
        return cache.compute(() -> compute(analysisId, overview));
    }

    /** 자리를 얻은 뒤에 파일을 읽고 예산을 만든다. 예산을 다 쓴 결과(시간에 따라 달라질 수 있음)는 캐시하지 않는다. */
    private Computed compute(long analysisId, AnalysisOverview overview) {
        var files = analysisService.findFiles(analysisId);
        List<ApiEndpoint> apis = overview == null ? null : overview.apis();
        // 개요의 API 목록은 같은 선별 파일로 탐지한 것이다. 잘렸거나 건너뛴 매핑이 있었는지, 다른 파일의 같은 method+path를
        // 목록에서 뺐는지(#122)는 다시 훑어 안다
        boolean incomplete = false;
        if (overview != null) {
            ApiDetector.Detection detection = ApiDetector.scan(files);
            incomplete = detection.incomplete();
            if (apis != null && !detection.duplicates().isEmpty()) {
                apis = new ArrayList<>(apis);
                apis.addAll(detection.duplicates());
            }
        }
        WorkBudget budget = analyzer.newBudget();
        AnalysisStructureResponse response =
                analyzer.analyze(files, apis, ContextPathReader.read(files), incomplete, budget);
        return new Computed(response, !budget.exhausted());
    }
}
