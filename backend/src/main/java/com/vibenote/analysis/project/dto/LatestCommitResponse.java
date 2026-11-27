package com.vibenote.analysis.project.dto;

import com.vibenote.analysis.fetch.RepositoryCommit;
import com.vibenote.analysis.project.LatestCommitStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/** 새 커밋 배지 (P-1a AC9·AC10, 3절 표). */
@Schema(description = "새 커밋 배지")
public record LatestCommitResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description =
                                "NEW_COMMIT: 최신 커밋 ≠ 분석한 최신 커밋 · UP_TO_DATE: 같음 · UNKNOWN: 조회"
                                        + " 실패·비공개로 바뀜·분석한 커밋 없음 · NOT_SUPPORTED: ZIP 프로젝트",
                        example = "NEW_COMMIT")
                LatestCommitStatus status,
        // $ref에 nullable=true를 붙이면 springdoc이 type: null만 남겨 객체를 허용하지 않는다 (AnalysisDetailResponse
        // overview와 같은 이유)
        @Schema(description = "기본 브랜치 최신 커밋. 조회하지 못했거나 ZIP이면 null") Commit latest,
        @Schema(
                        description = "분석한 최신 커밋 SHA. 없거나 ZIP이면 null",
                        example = "9f2c4e1a7b3d5f60812c9e4a1b2c3d4e5f607182",
                        nullable = true)
                String analyzedSha,
        @Schema(
                        description = "외부 조회 시각 (5분 동안 다시 쓴다). ZIP이면 null",
                        example = "2026-11-29T10:00:00+09:00",
                        nullable = true)
                OffsetDateTime checkedAt) {

    /** 기본 브랜치 최신 커밋. */
    @Schema(name = "LatestCommit", description = "기본 브랜치 최신 커밋")
    public record Commit(
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "커밋 SHA (40자)",
                            example = "a1b2c3d4e5f60718293a4b5c6d7e8f9012345678")
                    String sha,
            @Schema(
                            description = "커밋 메시지 첫 줄 (200자까지)",
                            example = "feat: 비밀번호 재설정",
                            nullable = true)
                    String message,
            @Schema(description = "커밋 시각", example = "2026-11-29T09:12:40+09:00", nullable = true)
                    OffsetDateTime committedAt) {

        static Commit from(RepositoryCommit commit) {
            return commit == null
                    ? null
                    : new Commit(
                            commit.sha(),
                            commit.message(),
                            ProjectVersionResponse.offset(commit.committedAt()));
        }
    }

    public static LatestCommitResponse notSupported() {
        return new LatestCommitResponse(LatestCommitStatus.NOT_SUPPORTED, null, null, null);
    }

    /** 조회 결과와 분석한 커밋으로 상태를 정한다. 둘 중 하나라도 없으면 UNKNOWN이다. */
    public static LatestCommitResponse of(
            RepositoryCommit latest, String analyzedSha, LocalDateTime checkedAt) {
        LatestCommitStatus status;
        if (latest == null || analyzedSha == null) {
            status = LatestCommitStatus.UNKNOWN;
        } else if (latest.sha().equals(analyzedSha)) {
            status = LatestCommitStatus.UP_TO_DATE;
        } else {
            status = LatestCommitStatus.NEW_COMMIT;
        }
        return new LatestCommitResponse(
                status, Commit.from(latest), analyzedSha, ProjectVersionResponse.offset(checkedAt));
    }
}
