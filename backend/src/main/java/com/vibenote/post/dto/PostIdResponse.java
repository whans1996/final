package com.vibenote.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 글 작성 응답 (F-17 AC6). */
@Schema(description = "만든 글 ID")
public record PostIdResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "글 ID", example = "42")
                long id) {}
