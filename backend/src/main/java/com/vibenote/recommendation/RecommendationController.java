package com.vibenote.recommendation;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.security.AuthUser;
import com.vibenote.recommendation.dto.RecommendationListResponse;
import com.vibenote.recommendation.dto.RecommendationQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 맞춤 개념 카드 추천 (docs/specs/F-16-recommendation.md 3절). 로그인 필요. */
@Tag(name = "추천", description = "퀴즈 오답·내 코드·찜한 분야·수준으로 고른 맞춤 개념 카드와 AI 추천 이유")
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Operation(
            summary = "맞춤 추천 카드 조회",
            description =
                    "규칙 점수(min(오답,3)×3 + 내 분석 등장 2 + 찜한 분야 1 + 수준 적합도) 순으로 size개."
                            + " 복습을 끝낸 카드, 오늘까지 복습할 카드, 찜한 카드는 뺀다. 이유는 캐시가 없거나 근거가 바뀐 카드만 LLM으로"
                            + " 만들고, LLM이 실패하면 규칙 문장(RULE)을 준다.")
    @ApiResponse(responseCode = "200", description = "조회 성공 (공개 카드가 없으면 빈 목록)")
    @ApiResponse(
            responseCode = "400",
            description = "size가 정수가 아님, 1 미만, 10 초과 (COMMON_400, details.size)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public RecommendationListResponse listRecommendations(
            @AuthenticationPrincipal AuthUser user,
            @Valid @ParameterObject RecommendationQuery query) {
        return recommendationService.recommend(user.id(), query.size());
    }
}
