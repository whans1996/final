package com.vibenote.challenge.dto;

import com.vibenote.challenge.ChallengeStatus;
import com.vibenote.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * 회원용 챌린지 목록 조건 {@code ?status=&page=&size=} (F-25 AC1). 상태가 없으면 진행 중이다. 챌린지 카드 격자에 맞춰 {@code
 * size}는 기본 12, 최대 50이다 (공통 {@link PageQuery}와 다르다).
 */
@Schema(description = "챌린지 목록 조건")
public record ChallengeSearchRequest(
        @Schema(
                        description = "상태 (없으면 ONGOING). 오늘(Asia/Seoul)과 기간으로 계산한다",
                        allowableValues = {"ONGOING", "UPCOMING", "ENDED"},
                        defaultValue = "ONGOING",
                        example = "ONGOING")
                @Pattern(
                        regexp = "UPCOMING|ONGOING|ENDED",
                        message = "상태는 UPCOMING, ONGOING, ENDED 중 하나여야 합니다.")
                String status,
        @Schema(description = "페이지 번호 (1부터)", example = "1", defaultValue = "1")
                @Min(value = 1, message = "page는 1 이상이어야 합니다.")
                Integer page,
        @Schema(description = "페이지 크기 (최대 50)", example = "12", defaultValue = "12")
                @Min(value = 1, message = "size는 1 이상이어야 합니다.")
                @Max(value = 50, message = "size는 50 이하여야 합니다.")
                Integer size) {

    public static final int DEFAULT_SIZE = 12;

    public ChallengeSearchRequest {
        if (status != null) {
            status = status.strip();
            if (status.isEmpty()) status = null;
        }
        if (page == null) page = 1;
        if (size == null) size = DEFAULT_SIZE;
    }

    public ChallengeStatus statusValue() {
        return status == null ? ChallengeStatus.ONGOING : ChallengeStatus.valueOf(status);
    }

    public PageQuery pageQuery() {
        return new PageQuery(page, size);
    }
}
