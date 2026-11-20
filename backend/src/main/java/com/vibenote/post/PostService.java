package com.vibenote.post;

import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.post.dto.PostSearchRequest;
import com.vibenote.post.dto.PostSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 커뮤니티 게시글 (F-17). */
@Service
public class PostService {

    private final PostMapper postMapper;

    public PostService(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> search(PostSearchRequest search, PageQuery page) {
        BoardType boardType = search.boardTypeValue();
        String keyword = search.keyword() == null ? null : escapeLike(search.keyword());
        List<PostSummaryResponse> items =
                postMapper.findPage(boardType, keyword, page.size(), page.offset()).stream()
                        .map(PostSummaryResponse::from)
                        .toList();
        return PageResponse.of(items, page, postMapper.count(boardType, keyword));
    }

    /**
     * LIKE 패턴의 와일드카드를 글자 그대로 찾도록 {@code !}로 이스케이프한다 (SQL은 {@code ESCAPE '!'}). F-09 {@code
     * ConceptService.escapeLike}와 같은 규칙이다. MySQL 기본 모드에서 백슬래시가 문자열 이스케이프라 {@code !}를 쓴다.
     */
    static String escapeLike(String keyword) {
        return keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
