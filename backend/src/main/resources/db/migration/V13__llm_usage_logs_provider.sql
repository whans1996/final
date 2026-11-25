-- F-27 GMS 공급자 (docs/specs/F-27-gms-provider.md 4절)
-- 호출 시도마다 실제 공급자를 남긴다. 분석 상세의 간이 모드(liteMode)는 analysis_id로 성공한 ollama 기록을 찾는다.
ALTER TABLE llm_usage_logs
    ADD COLUMN provider VARCHAR(20) NULL COMMENT 'gms|ollama|gemini|mock. V13 이전 행은 NULL' AFTER stage,
    ADD KEY idx_llm_usage_logs_analysis (analysis_id);
