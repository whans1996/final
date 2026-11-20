package com.vibenote.analysis.concept;

import com.vibenote.analysis.AnalysisMapper;
import com.vibenote.analysis.concept.AnalysisConceptMapper.LinkRow;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.concept.ConceptQueryService;
import com.vibenote.concept.ConceptStatus;
import com.vibenote.concept.dto.ConceptSummaryResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 내 분석에 연결된 개념 조회 (F-08 AC7). LLM을 부르지 않는다. */
@Service
public class AnalysisConceptService {

    private final AnalysisMapper analysisMapper;
    private final AnalysisConceptMapper mapper;
    private final ConceptQueryService conceptQueryService;

    public AnalysisConceptService(
            AnalysisMapper analysisMapper,
            AnalysisConceptMapper mapper,
            ConceptQueryService conceptQueryService) {
        this.analysisMapper = analysisMapper;
        this.mapper = mapper;
        this.conceptQueryService = conceptQueryService;
    }

    /** 공개 카드만 위치와 함께, 검수 대기 초안은 수만. 내 미삭제 분석이 아니면 ANALYSIS_404. */
    @Transactional(readOnly = true)
    public AnalysisConceptsResponse getConcepts(long userId, long analysisId) {
        if (analysisMapper.findOwned(analysisId, userId).isEmpty()) {
            throw new BusinessException(ErrorCode.ANALYSIS_404);
        }
        List<LinkRow> links = mapper.findByAnalysis(analysisId);
        Map<Long, ConceptSummaryResponse> published =
                conceptQueryService
                        .findPublishedSummaries(
                                links.stream()
                                        .filter(
                                                l ->
                                                        ConceptStatus.PUBLISHED
                                                                .name()
                                                                .equals(l.status()))
                                        .map(LinkRow::conceptId)
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(ConceptSummaryResponse::id, Function.identity()));
        List<AnalysisConceptItem> items =
                links.stream()
                        .filter(l -> published.containsKey(l.conceptId()))
                        .map(
                                l ->
                                        new AnalysisConceptItem(
                                                published.get(l.conceptId()),
                                                l.fileId(),
                                                l.filePath(),
                                                l.lineHint(),
                                                l.contextNote()))
                        .toList();
        int pending =
                (int)
                        links.stream()
                                .filter(l -> ConceptStatus.DRAFT.name().equals(l.status()))
                                .count();
        return new AnalysisConceptsResponse(items, pending);
    }

    /** 퀴즈 프롬프트에 넣을 연결 개념 (F-14 AC2). 파이프라인이 부르므로 소유를 확인하지 않는다. */
    @Transactional(readOnly = true)
    public List<LinkedConcept> findForQuiz(long analysisId) {
        return mapper.findForQuiz(analysisId);
    }
}
