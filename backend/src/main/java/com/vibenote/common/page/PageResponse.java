package com.vibenote.common.page;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 페이지 응답 {@code {items, page, size, total}} (docs/conventions.md 2절). */
@Schema(description = "페이지 응답")
public record PageResponse<T>(
        @Schema(description = "현재 페이지 항목") List<T> items,
        @Schema(description = "페이지 번호 (1부터)", example = "1") int page,
        @Schema(description = "페이지 크기", example = "20") int size,
        @Schema(description = "전체 항목 수", example = "134") long total) {

    public static <T> PageResponse<T> of(List<T> items, PageQuery query, long total) {
        return new PageResponse<>(items, query.page(), query.size(), total);
    }
}
