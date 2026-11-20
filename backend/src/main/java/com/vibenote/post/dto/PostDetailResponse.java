package com.vibenote.post.dto;

import com.vibenote.common.config.ClockConfig;
import com.vibenote.post.BoardType;
import com.vibenote.post.domain.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 게시글 상세 (F-17 AC3~AC5·AC9). 첨부는 분석·카드 중 하나만 있고, 없으면 둘 다 null. */
@Schema(description = "게시글 상세")
public record PostDetailResponse(
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
                        description = "본문 (글자 그대로. 화면은 HTML로 해석하지 않는다)",
                        example = "같은 클래스 안에서 메서드를 호출하면 트랜잭션이 안 걸린다는데\n왜 그런지 모르겠어요.")
                String content,
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
                        description = "조회수 (상세 조회는 이번 조회를 더한 값)",
                        example = "43")
                int viewCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "댓글 수", example = "3")
                long commentCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "분석 첨부")
                PostAnalysisAttachment analysis,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "개념 카드 첨부")
                PostConceptAttachment concept,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "작성 시각",
                        example = "2026-11-20T14:02:11+09:00")
                OffsetDateTime createdAt,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "마지막 수정 시각 (조회수는 바꾸지 않는다)",
                        example = "2026-11-20T14:02:11+09:00")
                OffsetDateTime updatedAt) {

    /** viewerId는 분석 첨부의 "주인" 판단에만 쓴다 (로그인하지 않았으면 null). */
    public static PostDetailResponse from(Post row, Long viewerId) {
        boolean withdrawn = row.isAuthorWithdrawn();
        return new PostDetailResponse(
                row.getId(),
                row.getBoardType(),
                row.getTitle(),
                row.getContent(),
                withdrawn ? null : row.getUserId(),
                withdrawn ? PostSummaryResponse.WITHDRAWN_NICKNAME : row.getAuthorNickname(),
                row.getViewCount(),
                row.getCommentCount(),
                PostAnalysisAttachment.from(row, viewerId),
                PostConceptAttachment.from(row),
                row.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime(),
                row.getUpdatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
