package com.vibenote.follow;

import com.vibenote.follow.domain.FollowUser;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/follow/FollowMapper.xml */
@Mapper
public interface FollowMapper {

    /** 이미 있으면 PK 중복을 무시한다 (멱등, 동시 요청에도 행 1개). */
    void insertIgnore(
            @Param("followerId") long followerId,
            @Param("followingId") long followingId,
            @Param("createdAt") LocalDateTime createdAt);

    void delete(@Param("followerId") long followerId, @Param("followingId") long followingId);

    boolean exists(@Param("followerId") long followerId, @Param("followingId") long followingId);

    /** 그 회원을 팔로우하는 활성 회원 수. */
    long countFollowers(long userId);

    /** 그 회원이 팔로우하는 활성 회원 수. */
    long countFollowings(long userId);

    List<FollowUser> findFollowers(
            @Param("userId") long userId, @Param("size") int size, @Param("offset") long offset);

    List<FollowUser> findFollowings(
            @Param("userId") long userId, @Param("size") int size, @Param("offset") long offset);

    /** 그 회원이 팔로우하는 관계와 그 회원을 팔로우하는 관계를 모두 지운다. */
    int deleteAllOf(long userId);
}
