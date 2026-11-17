-- 바이브노트 초기 스키마 (docs/기획서.md 3절 "ERD 초안"의 P0 테이블)
-- 이 파일은 커밋 후 수정하지 않는다. 컬럼·제약 변경은 V2__설명.sql 등 새 버전으로 추가한다.
-- 규칙: snake_case, 테이블 복수형, 시간은 DATETIME(Asia/Seoul), 회원은 탈퇴해도 행을 남긴다(status=WITHDRAWN).
-- 관계만 정의되어 있던 테이블(refresh_tokens, quizzes 등)은 최소 컬럼으로 만들고, 기능 spec에서 필요한 컬럼을 V2+로 보강한다.

-- ───────────── 회원·인증·소셜 (F-01~F-04) ─────────────

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname      VARCHAR(30)  NOT NULL,
    bio           VARCHAR(300) NULL,
    level         ENUM('BEGINNER','JUNIOR','SENIOR') NOT NULL DEFAULT 'BEGINNER',
    role          ENUM('USER','ADMIN')               NOT NULL DEFAULT 'USER',
    status        ENUM('ACTIVE','WITHDRAWN')         NOT NULL DEFAULT 'ACTIVE',
    withdrawn_at  DATETIME     NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_nickname (nickname)
);

-- Refresh 토큰은 원문 대신 SHA-256 해시를 저장한다. 전달 방식(쿠키/본문)은 F-02 spec에서 정한다.
CREATE TABLE refresh_tokens (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    token_hash  CHAR(64) NOT NULL,
    expires_at  DATETIME NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_hash (token_hash),
    KEY idx_refresh_tokens_user (user_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 자기 자신 팔로우 금지는 서비스에서 검사한다 (MySQL은 FK 동작이 있는 컬럼에 CHECK를 걸 수 없다).
CREATE TABLE follows (
    follower_id  BIGINT   NOT NULL,
    following_id BIGINT   NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, following_id),
    KEY idx_follows_following (following_id),
    CONSTRAINT fk_follows_follower  FOREIGN KEY (follower_id)  REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_follows_following FOREIGN KEY (following_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 공개 활동 피드. target_id는 type에 따라 analyses/concepts/posts/challenges의 id를 가리킨다(다형 참조라 FK 없음).
CREATE TABLE activities (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    type        ENUM('ANALYSIS','REVIEW','WISH','POST','CHALLENGE') NOT NULL,
    target_id   BIGINT   NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_activities_user_created (user_id, created_at),
    CONSTRAINT fk_activities_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- ───────────── 개념 카드 (F-08~F-13) ─────────────

CREATE TABLE concepts (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    category      VARCHAR(50)  NOT NULL,
    difficulty    ENUM('BEGINNER','JUNIOR','SENIOR') NOT NULL,
    summary       VARCHAR(500) NOT NULL,
    body          TEXT         NULL,
    example_code  TEXT         NULL,
    status        ENUM('DRAFT','PUBLISHED','HIDDEN') NOT NULL DEFAULT 'DRAFT',
    source        ENUM('SEED','AI')                  NOT NULL,
    reference_url VARCHAR(500) NULL COMMENT '공식 문서 출처 링크',
    wish_count    INT          NOT NULL DEFAULT 0 COMMENT '반정규화: 찜 변경 시 같은 트랜잭션에서 갱신',
    avg_rating    DECIMAL(2,1) NOT NULL DEFAULT 0.0 COMMENT '반정규화: 리뷰 변경 시 같은 트랜잭션에서 갱신',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_concepts_name (name),
    KEY idx_concepts_status_category (status, category)
);

CREATE TABLE concept_reviews (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    concept_id  BIGINT        NOT NULL,
    user_id     BIGINT        NOT NULL,
    rating      TINYINT       NOT NULL,
    content     VARCHAR(1000) NOT NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_concept_reviews_concept (concept_id, created_at),
    KEY idx_concept_reviews_user (user_id),
    CONSTRAINT fk_concept_reviews_concept FOREIGN KEY (concept_id) REFERENCES concepts (id) ON DELETE CASCADE,
    CONSTRAINT fk_concept_reviews_user    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT ck_concept_reviews_rating  CHECK (rating BETWEEN 1 AND 5)
);

CREATE TABLE concept_wishes (
    user_id     BIGINT   NOT NULL,
    concept_id  BIGINT   NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, concept_id),
    KEY idx_concept_wishes_concept (concept_id),
    CONSTRAINT fk_concept_wishes_user    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_concept_wishes_concept FOREIGN KEY (concept_id) REFERENCES concepts (id) ON DELETE CASCADE
);

-- ───────────── 분석 (F-05~F-08, F-19) ─────────────

CREATE TABLE analyses (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    source_type    ENUM('URL','ZIP') NOT NULL,
    source_url     VARCHAR(500) NULL,
    project_name   VARCHAR(200) NULL,
    title          VARCHAR(100) NULL,
    memo           VARCHAR(1000) NULL,
    level          ENUM('BEGINNER','JUNIOR','SENIOR') NOT NULL,
    status         ENUM('FETCHING','FILTERING','SELECTING','EXPLAINING','EXTRACTING','QUIZ','DONE','FAILED')
                   NOT NULL DEFAULT 'FETCHING',
    progress       TINYINT UNSIGNED NOT NULL DEFAULT 0,
    is_public      BOOLEAN      NOT NULL DEFAULT FALSE,
    overview       JSON         NULL COMMENT '개요·레이어 구조·API 목록',
    error_message  VARCHAR(500) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_analyses_user_created (user_id, created_at),
    CONSTRAINT fk_analyses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE analysis_files (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    analysis_id  BIGINT       NOT NULL,
    path         VARCHAR(500) NOT NULL,
    layer        VARCHAR(30)  NOT NULL COMMENT 'controller|service|mapper|vue 등',
    content      MEDIUMTEXT   NOT NULL,
    summary      TEXT         NULL,
    PRIMARY KEY (id),
    KEY idx_analysis_files_analysis (analysis_id),
    CONSTRAINT fk_analysis_files_analysis FOREIGN KEY (analysis_id) REFERENCES analyses (id) ON DELETE CASCADE
);

CREATE TABLE analysis_flows (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    analysis_id  BIGINT       NOT NULL,
    endpoint     VARCHAR(200) NOT NULL COMMENT '예: POST /api/orders',
    steps        JSON         NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_analysis_flows_endpoint (analysis_id, endpoint),
    CONSTRAINT fk_analysis_flows_analysis FOREIGN KEY (analysis_id) REFERENCES analyses (id) ON DELETE CASCADE
);

CREATE TABLE analysis_concepts (
    analysis_id   BIGINT       NOT NULL,
    concept_id    BIGINT       NOT NULL,
    file_id       BIGINT       NULL,
    line_hint     VARCHAR(50)  NULL COMMENT '예: L12-L30',
    context_note  VARCHAR(500) NULL,
    PRIMARY KEY (analysis_id, concept_id),
    KEY idx_analysis_concepts_concept (concept_id),
    CONSTRAINT fk_analysis_concepts_analysis FOREIGN KEY (analysis_id) REFERENCES analyses (id)       ON DELETE CASCADE,
    CONSTRAINT fk_analysis_concepts_concept  FOREIGN KEY (concept_id)  REFERENCES concepts (id)       ON DELETE CASCADE,
    CONSTRAINT fk_analysis_concepts_file     FOREIGN KEY (file_id)     REFERENCES analysis_files (id) ON DELETE SET NULL
);

-- ───────────── 퀴즈·복습·추천 (F-14~F-16) ─────────────

CREATE TABLE quizzes (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    analysis_id  BIGINT   NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_quizzes_analysis (analysis_id),
    CONSTRAINT fk_quizzes_analysis FOREIGN KEY (analysis_id) REFERENCES analyses (id) ON DELETE CASCADE
);

CREATE TABLE quiz_questions (
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    quiz_id       BIGINT   NOT NULL,
    concept_id    BIGINT   NULL,
    question      TEXT     NOT NULL,
    choices       JSON     NOT NULL COMMENT '선택지 문자열 배열',
    answer_index  TINYINT  NOT NULL COMMENT '0부터',
    explanation   TEXT     NULL,
    PRIMARY KEY (id),
    KEY idx_quiz_questions_quiz (quiz_id),
    KEY idx_quiz_questions_concept (concept_id),
    CONSTRAINT fk_quiz_questions_quiz    FOREIGN KEY (quiz_id)    REFERENCES quizzes (id)  ON DELETE CASCADE,
    CONSTRAINT fk_quiz_questions_concept FOREIGN KEY (concept_id) REFERENCES concepts (id) ON DELETE SET NULL
);

CREATE TABLE quiz_attempts (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    quiz_id     BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    score       INT      NOT NULL DEFAULT 0 COMMENT '맞힌 문항 수',
    total       INT      NOT NULL DEFAULT 0 COMMENT '전체 문항 수',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_quiz_attempts_user_created (user_id, created_at),
    KEY idx_quiz_attempts_quiz (quiz_id),
    CONSTRAINT fk_quiz_attempts_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE CASCADE,
    CONSTRAINT fk_quiz_attempts_user FOREIGN KEY (user_id) REFERENCES users (id)   ON DELETE CASCADE
);

CREATE TABLE quiz_answers (
    id              BIGINT  NOT NULL AUTO_INCREMENT,
    attempt_id      BIGINT  NOT NULL,
    question_id     BIGINT  NOT NULL,
    selected_index  TINYINT NOT NULL,
    is_correct      BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_quiz_answers_attempt_question (attempt_id, question_id),
    KEY idx_quiz_answers_question (question_id),
    CONSTRAINT fk_quiz_answers_attempt  FOREIGN KEY (attempt_id)  REFERENCES quiz_attempts (id)  ON DELETE CASCADE,
    CONSTRAINT fk_quiz_answers_question FOREIGN KEY (question_id) REFERENCES quiz_questions (id) ON DELETE CASCADE
);

CREATE TABLE review_plans (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    concept_id  BIGINT   NOT NULL,
    plan_date   DATE     NOT NULL,
    status      ENUM('TODO','DONE')            NOT NULL DEFAULT 'TODO',
    reason      ENUM('WRONG','WISH','MANUAL')  NOT NULL,
    done_at     DATETIME NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_review_plans_user_date (user_id, plan_date),
    KEY idx_review_plans_concept (concept_id),
    CONSTRAINT fk_review_plans_user    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_review_plans_concept FOREIGN KEY (concept_id) REFERENCES concepts (id) ON DELETE CASCADE
);

-- 추천 이유 캐시 (사용자 × 카드당 1줄)
CREATE TABLE recommendation_reasons (
    user_id     BIGINT       NOT NULL,
    concept_id  BIGINT       NOT NULL,
    reason      VARCHAR(300) NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, concept_id),
    KEY idx_recommendation_reasons_concept (concept_id),
    CONSTRAINT fk_recommendation_reasons_user    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_recommendation_reasons_concept FOREIGN KEY (concept_id) REFERENCES concepts (id) ON DELETE CASCADE
);

-- ───────────── 커뮤니티 (F-17, F-18) ─────────────

CREATE TABLE posts (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    board_type   ENUM('QNA','REVIEW','FREE') NOT NULL,
    title        VARCHAR(200) NOT NULL,
    content      TEXT         NOT NULL,
    analysis_id  BIGINT       NULL,
    concept_id   BIGINT       NULL,
    view_count   INT          NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_posts_board_created (board_type, created_at),
    KEY idx_posts_user (user_id),
    CONSTRAINT fk_posts_user     FOREIGN KEY (user_id)     REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_posts_analysis FOREIGN KEY (analysis_id) REFERENCES analyses (id) ON DELETE SET NULL,
    CONSTRAINT fk_posts_concept  FOREIGN KEY (concept_id)  REFERENCES concepts (id) ON DELETE SET NULL
);

CREATE TABLE comments (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    post_id     BIGINT        NOT NULL,
    user_id     BIGINT        NOT NULL,
    content     VARCHAR(1000) NOT NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_comments_post_created (post_id, created_at),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- ───────────── 학습 챌린지 (F-24, F-25) ─────────────

CREATE TABLE challenges (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    title        VARCHAR(100) NOT NULL,
    description  TEXT         NULL,
    image_url    VARCHAR(500) NULL,
    start_date   DATE         NOT NULL,
    end_date     DATE         NOT NULL,
    goal_type    ENUM('REVIEW_COUNT','QUIZ_COUNT','ANALYSIS_COUNT') NOT NULL,
    goal_value   INT          NOT NULL,
    created_by   BIGINT       NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_challenges_period (start_date, end_date),
    CONSTRAINT fk_challenges_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_challenges_period CHECK (end_date >= start_date),
    CONSTRAINT ck_challenges_goal   CHECK (goal_value > 0)
);

CREATE TABLE challenge_participants (
    challenge_id  BIGINT   NOT NULL,
    user_id       BIGINT   NOT NULL,
    progress      INT      NOT NULL DEFAULT 0,
    completed_at  DATETIME NULL,
    joined_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (challenge_id, user_id),
    KEY idx_challenge_participants_user (user_id),
    CONSTRAINT fk_challenge_participants_challenge FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE,
    CONSTRAINT fk_challenge_participants_user      FOREIGN KEY (user_id)      REFERENCES users (id)      ON DELETE CASCADE
);

-- ───────────── LLM 사용량 (NF-07) ─────────────

-- 분석이 삭제되어도 사용량 기록은 남기므로 analysis_id에는 FK를 걸지 않는다.
CREATE TABLE llm_usage_logs (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NULL,
    analysis_id    BIGINT       NULL,
    stage          VARCHAR(20)  NOT NULL COMMENT 'overview|file|flow|concept|quiz|recommend',
    model          VARCHAR(50)  NOT NULL,
    input_tokens   INT          NOT NULL DEFAULT 0,
    output_tokens  INT          NOT NULL DEFAULT 0,
    latency_ms     INT          NOT NULL DEFAULT 0,
    success        BOOLEAN      NOT NULL,
    error_message  VARCHAR(500) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_llm_usage_logs_user_created (user_id, created_at),
    KEY idx_llm_usage_logs_created (created_at),
    CONSTRAINT fk_llm_usage_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);
