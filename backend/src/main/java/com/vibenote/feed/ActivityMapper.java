package com.vibenote.feed;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/feed/ActivityMapper.xml */
@Mapper
public interface ActivityMapper {

    void insert(
            @Param("userId") long userId,
            @Param("type") ActivityType type,
            @Param("targetId") long targetId);

    boolean exists(
            @Param("userId") long userId,
            @Param("type") ActivityType type,
            @Param("targetId") long targetId);
}
