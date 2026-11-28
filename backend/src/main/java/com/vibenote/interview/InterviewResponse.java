package com.vibenote.interview;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** 분석 하나의 면접 예상 질문 (I-1a 3절). DONE이 아니면 questions·generatedAt이 null. */
@Schema(description = "면접 예상 질문")
public record InterviewResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분석 ID", example = "41")
                long analysisId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "생성 상태. NONE은 요청한 적 없음",
                        example = "DONE")
                InterviewStatus status,
        @Schema(
                        description = "실패 사유 코드. FAILED가 아니면 null",
                        example = "LLM_503",
                        nullable = true,
                        allowableValues = {"ANALYSIS_500", "LLM_502", "LLM_503"})
                String errorCode,
        @Schema(
                        description = "사용자에게 보여 줄 실패 사유. FAILED가 아니면 null",
                        example = "AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.",
                        nullable = true)
                String errorMessage,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "남은 다시 만들기 횟수 (DONE 기준, 최소 0)",
                        example = "1")
                int regenerationsLeft,
        @Schema(
                        description = "마지막으로 DONE이 된 시각. DONE이 아니면 null",
                        example = "2026-11-30T14:05:00+09:00",
                        nullable = true)
                OffsetDateTime generatedAt,
        @Schema(description = "질문 (no 오름차순, 5~12개). DONE이 아니면 null", nullable = true)
                List<Question> questions) {

    @Schema(name = "InterviewQuestion", description = "면접 질문 하나")
    public record Question(
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "질문 ID",
                            example = "301")
                    long id,
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "번호 (1부터)",
                            example = "1")
                    int no,
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "범주: 구조·기술 선택 이유·핵심 흐름·한계와 개선",
                            example = "STRUCTURE")
                    InterviewCategory category,
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "질문 (300자 이내)",
                            example = "글 작성 기능을 controller·service·mapper로 나눈 이유는 무엇인가요?")
                    String question,
            @ArraySchema(
                            arraySchema =
                                    @Schema(
                                            requiredMode = Schema.RequiredMode.REQUIRED,
                                            description = "답의 핵심 포인트 (1~5개, 항목 200자 이내)"),
                            schema = @Schema(example = "ArticleController는 요청 검증과 응답 변환만 한다"))
                    List<String> keyPoints,
            @ArraySchema(
                            arraySchema =
                                    @Schema(
                                            requiredMode = Schema.RequiredMode.REQUIRED,
                                            description = "근거 코드 (1~3개)"))
                    List<InterviewEvidence> evidence,
            @ArraySchema(
                            arraySchema =
                                    @Schema(
                                            requiredMode = Schema.RequiredMode.REQUIRED,
                                            description = "관련 개념 카드 (0~3개, 지금 숨김이 아닌 카드만)"))
                    List<ConceptRef> concepts) {}

    @Schema(name = "InterviewConceptRef", description = "관련 개념 카드")
    public record ConceptRef(
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "카드 ID",
                            example = "12")
                    long id,
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "카드 이름",
                            example = "계층형 아키텍처")
                    String name) {}
}
