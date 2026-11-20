package com.vibenote.concept.review;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.concept.ConceptQueryService;
import com.vibenote.concept.review.domain.ConceptReview;
import com.vibenote.concept.review.dto.ConceptRatingResponse;
import com.vibenote.concept.review.dto.ConceptReviewRequest;
import com.vibenote.concept.review.dto.ConceptReviewResponse;
import com.vibenote.concept.review.dto.ConceptReviewSaveResponse;
import com.vibenote.feed.ActivityService;
import com.vibenote.feed.ActivityType;
import com.vibenote.user.UserService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카드 리뷰 (F-11). 모든 요청은 공개 카드만 대상으로 하고(아니면 CONCEPT_404), 쓰기는 카드 행을 잠근 뒤 리뷰를 바꾸고 같은 트랜잭션에서 {@code
 * concepts.avg_rating}을 다시 계산한다.
 */
@Service
public class ConceptReviewService {

    private final ConceptReviewMapper reviewMapper;
    private final ConceptQueryService conceptQueryService;
    private final ActivityService activityService;
    private final UserService userService;
    private final Clock clock;

    public ConceptReviewService(
            ConceptReviewMapper reviewMapper,
            ConceptQueryService conceptQueryService,
            ActivityService activityService,
            UserService userService,
            Clock clock) {
        this.reviewMapper = reviewMapper;
        this.conceptQueryService = conceptQueryService;
        this.activityService = activityService;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<ConceptReviewResponse> list(long conceptId, PageQuery page) {
        conceptQueryService.getPublished(conceptId);
        List<ConceptReviewResponse> items =
                reviewMapper.findPage(conceptId, page.size(), page.offset()).stream()
                        .map(ConceptReviewResponse::from)
                        .toList();
        return PageResponse.of(items, page, reviewMapper.countByConcept(conceptId));
    }

    @Transactional(readOnly = true)
    public Optional<ConceptReviewResponse> findMine(long conceptId, long userId) {
        conceptQueryService.getPublished(conceptId);
        return reviewMapper
                .findByConceptAndUser(conceptId, userId)
                .map(ConceptReviewResponse::from);
    }

    @Transactional
    public ConceptReviewSaveResponse create(
            long conceptId, long userId, ConceptReviewRequest request) {
        lockForWrite(conceptId, userId);
        if (reviewMapper.findByConceptAndUser(conceptId, userId).isPresent()) {
            throw new BusinessException(ErrorCode.CONCEPT_409);
        }
        ConceptReview review =
                new ConceptReview(
                        conceptId,
                        userId,
                        request.rating(),
                        request.content(),
                        LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.CONCEPT_409);
        }
        activityService.record(userId, ActivityType.REVIEW, review.getId());
        return saved(conceptId, userId);
    }

    @Transactional
    public ConceptReviewSaveResponse update(
            long conceptId, long userId, ConceptReviewRequest request) {
        lockForWrite(conceptId, userId);
        ConceptReview mine = getMine(conceptId, userId);
        reviewMapper.update(mine.getId(), request.rating(), request.content());
        return saved(conceptId, userId);
    }

    @Transactional
    public ConceptRatingResponse delete(long conceptId, long userId) {
        lockForWrite(conceptId, userId);
        ConceptReview mine = getMine(conceptId, userId);
        reviewMapper.delete(mine.getId());
        return refreshRating(conceptId);
    }

    /**
     * 카드 행을 먼저 잠그고(FOR UPDATE) 공개 여부와 활성 회원인지 본다. 순서를 바꾸면 첫 일반 SELECT가 REPEATABLE READ 스냅샷을 잠금 전에
     * 잡아, 잠금을 기다린 트랜잭션이 앞 트랜잭션이 커밋한 리뷰를 보지 못한다 ({@code ConceptReviewConcurrencyTest}). 없는 id면 잠글 행이
     * 없고 아래에서 404다. 필터는 DB를 보지 않으므로 탈퇴 전에 받은 Access 토큰은 여기서 AUTH_401로 막는다 (Codex 리뷰).
     */
    private void lockForWrite(long conceptId, long userId) {
        reviewMapper.lockConcept(conceptId);
        conceptQueryService.getPublished(conceptId);
        userService.getActiveMember(userId);
    }

    private ConceptReview getMine(long conceptId, long userId) {
        return reviewMapper
                .findByConceptAndUser(conceptId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONCEPT_001));
    }

    private ConceptReviewSaveResponse saved(long conceptId, long userId) {
        ConceptRatingResponse rating = refreshRating(conceptId);
        ConceptReview review = getMine(conceptId, userId);
        return ConceptReviewSaveResponse.of(ConceptReviewResponse.from(review), rating);
    }

    private ConceptRatingResponse refreshRating(long conceptId) {
        reviewMapper.refreshAvgRating(conceptId);
        return new ConceptRatingResponse(
                reviewMapper.findAvgRating(conceptId).doubleValue(),
                reviewMapper.countByConcept(conceptId));
    }
}
