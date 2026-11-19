package com.vibenote.concept;

import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.concept.dto.ConceptSearchRequest;
import com.vibenote.concept.dto.ConceptSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 개념 카드 목록·검색 (F-09). 공개(PUBLISHED) 카드만 다룬다 (NF-04). */
@Service
@Transactional(readOnly = true)
public class ConceptService {

    private final ConceptMapper conceptMapper;

    public ConceptService(ConceptMapper conceptMapper) {
        this.conceptMapper = conceptMapper;
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
     * LIKE 패턴의 와일드카드를 글자 그대로 찾도록 {@code !}로 이스케이프한다 (SQL은 {@code ESCAPE '!'}). MySQL 기본 모드에서 백슬래시가
     * 문자열 이스케이프라 쓰지 않는다.
     */
    static String escapeLike(String keyword) {
        return keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
