package com.vibenote.concept.admin;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.concept.admin.dto.AdminConceptCreateResponse;
import com.vibenote.concept.admin.dto.AdminConceptDetailResponse;
import com.vibenote.concept.admin.dto.AdminConceptRequest;
import com.vibenote.concept.admin.dto.AdminConceptSearchRequest;
import com.vibenote.concept.admin.dto.AdminConceptStatusCountsResponse;
import com.vibenote.concept.admin.dto.AdminConceptSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 개념 카드 관리 (F-13): 목록·상태별 개수·상세·생성·수정·초안 삭제. ADMIN만 (SecurityConfig). */
@Tag(name = "관리자", description = "관리자 전용 기능 (개념 카드 검수·관리)")
@ApiResponse(
        responseCode = "401",
        description = "토큰 없음·만료 (AUTH_401)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(
        responseCode = "403",
        description = "ADMIN이 아님 (AUTH_403)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@RestController
@RequestMapping("/api/admin/concepts")
public class AdminConceptController {

    private final AdminConceptService adminConceptService;

    public AdminConceptController(AdminConceptService adminConceptService) {
        this.adminConceptService = adminConceptService;
    }

    @Operation(
            summary = "관리자 카드 목록",
            description = "상태와 상관없이 모든 카드를 상태·출처·키워드(이름·요약 부분 일치)로 거르고 수정일 최신순으로 페이지 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "조건·페이지 값 오류 (COMMON_400, details에 필드별 사유)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public PageResponse<AdminConceptSummaryResponse> list(
            @Valid @ParameterObject AdminConceptSearchRequest search,
            @Valid @ParameterObject PageQuery page) {
        return adminConceptService.search(search, page);
    }

    @Operation(summary = "상태별 카드 수", description = "검수 대기·공개·숨김 카드 수. 카드가 없는 상태도 0으로 준다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/status-counts")
    public AdminConceptStatusCountsResponse statusCounts() {
        return adminConceptService.countByStatus();
    }

    @Operation(summary = "관리자 카드 상세", description = "상태와 상관없이 카드의 모든 내용과 연결된 분석 수·리뷰 수를 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "404",
            description = "없는 카드 (CONCEPT_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public AdminConceptDetailResponse detail(
            @Parameter(description = "카드 ID", example = "91") @PathVariable long id) {
        return adminConceptService.getDetail(id);
    }

    @Operation(
            summary = "카드 생성",
            description = "관리자가 카드를 직접 만든다 (출처 ADMIN). 상태는 DRAFT(초안)·PUBLISHED(바로 공개)만 받는다.")
    @ApiResponse(responseCode = "201", description = "생성 성공")
    @ApiResponse(
            responseCode = "400",
            description = "입력값 오류·HIDDEN으로 생성 (COMMON_400, details에 필드별 사유)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "같은 이름의 카드가 있음 (CONCEPT_002, details.name)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminConceptCreateResponse create(@Valid @RequestBody AdminConceptRequest request) {
        return new AdminConceptCreateResponse(adminConceptService.create(request));
    }

    @Operation(
            summary = "카드 수정·검수",
            description =
                    "내용과 상태를 전체 교체한다. 초안 공개(DRAFT→PUBLISHED)·숨김·다시 공개에 쓴다. 출처·찜 수·평점은 바뀌지 않는다."
                            + " 공개·숨김 카드는 DRAFT로 되돌릴 수 없고, 시드 카드의 이름은 바꿀 수 없다.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(
            responseCode = "400",
            description = "입력값 오류·시드 카드 이름 변경 (COMMON_400, details에 필드별 사유)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 카드 (CONCEPT_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "같은 이름의 카드가 있음 (CONCEPT_002) / 공개·숨김 카드를 DRAFT로 (CONCEPT_003)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    public AdminConceptDetailResponse update(
            @Parameter(description = "카드 ID", example = "91") @PathVariable long id,
            @Valid @RequestBody AdminConceptRequest request) {
        return adminConceptService.update(id, request);
    }

    @Operation(
            summary = "초안 삭제",
            description =
                    "검수 대기(DRAFT) 카드와 그 분석 연결을 지운다. 공개 이력이 있는 카드의 찜·리뷰가 사라지지 않게 공개·숨김 카드는"
                            + " 지울 수 없다 (숨김을 쓴다).")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(
            responseCode = "404",
            description = "없는 카드 (CONCEPT_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "공개·숨김 카드 (CONCEPT_003)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "카드 ID", example = "91") @PathVariable long id) {
        adminConceptService.delete(id);
    }
}
