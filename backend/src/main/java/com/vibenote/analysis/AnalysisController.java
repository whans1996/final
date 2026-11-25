package com.vibenote.analysis;

import com.vibenote.analysis.concept.AnalysisConceptService;
import com.vibenote.analysis.concept.AnalysisConceptsResponse;
import com.vibenote.analysis.dto.AnalysisDetailResponse;
import com.vibenote.analysis.dto.AnalysisFileResponse;
import com.vibenote.analysis.dto.AnalysisFileSummaryResponse;
import com.vibenote.analysis.dto.AnalysisStatusResponse;
import com.vibenote.analysis.dto.AnalysisSummaryResponse;
import com.vibenote.analysis.dto.CreateAnalysisRequest;
import com.vibenote.analysis.dto.CreateAnalysisResponse;
import com.vibenote.analysis.dto.UpdateAnalysisRequest;
import com.vibenote.analysis.dto.UpdateVisibilityRequest;
import com.vibenote.analysis.dto.VisibilityResponse;
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
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 분석 요청(ZIP F-05a·저장소 URL F-05b), 내 분석 목록, 진행 상태 (F-05a), 상세·파일 조회 (F-06), 개념 조회 (F-08), 수정·공개·삭제와
 * 공개 분석 열람 (F-19).
 */
