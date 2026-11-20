package com.vibenote.concept;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.common.security.AuthUser;
import com.vibenote.concept.dto.ConceptDetailResponse;
import com.vibenote.concept.dto.ConceptSearchRequest;
import com.vibenote.concept.dto.ConceptSummaryResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 개념 카드 목록·검색 (F-09)·상세 (F-10). 리뷰(F-11)·찜(F-12)은 하위 패키지 Controller에 있다. */
@Tag(name = ConceptApiDocs.TAG, description = ConceptApiDocs.TAG_DESCRIPTION)
@RestController
@RequestMapping("/api/concepts")
public class ConceptController {

    private final ConceptService conceptService;

    public ConceptController(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    @Operation(
            summary = "개념 카드 목록·검색",
            description =
                    "공개(PUBLISHED) 카드를 키워드(이름·요약 부분 일치)·카테고리·난이도로 거르고 인기순(찜 수→평점) 또는 최신순으로"
                            + " 페이지 조회한다. 비로그인도 쓸 수 있다.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "검색 조건·페이지 값 오류 (COMMON_400, details에 필드별 사유)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public PageResponse<ConceptSummaryResponse> list(
            @Valid @ParameterObject ConceptSearchRequest search,
            @Valid @ParameterObject PageQuery page) {
        return conceptService.search(search, page);
    }

    @Operation(
            summary = "개념 카드 상세",
            description =
                    "공개(PUBLISHED) 카드의 본문·예제 코드·출처와 관련 카드(최대 4개)를 조회한다. 비로그인도 쓸 수 있고, 로그인하면"
                            + " 내 분석(삭제되지 않은 DONE)에서 이 개념이 등장한 위치(최신 10개)를 함께 받는다. 토큰이 깨졌거나 만료되면"
                            + " 비로그인과 같다.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "404",
            description = "없는 카드이거나 공개되지 않은 카드 (CONCEPT_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ConceptDetailResponse detail(
            @Parameter(description = "카드 ID", example = "57") @PathVariable long id,
            @AuthenticationPrincipal AuthUser user) {
        return conceptService.getDetail(id, user == null ? null : user.id());
    }
}
