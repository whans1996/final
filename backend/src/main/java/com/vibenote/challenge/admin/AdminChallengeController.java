package com.vibenote.challenge.admin;

import com.vibenote.challenge.admin.dto.AdminChallengeCreateResponse;
import com.vibenote.challenge.admin.dto.AdminChallengeRequest;
import com.vibenote.challenge.admin.dto.AdminChallengeResponse;
import com.vibenote.challenge.admin.dto.AdminChallengeSearchRequest;
import com.vibenote.challenge.admin.dto.AdminChallengeSummaryResponse;
import com.vibenote.common.AdminApiDocs;
import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.common.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 학습 챌린지 관리 (F-24): 목록·상세·등록·수정·삭제. ADMIN만 (SecurityConfig). 등록·수정은 대표 이미지 때문에
 * multipart/form-data로 받는다.
 */
@Tag(name = AdminApiDocs.TAG, description = AdminApiDocs.TAG_DESCRIPTION)
@ApiResponse(
        responseCode = "401",
        description = "토큰 없음·만료 (AUTH_401)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(
        responseCode = "403",
        description = "ADMIN이 아님 (AUTH_403)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@RestController
@RequestMapping("/api/admin/challenges")
public class AdminChallengeController {

    private final AdminChallengeService adminChallengeService;

    public AdminChallengeController(AdminChallengeService adminChallengeService) {
        this.adminChallengeService = adminChallengeService;
    }

    @Operation(
            summary = "관리자 챌린지 목록",
            description = "상태(오늘 기준으로 계산)로 거르고 시작일 최신순(같으면 id 역순)으로 페이지 조회한다. 항목마다 참여자 수가 있다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "상태·페이지 값 오류 (COMMON_400, details에 필드별 사유)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public PageResponse<AdminChallengeSummaryResponse> list(
            @Valid @ParameterObject AdminChallengeSearchRequest search,
            @Valid @ParameterObject PageQuery page) {
        return adminChallengeService.search(search, page);
    }

    @Operation(summary = "관리자 챌린지 상세", description = "설명·상태·참여자 수·등록·수정 시각을 포함한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "404",
            description = "없는 챌린지 (CHALLENGE_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public AdminChallengeResponse detail(
            @Parameter(description = "챌린지 ID", example = "7") @PathVariable long id) {
        return adminChallengeService.getDetail(id);
    }

    @Operation(
            summary = "챌린지 등록",
            description =
                    "기간·목표·대표 이미지(선택)로 챌린지를 만든다. 종료일은 오늘 이후, 기간은 90일 이하이고 시작일은 과거여도 된다. 이미지는"
                            + " /uploads/challenges/{UUID}.{ext}로 공개된다.")
    @ApiResponse(responseCode = "201", description = "등록 성공")
    @ApiResponse(
            responseCode = "400",
            description = "입력값·이미지 오류 (COMMON_400, details에 필드별 사유, 이미지는 details.image)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "413",
            description = "요청 크기 한도 초과 (COMMON_413, 파일 10MB·요청 12MB)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AdminChallengeCreateResponse create(
            @AuthenticationPrincipal AuthUser user, @ModelAttribute AdminChallengeRequest request) {
        return new AdminChallengeCreateResponse(adminChallengeService.create(user.id(), request));
    }

    @Operation(
            summary = "챌린지 수정",
            description =
                    "전체 교체한다. image가 없고 removeImage가 false면 기존 이미지를 유지한다. 시작한(진행 중·종료) 챌린지는 목표"
                            + " 유형·목표값·시작일을 바꿀 수 없다. 종료일 ≥ 오늘은 종료일이 바뀔 때만 검사한다.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(
            responseCode = "400",
            description =
                    "입력값·이미지 오류, image와 removeImage=true를 함께 보냄 (COMMON_400, details에 필드별 사유)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 챌린지 (CHALLENGE_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "시작한 챌린지의 목표 유형·목표값·시작일 변경 (CHALLENGE_001)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "413",
            description = "요청 크기 한도 초과 (COMMON_413, 파일 10MB·요청 12MB)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminChallengeResponse update(
            @Parameter(description = "챌린지 ID", example = "7") @PathVariable long id,
            @ModelAttribute AdminChallengeRequest request) {
        return adminChallengeService.update(id, request);
    }

    @Operation(summary = "챌린지 삭제", description = "언제든 실제로 지운다. 참여 기록도 함께 지워지고 대표 이미지 파일도 지운다.")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(
            responseCode = "404",
            description = "없는 챌린지 (CHALLENGE_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "챌린지 ID", example = "7") @PathVariable long id) {
        adminChallengeService.delete(id);
    }
}
