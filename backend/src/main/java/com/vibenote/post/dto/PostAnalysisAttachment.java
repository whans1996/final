package com.vibenote.post.dto;

import com.vibenote.post.domain.Post;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 상세의 분석 첨부 (F-17 AC5). 삭제되지 않았고, 요청한 사람이 주인이거나 공개이면서 주인이 탈퇴하지 않았으면 viewable이고, 아니면 제목·프로젝트명을 숨긴다.
 * 분석 상세의 열람 기준(F-19 AC10 {@code AnalysisMapper} viewable)과 맞춘다 (#92). 첨부는 DONE 분석만 되고 미완료 분석은 공개할 수
 * 없으므로 상태는 따로 보지 않는다.
 */
@Schema(description = "분석 첨부")
public record PostAnalysisAttachment(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분석 ID", example = "15")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "요청한 사람이 볼 수 있는지 (삭제되지 않았고, 내 분석이거나 탈퇴하지 않은 회원의 공개 분석)",
                        example = "true")
                boolean viewable,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "분석 제목 (볼 수 없거나 제목이 없으면 null)",
                        example = "쇼핑몰 과제")
                String title,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "프로젝트명 (볼 수 없으면 null)",
                        example = "shop-backend")
                String projectName) {

    /** 첨부한 분석이 없으면 null. viewerId는 로그인하지 않았으면 null. */
    public static PostAnalysisAttachment from(Post row, Long viewerId) {
        if (row.getAnalysisId() == null) return null;
        boolean viewable =
                !row.isAnalysisDeleted()
                        && ((row.isAnalysisPublic() && row.isAnalysisOwnerActive())
                                || (viewerId != null && viewerId.equals(row.getAnalysisOwnerId())));
        return viewable
                ? new PostAnalysisAttachment(
                        row.getAnalysisId(),
                        true,
                        row.getAnalysisTitle(),
                        row.getAnalysisProjectName())
                : new PostAnalysisAttachment(row.getAnalysisId(), false, null, null);
    }
}
