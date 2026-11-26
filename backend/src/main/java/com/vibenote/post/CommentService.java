package com.vibenote.post;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.post.domain.Comment;
import com.vibenote.post.dto.CommentListResponse;
import com.vibenote.post.dto.CommentRequest;
import com.vibenote.post.dto.CommentResponse;
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
 * 커뮤니티 댓글 (F-18). 쓰기는 모두 활성 회원인지 먼저 본다 (탈퇴 전에 받은 Access 토큰은 여기서 AUTH_401). 수정·삭제의 판단 순서는
 * 글(POST_404) → 그 글의 댓글(POST_003) → 작성자(POST_004) → 형식 검증(400)이다. 피드 활동은 남기지 않는다 (11-20 결정).
 */
@Service
public class CommentService {

    /** 목록 응답 크기 안전장치 (spec AC2). 더 많으면 최근 것만 보내고 total로 알린다 (#208: 새 댓글이 보여야 한다). */
    static final int MAX_COMMENTS = 200;

    private final CommentMapper commentMapper;
    private final UserService userService;
    private final Validator validator;
    private final Clock clock;

    public CommentService(
            CommentMapper commentMapper,
            UserService userService,
            Validator validator,
            Clock clock) {
        this.commentMapper = commentMapper;
        this.userService = userService;
        this.validator = validator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CommentListResponse list(long postId) {
        if (!commentMapper.existsPost(postId)) {
            throw new BusinessException(ErrorCode.POST_404);
        }
        List<CommentResponse> items =
                commentMapper.findByPost(postId, MAX_COMMENTS).stream()
                        .map(CommentResponse::from)
                        .toList();
        return new CommentListResponse(items, commentMapper.countByPost(postId));
    }

    /** 본문 형식은 Controller의 {@code @Valid}가 이미 검증했다. 남의 글·자기 글 모두 쓸 수 있다. */
    @Transactional
    public CommentResponse create(long postId, long userId, CommentRequest request) {
        userService.getActiveMember(userId);
        if (commentMapper.lockPost(postId) == 0) {
            throw new BusinessException(ErrorCode.POST_404);
        }
        Comment comment = new Comment(postId, userId, request.content(), now());
        commentMapper.insert(comment);
        return getResponse(comment.getId());
    }

    /**
     * 남의 댓글에 잘못된 본문을 보내도 403이 먼저라, 형식 검증을 작성자 확인 뒤 여기서 한다. 내용이 같으면 바꾸지 않아 updated_at(edited)도 그대로다
     * (AC7). updated_at은 DB 세션 시간대와 무관하게 서비스 시각으로 채운다.
     */
    @Transactional
    public CommentResponse update(
            long postId, long commentId, long userId, CommentRequest request) {
        userService.getActiveMember(userId);
        Comment comment = getOwnComment(postId, commentId, userId);
        validate(request);
        if (!request.content().equals(comment.getContent())) {
            commentMapper.updateContent(commentId, request.content(), now());
        }
        return getResponse(commentId);
    }

    @Transactional
    public void delete(long postId, long commentId, long userId) {
        userService.getActiveMember(userId);
        getOwnComment(postId, commentId, userId);
        commentMapper.delete(commentId);
    }

    private Comment getOwnComment(long postId, long commentId, long userId) {
        if (!commentMapper.existsPost(postId)) {
            throw new BusinessException(ErrorCode.POST_404);
        }
        Comment comment =
                commentMapper
                        .findInPostForUpdate(postId, commentId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.POST_003));
        if (comment.getUserId() != userId) {
            throw new BusinessException(ErrorCode.POST_004);
        }
        return comment;
    }

    /** {@code @Valid}와 같은 제약을 직접 검증하고 같은 형식({@code COMMON_400}, 필드별 details)으로 던진다. */
    private void validate(CommentRequest request) {
        Set<ConstraintViolation<CommentRequest>> violations = validator.validate(request);
        if (violations.isEmpty()) return;
        Map<String, String> details = new LinkedHashMap<>();
        violations.forEach(
                v -> details.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        throw new BusinessException(ErrorCode.COMMON_400, details);
    }

    private CommentResponse getResponse(long commentId) {
        return commentMapper
                .findById(commentId)
                .map(CommentResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_003));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
