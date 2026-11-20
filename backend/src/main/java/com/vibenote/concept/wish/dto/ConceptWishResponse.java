package com.vibenote.concept.wish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** 찜·해제 결과 (F-12). 새로 찜했든 이미 찜했든, 해제했든 찜하지 않았든 같은 모양이다 (멱등). */
@Schema(description = "찜 여부와 찜 수")
public record ConceptWishResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "요청 후 내가 찜했는지",
                        example = "true")
                boolean wished,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "요청 처리 후 카드의 찜 수",
                        example = "13")
                int wishCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        type = "string",
                        format = "date",
                        description =
                                "이번 찜으로 복습 계획이 새로 생겼으면 그 날짜(내일, Asia/Seoul). 이미 찜했거나 그 카드의 TODO·내일"
                                        + " 계획이 있었거나 해제면 null",
                        example = "2026-12-04")
                LocalDate reviewPlanDate) {}
