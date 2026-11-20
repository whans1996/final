-- F-13 관리자 카드 관리: 관리자가 직접 작성한 카드의 출처 ADMIN 추가
ALTER TABLE concepts
    MODIFY source ENUM('SEED','AI','ADMIN') NOT NULL COMMENT 'SEED 시드 파일, AI 분석 초안(F-08), ADMIN 관리자 작성(F-13)';
