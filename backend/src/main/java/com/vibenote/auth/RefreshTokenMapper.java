package com.vibenote.auth;

import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/auth/RefreshTokenMapper.xml. 시간 비교는 DB 시계(NOW())로만 한다. */
@Mapper
public interface RefreshTokenMapper {

    /** 만료 시각 = 지금 + validitySeconds. 활성 회원일 때만 넣고 넣은 행 수(0 또는 1)를 돌려준다 (F-03). */
    int insert(
            @Param("userId") long userId,
            @Param("tokenHash") String tokenHash,
            @Param("validitySeconds") long validitySeconds);

    Optional<StoredRefreshToken> findByHash(String tokenHash);

    int deleteByHash(String tokenHash);

    /** 그 회원의 토큰을 모두 지운다 (탈퇴, F-03). */
    int deleteByUserId(long userId);

    /** refresh_tokens 행. {@code expired}는 조회 시점에 DB가 계산한다. */
    record StoredRefreshToken(long id, long userId, boolean expired) {}
}
