package com.vibenote.concept;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.concept.domain.Concept;
import com.vibenote.concept.dto.ConceptSummaryResponse;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 다른 도메인(분석·찜·복습·추천)이 카드를 읽는 공통 조회 (F-00b). 일반 사용자에게는 PUBLISHED만 내보낸다 (NF-04). */
@Service
@Transactional(readOnly = true)
public class ConceptQueryService {

    private final ConceptMapper conceptMapper;

    public ConceptQueryService(ConceptMapper conceptMapper) {
        this.conceptMapper = conceptMapper;
    }

    /** PUBLISHED 카드. 없거나 DRAFT·HIDDEN이면 존재 여부를 드러내지 않고 CONCEPT_404 (AC2). */
    public Concept getPublished(long id) {
        return conceptMapper
                .findById(id)
                .filter(Concept::isPublished)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONCEPT_404));
    }

    /** ids 중 PUBLISHED 카드의 요약을 입력 순서대로 (중복·없는 id·비공개 카드는 뺀다, AC3). */
    public List<ConceptSummaryResponse> findPublishedSummaries(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Long> distinctIds = List.copyOf(new LinkedHashSet<>(ids));
        Map<Long, Concept> byId =
                conceptMapper.findPublishedByIds(distinctIds).stream()
                        .collect(Collectors.toMap(Concept::getId, Function.identity()));
        return distinctIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(ConceptSummaryResponse::from)
                .toList();
    }
}
