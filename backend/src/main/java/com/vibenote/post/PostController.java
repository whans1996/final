package com.vibenote.post;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.common.security.AuthUser;
import com.vibenote.post.dto.PostDetailResponse;
import com.vibenote.post.dto.PostIdResponse;
import com.vibenote.post.dto.PostRequest;
import com.vibenote.post.dto.PostSearchRequest;
import com.vibenote.post.dto.PostSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 커뮤니티 게시글 (F-17). 목록·상세는 공개, 작성·수정·삭제는 로그인 필요. */
@Tag(name = PostApiDocs.TAG, description = PostApiDocs.TAG_DESCRIPTION)
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @Operation(
            summary = "게시글 목록·검색",
            description =
                    "게시판 구분 없이 또는 한 게시판의 글을 최신순(작성 시각 내림차순)으로 페이지 조회한다. keyword는 제목·본문 부분 일치."
                            + " 비로그인도 쓸 수 있다.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "게시판·검색어·페이지 값 오류 (COMMON_400, details에 필드별 사유)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public PageResponse<PostSummaryResponse> list(
            @Valid @ParameterObject PostSearchRequest search,
            @Valid @ParameterObject PageQuery page) {
        return postService.search(search, page);
    }

    @Operation(
            summary = "게시글 상세",
            description =
                    "조회수를 1 올리고 글을 돌려준다(작성자 본인도 오른다. countView=false면 올리지 않는다). 비로그인도 쓸 수 있고, 유효한"
                            + " 토큰이 있으면 분석 첨부의 볼 수 있는지 판단(내 분석)에 쓴다.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "404",
            description = "없는 글 (POST_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{postId}")
    public PostDetailResponse detail(
            @Parameter(description = "글 ID", example = "42") @PathVariable long postId,
            @Parameter(
                            description = "false면 조회수를 올리지 않고 지금 값을 돌려준다 (글 수정 화면이 글을 불러올 때, #209)",
                            example = "true")
                    @RequestParam(defaultValue = "true")
                    boolean countView,
            @AuthenticationPrincipal AuthUser user) {
        return postService.getDetail(postId, user == null ? null : user.id(), countView);
    }

    @Operation(
            summary = "게시글 작성",
            description =
                    "게시판·제목(앞뒤 공백 제외 1~100자)·본문(1~5000자)으로 글을 쓴다. 내 공개 DONE 분석이나 공개 개념 카드 중 하나를 첨부할"
                            + " 수 있다. 팔로워 피드용 POST 활동을 함께 남긴다.")
    @ApiResponse(responseCode = "201", description = "작성 성공")
    @ApiResponse(
            responseCode = "400",
            description =
                    "검증 실패 (COMMON_400, details에 필드별 사유. 첨부 둘 다는 details.conceptId) 또는 첨부할 수 없는 분석"
                            + " (POST_001)·카드 (POST_002)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostIdResponse create(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody PostRequest request) {
        return postService.create(user.id(), request);
    }

    @Operation(
            summary = "게시글 수정",
            description =
                    "내 글의 게시판·제목·본문·첨부를 보낸 값으로 통째로 바꾼다(첨부를 없애려면 둘 다 null). 조회수·작성 시각은 그대로다. 검증은"
                            + " 작성과 같고, 글 존재(404)·작성자(403)를 먼저 본다.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(
            responseCode = "400",
            description = "검증 실패 (COMMON_400) 또는 첨부할 수 없는 분석 (POST_001)·카드 (POST_002)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "403",
            description = "남의 글 (POST_403)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 글 (POST_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{postId}")
    public PostDetailResponse update(
            @Parameter(description = "글 ID", example = "42") @PathVariable long postId,
            @AuthenticationPrincipal AuthUser user,
            @RequestBody PostRequest request) {
        return postService.update(postId, user.id(), request);
    }

    @Operation(summary = "게시글 삭제", description = "내 글과 그 댓글을 지운다.")
    @ApiResponse(responseCode = "204", description = "삭제 성공", content = @Content)
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "403",
            description = "남의 글 (POST_403)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 글 (POST_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "글 ID", example = "42") @PathVariable long postId,
            @AuthenticationPrincipal AuthUser user) {
        postService.delete(postId, user.id());
    }
}
