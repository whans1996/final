-- #26: endpoint UNIQUE (analysis_id, endpoint)가 기본 collation(utf8mb4_0900_ai_ci)을 따라 대소문자를 구분하지 않아
-- `GET /api/users`와 `GET /api/Users`가 중복 키 오류(500)가 됐다. ApiDetector 중복 제거·FlowService 검증과 같게 대소문자를 구분한다.
ALTER TABLE analysis_flows
    MODIFY COLUMN endpoint VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '예: POST /api/orders';
