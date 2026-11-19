package com.vibenote.concept;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.concept.dto.ConceptSearchRequest;
import com.vibenote.concept.dto.ConceptSummaryResponse;
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

/** 개념 카드 목록·검색 (F-09). 상세(F-10)·리뷰(F-11)·찜(F-12)은 각 기능에서 추가한다. */
@Tag(name = "개념 카드", description = "공개 개념 카드 목록·검색·상세")
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
}
