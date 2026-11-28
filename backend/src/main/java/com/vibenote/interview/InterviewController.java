package com.vibenote.interview;

import com.vibenote.analysis.AnalysisApiDocs;
import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 면접 예상 질문 요청·조회 (I-1a). 경로가 분석 아래라 Tag는 AnalysisController와 같다. */
@Tag(name = AnalysisApiDocs.TAG, description = AnalysisApiDocs.TAG_DESCRIPTION)
@RestController
@RequestMapping("/api/analyses/{id:\\d+}/interview")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @Operation(
            summary = "면접 예상 질문 요청",
            description =
                    "내 DONE 분석의 코드를 근거로 면접 예상 질문(5~12개) 생성을 접수하고 바로 202를 준다. 생성은 백그라운드에서 진행되며 GET으로"
                            + " 확인한다. 처음 만들기(NONE)·다시 시도(FAILED, 횟수 안 씀)·다시 만들기(DONE, 분석당 2회)를 상태로"
                            + " 고른다.")
    @ApiResponse(responseCode = "202", description = "접수됨 (status PENDING)")
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
            description = "분석이 DONE이 아님, 이미 만드는 중(PENDING) (INTERVIEW_409)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "429",
            description = "DONE 다시 만들기 횟수를 모두 씀 (INTERVIEW_429)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public InterviewResponse requestInterview(
            @AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        return interviewService.request(user.id(), id);
    }

    @Operation(
            summary = "면접 예상 질문 조회",
            description =
                    "내 분석의 면접 질문. 요청한 적이 없으면 status NONE. DONE이 아니면 questions는 null. 관련 카드는 지금 숨김이"
                            + " 아닌 카드만 준다. 화면은 PENDING이면 3초마다 다시 부른다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 분석, 다른 회원의 분석, 삭제된 분석 (ANALYSIS_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public InterviewResponse getInterview(
            @AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        return interviewService.get(user.id(), id);
    }
}
