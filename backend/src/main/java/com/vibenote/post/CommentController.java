package com.vibenote.post;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.security.AuthUser;
import com.vibenote.post.dto.CommentListResponse;
import com.vibenote.post.dto.CommentRequest;
import com.vibenote.post.dto.CommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 커뮤니티 댓글 (F-18). 목록은 공개, 작성·수정·삭제는 로그인 필요. 수정·삭제는 댓글 작성자 본인만. */
@Tag(name = PostApiDocs.TAG, description = PostApiDocs.TAG_DESCRIPTION)
@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(
            summary = "댓글 목록",
            description =
                    "한 글의 댓글을 오래된순(작성 시각, id 오름차순)으로 최대 200개 돌려준다. total은 전체 댓글 수. 비로그인도 쓸 수 있다.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "404",
            description = "없는 글 (POST_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public CommentListResponse list(
            @Parameter(description = "글 ID", example = "42") @PathVariable long postId) {
        return commentService.list(postId);
    }

    @Operation(
            summary = "댓글 작성",
            description = "글에 댓글(앞뒤 공백 제외 1~500자)을 쓴다. 남의 글·자기 글 모두 쓸 수 있고, 피드 활동은 남기지 않는다.")
    @ApiResponse(responseCode = "201", description = "작성 성공")
    @ApiResponse(
            responseCode = "400",
            description = "검증 실패 (COMMON_400, details.content)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 글 (POST_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
            @Parameter(description = "글 ID", example = "42") @PathVariable long postId,
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody CommentRequest request) {
        return commentService.create(postId, user.id(), request);
    }

    @Operation(
            summary = "댓글 수정",
            description =
                    "내 댓글의 내용을 바꾼다. 같은 내용이면 바꾸지 않는다(edited 유지). 글(404) → 댓글(404) → 작성자(403)를 본 뒤"
                            + " 내용을 검증한다.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(
            responseCode = "400",
            description = "검증 실패 (COMMON_400, details.content)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "403",
            description = "남의 댓글 (POST_004, 글 작성자여도)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 글 (POST_404) 또는 없는 댓글·그 글의 댓글이 아님 (POST_003)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{commentId}")
    public CommentResponse update(
            @Parameter(description = "글 ID", example = "42") @PathVariable long postId,
            @Parameter(description = "댓글 ID", example = "101") @PathVariable long commentId,
            @AuthenticationPrincipal AuthUser user,
            @RequestBody CommentRequest request) {
        return commentService.update(postId, commentId, user.id(), request);
    }

    @Operation(summary = "댓글 삭제", description = "내 댓글을 지운다.")
    @ApiResponse(responseCode = "204", description = "삭제 성공", content = @Content)
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "403",
            description = "남의 댓글 (POST_004, 글 작성자여도)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 글 (POST_404) 또는 없는 댓글·그 글의 댓글이 아님 (POST_003)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "글 ID", example = "42") @PathVariable long postId,
            @Parameter(description = "댓글 ID", example = "101") @PathVariable long commentId,
            @AuthenticationPrincipal AuthUser user) {
        commentService.delete(postId, commentId, user.id());
    }
}
