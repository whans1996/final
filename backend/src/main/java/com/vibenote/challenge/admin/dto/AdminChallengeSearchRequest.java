package com.vibenote.challenge.admin.dto;

import com.vibenote.challenge.ChallengeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

/**
 * 관리자 챌린지 목록 조건 {@code ?status=} (F-24 AC4). 페이지는 {@code PageQuery}로 따로 받는다. 허용되지 않은 값을 {@code
 * details.status}로 알리려고 문자열로 받고, 비면 전체(null)로 둔다.
 */
@Schema(description = "관리자 챌린지 목록 조건")
public record AdminChallengeSearchRequest(
        @Schema(
                        description = "상태 (없으면 전체). 오늘(Asia/Seoul)과 기간으로 계산한다",
                        allowableValues = {"UPCOMING", "ONGOING", "ENDED"},
                        example = "ONGOING")
                @Pattern(
                        regexp = "UPCOMING|ONGOING|ENDED",
                        message = "상태는 UPCOMING, ONGOING, ENDED 중 하나여야 합니다.")
                String status) {

    public AdminChallengeSearchRequest {
        if (status != null) {
            status = status.strip();
            if (status.isEmpty()) status = null;
        }
    }

    public ChallengeStatus statusValue() {
        return status == null ? null : ChallengeStatus.valueOf(status);
    }
}
