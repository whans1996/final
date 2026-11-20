package com.vibenote.post.dto;

import com.vibenote.common.Level;
import com.vibenote.post.domain.Post;
import io.swagger.v3.oas.annotations.media.Schema;

/** 상세의 개념 카드 첨부 (F-17 AC5). 카드가 PUBLISHED가 아니면 viewable이 false이고 나머지를 숨긴다. */
@Schema(description = "개념 카드 첨부")
public record PostConceptAttachment(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "카드 ID", example = "12")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "볼 수 있는지 (공개 카드인지)",
                        example = "true")
                boolean viewable,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "카드 이름 (볼 수 없으면 null)",
                        example = "JWT")
                String name,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "카테고리 (볼 수 없으면 null)",
                        example = "security")
                String category,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "난이도 (볼 수 없으면 null)",
                        example = "JUNIOR")
                Level difficulty,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "한 줄 요약 (볼 수 없으면 null)",
                        example = "서버가 서명한 토큰으로 로그인 상태를 전달하는 방식")
                String summary) {

    /** 첨부한 카드가 없으면 null. */
    public static PostConceptAttachment from(Post row) {
        if (row.getConceptId() == null) return null;
        if (!"PUBLISHED".equals(row.getConceptStatus())) {
            return new PostConceptAttachment(row.getConceptId(), false, null, null, null, null);
        }
        return new PostConceptAttachment(
                row.getConceptId(),
                true,
                row.getConceptName(),
                row.getConceptCategory(),
                Level.valueOf(row.getConceptDifficulty()),
                row.getConceptSummary());
    }
}
