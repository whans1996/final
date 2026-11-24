package com.vibenote.analysis.structure;

import com.vibenote.analysis.AnalysisService;
import com.vibenote.analysis.explain.AnalysisOverview;
import com.vibenote.analysis.explain.ApiEndpoint;
import com.vibenote.analysis.structure.http.ContextPathReader;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 분석 구조 그래프 조회 (F-21a AC12·AC13). 결과는 저장하지 않고 요청 때 계산한다. */
@Service
public class StructureService {

    private final AnalysisService analysisService;
    private final StructureAnalyzer analyzer;

    public StructureService(AnalysisService analysisService, StructureProperties properties) {
        this.analysisService = analysisService;
        this.analyzer = new StructureAnalyzer(properties.maxEdges(), properties.maxUnresolved());
    }

    /** 볼 수 없으면 ANALYSIS_404. 개요가 없으면(진행 중) HTTP 판정을 건너뛴다. */
    @Transactional(readOnly = true)
    public AnalysisStructureResponse get(long userId, long analysisId) {
        AnalysisOverview overview = analysisService.getViewableOverview(userId, analysisId);
        var files = analysisService.findFiles(analysisId);
        List<ApiEndpoint> apis = overview == null ? null : overview.apis();
        return analyzer.analyze(files, apis, ContextPathReader.read(files));
    }
}
