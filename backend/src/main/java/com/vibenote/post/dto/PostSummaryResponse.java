package com.vibenote.post.dto;

import com.vibenote.common.config.ClockConfig;
import com.vibenote.post.AttachmentType;
import com.vibenote.post.BoardType;
import com.vibenote.post.domain.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 목록의 글 한 건 (F-17 AC1·AC3). 본문은 담지 않는다. 탈퇴한 작성자는 id를 숨기고 닉네임을 "탈퇴한 사용자"로 바꾼다. */
@Schema(description = "게시글 요약 (목록 항목)")
public record PostSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "글 ID", example = "42")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "게시판 (QNA: 코드 이해 Q&A, REVIEW: 학습 후기, FREE: 자유)",
                        example = "QNA")
                BoardType boardType,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "제목",
                        example = "@Transactional이 안 먹는 이유가 뭔가요?")
                String title,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "첨부 종류 (볼 수 있는지와 상관없이 알린다). 첨부가 없으면 null",
                        example = "ANALYSIS")
                AttachmentType attachmentType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "댓글 수", example = "3")
                long commentCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "조회수", example = "42")
                int viewCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "작성자 ID (탈퇴한 회원이면 null)",
                        example = "7")
                Long authorId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "작성자 현재 닉네임 (탈퇴한 회원이면 \"탈퇴한 사용자\")",
                        example = "코딩초보")
                String authorNickname,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "작성 시각",
                        example = "2026-11-20T14:02:11+09:00")
                OffsetDateTime createdAt) {

    public static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

    public static PostSummaryResponse from(Post row) {
        boolean withdrawn = row.isAuthorWithdrawn();
        return new PostSummaryResponse(
                row.getId(),
                row.getBoardType(),
                row.getTitle(),
                row.getAttachmentType(),
                row.getCommentCount(),
                row.getViewCount(),
                withdrawn ? null : row.getUserId(),
                withdrawn ? WITHDRAWN_NICKNAME : row.getAuthorNickname(),
                row.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
