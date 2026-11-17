package com.vibenote.common.page;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 페이지 요청 {@code ?page=1&size=20}. Controller에서 {@code @Valid PageQuery query}로 받고, Mapper XML에서
 * {@code LIMIT #{size} OFFSET #{offset}}으로 쓴다.
 */
@Schema(description = "페이지 요청")
public record PageQuery(
        @Schema(description = "페이지 번호 (1부터)", example = "1", defaultValue = "1")
                @Min(value = 1, message = "page는 1 이상이어야 합니다.")
                Integer page,
        @Schema(description = "페이지 크기 (최대 100)", example = "20", defaultValue = "20")
                @Min(value = 1, message = "size는 1 이상이어야 합니다.")
                @Max(value = 100, message = "size는 100 이하여야 합니다.")
                Integer size) {

    public static final int DEFAULT_SIZE = 20;

    public PageQuery {
        if (page == null) page = 1;
        if (size == null) size = DEFAULT_SIZE;
    }

    public long offset() {
        return (long) (page - 1) * size;
    }
}
