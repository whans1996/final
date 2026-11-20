package com.vibenote.concept.review;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.common.security.AuthUser;
import com.vibenote.concept.review.dto.ConceptRatingResponse;
import com.vibenote.concept.review.dto.ConceptReviewRequest;
import com.vibenote.concept.review.dto.ConceptReviewResponse;
import com.vibenote.concept.review.dto.ConceptReviewSaveResponse;
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
import org.springframework.http.ResponseEntity;
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

/** 개념 카드 리뷰 (F-11). 목록은 공개, 내 리뷰 조회·작성·수정·삭제는 로그인 필요. */
@Tag(name = "개념 카드", description = ConceptReviewController.TAG_DESCRIPTION)
@RestController
@RequestMapping("/api/concepts/{conceptId}/reviews")
public class ConceptReviewController {

    /** ConceptController와 같은 값 (같은 Tag의 설명이 달라지지 않게). */
    public static final String TAG_DESCRIPTION = "공개 개념 카드 목록·검색·상세·리뷰";

    private final ConceptReviewService reviewService;

    public ConceptReviewController(ConceptReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(
            summary = "카드 리뷰 목록",
            description = "공개 카드의 리뷰를 최신순(작성 시각 내림차순)으로 페이지 조회한다. 비로그인도 쓸 수 있다.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "페이지 값 오류 (COMMON_400)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 카드이거나 공개되지 않은 카드 (CONCEPT_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public PageResponse<ConceptReviewResponse> list(
            @Parameter(description = "카드 ID", example = "57") @PathVariable long conceptId,
            @Valid @ParameterObject PageQuery page) {
        return reviewService.list(conceptId, page);
    }

    @Operation(summary = "내 리뷰 조회", description = "이 카드에 내가 쓴 리뷰. 없으면 204(본문 없음).")
    @ApiResponse(responseCode = "200", description = "내 리뷰")
    @ApiResponse(responseCode = "204", description = "이 카드에 쓴 리뷰가 없음", content = @Content)
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 카드이거나 공개되지 않은 카드 (CONCEPT_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/me")
    public ResponseEntity<ConceptReviewResponse> mine(
            @Parameter(description = "카드 ID", example = "57") @PathVariable long conceptId,
            @AuthenticationPrincipal AuthUser user) {
        return reviewService
                .findMine(conceptId, user.id())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(
            summary = "리뷰 작성",
            description =
                    "별점(1~5)과 내용(앞뒤 공백 제외 1~200자)으로 리뷰를 쓴다. 카드당 한 번만 쓸 수 있고, 카드 평균 별점을 다시 계산해"
                            + " 함께 돌려준다.")
    @ApiResponse(responseCode = "201", description = "작성 성공")
    @ApiResponse(
            responseCode = "400",
            description = "별점·내용 검증 실패 (COMMON_400, details에 필드별 사유)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 카드이거나 공개되지 않은 카드 (CONCEPT_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "이미 이 카드에 리뷰를 남김 (CONCEPT_409)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConceptReviewSaveResponse create(
            @Parameter(description = "카드 ID", example = "57") @PathVariable long conceptId,
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody ConceptReviewRequest request) {
        return reviewService.create(conceptId, user.id(), request);
    }

    @Operation(
            summary = "내 리뷰 수정",
            description = "내 리뷰의 별점·내용을 바꾼다. 작성 시각과 목록 순서는 그대로이고, 카드 평균 별점을 다시 계산해 돌려준다.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(
            responseCode = "400",
            description = "별점·내용 검증 실패 (COMMON_400, details에 필드별 사유)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는·비공개 카드 (CONCEPT_404) 또는 작성한 리뷰가 없음 (CONCEPT_001)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/me")
    public ConceptReviewSaveResponse update(
            @Parameter(description = "카드 ID", example = "57") @PathVariable long conceptId,
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody ConceptReviewRequest request) {
        return reviewService.update(conceptId, user.id(), request);
    }

    @Operation(
            summary = "내 리뷰 삭제",
            description = "내 리뷰를 지우고 다시 계산한 카드 평균 별점·리뷰 수를 돌려준다 (반올림된 평균을 화면이 다시 계산할 수 없어 200).")
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는·비공개 카드 (CONCEPT_404) 또는 작성한 리뷰가 없음 (CONCEPT_001)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/me")
    public ConceptRatingResponse delete(
            @Parameter(description = "카드 ID", example = "57") @PathVariable long conceptId,
            @AuthenticationPrincipal AuthUser user) {
        return reviewService.delete(conceptId, user.id());
    }
}