@Tag(name = AnalysisApiDocs.TAG, description = AnalysisApiDocs.TAG_DESCRIPTION)
@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AnalysisConceptService analysisConceptService;

    public AnalysisController(
            AnalysisService analysisService, AnalysisConceptService analysisConceptService) {
        this.analysisService = analysisService;
        this.analysisConceptService = analysisConceptService;
    }

    @Operation(
            summary = "분석 요청 (ZIP·저장소 URL)",
            description =
                    "ZIP 파일(file) 또는 공개 GitHub·GitLab(gitlab.com) 저장소 주소(sourceUrl) 중 하나를 접수하고 바로"
                            + " 202를 준다. 저장소는 기본 브랜치 아카이브를 백그라운드 FETCHING 단계에서 받는다. 코드 수집·필터·선별은"
                            + " 백그라운드에서 진행되며 상태는 /api/analyses/{id}/status로 확인한다. 하루(Asia/Seoul)"
                            + " 5회까지(ZIP·URL 합산)이며 FAILED는 세지 않는다.")
    @ApiResponse(responseCode = "202", description = "접수됨")
    @ApiResponse(
            responseCode = "400",
            description = "검증 실패 (COMMON_400, details.file·sourceUrl·consent·level)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "413",
            description = "업로드 10MB 초과 (COMMON_413)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "429",
            description = "오늘 분석 요청 횟수를 다 씀 (ANALYSIS_429)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CreateAnalysisResponse create(
            @AuthenticationPrincipal AuthUser user,
            @Parameter(description = "프로젝트 ZIP (.zip, 10MB 이하). sourceUrl과 둘 중 하나만 보낸다")
                    @RequestPart(value = "file", required = false)
                    MultipartFile file,
            @Parameter(
                            description =
                                    "공개 GitHub·GitLab(gitlab.com) 저장소 주소 (500자 이하, 브랜치·파일 경로 없이)."
                                            + " file과 둘 중 하나만 보낸다",
                            schema =
                                    @Schema(
                                            type = "string",
                                            maxLength = 500,
                                            example = "https://github.com/octo/todo-app"))
                    @RequestPart(value = "sourceUrl", required = false)
                    String sourceUrl,
            @Parameter(
                            description = "분석 수준. 생략하면 내 프로필 수준",
                            schema =
                                    @Schema(
                                            type = "string",
                                            allowableValues = {"BEGINNER", "JUNIOR", "SENIOR"}))
                    @RequestPart(value = "level", required = false)
                    String level,
            @Parameter(
                            description = "SSAFY GMS를 거친 외부 AI 전송 고지 동의. true여야 한다",
                            required = true,
                            schema =
                                    @Schema(
                                            type = "string",
                                            allowableValues = {"true"}))
                    @RequestPart(value = "consent", required = false)
                    String consent) {
        return analysisService.request(
                user.id(), CreateAnalysisRequest.of(file, sourceUrl, level, consent));
    }

    @Operation(summary = "내 분석 목록", description = "삭제하지 않은 내 분석을 요청 시각 최신순으로 준다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "page·size 범위 밖 (COMMON_400)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public PageResponse<AnalysisSummaryResponse> list(
            @AuthenticationPrincipal AuthUser user, @Valid @ParameterObject PageQuery query) {
        return analysisService.listMine(user.id(), query);
    }

    @Operation(
            summary = "분석 진행 상태",
            description = "내 분석 또는 공개·완료된 남의 분석의 단계·진행률·실패 사유·저장소 주소. 진행 화면이 2초마다 폴링한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 분석, 삭제된 분석, 볼 수 없는 남의 분석(비공개·미완료·주인 탈퇴) (ANALYSIS_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id:\\d+}/status")
    public AnalysisStatusResponse getStatus(
            @AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        return analysisService.getStatus(user.id(), id);
    }

    @Operation(
            summary = "분석 상세",
            description =
                    "내 분석 또는 공개·완료된 남의 분석의 정보와 개요(요약·기술 스택·레이어·읽는 순서·API 목록). 진행 중이거나 개요 생성에 실패했으면"
                            + " overview가 null이다. 남의 분석이면 mine = false, memo = null. LLM을 부르지 않는다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 분석, 삭제된 분석, 볼 수 없는 남의 분석(비공개·미완료·주인 탈퇴) (ANALYSIS_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id:\\d+}")
    public AnalysisDetailResponse getDetail(
            @AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        return analysisService.getDetail(user.id(), id);
    }

    @Operation(
            summary = "분석 파일 목록",
            description =
                    "내 분석 또는 공개·완료된 남의 분석에서 선별한 파일(최대 30개)을 선별 우선순위(id 오름차순)로 준다. 코드 내용은 단건"
                            + " 조회에서 받는다.")
    @ApiResponse(responseCode = "200", description = "조회 성공 (파일 저장 전이면 빈 배열)")
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 분석, 삭제된 분석, 볼 수 없는 남의 분석(비공개·미완료·주인 탈퇴) (ANALYSIS_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id:\\d+}/files")
    public List<AnalysisFileSummaryResponse> listFiles(
            @AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        return analysisService.listFiles(user.id(), id);
    }

    @Operation(
            summary = "분석 파일 하나",
            description = "내 분석 또는 공개·완료된 남의 분석의 파일 역할·설명과 코드 내용. 키·비밀 설정값은 [REDACTED]로 가려져 있다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 분석·삭제된 분석·볼 수 없는 남의 분석 (ANALYSIS_404), 그 분석의 파일이 아님 (ANALYSIS_001)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id:\\d+}/files/{fileId:\\d+}")
    public AnalysisFileResponse getFile(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable long id,
            @PathVariable long fileId) {
        return analysisService.getFile(user.id(), id, fileId);
    }

    @Operation(
            summary = "분석의 개념 카드",
            description =
                    "내 분석 또는 공개·완료된 남의 분석의 코드에 쓰인 공개 개념 카드와 위치(파일·줄·맥락). 검수 전 AI 초안은"
                            + " 수(pendingCount)만 준다. 추출 전이거나 연결이 없으면 빈 목록이다. LLM을 부르지 않는다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 분석, 삭제된 분석, 볼 수 없는 남의 분석(비공개·미완료·주인 탈퇴) (ANALYSIS_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id:\\d+}/concepts")
    public AnalysisConceptsResponse listConcepts(
            @AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        return analysisConceptService.getConcepts(user.id(), id);
    }

    @Operation(
            summary = "분석 제목·메모 수정",
            description =
                    "내 분석의 제목(100자)·메모(1000자)를 보낸 값으로 바꾼다 (누락·빈 문자열은 null). 진행 중이어도 되고 파이프라인에는"
                            + " 영향이 없다.")
    @ApiResponse(responseCode = "200", description = "수정됨 (바뀐 상세)")
    @ApiResponse(
            responseCode = "400",
            description = "제목 100자·메모 1000자 초과 (COMMON_400, details.title·memo)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 분석, 다른 회원의 분석(공개여도), 삭제된 분석 (ANALYSIS_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id:\\d+}")
    public AnalysisDetailResponse update(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable long id,
            @Valid @RequestBody UpdateAnalysisRequest request) {
        return analysisService.update(user.id(), id, request);
    }

    @Operation(
            summary = "분석 공개 설정",
            description =
                    "공개하면 완료 때 기록된 피드 활동이 팔로워에게 보이고, 다른 회원이 읽기 전용으로 열람할 수 있다. 공개는 DONE일 때만"
                            + " 되고, 비공개는 상태와 관계없이 된다.")
    @ApiResponse(responseCode = "200", description = "바뀜")
    @ApiResponse(
            responseCode = "400",
            description = "isPublic 누락·null (COMMON_400)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 분석, 다른 회원의 분석(공개여도), 삭제된 분석 (ANALYSIS_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "DONE이 아닌 분석을 공개로 바꾸려 함 (ANALYSIS_006)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id:\\d+}/visibility")
    public VisibilityResponse updateVisibility(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable long id,
            @Valid @RequestBody UpdateVisibilityRequest request) {
        return analysisService.updateVisibility(user.id(), id, request.isPublic());
    }

    @Operation(
            summary = "분석 삭제",
            description = "끝난(DONE·FAILED) 내 분석을 지운다. 파일·실행 흐름·개념 연결은 지우고 퀴즈 풀이 기록과 일일 한도 집계는 남긴다.")
    @ApiResponse(responseCode = "204", description = "삭제됨")
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 분석, 다른 회원의 분석(공개여도), 이미 삭제된 분석 (ANALYSIS_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "진행 중(FETCHING~QUIZ)인 분석 (ANALYSIS_005)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        analysisService.delete(user.id(), id);
    }
}
