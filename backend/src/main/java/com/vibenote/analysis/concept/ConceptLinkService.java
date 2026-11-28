package com.vibenote.analysis.concept;

import com.vibenote.analysis.AnalysisService;
import com.vibenote.analysis.concept.AnalysisConceptMapper.LinkRow;
import com.vibenote.analysis.concept.ExtractedConcept.NewConcept;
import com.vibenote.concept.ConceptDraftService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 검증한 개념 항목을 저장한다 (F-08 AC1·AC5, K-1 AC7·AC10). 새 초안 INSERT와 analysis_concepts INSERT를 한 트랜잭션으로 한다.
 */
@Service
public class ConceptLinkService {

    private static final Logger log = LoggerFactory.getLogger(ConceptLinkService.class);

    private final ConceptDraftService draftService;
    private final AnalysisConceptMapper mapper;
    private final AnalysisService analysisService;

    public ConceptLinkService(
            ConceptDraftService draftService,
            AnalysisConceptMapper mapper,
            AnalysisService analysisService) {
        this.draftService = draftService;
        this.mapper = mapper;
        this.analysisService = analysisService;
    }

    /**
     * @param items {@link ConceptResponseParser}가 정리한 항목 (상한·중복 처리 끝)
     */
    @Transactional
    public void save(long analysisId, List<ExtractedConcept> items) {
        // LLM을 기다리는 동안 탈퇴로 중단·삭제됐으면 초안도 연결도 남기지 않는다 (#91)
        if (!analysisService.lockInProgress(analysisId)) {
            return;
        }
        // 동시에 도는 분석끼리 같은 이름들을 반대 순서로 INSERT하면 데드락이 나므로 초안은 이름 순으로 먼저 만든다
        Map<NewConcept, Long> created = new HashMap<>();
        items.stream()
                .map(ExtractedConcept::draft)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(d -> d.name().toLowerCase(Locale.ROOT)))
                .forEach(d -> created.put(d, create(d)));

        // 이름이 콜레이션으로만 같거나(악센트 등) 카탈로그를 읽은 뒤 숨겨진 카드에 UNIQUE 충돌로 닿으면 연결하지 않는다 (K-1 AC10)
        Set<Long> hidden = draftService.findHiddenIds(created.values());
        List<LinkRow> links = new ArrayList<>();
        Set<Long> seen = new HashSet<>(hidden);
        for (ExtractedConcept item : items) {
            long conceptId =
                    item.conceptId() != null ? item.conceptId() : created.get(item.draft());
            // 초안이 UNIQUE 충돌로 이미 연결한 카드를 가리키면 PK가 겹치므로 첫 항목만 쓴다
            if (seen.add(conceptId)) {
                links.add(
                        LinkRow.of(conceptId, item.fileId(), item.lineHint(), item.contextNote()));
            }
        }
        if (links.isEmpty()) {
            return;
        }
        // 카탈로그를 읽은 뒤 관리자가 지운 카드(F-13)는 FK 위반 대신 빠진다 (#77)
        int inserted = mapper.insertAll(analysisId, links);
        if (inserted < links.size()) {
            log.warn(
                    "analysis {}: 추출 중 삭제된 개념 {}개를 연결하지 않았다 (요청 id 목록 {})",
                    analysisId,
                    links.size() - inserted,
                    links.stream().map(LinkRow::conceptId).toList());
        }
    }

    private long create(NewConcept draft) {
        return draftService.createDraft(
                draft.name(),
                draft.category(),
                draft.difficulty(),
                draft.summary(),
                draft.body(),
                draft.summaryBeginner(),
                draft.summaryJunior(),
                draft.summarySenior());
    }
}
