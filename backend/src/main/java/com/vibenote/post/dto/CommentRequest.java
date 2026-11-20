package com.vibenote.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 작성·수정 요청 (F-18 AC5~AC7). 앞뒤 공백을 뺀 값으로 검증하고 저장한다. 수정({@code PUT})은 작성자 확인 뒤에 Service가 이 제약을 직접
 * 검증한다 (spec 3절 판단 순서).
 */
@Schema(description = "댓글 작성·수정 요청")
public record CommentRequest(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "내용 (앞뒤 공백 제외 1~500자, 글자 그대로 저장)",
                        maxLength = 500,
                        example = "같은 클래스 안에서 호출하면 프록시를 거치지 않아서 트랜잭션이 안 걸려요.")
                @NotBlank(message = "댓글 내용을 입력해 주세요.")
                @Size(max = 500, message = "댓글은 500자 이하로 입력해 주세요.")
                String content) {

    public CommentRequest {
        if (content != null) content = content.strip();
    }
}
