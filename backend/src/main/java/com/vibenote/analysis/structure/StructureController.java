package com.vibenote.analysis.structure;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 분석 구조 그래프 (F-21a). 경로와 Tag는 AnalysisController와 같다. */
@Tag(name = "분석", description = "프로젝트 분석 요청, 내 분석 목록, 진행 상태, 분석 결과(개요·파일) 조회")
@RestController
@RequestMapping("/api/analyses/{id:\\d+}/structure")
public class StructureController {

    private final StructureService structureService;

    public StructureController(StructureService structureService) {
        this.structureService = structureService;
    }

    @Operation(
            summary = "구조 그래프",
            description =
                    "선별 파일 사이의 의존 관계(Java 타입 참조·MyBatis XML·JS import·FE→API 호출)를 코드에서 확정한 것만 간선으로"
                        + " 준다. 확정하지 못한 연결은 사유와 함께 unresolved에 담는다. 요청 때 계산하며 DB에 저장하지 않는다(DONE 분석은"
                        + " 서버 메모리에 캐시). 파일이 없으면 세 배열이 비고, 개요가 없으면 FE→API 판정을 건너뛴다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 분석, 삭제된 분석, 볼 수 없는 남의 분석(비공개·미완료·주인 탈퇴) (ANALYSIS_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "503",
            description = "동시 계산 상한이 차서 기다려도 자리가 나지 않음 (ANALYSIS_007)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public AnalysisStructureResponse getStructure(
            @AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        return structureService.get(user.id(), id);
    }
}
