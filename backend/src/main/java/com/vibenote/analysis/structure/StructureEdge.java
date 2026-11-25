package com.vibenote.analysis.structure;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 파일 사이 연결 하나 (F-21a 3절).
 *
 * @param line 연결의 근거가 처음 나온 from 파일의 줄 번호 (1부터)
 */
@Schema(description = "파일 사이 연결 (코드에서 확정한 것만)")
public record StructureEdge(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "부르는 파일 ID",
                        example = "101")
                long from,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "불리는 파일 ID",
                        example = "102")
                long to,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "연결 종류",
                        example = "IMPORT")
                EdgeType type,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "근거가 처음 나온 from 파일의 줄 번호 (1부터)",
                        example = "18")
                int line) {}
