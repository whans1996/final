package com.vibenote.follow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 팔로우 (F-04에서 확장). 지금은 탈퇴 시 관계 정리(F-03)만 있다. */
@Service
public class FollowService {

    private final FollowMapper followMapper;

    public FollowService(FollowMapper followMapper) {
        this.followMapper = followMapper;
    }

    /** 탈퇴한 회원이 팔로워 수·피드에 남지 않게 양방향 관계를 모두 지운다. */
    @Transactional
    public void removeAllOf(long userId) {
        followMapper.deleteAllOf(userId);
    }
}
