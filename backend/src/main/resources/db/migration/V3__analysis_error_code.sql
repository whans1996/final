-- F-05a: 파이프라인 실패 사유 코드 (ANALYSIS_400·413·415·500). 화면 문구는 error_message에 둔다.
ALTER TABLE analyses
    ADD COLUMN error_code VARCHAR(30) NULL AFTER progress;
