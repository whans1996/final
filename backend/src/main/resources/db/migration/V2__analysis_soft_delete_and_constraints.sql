-- V1 리뷰 반영 (2026-11-18, 기획서 결정 로그)
-- 1) 분석 삭제는 soft delete: 행을 지우면 quizzes → quiz_attempts·quiz_answers가 CASCADE로 함께 지워져
--    F-19 "퀴즈 풀이 기록은 유지", 학습 통계(F-03), 추천 오답 데이터(F-16), 챌린지 달성도(F-25), 일일 분석 한도가 깨진다.
--    삭제 시 서비스가 analysis_files·analysis_flows·analysis_concepts를 지우고 deleted_at을 채운다.
--    목록·상세·피드·게시글 첨부 조회는 deleted_at IS NULL로 거른다. 일일 한도는 deleted_at과 상관없이 센다.
ALTER TABLE analyses
    ADD COLUMN deleted_at DATETIME NULL AFTER error_message;

-- 2) 같은 날 같은 카드의 복습 계획은 하나만 (오답·찜으로 중복 등록 방지)
ALTER TABLE review_plans
    ADD CONSTRAINT uk_review_plans_user_concept_date UNIQUE (user_id, concept_id, plan_date);

-- 3) 카드 리뷰는 사용자당 1개 (avg_rating 부풀리기 방지)
ALTER TABLE concept_reviews
    ADD CONSTRAINT uk_concept_reviews_concept_user UNIQUE (concept_id, user_id);

-- 4) 작성물(게시글·댓글·리뷰)의 작성자 FK는 RESTRICT. 탈퇴는 users.status로 처리하고 글은 남긴다.
ALTER TABLE posts           DROP FOREIGN KEY fk_posts_user;
ALTER TABLE posts           ADD CONSTRAINT fk_posts_user           FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE comments        DROP FOREIGN KEY fk_comments_user;
ALTER TABLE comments        ADD CONSTRAINT fk_comments_user        FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE concept_reviews DROP FOREIGN KEY fk_concept_reviews_user;
ALTER TABLE concept_reviews ADD CONSTRAINT fk_concept_reviews_user FOREIGN KEY (user_id) REFERENCES users (id);

-- 5) 챌린지 활동은 참여와 달성을 구분한다 (F-04 피드, F-25)
ALTER TABLE activities
    MODIFY COLUMN type ENUM('ANALYSIS','REVIEW','WISH','POST','CHALLENGE_JOIN','CHALLENGE_COMPLETE') NOT NULL;
