package com.vibenote.feed;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.common.security.AuthUser;
import com.vibenote.feed.dto.FeedItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 활동 피드 (F-04). 로그인 필요. */
@Tag(name = "피드", description = "팔로우한 사람의 공개 활동 피드 (F-04)")
@RestController
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @Operation(
            summary = "내 피드",
            description =
                    "팔로우한 사람이 공개한 분석·남긴 리뷰·찜한 카드·쓴 글·챌린지 참여/달성을 활동 시각 최신순(같으면 활동 id"
                            + " 내림차순)으로 페이지 조회한다. 내 활동은 넣지 않는다. 대상을 지금 볼 수 없으면(비공개·삭제된 분석, 지운"
                            + " 리뷰·글·챌린지, 해제한 찜, 공개가 아닌 카드) 목록과 total에서 뺀다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "페이지 값 오류 (COMMON_400)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요, 만료·깨진 토큰 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/api/feed")
    public PageResponse<FeedItemResponse> feed(
            @AuthenticationPrincipal AuthUser user, @Valid @ParameterObject PageQuery page) {
        return feedService.getFeed(user.id(), page);
    }
}
