-- I-1a 면접 준비: 분석 하나의 예상 질문 생성 상태와 질문 (docs/specs/I-1a-interview-prep.md 4절)
CREATE TABLE analysis_interviews (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    analysis_id    BIGINT      NOT NULL,
    status         VARCHAR(16) NOT NULL COMMENT 'PENDING·DONE·FAILED (I-1a). 행이 없으면 NONE',
    error_code     VARCHAR(30) NULL,
    regenerations  INT         NOT NULL DEFAULT 0 COMMENT 'DONE 다시 만들기 사용 수',
    generated_at   DATETIME    NULL COMMENT '마지막으로 DONE이 된 시각',
    created_at     DATETIME    NOT NULL,
    updated_at     DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_analysis_interviews_analysis (analysis_id),
    KEY idx_analysis_interviews_status (status),
    CONSTRAINT fk_analysis_interviews_analysis FOREIGN KEY (analysis_id) REFERENCES analyses (id) ON DELETE CASCADE
);

CREATE TABLE interview_questions (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    interview_id  BIGINT       NOT NULL,
    no            INT          NOT NULL,
    category      VARCHAR(20)  NOT NULL COMMENT 'STRUCTURE·TECH_CHOICE·FLOW·LIMITATION (T-1에서 NOT_MINE)',
    question      VARCHAR(300) NOT NULL,
    key_points    JSON         NOT NULL COMMENT '문자열 배열 (1~5개)',
    evidence      JSON         NOT NULL COMMENT '[{fileId, path, startLine, endLine}] (1~3개)',
    concept_ids   JSON         NOT NULL COMMENT '관련 카드 id 배열 (0~3개). 조회 때 숨김·삭제 카드를 뺀다',
    PRIMARY KEY (id),
    UNIQUE KEY uk_interview_questions_no (interview_id, no),
    CONSTRAINT fk_interview_questions_interview FOREIGN KEY (interview_id) REFERENCES analysis_interviews (id) ON DELETE CASCADE
);
