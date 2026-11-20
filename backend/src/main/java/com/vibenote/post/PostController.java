package com.vibenote.post;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.post.dto.PostSearchRequest;
import com.vibenote.post.dto.PostSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
