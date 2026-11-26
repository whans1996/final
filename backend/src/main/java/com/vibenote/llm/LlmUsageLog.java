package com.vibenote.llm;

/**
 * {@code llm_usage_logs} 한 행. 실제 호출 시도마다 1행 (F-00 AC6).
 *
 * @param userId 요청한 회원 (없으면 null)
 * @param analysisId 분석 id (FK 없음, 없으면 null)
 * @param stage {@link LlmStage#key()} (예: {@code overview})
 * @param provider 실제로 호출한 공급자 ({@code gms|ollama|gemini|mock}, F-27 AC9)
 * @param model 성공이면 응답 모델, 실패면 설정 모델
 * @param latencyMs 호출 시작부터 응답·실패까지
 * @param errorMessage 실패 시만. 500자 이하, 키·프롬프트 없음
 */
public record LlmUsageLog(
        Long userId,
        Long analysisId,
        String stage,
        String provider,
        String model,
        int inputTokens,
        int outputTokens,
        int latencyMs,
        boolean success,
        String errorMessage) {}
