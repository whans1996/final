package com.vibenote.user;

import com.vibenote.user.dto.UserStatsResponse;
import org.springframework.stereotype.Service;

/** 프로필 학습 통계 (F-03 AC3). */
@Service
public class UserStatsService {

    private final UserService userService;
    private final UserStatsMapper userStatsMapper;

    public UserStatsService(UserService userService, UserStatsMapper userStatsMapper) {
        this.userService = userService;
        this.userStatsMapper = userStatsMapper;
    }

    /** 없거나 탈퇴한 회원이면 USER_404. */
    public UserStatsResponse getStats(long userId) {
        userService.getPublicMember(userId);
        return userStatsMapper.countByUser(userId);
    }
}
