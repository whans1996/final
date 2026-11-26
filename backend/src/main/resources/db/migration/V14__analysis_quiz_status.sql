-- L-1a 실패 분석 재실행·퀴즈 재생성 (docs/specs/L-1a-rerun-quiz-regenerate.md 4절)
-- quiz_status: QUIZ 단계에서 문항을 저장하면 READY, 빈 결과면 FAILED, 재생성 중이면 GENERATING. QUIZ 단계를 끝내기 전에는 NULL.
ALTER TABLE analyses
    ADD COLUMN quiz_status VARCHAR(16) NULL COMMENT 'GENERATING·READY·FAILED, QUIZ 단계 전 NULL (L-1a)',
    ADD COLUMN quiz_regenerations INT NOT NULL DEFAULT 0 COMMENT '퀴즈 재생성 요청 수 (L-1a)';

-- 기존 행: 퀴즈가 있으면 READY, 없고 DONE이면 FAILED(생성 실패), 나머지는 NULL
UPDATE analyses a
SET quiz_status = CASE
    WHEN EXISTS (SELECT 1 FROM quizzes q WHERE q.analysis_id = a.id) THEN 'READY'
    WHEN a.status = 'DONE' THEN 'FAILED'
    ELSE NULL END;
