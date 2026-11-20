package com.vibenote.feed;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 활동 기록. 각 도메인이 활동이 생긴 트랜잭션 안에서 부른다 (피드 조회는 F-04). */
@Service
public class ActivityService {

    private final ActivityMapper activityMapper;

    public ActivityService(ActivityMapper activityMapper) {
        this.activityMapper = activityMapper;
    }

    @Transactional
    public void record(long userId, ActivityType type, long targetId) {
        activityMapper.insert(userId, type, targetId);
    }

    /**
     * 같은 (회원, 종류, 대상) 활동이 이미 있으면 넣지 않는다. 다시 찜해도 피드에 같은 활동이 쌓이지 않게 한다 (F-12 AC5).
     *
     * <p>확인 후 넣는 방식이고 {@code activities}에 (user_id, type, target_id) 유니크 키가 없으므로, 호출자가 같은 (회원, 종류,
     * 대상)의 쓰기를 직렬화해야 한다. F-12 찜은 {@code concepts} 행 잠금(FOR UPDATE) 안에서 부른다.
     */
    @Transactional
    public void recordOnce(long userId, ActivityType type, long targetId) {
        if (!activityMapper.exists(userId, type, targetId)) {
            activityMapper.insert(userId, type, targetId);
        }
    }
}
