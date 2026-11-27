package com.vibenote.concept.admin;

import com.vibenote.common.Level;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.concept.ConceptMapper;
import com.vibenote.concept.ConceptService;
import com.vibenote.concept.ConceptSource;
import com.vibenote.concept.ConceptStatus;
import com.vibenote.concept.admin.dto.AdminConceptDetailResponse;
import com.vibenote.concept.admin.dto.AdminConceptLevelRequest;
import com.vibenote.concept.admin.dto.AdminConceptRequest;
import com.vibenote.concept.admin.dto.AdminConceptSearchRequest;
import com.vibenote.concept.admin.dto.AdminConceptStatusCountsResponse;
import com.vibenote.concept.admin.dto.AdminConceptSummaryResponse;
import com.vibenote.concept.domain.Concept;
import com.vibenote.concept.domain.ConceptStatusCount;
import com.vibenote.concept.review.ConceptReviewMapper;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 카드 관리 (F-13). 상태와 상관없이 모든 카드를 다룬다. 권한은 SecurityConfig의 {@code /api/admin/**} 규칙이 막는다. */
@Service
@Transactional(readOnly = true)
public class AdminConceptService {

    static final String NAME_TAKEN = "같은 이름의 개념 카드가 이미 있습니다.";
    static final String SEED_NAME_FIXED = "시드 카드의 이름은 바꿀 수 없습니다.";
    static final String CREATE_STATUS = "새 카드는 DRAFT 또는 PUBLISHED로만 만들 수 있습니다.";
    static final String LEVELS_DUPLICATE = "수준별 항목의 수준이 겹칩니다.";

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

    /** 관리자 작성 카드 생성 (AC4). source=ADMIN, 찜·평점은 기본값 0. */
    @Transactional
    public long create(AdminConceptRequest request) {
        if (request.statusValue() == ConceptStatus.HIDDEN) {
            throw new BusinessException(ErrorCode.COMMON_400, Map.of("status", CREATE_STATUS));
        }
        ensureDistinctLevels(request);
        ensureNameAvailable(request.name(), null);
        Concept concept = new Concept();
        apply(concept, request);
        try {
            conceptMapper.insertByAdmin(concept);
        } catch (DuplicateKeyException e) {
            throw nameTaken();
        }
        return concept.getId();
    }

    /**
     * 내용·상태 전체 교체 (AC7). 공개·숨김 카드를 DRAFT로 되돌리지 않고 (AC8), 시드 카드의 이름은 바꾸지 않는다 (AC9). 카드를 잠그고 읽어 상태
     * 확인과 변경 사이에 삭제·다른 수정이 끼지 않게 한다.
     */
    @Transactional
    public AdminConceptDetailResponse update(long id, AdminConceptRequest request) {
        ensureDistinctLevels(request);
        Concept concept = lockOrThrow(id);
        if (request.statusValue() == ConceptStatus.DRAFT
                && concept.getStatus() != ConceptStatus.DRAFT) {
            throw new BusinessException(ErrorCode.CONCEPT_003);
        }
        if (concept.getSource() == ConceptSource.SEED
                && !concept.getName().equals(request.name())) {
            throw new BusinessException(ErrorCode.COMMON_400, Map.of("name", SEED_NAME_FIXED));
        }
        ensureNameAvailable(request.name(), id);
        apply(concept, request);
        try {
            conceptMapper.updateByAdmin(concept);
        } catch (DuplicateKeyException e) {
            throw nameTaken();
        }
        return getDetail(id);
    }

    /** DRAFT 카드만 지운다 (AC10). 분석 연결은 FK CASCADE로 함께 지워진다. */
    @Transactional
    public void delete(long id) {
        Concept concept = lockOrThrow(id);
        if (concept.getStatus() != ConceptStatus.DRAFT || conceptMapper.deleteDraft(id) == 0) {
            throw new BusinessException(ErrorCode.CONCEPT_003);
        }
    }

    private Concept lockOrThrow(long id) {
        return conceptMapper
                .findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONCEPT_404));
    }

    /** 먼저 확인하고, 확인과 저장 사이에 같은 이름이 먼저 들어오면 UNIQUE 충돌을 같은 응답으로 바꾼다 (AC6). */
    private void ensureNameAvailable(String name, Long excludeId) {
        if (conceptMapper.existsByName(name, excludeId)) {
            throw nameTaken();
        }
    }

    /** 같은 수준이 두 번 오면 400 (K-1 AC4). 개수·값 검증은 Bean Validation이 먼저 한다. */
    private static void ensureDistinctLevels(AdminConceptRequest request) {
        if (request.levels() == null) {
            return;
        }
        Set<Level> seen = EnumSet.noneOf(Level.class);
        for (AdminConceptLevelRequest level : request.levels()) {
            if (!seen.add(level.levelValue())) {
                throw new BusinessException(
                        ErrorCode.COMMON_400, Map.of("levels", LEVELS_DUPLICATE));
            }
        }
    }

    private static BusinessException nameTaken() {
        return new BusinessException(ErrorCode.CONCEPT_002, Map.of("name", NAME_TAKEN));
    }

    private static void apply(Concept concept, AdminConceptRequest request) {
        concept.setName(request.name());
        concept.setCategory(request.category());
        concept.setDifficulty(request.difficultyValue());
        concept.setSummary(request.summary());
        concept.setBody(request.body());
        concept.setExampleCode(request.exampleCode());
        concept.setReferenceUrl(request.referenceUrl());
        concept.setStatus(request.statusValue());
        // levels가 없으면 PUT은 잠그고 읽은 기존 값을 그대로 다시 쓴다 (K-1 AC3. 지금 관리자 폼은 이 칸을 모른다)
        if (request.levels() != null) {
            for (Level level : Level.values()) {
                concept.setLevelText(level, null, null);
            }
            request.levels()
                    .forEach(l -> concept.setLevelText(l.levelValue(), l.summary(), l.body()));
        }
    }

    private AdminConceptDetailResponse detailOf(Concept concept) {
        long id = concept.getId();
        return AdminConceptDetailResponse.of(
                concept,
                conceptReviewMapper.countByConcept(id),
                conceptMapper.countLinkedAnalyses(id));
    }
}
