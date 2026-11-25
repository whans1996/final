-- F-26a 넓은 인덱스와 중요도 선별 (docs/specs/F-26a-index-selection.md 4절)
-- analysis_files에 인덱스 전체를 저장하고, 설명 대상으로 고른 파일만 selected = TRUE다.
-- 기존 행은 모두 선별 파일이었으므로 기본값 TRUE로 채운다 (AC10).
ALTER TABLE analysis_files
    ADD COLUMN selected   BOOLEAN NOT NULL DEFAULT TRUE COMMENT '설명 대상으로 선별됨 (F-26a)' AFTER layer,
    ADD COLUMN importance INT     NULL COMMENT '선별 점수. test·migration·doc은 NULL' AFTER selected,
    ADD KEY idx_analysis_files_selected (analysis_id, selected);

ALTER TABLE analyses
    ADD COLUMN indexed_files INT NULL COMMENT '인덱스에 담은 파일 수 (F-26a)',
    ADD COLUMN dropped_files INT NULL COMMENT '상한으로 버린 파일 수 (F-26a)';
