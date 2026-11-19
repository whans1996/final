package com.vibenote.analysis.concept;

import com.vibenote.analysis.concept.AnalysisConceptMapper.LinkRow;
import com.vibenote.analysis.concept.ExtractedConcept.NewConcept;
import com.vibenote.concept.ConceptDraftService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 검증한 개념 항목을 저장한다 (F-08 AC1·AC5). 새 초안 INSERT와 analysis_concepts INSERT를 한 트랜잭션으로 한다. */
@Service
public class ConceptLinkService {

    private final ConceptDraftService draftService;
    private final AnalysisConceptMapper mapper;

    public ConceptLinkService(ConceptDraftService draftService, AnalysisConceptMapper mapper) {
        this.draftService = draftService;
        this.mapper = mapper;
    }

    /**
     * @param items {@link ConceptResponseParser}가 정리한 항목 (상한·중복 처리 끝)
     */
    @Transactional
    public void save(long analysisId, List<ExtractedConcept> items) {
        List<LinkRow> links = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (ExtractedConcept item : items) {
            long conceptId = item.conceptId() != null ? item.conceptId() : create(item.draft());
            // 초안이 UNIQUE 충돌로 이미 연결한 카드를 가리키면 PK가 겹치므로 첫 항목만 쓴다
            if (seen.add(conceptId)) {
                links.add(
                        LinkRow.of(conceptId, item.fileId(), item.lineHint(), item.contextNote()));
            }
        }
        if (!links.isEmpty()) {
            mapper.insertAll(analysisId, links);
        }
    }

    private long create(NewConcept draft) {
        return draftService.createDraft(
                draft.name(), draft.category(), draft.difficulty(), draft.summary(), draft.body());
    }
}
