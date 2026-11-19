-- F-07 실행 흐름: 요청 시 비동기로 생성하므로 상태·요약·실패 코드를 둔다 (docs/specs/F-07-execution-flow.md 4절).
-- endpoint는 "{METHOD} {path}"로 저장하고 기존 UNIQUE (analysis_id, endpoint)로 중복을 막는다.
ALTER TABLE analysis_flows
    ADD COLUMN status     VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER endpoint,
    ADD COLUMN summary    TEXT        NULL AFTER status,
    ADD COLUMN error_code VARCHAR(30) NULL AFTER steps,
    ADD COLUMN updated_at DATETIME    NULL AFTER created_at;
