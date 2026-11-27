package com.vibenote.analysis.project.dto;

import com.vibenote.analysis.project.ProjectVersion;
import com.vibenote.common.config.ClockConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/** 프로젝트 버전 (P-1a AC6·AC7). 커밋 정보가 없는 버전(ZIP·커밋 조회 실패·V15 이전 분석)은 커밋 필드가 null이다. */
@Schema(description = "프로젝트 버전")
public record ProjectVersionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "버전 ID", example = "31")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "버전 번호 (프로젝트 안에서 1부터)",
                        example = "2")
                int number,
        @Schema(
                        description = "커밋 SHA (40자)",
                        example = "9f2c4e1a7b3d5f60812c9e4a1b2c3d4e5f607182",
                        nullable = true)
                String commitSha,
        @Schema(description = "커밋 메시지 첫 줄 (200자까지)", example = "feat: JWT 로그인", nullable = true)
                String commitMessage,
        @Schema(description = "커밋 시각", example = "2026-11-28T14:02:11+09:00", nullable = true)
                OffsetDateTime committedAt,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "버전이 생긴 시각",
                        example = "2026-11-28T17:30:05+09:00")
                OffsetDateTime createdAt) {

    public static ProjectVersionResponse from(ProjectVersion version) {
        return new ProjectVersionResponse(
                version.getId(),
                version.getNumber(),
                version.getCommitSha(),
                version.getCommitMessage(),
                offset(version.getCommittedAt()),
                offset(version.getCreatedAt()));
    }

    static OffsetDateTime offset(LocalDateTime time) {
        return time == null ? null : time.atZone(ClockConfig.ZONE).toOffsetDateTime();
    }
}
