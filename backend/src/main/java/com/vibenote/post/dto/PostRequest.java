package com.vibenote.post.dto;

import com.vibenote.post.BoardType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 글 작성·수정 요청 (F-17 AC6·AC7·AC9). 제목·본문은 앞뒤 공백을 뺀 값으로 검증하고 저장한다. 수정은 보낸 값으로 통째로 바꾼다 (첨부를 없애려면 둘 다
 * null). boardType은 허용되지 않은 값도 {@code details.boardType}으로 알리려고 문자열로 받는다.
 *
 * <p>분석·카드를 둘 다 보내는 경우는 {@code PostService}가 {@code details.conceptId}로 막는다. 수정({@code PUT})은 작성자
 * 확인 뒤에 Service가 이 제약을 직접 검증한다 (spec 9절).
 */
@Schema(description = "게시글 작성·수정 요청")
public record PostRequest(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "게시판 (QNA: 코드 이해 Q&A, REVIEW: 학습 후기, FREE: 자유)",
                        allowableValues = {"QNA", "REVIEW", "FREE"},
                        example = "QNA")
                @NotNull(message = "게시판을 선택해 주세요.")
                @Pattern(regexp = "QNA|REVIEW|FREE", message = "게시판은 QNA, REVIEW, FREE 중 하나여야 합니다.")
                String boardType,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "제목 (앞뒤 공백 제외 1~100자)",
                        maxLength = 100,
                        example = "@Transactional이 안 먹는 이유가 뭔가요?")
                @NotBlank(message = "제목을 입력해 주세요.")
                @Size(max = 100, message = "제목은 100자 이하로 입력해 주세요.")
                String title,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "본문 (앞뒤 공백 제외 1~5000자, 글자 그대로 저장)",
                        maxLength = 5000,
                        example = "같은 클래스 안에서 메서드를 호출하면 트랜잭션이 안 걸린다는데\n왜 그런지 모르겠어요.")
                @NotBlank(message = "본문을 입력해 주세요.")
                @Size(max = 5000, message = "본문은 5000자 이하로 입력해 주세요.")
                String content,
        @Schema(
                        nullable = true,
                        description =
                                "첨부할 내 분석 ID (공개·DONE이고 삭제되지 않은 내 분석, 비공개는 POST_001). conceptId와 함께"
                                        + " 보낼 수 없다",
                        example = "15")
                Long analysisId,
        @Schema(
                        nullable = true,
                        description = "첨부할 공개 개념 카드 ID. analysisId와 함께 보낼 수 없다",
                        example = "12")
                Long conceptId) {

    public PostRequest {
        if (title != null) title = title.strip();
        if (content != null) content = content.strip();
    }

    /** 검증을 통과한 뒤에만 부른다. */
    public BoardType boardTypeValue() {
        return BoardType.valueOf(boardType);
    }
}
