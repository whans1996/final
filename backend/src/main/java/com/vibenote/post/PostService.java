package com.vibenote.post;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.feed.ActivityService;
import com.vibenote.feed.ActivityType;
import com.vibenote.post.domain.Post;
import com.vibenote.post.dto.PostDetailResponse;
import com.vibenote.post.dto.PostIdResponse;
import com.vibenote.post.dto.PostRequest;
import com.vibenote.post.dto.PostSearchRequest;
import com.vibenote.post.dto.PostSummaryResponse;
import com.vibenote.user.UserService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커뮤니티 게시글 (F-17). 쓰기는 모두 활성 회원인지 먼저 본다 (필터는 DB를 보지 않으므로 탈퇴 전에 받은 Access 토큰은 여기서 AUTH_401). 수정·삭제의
 * 판단 순서는 글 존재(404) → 작성자(403) → 본문 검증(400) → 첨부(POST_001·002)다.
 */
@Service
public class PostService {

    static final String SINGLE_ATTACHMENT_MESSAGE = "분석과 개념 카드 중 하나만 첨부할 수 있습니다.";

    private final PostMapper postMapper;
    private final UserService userService;
    private final ActivityService activityService;
    private final Validator validator;
    private final Clock clock;

    public PostService(
            PostMapper postMapper,
            UserService userService,
            ActivityService activityService,
            Validator validator,
            Clock clock) {
        this.postMapper = postMapper;
        this.userService = userService;
        this.activityService = activityService;
        this.validator = validator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> search(PostSearchRequest search, PageQuery page) {
        BoardType boardType = search.boardTypeValue();
        String keyword = search.keyword() == null ? null : escapeLike(search.keyword());
        List<PostSummaryResponse> items =
                postMapper.findPage(boardType, keyword, page.size(), page.offset()).stream()
                        .map(PostSummaryResponse::from)
                        .toList();
        return PageResponse.of(items, page, postMapper.count(boardType, keyword));
    }

    /** 조회수를 1 올린 뒤 그 값을 담아 돌려준다 (같은 트랜잭션). 없는 글이면 아무 글도 바꾸지 않고 POST_404. */
    @Transactional
    public PostDetailResponse getDetail(long postId, Long viewerId) {
        if (postMapper.incrementViewCount(postId) == 0) {
            throw new BusinessException(ErrorCode.POST_404);
        }
        return PostDetailResponse.from(getRow(postId), viewerId);
    }

    /** 본문 형식은 Controller의 {@code @Valid}가 이미 검증했다. 같은 트랜잭션에서 POST 활동을 남긴다. */
    @Transactional
    public PostIdResponse create(long userId, PostRequest request) {
        userService.getActiveMember(userId);
        checkAttachments(userId, request);
        Post post =
                new Post(
                        userId,
                        request.boardTypeValue(),
                        request.title(),
                        request.content(),
                        request.analysisId(),
                        request.conceptId(),
                        now());
        postMapper.insert(post);
        activityService.record(userId, ActivityType.POST, post.getId());
        return new PostIdResponse(post.getId());
    }

    /**
     * 보낸 값으로 통째로 바꾼다. 남의 글에 잘못된 본문을 보내도 403이 먼저라, 형식 검증을 Controller가 아니라 작성자 확인 뒤 여기서 한다 (spec 9절).
     * 활동은 새로 남기지 않는다.
     */
    @Transactional
    public PostDetailResponse update(long postId, long userId, PostRequest request) {
        userService.getActiveMember(userId);
        checkAuthor(postId, userId);
        validate(request);
        checkAttachments(userId, request);
        postMapper.update(
                postId,
                request.boardTypeValue(),
                request.title(),
                request.content(),
                request.analysisId(),
                request.conceptId(),
                now());
        return PostDetailResponse.from(getRow(postId), userId);
    }

    /** 댓글은 FK CASCADE로 함께 지워진다. POST 활동 행은 남긴다 (F-04 피드가 posts와 JOIN해 거른다). */
    @Transactional
    public void delete(long postId, long userId) {
        userService.getActiveMember(userId);
        checkAuthor(postId, userId);
        postMapper.delete(postId);
    }

    private void checkAuthor(long postId, long userId) {
        Long authorId = postMapper.findAuthorIdForUpdate(postId);
        if (authorId == null) {
            throw new BusinessException(ErrorCode.POST_404);
        }
        if (authorId != userId) {
            throw new BusinessException(ErrorCode.POST_403);
        }
    }

    /** {@code @Valid}와 같은 제약을 직접 검증하고 같은 형식({@code COMMON_400}, 필드별 details)으로 던진다. */
    private void validate(PostRequest request) {
        Set<ConstraintViolation<PostRequest>> violations = validator.validate(request);
        if (violations.isEmpty()) return;
        Map<String, String> details = new LinkedHashMap<>();
        violations.stream()
                .sorted(
                        (a, b) ->
                                a.getPropertyPath()
                                        .toString()
                                        .compareTo(b.getPropertyPath().toString()))
                .forEach(v -> details.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        throw new BusinessException(ErrorCode.COMMON_400, details);
    }

    private void checkAttachments(long userId, PostRequest request) {
        Long analysisId = request.analysisId();
        Long conceptId = request.conceptId();
        if (analysisId != null && conceptId != null) {
            throw new BusinessException(
                    ErrorCode.COMMON_400, Map.of("conceptId", SINGLE_ATTACHMENT_MESSAGE));
        }
        if (analysisId != null && !postMapper.existsAttachableAnalysis(analysisId, userId)) {
            throw new BusinessException(ErrorCode.POST_001);
        }
        if (conceptId != null && !postMapper.existsPublishedConcept(conceptId)) {
            throw new BusinessException(ErrorCode.POST_002);
        }
    }

    private Post getRow(long postId) {
        return postMapper
                .findDetail(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_404));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * LIKE 패턴의 와일드카드를 글자 그대로 찾도록 {@code !}로 이스케이프한다 (SQL은 {@code ESCAPE '!'}). F-09 {@code
     * ConceptService.escapeLike}와 같은 규칙이다. MySQL 기본 모드에서 백슬래시가 문자열 이스케이프라 {@code !}를 쓴다.
     */
    static String escapeLike(String keyword) {
        return keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
