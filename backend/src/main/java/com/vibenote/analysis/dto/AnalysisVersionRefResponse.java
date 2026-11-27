package com.vibenote.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 분석이 붙은 버전 (P-1a AC12). 커밋 정보가 없는 버전(ZIP·커밋 조회 실패·V15 이전 분석)은 커밋 필드가 null이다. */
@Schema(description = "분석이 붙은 버전")
public record AnalysisVersionRefResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "버전 ID", example = "31")
                long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "버전 번호", example = "2")
                int number,
        @Schema(
                        description = "커밋 SHA (40자)",
                        example = "9f2c4e1a7b3d5f60812c9e4a1b2c3d4e5f607182",
                        nullable = true)
                String commitSha,
        @Schema(description = "커밋 메시지 첫 줄", example = "feat: JWT 로그인", nullable = true)
                String commitMessage,
        @Schema(description = "커밋 시각", example = "2026-11-28T14:02:11+09:00", nullable = true)
                OffsetDateTime committedAt) {}
