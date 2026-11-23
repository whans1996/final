package com.vibenote.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 팀 만들기·수정 요청 (F-20a AC1·AC2·AC7). 앞뒤 공백을 뺀 값으로 검증하고 저장한다. 소개가 없거나 공백뿐이면 null로 저장한다. 수정은 보낸 값으로 통째로
 * 바꾸고, 팀장 확인 뒤에 TeamService가 이 제약을 직접 검증한다 (판단 순서 403 → 400).
 */
@Schema(description = "팀 만들기·수정 요청")
public record TeamRequest(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "팀 이름 (앞뒤 공백 제외 1~30자, 중복 허용)",
                        maxLength = 30,
                        example = "스프링 스터디")
                @NotBlank(message = "팀 이름을 입력해 주세요.")
                @Size(max = 30, message = "팀 이름은 30자 이하로 입력해 주세요.")
                String name,
        @Schema(
                        nullable = true,
                        description = "소개 (앞뒤 공백 제외 200자 이하, 없거나 공백뿐이면 null)",
                        maxLength = 200,
                        example = "F-05 분석 결과를 같이 공부합니다")
                @Size(max = 200, message = "소개는 200자 이하로 입력해 주세요.")
                String description) {

    public TeamRequest {
        if (name != null) name = name.strip();
        if (description != null) {
            description = description.strip();
            if (description.isEmpty()) description = null;
        }
    }
}
