package com.vibenote.follow;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.follow.domain.FollowUser;
import com.vibenote.follow.dto.FollowStatusResponse;
import com.vibenote.follow.dto.FollowUserResponse;
import com.vibenote.user.UserService;
import com.vibenote.user.domain.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 팔로우 (F-04). 팔로우·언팔로우는 멱등이고, 응답의 수는 같은 트랜잭션에서 실제 행 수로 다시 센다. 탈퇴 시 관계 정리(F-03)도 여기서 한다. */
@Service
public class FollowService {

    private final FollowMapper followMapper;
    private final UserService userService;
    private final Clock clock;

    public FollowService(FollowMapper followMapper, UserService userService, Clock clock) {
        this.followMapper = followMapper;
        this.userService = userService;
        this.clock = clock;
    }

    /**
     * 팔로우한다. 이미 팔로우 중이면 행을 그대로 둔다 (AC1). 나와 대상의 users 행을 id 오름차순으로 공유 잠금(FOR SHARE)한 뒤 상태를 본다. 탈퇴는
     * 같은 행을 UPDATE(배타 잠금)한 뒤 follows를 지우므로, 탈퇴와 겹쳐도 탈퇴한 회원의 팔로우 행이 남지 않는다. 이 트랜잭션은 users를 고치지 않아
     * 잠금을 올릴 일이 없으므로 공유 잠금이면 충분하고, 인기 회원을 여러 사람이 동시에 팔로우해도 서로 기다리지 않는다 (PR 리뷰).
     */
    @Transactional
    public FollowStatusResponse follow(long targetId, long userId) {
        rejectSelf(targetId, userId);
        Optional<User> me;
        Optional<User> target;
        if (userId < targetId) {
            me = userService.findByIdForShare(userId);
            target = userService.findByIdForShare(targetId);
        } else {
            target = userService.findByIdForShare(targetId);
            me = userService.findByIdForShare(userId);
        }
        requireActive(target, () -> new BusinessException(ErrorCode.USER_404));
        requireActive(me, () -> new BusinessException(ErrorCode.AUTH_401));
        followMapper.insertIgnore(
                userId, targetId, LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
        return statusOf(targetId, true);
    }

    /** 언팔로우한다. 팔로우하지 않았어도 200이다 (AC2). */
    @Transactional
    public FollowStatusResponse unfollow(long targetId, long userId) {
        rejectSelf(targetId, userId);
        userService.getPublicMember(targetId);
        userService.getActiveMember(userId);
        followMapper.delete(userId, targetId);
        return statusOf(targetId, false);
    }

    /** 대상 회원의 팔로우 상태 (AC4). 비로그인(viewerId null)·본인이면 following=false. */
    @Transactional(readOnly = true)
    public FollowStatusResponse getStatus(long targetId, Long viewerId) {
        userService.getPublicMember(targetId);
        boolean following =
                viewerId != null && viewerId != targetId && followMapper.exists(viewerId, targetId);
        return statusOf(targetId, following);
    }

    /** 대상 회원을 팔로우하는 사람, 팔로우한 시각 최신순 (AC5). */
    @Transactional(readOnly = true)
    public PageResponse<FollowUserResponse> followers(long userId, PageQuery page) {
        userService.getPublicMember(userId);
        long total = followMapper.countFollowers(userId);
        List<FollowUser> rows =
                total == 0
                        ? List.of()
                        : followMapper.findFollowers(userId, page.size(), page.offset());
        return PageResponse.of(rows.stream().map(FollowUserResponse::from).toList(), page, total);
    }

    /** 대상 회원이 팔로우하는 사람, 팔로우한 시각 최신순 (AC5). */
    @Transactional(readOnly = true)
    public PageResponse<FollowUserResponse> followings(long userId, PageQuery page) {
        userService.getPublicMember(userId);
        long total = followMapper.countFollowings(userId);
        List<FollowUser> rows =
                total == 0
                        ? List.of()
                        : followMapper.findFollowings(userId, page.size(), page.offset());
        return PageResponse.of(rows.stream().map(FollowUserResponse::from).toList(), page, total);
    }

    /** 탈퇴한 회원이 팔로워 수·피드에 남지 않게 양방향 관계를 모두 지운다. */
    @Transactional
    public void removeAllOf(long userId) {
        followMapper.deleteAllOf(userId);
    }

    private FollowStatusResponse statusOf(long targetId, boolean following) {
        return new FollowStatusResponse(
                following,
                followMapper.countFollowers(targetId),
                followMapper.countFollowings(targetId));
    }

    private static void rejectSelf(long targetId, long userId) {
        if (targetId == userId) {
            throw new BusinessException(ErrorCode.USER_002);
        }
    }

    private static void requireActive(Optional<User> user, Supplier<BusinessException> otherwise) {
        if (user.filter(User::isActive).isEmpty()) {
            throw otherwise.get();
        }
    }
}
