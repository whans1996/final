package com.vibenote.follow;

import org.apache.ibatis.annotations.Mapper;

/** SQL: resources/mapper/follow/FollowMapper.xml. 팔로우·목록은 F-04에서 추가한다. */
@Mapper
public interface FollowMapper {

    /** 그 회원이 팔로우하는 관계와 그 회원을 팔로우하는 관계를 모두 지운다. */
    int deleteAllOf(long userId);
}
