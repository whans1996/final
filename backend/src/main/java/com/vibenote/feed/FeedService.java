package com.vibenote.feed;

import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.feed.domain.FeedRow;
import com.vibenote.feed.dto.FeedItemResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 활동 피드 (F-04). 팔로우한 사람의 활동 중 지금 볼 수 있는 것만 최신순으로 준다 (내 활동은 넣지 않는다). */
@Service
public class FeedService {

    private final FeedMapper feedMapper;

    public FeedService(FeedMapper feedMapper) {
        this.feedMapper = feedMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<FeedItemResponse> getFeed(long me, PageQuery page) {
        long total = feedMapper.countVisible(me);
        List<FeedRow> rows =
                total == 0 ? List.of() : feedMapper.findVisiblePage(me, page.size(), page.offset());
        return PageResponse.of(rows.stream().map(FeedItemResponse::from).toList(), page, total);
    }
}
