package com.vibenote.feed;

import com.vibenote.feed.domain.FeedRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/feed/FeedMapper.xml. 팔로우한 회원의 활동 중 지금 볼 수 있는 것만 (F-04 AC6·AC7). */
@Mapper
public interface FeedMapper {

    long countVisible(@Param("me") long me);

    List<FeedRow> findVisiblePage(
            @Param("me") long me, @Param("size") int size, @Param("offset") long offset);
}
