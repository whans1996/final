package com.vibenote.analysis.flow;

import com.vibenote.analysis.AnalysisApiDocs;
import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 실행 흐름 요청·조회 (F-07). 경로와 Tag는 AnalysisController와 같다. */
@Tag(name = AnalysisApiDocs.TAG, description = AnalysisApiDocs.TAG_DESCRIPTION)
@RestController
@RequestMapping("/api/analyses/{id:\\d+}/flows")
public class FlowController {

    private final FlowService flowService;

    public FlowController(FlowService flowService) {
        this.flowService = flowService;
    }

    @Operation(
            summary = "실행 흐름 요청",
            description =
                    "개요 apis에서 고른 API(1~3개)의 실행 흐름 생성을 접수하고 바로 202와 이 분석의 흐름 전체를 준다. 생성은 백그라운드에서"
                        + " 진행되며 GET으로 확인한다. 이미 PENDING·DONE인 API는 다시 만들지 않고, FAILED면 다시 만든다. 분석당"
                        + " FAILED가 아닌 흐름은 3개까지.")
    @ApiResponse(responseCode = "202", description = "접수됨 (흐름 전체, 방금 요청한 것은 PENDING)")
    @ApiResponse(
            responseCode = "400",
            description =
                    "endpoints 0개·4개 이상·중복·빈 값 (COMMON_400), 개요에 없는 API (ANALYSIS_002), 3개 초과"
                            + " (ANALYSIS_003)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 분석, 다른 회원의 분석, 삭제된 분석 (ANALYSIS_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "분석이 아직 DONE이 아님 (ANALYSIS_409)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<FlowResponse> request(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable long id,
            @Valid @RequestBody CreateFlowsRequest request) {
        return flowService.request(user.id(), id, request.endpoints());
    }

    @Operation(
            summary = "실행 흐름 목록",
            description =
                    "내 분석 또는 공개·완료된 남의 분석의 흐름 전체(id 오름차순). DONE이 아니면 summary·steps는 null. 화면은"
                            + " PENDING이 있으면 3초마다 다시 부른다.")
    @ApiResponse(responseCode = "200", description = "조회 성공 (흐름이 없으면 빈 배열)")
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 분석, 삭제된 분석, 볼 수 없는 남의 분석(비공개·미완료·주인 탈퇴) (ANALYSIS_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public List<FlowResponse> list(@AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        return flowService.list(user.id(), id);
    }
}
