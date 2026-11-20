package com.vibenote.post.dto;

import com.vibenote.common.config.ClockConfig;
import com.vibenote.post.domain.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 댓글 한 개 (F-18 AC1·AC3·AC5·AC7). */
@Schema(description = "댓글")
public record CommentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "댓글 ID", example = "101")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "내용 (글자 그대로. 화면은 HTML로 해석하지 않는다)",
                        example = "같은 클래스 안에서 호출하면 프록시를 거치지 않아서 트랜잭션이 안 걸려요.\n별도 빈으로 빼 보세요.")
                String content,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "작성자 ID (탈퇴한 회원이면 null)",
                        example = "9")
                Long authorId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "작성자 현재 닉네임 (탈퇴한 회원이면 \"탈퇴한 사용자\")",
                        example = "스프링러버")
                String authorNickname,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "수정한 적이 있는지 (updatedAt이 createdAt보다 뒤)",
                        example = "false")
                boolean edited,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "작성 시각",
                        example = "2026-11-20T14:10:05+09:00")
                OffsetDateTime createdAt,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "마지막 수정 시각 (수정한 적 없으면 작성 시각)",
                        example = "2026-11-20T14:10:05+09:00")
                OffsetDateTime updatedAt) {

    public static CommentResponse from(Comment row) {
        boolean withdrawn = row.isAuthorWithdrawn();
        return new CommentResponse(
                row.getId(),
                row.getContent(),
                withdrawn ? null : row.getUserId(),
                withdrawn ? PostSummaryResponse.WITHDRAWN_NICKNAME : row.getAuthorNickname(),
                row.isEdited(),
                row.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime(),
                row.getUpdatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
