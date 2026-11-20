package com.vibenote.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 풀이 화면의 문항 하나 (F-14 AC5). 정답·해설·개념은 담지 않는다. */
@Schema(description = "퀴즈 문항 (정답·해설 없음)")
public record QuizQuestionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "문항 ID", example = "21")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "문항 번호 (1부터)",
                        example = "1")
                int no,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "질문 (줄바꿈 포함 가능)",
                        example = "ArticleApi의 updateArticle()이 수정 전에 하는 일은 무엇인가요?")
                String question,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "선택지 4개",
                        example =
                                "[\"작성자인지 확인한다\", \"slug를 바꾼다\", \"토큰을 새로 발급한다\","
                                        + " \"글을 지우고 다시 만든다\"]")
                List<String> choices) {}
