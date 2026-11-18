package com.vibenote.user;

import com.vibenote.user.dto.UserStatsResponse;
import org.apache.ibatis.annotations.Mapper;

/**
 * 학습 통계 집계. SQL: resources/mapper/user/UserStatsMapper.xml
 *
 * <p>분석·퀴즈·복습 계획 테이블을 읽기 전용으로 모아 센다. 집계 한 번을 위해 각 도메인 Service를 거치지 않고 여기서 직접 읽는다 (쓰기는 하지 않는다).
 */
@Mapper
public interface UserStatsMapper {

    UserStatsResponse countByUser(long userId);
}
