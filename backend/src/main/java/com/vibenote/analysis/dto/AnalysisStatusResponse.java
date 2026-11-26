package com.vibenote.analysis.dto;

import com.vibenote.analysis.AnalysisStatus;
import com.vibenote.analysis.domain.Analysis;
import com.vibenote.common.config.ClockConfig;
import com.vibenote.quiz.QuizStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 분석 진행 상태 (F-05a AC13). 화면이 2초마다 폴링한다. */
@Schema(description = "분석 진행 상태")
public record AnalysisStatusResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분석 ID", example = "42")
                long analysisId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "진행 상태",
                        example = "FILTERING")
                AnalysisStatus status,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "진행률 (0~100). FAILED면 실패 직전 값",
                        example = "15")
                int progress,
        @Schema(
                        description =
                                "실패 사유 코드. FAILED가 아니면 null. ANALYSIS_413은 저장소 아카이브 크기 초과(URL"
                                        + " 분석)에서만 나오고, 코드 파일 수·합계로는 실패하지 않는다",
                        example = "ANALYSIS_413",
                        nullable = true,
                        allowableValues = {
                            "ANALYSIS_004",
                            "ANALYSIS_400",
                            "ANALYSIS_413",
                            "ANALYSIS_415",
                            "ANALYSIS_500",
                            "ANALYSIS_502",
                            "ANALYSIS_503",
                            "LLM_502",
                            "LLM_503"
                        })
                String errorCode,
        @Schema(
                        description = "사용자에게 보여 줄 실패 사유. FAILED가 아니면 null",
                        example = "저장소 아카이브가 30MB를 넘습니다. ZIP으로 필요한 코드만 올려 주세요.",
                        nullable = true)
                String errorMessage,
        @Schema(
                        description =
                                "저장소 주소(정규화). ZIP 분석이면 null. FAILED 화면의 \"저장소 보기\" 링크 (F-19 AC12)",
                        example = "https://github.com/octo/todo-app",
                        nullable = true)
                String sourceUrl,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "마지막 상태 변경 시각",
                        example = "2026-11-24T14:20:01+09:00")
                OffsetDateTime updatedAt,
        @Schema(
                        description =
                                "퀴즈 상태. GENERATING 재생성 중, READY 퀴즈 있음, FAILED 생성 실패(다시 만들 수 있음)."
                                        + " QUIZ 단계를 끝내기 전이면 null (L-1a)",
                        example = "READY",
                        nullable = true)
                QuizStatus quizStatus) {

    public static AnalysisStatusResponse from(Analysis analysis) {
        return new AnalysisStatusResponse(
                analysis.getId(),
                analysis.getStatus(),
                analysis.getProgress(),
                analysis.getErrorCode(),
                analysis.getErrorMessage(),
                analysis.getSourceUrl(),
                analysis.getUpdatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime(),
                analysis.getQuizStatus());
    }
}
