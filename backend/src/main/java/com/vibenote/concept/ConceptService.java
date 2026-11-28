package com.vibenote.concept;

import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.concept.domain.Concept;
import com.vibenote.concept.dto.ConceptAppearanceResponse;
import com.vibenote.concept.dto.ConceptDetailResponse;
import com.vibenote.concept.dto.ConceptSearchRequest;
import com.vibenote.concept.dto.ConceptSummaryResponse;
import com.vibenote.concept.wish.ConceptWishService;
import com.vibenote.user.UserService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 개념 카드 목록·검색 (F-09)·상세 (F-10). 공개(PUBLISHED) 카드만 다룬다 (NF-04). */
@Service
@Transactional(readOnly = true)
public class ConceptService {

    static final int RELATED_LIMIT = 4;

    private final ConceptMapper conceptMapper;
    private final ConceptQueryService conceptQueryService;
    private final ConceptWishService conceptWishService;
    private final UserService userService;

    public ConceptService(
            ConceptMapper conceptMapper,
            ConceptQueryService conceptQueryService,
            ConceptWishService conceptWishService,
            UserService userService) {
        this.conceptMapper = conceptMapper;
        this.conceptQueryService = conceptQueryService;
        this.conceptWishService = conceptWishService;
        this.userService = userService;
    }

    public PageResponse<ConceptSummaryResponse> search(
            ConceptSearchRequest search, PageQuery page) {
        String keyword = search.keyword() == null ? null : escapeLike(search.keyword());
        long total =
                conceptMapper.countPublished(keyword, search.category(), search.difficultyValue());
        List<ConceptSummaryResponse> items =
                total == 0
                        ? List.of()
                        : conceptMapper
                                .findPublished(
                                        keyword,
                                        search.category(),
                                        search.difficultyValue(),
                                        search.sortValue(),
                                        page.size(),
                                        page.offset())
                                .stream()
                                .map(ConceptSummaryResponse::from)
                                .toList();
        return PageResponse.of(items, page, total);
    }

    /**
     * 공개 카드 상세 (F-10). userId가 null(비로그인·깨진 토큰)이면 등장 위치는 빈 배열이고 찜 여부는 false, 내 수준은 null이다 (F-12
     * AC7, K-1 AC2). 없거나 비공개 카드면 CONCEPT_404 (AC2).
     */
    public ConceptDetailResponse getDetail(long id, Long userId) {
        Concept concept = conceptQueryService.getPublished(id);
        List<ConceptAppearanceResponse> appearances =
                userId == null
                        ? List.of()
                        : conceptMapper.findAppearances(id, userId).stream()
                                .map(ConceptAppearanceResponse::from)
                                .toList();
        return ConceptDetailResponse.of(
                concept,
                conceptWishService.isWished(id, userId),
                appearances,
                related(concept),
                userService.findActiveLevel(userId));
    }

    /** 함께 등장한 카드를 먼저 담고, 모자란 자리를 같은 카테고리 인기순으로 채운다 (AC6·AC7). */
    private List<ConceptSummaryResponse> related(Concept concept) {
        List<Concept> picked =
                new ArrayList<>(conceptMapper.findCoOccurring(concept.getId(), RELATED_LIMIT));
        int remaining = RELATED_LIMIT - picked.size();
        if (remaining > 0) {
            List<Long> excludeIds = new ArrayList<>();
            excludeIds.add(concept.getId());
            picked.forEach(c -> excludeIds.add(c.getId()));
            picked.addAll(
                    conceptMapper.findSameCategoryPopular(
                            concept.getCategory(), excludeIds, remaining));
        }
        return picked.stream().map(ConceptSummaryResponse::from).toList();
    }

    /**
     * LIKE 패턴의 와일드카드를 글자 그대로 찾도록 {@code !}로 이스케이프한다 (SQL은 {@code ESCAPE '!'}). MySQL 기본 모드에서 백슬래시가
     * 문자열 이스케이프라 쓰지 않는다.
     */
    public static String escapeLike(String keyword) {
        return keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
