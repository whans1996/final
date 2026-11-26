package com.vibenote.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 한 글의 댓글 목록 (F-18 AC1·AC2). 페이지 없이 가장 최근 200개를 오래된순으로, total은 전체 댓글 수 (#208). */
@Schema(description = "댓글 목록")
public record CommentListResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "가장 최근 200개를 오래된순(작성 시각, id 오름차순)으로")
                List<CommentResponse> items,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "그 글의 전체 댓글 수 (items 수보다 크면 오래된 댓글은 빠진 것)",
                        example = "2")
                long total) {}
