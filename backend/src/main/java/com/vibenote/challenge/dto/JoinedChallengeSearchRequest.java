package com.vibenote.challenge.dto;

import com.vibenote.challenge.ChallengeStatus;
import com.vibenote.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * 참여한 챌린지 목록 조건 {@code ?status=&page=&size=} (F-25 AC10·AC11). 상태가 없으면 전체다. {@code size}는 기본 12, 최대
 * 50이다. 회원 확인(404)을 먼저 하려고 Controller가 아니라 Service가 검증한다.
 */
@Schema(description = "참여한 챌린지 목록 조건")
public record JoinedChallengeSearchRequest(
        @Schema(
                        description = "상태 (없으면 전체). 오늘(Asia/Seoul)과 기간으로 계산한다",
                        allowableValues = {"ONGOING", "UPCOMING", "ENDED"},
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

    public JoinedChallengeSearchRequest {
        if (status != null) {
            status = status.strip();
            if (status.isEmpty()) status = null;
        }
        if (page == null) page = 1;
        if (size == null) size = ChallengeSearchRequest.DEFAULT_SIZE;
    }

    public ChallengeStatus statusValue() {
        return status == null ? null : ChallengeStatus.valueOf(status);
    }

    public PageQuery pageQuery() {
        return new PageQuery(page, size);
    }
}
