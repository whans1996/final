package com.vibenote.analysis.structure;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 분석 구조 그래프 응답 (F-21a 3절). 요청 때 계산하고 저장하지 않는다. */
@Schema(description = "파일 의존 그래프")
public record AnalysisStructureResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "모든 선별 파일 (fileId 오름차순)")
                List<StructureNode> nodes,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "코드에서 확정한 연결 (from → to → type 순, 상한까지)")
                List<StructureEdge> edges,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "확인하지 못한 연결 (fileId → line 순, 상한까지)")
                List<UnresolvedLink> unresolved,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "연결이 상한을 넘어 잘렸는가",
                        example = "false")
                boolean edgesTruncated,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "확인하지 못한 연결이 상한을 넘어 잘렸는가",
                        example = "false")
                boolean unresolvedTruncated) {}
