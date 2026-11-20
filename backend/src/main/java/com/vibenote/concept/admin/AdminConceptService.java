package com.vibenote.concept.admin;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.concept.ConceptMapper;
import com.vibenote.concept.ConceptService;
import com.vibenote.concept.ConceptStatus;
import com.vibenote.concept.admin.dto.AdminConceptDetailResponse;
import com.vibenote.concept.admin.dto.AdminConceptSearchRequest;
import com.vibenote.concept.admin.dto.AdminConceptStatusCountsResponse;
import com.vibenote.concept.admin.dto.AdminConceptSummaryResponse;
import com.vibenote.concept.domain.Concept;
import com.vibenote.concept.domain.ConceptStatusCount;
import com.vibenote.concept.review.ConceptReviewMapper;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 카드 관리 (F-13). 상태와 상관없이 모든 카드를 다룬다. 권한은 SecurityConfig의 {@code /api/admin/**} 규칙이 막는다. */
@Service
@Transactional(readOnly = true)
public class AdminConceptService {

    private final ConceptMapper conceptMapper;
    private final ConceptReviewMapper conceptReviewMapper;

    public AdminConceptService(
            ConceptMapper conceptMapper, ConceptReviewMapper conceptReviewMapper) {
        this.conceptMapper = conceptMapper;
        this.conceptReviewMapper = conceptReviewMapper;
    }

    public PageResponse<AdminConceptSummaryResponse> search(
            AdminConceptSearchRequest search, PageQuery page) {
        String keyword =
                search.keyword() == null ? null : ConceptService.escapeLike(search.keyword());
        long total =
                conceptMapper.countForAdmin(search.statusValue(), search.sourceValue(), keyword);
        List<AdminConceptSummaryResponse> items =
                total <= page.offset()
                        ? List.of()
                        : conceptMapper
                                .findForAdmin(
                                        search.statusValue(),
                                        search.sourceValue(),
                                        keyword,
                                        page.size(),
                                        page.offset())
                                .stream()
                                .map(AdminConceptSummaryResponse::from)
                                .toList();
        return PageResponse.of(items, page, total);
    }

    public AdminConceptStatusCountsResponse countByStatus() {
        Map<ConceptStatus, Long> counts = new EnumMap<>(ConceptStatus.class);
        for (ConceptStatusCount row : conceptMapper.countByStatus()) {
            counts.put(row.status(), row.count());
        }
        return new AdminConceptStatusCountsResponse(
                counts.getOrDefault(ConceptStatus.DRAFT, 0L),
                counts.getOrDefault(ConceptStatus.PUBLISHED, 0L),
                counts.getOrDefault(ConceptStatus.HIDDEN, 0L));
    }

    public AdminConceptDetailResponse getDetail(long id) {
        return detailOf(
                conceptMapper
                        .findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.CONCEPT_404)));
    }

    private AdminConceptDetailResponse detailOf(Concept concept) {
        long id = concept.getId();
        return AdminConceptDetailResponse.of(
                concept,
                conceptReviewMapper.countByConcept(id),
                conceptMapper.countLinkedAnalyses(id));
    }
}
