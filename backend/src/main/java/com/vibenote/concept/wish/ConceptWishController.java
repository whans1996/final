package com.vibenote.concept.wish;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.common.security.AuthUser;
import com.vibenote.concept.ConceptApiDocs;
import com.vibenote.concept.dto.ConceptSummaryResponse;
import com.vibenote.concept.wish.dto.ConceptWishResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 개념 카드 찜 (F-12). 찜·해제는 로그인 필요, 회원의 찜 목록은 공개. */
@Tag(name = ConceptApiDocs.TAG, description = ConceptApiDocs.TAG_DESCRIPTION)
@RestController
public class ConceptWishController {

    private final ConceptWishService wishService;

    public ConceptWishController(ConceptWishService wishService) {
        this.wishService = wishService;
    }

    @Operation(
            summary = "카드 찜",
            description =
                    "공개 카드를 찜한다. 이미 찜했어도 200(멱등)이다. 새로 찜하면 내일 복습 계획(reason WISH)에 넣어 보고, 넣었으면"
                            + " reviewPlanDate로 그 날짜를 돌려준다.")
    @ApiResponse(responseCode = "200", description = "찜 상태")
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 카드이거나 공개되지 않은 카드 (CONCEPT_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/api/concepts/{conceptId}/wish")
    public ConceptWishResponse wish(
            @Parameter(description = "카드 ID", example = "57") @PathVariable long conceptId,
            @AuthenticationPrincipal AuthUser user) {
        return wishService.wish(conceptId, user.id());
    }

    @Operation(
            summary = "카드 찜 해제",
            description = "찜을 푼다. 찜하지 않은 카드여도 200(멱등)이고 바뀐 찜 수를 돌려준다. 복습 계획은 지우지 않는다.")
    @ApiResponse(responseCode = "200", description = "찜 상태")
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 카드이거나 공개되지 않은 카드 (CONCEPT_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/api/concepts/{conceptId}/wish")
    public ConceptWishResponse unwish(
            @Parameter(description = "카드 ID", example = "57") @PathVariable long conceptId,
            @AuthenticationPrincipal AuthUser user) {
        return wishService.unwish(conceptId, user.id());
    }

    @Operation(
            summary = "회원의 찜 목록",
            description = "회원이 찜한 공개(PUBLISHED) 카드를 찜한 시각 최신순으로 페이지 조회한다. 비로그인도 쓸 수 있다.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "페이지 값 오류 (COMMON_400)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없거나 탈퇴한 회원 (USER_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/api/users/{userId}/wishes")
    public PageResponse<ConceptSummaryResponse> listByUser(
            @Parameter(description = "회원 ID", example = "3") @PathVariable long userId,
            @Valid @ParameterObject PageQuery page) {
        return wishService.listByUser(userId, page);
    }
}
