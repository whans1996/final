package com.vibenote.llm;

import com.vibenote.common.Level;

/**
 * LLM 호출 요청.
 *
 * @param stage 분석 단계
 * @param level 설명 수준
 * @param prompt 템플릿을 채운 최종 프롬프트 (출력은 JSON으로 요구한다)
 * @param userId 요청한 회원. {@code llm_usage_logs.user_id}에 기록 (모르면 null)
 * @param analysisId 분석 id. {@code llm_usage_logs.analysis_id}에 기록 (모르면 null)
 */
public record LlmRequest(LlmStage stage, Level level, String prompt, Long userId, Long analysisId) {

    /** 사용자·분석과 무관한 호출 (테스트, 관리 작업). */
    public LlmRequest(LlmStage stage, Level level, String prompt) {
        this(stage, level, prompt, null, null);
    }
}
