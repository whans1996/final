package com.vibenote.analysis.project;

import com.vibenote.analysis.AnalysisApiDocs;
import com.vibenote.analysis.project.dto.LatestCommitResponse;
import com.vibenote.analysis.project.dto.ProjectDetailResponse;
import com.vibenote.analysis.project.dto.ProjectSummaryResponse;
import com.vibenote.analysis.project.dto.UpdateProjectRequest;
import com.vibenote.analysis.project.dto.UpdateProjectResponse;
import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.common.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프로젝트(저장소 단위로 묶은 분석) 목록·상세·이름 바꾸기·새 커밋 배지 (docs/specs/P-1a-project-versions.md AC6~AC11). 주인만 볼 수
 * 있고, 분석을 모두 삭제한 프로젝트는 없는 것과 같다 (PROJECT_404).
 */
@Tag(name = AnalysisApiDocs.TAG, description = AnalysisApiDocs.TAG_DESCRIPTION)
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private static final String NOT_FOUND = "없는 프로젝트, 남의 프로젝트, 분석을 모두 삭제한 프로젝트 (PROJECT_404)";

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(
            summary = "내 프로젝트 목록",
            description = "삭제하지 않은 분석이 하나 이상 있는 내 프로젝트를 마지막 분석 요청 최신순으로 준다.")
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
    public PageResponse<ProjectSummaryResponse> list(
            @AuthenticationPrincipal AuthUser user, @Valid @ParameterObject PageQuery query) {
        return projectService.listMine(user.id(), query);
    }

    @Operation(
            summary = "프로젝트 상세",
            description =
                    "프로젝트 정보, 버전(번호 내림차순), 삭제하지 않은 분석(요청 최신순). 아직 버전이 없는 분석은 versionId가 null이다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id:\\d+}")
    public ProjectDetailResponse getDetail(
            @AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        return projectService.getDetail(user.id(), id);
    }

    @Operation(summary = "프로젝트 이름 바꾸기", description = "분석의 제목은 바뀌지 않는다.")
    @ApiResponse(responseCode = "200", description = "바뀜")
    @ApiResponse(
            responseCode = "400",
            description = "이름이 비었거나 100자 초과 (COMMON_400, details.name)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id:\\d+}")
    public UpdateProjectResponse rename(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable long id,
            @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.rename(user.id(), id, request.name());
    }

    @Operation(
            summary = "새 커밋 배지",
            description =
                    "저장소 프로젝트의 기본 브랜치 최신 커밋을 분석한 최신 커밋과 비교한다. 조회 결과(실패 포함)는 5분 동안 다시 쓰고 외부 요청을 하지"
                            + " 않는다. 조회 실패·비공개로 바뀜·분석한 커밋 없음은 UNKNOWN, ZIP 프로젝트는 NOT_SUPPORTED이며 모두"
                            + " 200이다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id:\\d+}/latest-commit")
    public LatestCommitResponse latestCommit(
            @AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        return projectService.latestCommit(user.id(), id);
    }
}
