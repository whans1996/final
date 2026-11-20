package com.vibenote.reviewplan;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.security.AuthUser;
import com.vibenote.reviewplan.dto.ReviewPlanCreateRequest;
import com.vibenote.reviewplan.dto.ReviewPlanItemResponse;
import com.vibenote.reviewplan.dto.ReviewPlanListResponse;
import com.vibenote.reviewplan.dto.ReviewPlanStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 복습 계획 (docs/specs/F-15-review-plan.md 3절). 로그인 필요 · 본인 계획만. */
@Tag(name = "복습 계획", description = "날짜별 복습 계획 조회, 직접 추가, 완료 체크, 삭제")
@RestController
@RequestMapping("/api/review-plans")
public class ReviewPlanController {

    private final ReviewPlanService reviewPlanService;

    public ReviewPlanController(ReviewPlanService reviewPlanService) {
        this.reviewPlanService = reviewPlanService;
    }

    @Operation(
            summary = "복습 계획 조회",
            description =
                    "기간(from~to, 생략하면 오늘부터 7일) 안의 내 계획과, 기간과 상관없이 오늘 이전 날짜의 TODO(밀린 복습)."
                            + " 공개되지 않은 카드의 계획은 뺀다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "날짜 형식 오류, 한쪽만 줌, from > to, 31일 초과 (COMMON_400, details에 from·to)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public ReviewPlanListResponse listReviewPlans(
            @AuthenticationPrincipal AuthUser user,
            @Parameter(description = "기간 시작 (YYYY-MM-DD, to와 함께)", example = "2026-11-20")
                    @RequestParam(required = false)
                    String from,
            @Parameter(
                            description = "기간 끝 (YYYY-MM-DD, from과 함께, from부터 30일 뒤까지)",
                            example = "2026-11-26")
                    @RequestParam(required = false)
                    String to) {
        return reviewPlanService.list(user.id(), from, to);
    }

    @Operation(
            summary = "복습 계획 직접 추가",
            description =
                    "공개 카드를 planDate(생략하면 내일)의 TODO로 넣는다 (reason MANUAL). 한 카드의 TODO는 하나만 둔다.")
    @ApiResponse(responseCode = "201", description = "추가 성공")
    @ApiResponse(
            responseCode = "400",
            description = "conceptId 없음·0 이하, planDate가 오늘 이전·오늘+365일 뒤 (COMMON_400, details에 필드)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 카드, 공개되지 않은 카드 (CONCEPT_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "그 카드의 TODO가 이미 있음, 같은 날짜의 계획이 있음 (REVIEW_PLAN_409)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewPlanItemResponse addReviewPlan(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody ReviewPlanCreateRequest request) {
        return reviewPlanService.addManual(user.id(), request.conceptId(), request.planDate());
    }

    @Operation(
            summary = "복습 완료·되돌림",
            description = "DONE이면 완료 시각을 지금으로, TODO면 완료 시각을 지운다. 이미 그 상태면 바꾸지 않는다.")
    @ApiResponse(responseCode = "200", description = "변경 성공")
    @ApiResponse(
            responseCode = "400",
            description = "status 없음, TODO·DONE이 아님 (COMMON_400)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 계획, 다른 회원의 계획, 공개되지 않은 카드의 계획 (REVIEW_PLAN_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "DONE→TODO인데 그 카드의 다른 TODO가 있음 (REVIEW_PLAN_409)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id:\\d+}")
    public ReviewPlanItemResponse changeReviewPlanStatus(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable long id,
            @Valid @RequestBody ReviewPlanStatusRequest request) {
        return reviewPlanService.changeStatus(user.id(), id, request.status());
    }

    @Operation(summary = "복습 계획 삭제")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 계획, 다른 회원의 계획, 공개되지 않은 카드의 계획 (REVIEW_PLAN_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReviewPlan(@AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        reviewPlanService.delete(user.id(), id);
    }
}
