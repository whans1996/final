-- K-1: 카드 수준별 한 줄 요약 3개(AI 초안·시드·관리자)와 수준별 본문 3개(시드·관리자). 기존 카드는 모두 NULL이다.
-- 화면은 회원 수준(users.level)의 본문이 있으면 그것을, 없으면 공통 body + 그 수준 요약을 보인다 (기획서 6.1절).
ALTER TABLE concepts
    ADD COLUMN summary_beginner VARCHAR(300) NULL COMMENT '수준별 한 줄 요약 (K-1)' AFTER summary,
    ADD COLUMN summary_junior   VARCHAR(300) NULL AFTER summary_beginner,
    ADD COLUMN summary_senior   VARCHAR(300) NULL AFTER summary_junior,
    ADD COLUMN body_beginner    TEXT NULL COMMENT '수준별 본문 (시드·관리자, K-1)' AFTER body,
    ADD COLUMN body_junior      TEXT NULL AFTER body_beginner,
    ADD COLUMN body_senior      TEXT NULL AFTER body_junior;
