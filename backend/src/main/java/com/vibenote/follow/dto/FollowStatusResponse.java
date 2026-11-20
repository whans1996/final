package com.vibenote.follow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 팔로우 상태와 대상 회원의 수 (F-04). 팔로우·언팔로우·상태 조회가 같은 모양이다 (멱등). */
@Schema(description = "팔로우 여부와 대상 회원의 팔로워·팔로잉 수")
public record FollowStatusResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "로그인한 내가 대상 회원을 팔로우 중인지. 비로그인·내 프로필이면 false",
                        example = "true")
                boolean following,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "대상 회원을 팔로우하는 사람 수",
                        example = "12")
                long followerCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "대상 회원이 팔로우하는 사람 수",
                        example = "3")
                long followingCount) {}
