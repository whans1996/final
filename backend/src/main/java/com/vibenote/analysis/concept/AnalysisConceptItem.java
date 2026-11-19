package com.vibenote.analysis.concept;

import com.vibenote.concept.dto.ConceptSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** 분석에 연결된 공개 카드 하나와 코드에서 쓰인 위치 (F-08 AC7). */
@Schema(description = "분석 코드에 쓰인 개념 카드와 위치")
public record AnalysisConceptItem(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "카드 요약")
                ConceptSummaryResponse concept,
        @Schema(description = "개념이 쓰인 분석 파일 ID (없으면 null)", example = "102") Long fileId,
        @Schema(
                        description = "그 파일 경로 (없으면 null)",
                        example = "src/main/java/com/example/todo/TodoService.java")
                String filePath,
        @Schema(description = "줄 위치 L12 또는 L12-L30 (없으면 null)", example = "L8-L15") String lineHint,
        @Schema(
                        description = "이 코드에서 개념이 쓰인 방식 (없으면 null)",
                        example = "생성자로 TodoMapper를 받아 필드에 저장합니다.")
                String contextNote) {}
