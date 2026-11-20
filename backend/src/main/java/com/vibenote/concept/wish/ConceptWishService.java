package com.vibenote.concept.wish;

import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.concept.ConceptQueryService;
import com.vibenote.concept.dto.ConceptSummaryResponse;
import com.vibenote.concept.wish.dto.ConceptWishResponse;
import com.vibenote.feed.ActivityService;
import com.vibenote.feed.ActivityType;
import com.vibenote.reviewplan.ReviewPlanService;
import com.vibenote.user.UserService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카드 찜 (F-12). 찜·해제는 공개 카드만 대상으로 하고(아니면 CONCEPT_404), 멱등이다. 카드 행을 잠근 뒤 찜 행을 바꾸고 같은 트랜잭션에서 {@code
 * concepts.wish_count}를 실제 행 수로 다시 계산한다.
 */
@Service
public class ConceptWishService {

    private final ConceptWishMapper wishMapper;
    private final ConceptQueryService conceptQueryService;
    private final ActivityService activityService;
    private final ReviewPlanService reviewPlanService;
    private final UserService userService;
    private final Clock clock;

    public ConceptWishService(
            ConceptWishMapper wishMapper,
            ConceptQueryService conceptQueryService,
            ActivityService activityService,
            ReviewPlanService reviewPlanService,
            UserService userService,
            Clock clock) {
        this.wishMapper = wishMapper;
        this.conceptQueryService = conceptQueryService;
        this.activityService = activityService;
        this.reviewPlanService = reviewPlanService;
        this.userService = userService;
        this.clock = clock;
    }

    /**
     * 찜한다. 이미 찜했으면 아무것도 바꾸지 않는다 (AC2). 새로 찜하면 WISH 활동을 카드당 처음 한 번만 남기고(AC5) 내일 복습 계획을 넣어 본다
     * (AC1·AC3).
     */
    @Transactional
    public ConceptWishResponse wish(long conceptId, long userId) {
        lockForWrite(conceptId, userId);
        if (wishMapper.exists(userId, conceptId)) {
            return alreadyWished(conceptId);
        }
        try {
            wishMapper.insert(
                    userId, conceptId, LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
        } catch (DuplicateKeyException e) {
            return alreadyWished(conceptId);
        }
        wishMapper.refreshWishCount(conceptId);
        activityService.recordOnce(userId, ActivityType.WISH, conceptId);
        LocalDate planDate =
                reviewPlanService
                        .addWish(userId, conceptId)
                        .map(ReviewPlanService.Added::planDate)
                        .orElse(null);
        return new ConceptWishResponse(true, wishMapper.findWishCount(conceptId), planDate);
    }

    /** 찜을 푼다. 찜하지 않았어도 200이다 (AC4). 복습 계획과 WISH 활동은 남긴다. */
    @Transactional
    public ConceptWishResponse unwish(long conceptId, long userId) {
        lockForWrite(conceptId, userId);
        wishMapper.delete(userId, conceptId);
        wishMapper.refreshWishCount(conceptId);
        return new ConceptWishResponse(false, wishMapper.findWishCount(conceptId), null);
    }

    /** 회원이 찜한 공개 카드, 찜한 시각 최신순 (AC8). 없거나 탈퇴한 회원이면 USER_404 (AC9). */
    @Transactional(readOnly = true)
    public PageResponse<ConceptSummaryResponse> listByUser(long userId, PageQuery page) {
        userService.getPublicMember(userId);
        long total = wishMapper.countPublishedByUser(userId);
        List<ConceptSummaryResponse> items =
                total == 0
                        ? List.of()
                        : wishMapper
                                .findPublishedPageByUser(userId, page.size(), page.offset())
                                .stream()
                                .map(ConceptSummaryResponse::from)
                                .toList();
        return PageResponse.of(items, page, total);
    }

    /** 로그인 회원이 이 카드를 찜했는지 (카드 상세 AC7). */
    @Transactional(readOnly = true)
    public boolean isWished(long conceptId, Long userId) {
        return userId != null && wishMapper.exists(userId, conceptId);
    }

    private ConceptWishResponse alreadyWished(long conceptId) {
        return new ConceptWishResponse(true, wishMapper.findWishCount(conceptId), null);
    }

    /**
     * 카드 행을 먼저 잠그고(FOR UPDATE) 공개 여부와 활성 회원인지 본다. 잠금보다 일반 SELECT가 먼저면 REPEATABLE READ 스냅샷이 잠금 전에
     * 잡혀, 잠금을 기다린 트랜잭션이 앞 트랜잭션이 커밋한 찜을 보지 못한다 (F-11 {@code ConceptReviewConcurrencyTest}와 같은 이유).
     * 없는 id면 잠글 행이 없고 아래에서 404다. 탈퇴 전에 받은 Access 토큰은 AUTH_401로 막는다.
     */
    private void lockForWrite(long conceptId, long userId) {
        wishMapper.lockConcept(conceptId);
        conceptQueryService.getPublished(conceptId);
        userService.getActiveMember(userId);
    }
}
