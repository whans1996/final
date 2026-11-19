package com.vibenote.analysis.flow;

import com.vibenote.common.config.ClockConfig;
import com.vibenote.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** 실행 흐름 하나 (F-07 3절). DONE이 아니면 summary·steps가 null. */
@Schema(description = "실행 흐름")
public record FlowResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "흐름 ID", example = "7")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "HTTP 메서드",
                        example = "POST")
                String method,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "경로",
                        example = "/api/todos")
                String path,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "생성 상태",
                        example = "DONE")
                FlowStatus status,
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
                        description = "전체 흐름 요약 (500자 이내). DONE이 아니면 null",
                        example = "새 할 일을 등록하면 컨트롤러가 입력을 검사하고, 서비스가 저장을 맡깁니다.",
                        nullable = true)
                String summary,
        @Schema(description = "단계 (최대 12개). DONE이 아니면 null", nullable = true) List<FlowStep> steps,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "요청 시각",
                        example = "2026-12-09T10:00:00+09:00")
                OffsetDateTime createdAt) {

    /** 흐름은 실패 메시지를 저장하지 않아 재시작·큐 거부·예기치 못한 오류를 구분할 수 없으므로 공통 문구를 쓴다 (AC11, 11-19 결정). */
    static final String INTERRUPTED_MESSAGE = "실행 흐름 생성이 중단되었습니다. 다시 요청해 주세요.";

    static FlowResponse from(AnalysisFlow flow) {
        String endpoint = flow.getEndpoint();
        int space = endpoint.indexOf(' ');
        boolean done = flow.getStatus() == FlowStatus.DONE;
        return new FlowResponse(
                flow.getId(),
                endpoint.substring(0, space),
                endpoint.substring(space + 1),
                flow.getStatus(),
                flow.getErrorCode(),
                errorMessage(flow.getErrorCode()),
                done ? flow.getSummary() : null,
                done && flow.getSteps() != null ? List.of(flow.getSteps()) : null,
                flow.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }

    private static String errorMessage(String errorCode) {
        if (errorCode == null) {
            return null;
        }
        if (errorCode.equals(ErrorCode.ANALYSIS_500.name())) {
            return INTERRUPTED_MESSAGE;
        }
        try {
            return ErrorCode.valueOf(errorCode).message();
        } catch (IllegalArgumentException e) {
            return INTERRUPTED_MESSAGE;
        }
    }
}
