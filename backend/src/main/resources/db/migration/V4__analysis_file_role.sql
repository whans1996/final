-- F-06 개요·파일별 설명: 파일마다 한 줄 역할을 저장한다 (docs/specs/F-06-overview-explanation.md 4절).
-- 설명 본문은 기존 analysis_files.summary(TEXT)에 저장한다.
ALTER TABLE analysis_files ADD COLUMN role VARCHAR(200) NULL AFTER layer;
