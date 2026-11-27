-- 바이브노트 DB 스키마 (MySQL 8.4)
-- 자동 생성 파일이다. 손으로 고치지 않는다. 다시 만들기: node scripts/export-schema.mjs (Docker 필요, 주간 /progress와 제출 전)
-- 정본: backend/src/main/resources/db/migration/V*.sql (Flyway). 반영한 마지막 버전: V14
-- 시드 데이터(R__seed_concepts.sql)는 넣지 않는다. 데이터 셋은 docs/산출물/09-데이터셋.md

CREATE DATABASE IF NOT EXISTS vibenote DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE vibenote;

SET FOREIGN_KEY_CHECKS=0;

CREATE TABLE `activities` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `type` enum('ANALYSIS','REVIEW','WISH','POST','CHALLENGE_JOIN','CHALLENGE_COMPLETE') NOT NULL,
  `target_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_activities_user_created` (`user_id`,`created_at`),
  CONSTRAINT `fk_activities_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `analyses` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `source_type` enum('URL','ZIP') NOT NULL,
  `source_url` varchar(500) DEFAULT NULL,
  `project_name` varchar(200) DEFAULT NULL,
  `title` varchar(100) DEFAULT NULL,
  `memo` varchar(1000) DEFAULT NULL,
  `level` enum('BEGINNER','JUNIOR','SENIOR') NOT NULL,
  `status` enum('FETCHING','FILTERING','SELECTING','EXPLAINING','EXTRACTING','QUIZ','DONE','FAILED') NOT NULL DEFAULT 'FETCHING',
  `progress` tinyint unsigned NOT NULL DEFAULT '0',
  `error_code` varchar(30) DEFAULT NULL,
  `is_public` tinyint(1) NOT NULL DEFAULT '0',
  `overview` json DEFAULT NULL COMMENT '개요·레이어 구조·API 목록',
  `error_message` varchar(500) DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `indexed_files` int DEFAULT NULL COMMENT '인덱스에 담은 파일 수 (F-26a)',
  `dropped_files` int DEFAULT NULL COMMENT '상한으로 버린 파일 수 (F-26a)',
  `quiz_status` varchar(16) DEFAULT NULL COMMENT 'GENERATING·READY·FAILED, QUIZ 단계 전 NULL (L-1a)',
  `quiz_regenerations` int NOT NULL DEFAULT '0' COMMENT '퀴즈 재생성 요청 수 (L-1a)',
  PRIMARY KEY (`id`),
  KEY `idx_analyses_user_created` (`user_id`,`created_at`),
  CONSTRAINT `fk_analyses_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `analysis_concepts` (
  `analysis_id` bigint NOT NULL,
  `concept_id` bigint NOT NULL,
  `file_id` bigint DEFAULT NULL,
  `line_hint` varchar(50) DEFAULT NULL COMMENT '예: L12-L30',
  `context_note` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`analysis_id`,`concept_id`),
  KEY `idx_analysis_concepts_concept` (`concept_id`),
  KEY `fk_analysis_concepts_file` (`file_id`),
  CONSTRAINT `fk_analysis_concepts_analysis` FOREIGN KEY (`analysis_id`) REFERENCES `analyses` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_analysis_concepts_concept` FOREIGN KEY (`concept_id`) REFERENCES `concepts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_analysis_concepts_file` FOREIGN KEY (`file_id`) REFERENCES `analysis_files` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `analysis_files` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `analysis_id` bigint NOT NULL,
  `path` varchar(500) NOT NULL,
  `layer` varchar(30) NOT NULL COMMENT 'controller|service|mapper|vue 등',
  `selected` tinyint(1) NOT NULL DEFAULT '1' COMMENT '설명 대상으로 선별됨 (F-26a)',
  `importance` int DEFAULT NULL COMMENT '선별 점수. test·migration·doc은 NULL',
  `role` varchar(200) DEFAULT NULL,
  `content` mediumtext NOT NULL,
  `summary` text,
  PRIMARY KEY (`id`),
  KEY `idx_analysis_files_analysis` (`analysis_id`),
  KEY `idx_analysis_files_selected` (`analysis_id`,`selected`),
  CONSTRAINT `fk_analysis_files_analysis` FOREIGN KEY (`analysis_id`) REFERENCES `analyses` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `analysis_flows` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `analysis_id` bigint NOT NULL,
  `endpoint` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL COMMENT '예: POST /api/orders',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `summary` text,
  `steps` json DEFAULT NULL,
  `error_code` varchar(30) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_analysis_flows_endpoint` (`analysis_id`,`endpoint`),
  CONSTRAINT `fk_analysis_flows_analysis` FOREIGN KEY (`analysis_id`) REFERENCES `analyses` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `challenge_participants` (
  `challenge_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `progress` int NOT NULL DEFAULT '0',
  `completed_at` datetime DEFAULT NULL,
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`challenge_id`,`user_id`),
  KEY `idx_challenge_participants_user` (`user_id`),
  CONSTRAINT `fk_challenge_participants_challenge` FOREIGN KEY (`challenge_id`) REFERENCES `challenges` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_challenge_participants_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `challenges` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL,
  `description` text,
  `image_url` varchar(500) DEFAULT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `goal_type` enum('REVIEW_COUNT','QUIZ_COUNT','ANALYSIS_COUNT') NOT NULL,
  `goal_value` int NOT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_challenges_period` (`start_date`,`end_date`),
  KEY `fk_challenges_created_by` (`created_by`),
  CONSTRAINT `fk_challenges_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_challenges_goal` CHECK ((`goal_value` > 0)),
  CONSTRAINT `ck_challenges_period` CHECK ((`end_date` >= `start_date`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` varchar(1000) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comments_post_created` (`post_id`,`created_at`),
  KEY `fk_comments_user` (`user_id`),
  CONSTRAINT `fk_comments_post` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `concept_reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `concept_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `rating` tinyint NOT NULL,
  `content` varchar(1000) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_concept_reviews_concept_user` (`concept_id`,`user_id`),
  KEY `idx_concept_reviews_concept` (`concept_id`,`created_at`),
  KEY `idx_concept_reviews_user` (`user_id`),
  CONSTRAINT `fk_concept_reviews_concept` FOREIGN KEY (`concept_id`) REFERENCES `concepts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_concept_reviews_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `ck_concept_reviews_rating` CHECK ((`rating` between 1 and 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `concept_wishes` (
  `user_id` bigint NOT NULL,
  `concept_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`,`concept_id`),
  KEY `idx_concept_wishes_concept` (`concept_id`),
  CONSTRAINT `fk_concept_wishes_concept` FOREIGN KEY (`concept_id`) REFERENCES `concepts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_concept_wishes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `concepts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `category` varchar(50) NOT NULL,
  `difficulty` enum('BEGINNER','JUNIOR','SENIOR') NOT NULL,
  `summary` varchar(500) NOT NULL,
  `body` text,
  `example_code` text,
  `status` enum('DRAFT','PUBLISHED','HIDDEN') NOT NULL DEFAULT 'DRAFT',
  `source` enum('SEED','AI','ADMIN') NOT NULL COMMENT 'SEED 시드 파일, AI 분석 초안(F-08), ADMIN 관리자 작성(F-13)',
  `reference_url` varchar(500) DEFAULT NULL COMMENT '공식 문서 출처 링크',
  `wish_count` int NOT NULL DEFAULT '0' COMMENT '반정규화: 찜 변경 시 같은 트랜잭션에서 갱신',
  `avg_rating` decimal(2,1) NOT NULL DEFAULT '0.0' COMMENT '반정규화: 리뷰 변경 시 같은 트랜잭션에서 갱신',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_concepts_name` (`name`),
  KEY `idx_concepts_status_category` (`status`,`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `follows` (
  `follower_id` bigint NOT NULL,
  `following_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`follower_id`,`following_id`),
  KEY `idx_follows_following` (`following_id`),
  CONSTRAINT `fk_follows_follower` FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_follows_following` FOREIGN KEY (`following_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `llm_usage_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `analysis_id` bigint DEFAULT NULL,
  `stage` varchar(20) NOT NULL COMMENT 'overview|file|flow|concept|quiz|recommend',
  `provider` varchar(20) DEFAULT NULL COMMENT 'gms|ollama|gemini|mock. V13 이전 행은 NULL',
  `model` varchar(50) NOT NULL,
  `input_tokens` int NOT NULL DEFAULT '0',
  `output_tokens` int NOT NULL DEFAULT '0',
  `latency_ms` int NOT NULL DEFAULT '0',
  `success` tinyint(1) NOT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_llm_usage_logs_user_created` (`user_id`,`created_at`),
  KEY `idx_llm_usage_logs_created` (`created_at`),
  KEY `idx_llm_usage_logs_analysis` (`analysis_id`),
  CONSTRAINT `fk_llm_usage_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `posts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `board_type` enum('QNA','REVIEW','FREE') NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `analysis_id` bigint DEFAULT NULL,
  `concept_id` bigint DEFAULT NULL,
  `view_count` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_posts_board_created` (`board_type`,`created_at`),
  KEY `idx_posts_user` (`user_id`),
  KEY `fk_posts_analysis` (`analysis_id`),
  KEY `fk_posts_concept` (`concept_id`),
  CONSTRAINT `fk_posts_analysis` FOREIGN KEY (`analysis_id`) REFERENCES `analyses` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_posts_concept` FOREIGN KEY (`concept_id`) REFERENCES `concepts` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_posts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `quiz_answers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attempt_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `selected_index` tinyint NOT NULL,
  `is_correct` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_quiz_answers_attempt_question` (`attempt_id`,`question_id`),
  KEY `idx_quiz_answers_question` (`question_id`),
  CONSTRAINT `fk_quiz_answers_attempt` FOREIGN KEY (`attempt_id`) REFERENCES `quiz_attempts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_quiz_answers_question` FOREIGN KEY (`question_id`) REFERENCES `quiz_questions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `quiz_attempts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quiz_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `score` int NOT NULL DEFAULT '0' COMMENT '맞힌 문항 수',
  `total` int NOT NULL DEFAULT '0' COMMENT '전체 문항 수',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_quiz_attempts_user_created` (`user_id`,`created_at`),
  KEY `idx_quiz_attempts_quiz` (`quiz_id`),
  CONSTRAINT `fk_quiz_attempts_quiz` FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_quiz_attempts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `quiz_questions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quiz_id` bigint NOT NULL,
  `concept_id` bigint DEFAULT NULL,
  `question` text NOT NULL,
  `choices` json NOT NULL COMMENT '선택지 문자열 배열',
  `answer_index` tinyint NOT NULL COMMENT '0부터',
  `explanation` text,
  PRIMARY KEY (`id`),
  KEY `idx_quiz_questions_quiz` (`quiz_id`),
  KEY `idx_quiz_questions_concept` (`concept_id`),
  CONSTRAINT `fk_quiz_questions_concept` FOREIGN KEY (`concept_id`) REFERENCES `concepts` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_quiz_questions_quiz` FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `quizzes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `analysis_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_quizzes_analysis` (`analysis_id`),
  CONSTRAINT `fk_quizzes_analysis` FOREIGN KEY (`analysis_id`) REFERENCES `analyses` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `recommendation_reasons` (
  `user_id` bigint NOT NULL,
  `concept_id` bigint NOT NULL,
  `reason` varchar(300) NOT NULL,
  `signal_key` varchar(20) NOT NULL DEFAULT '' COMMENT '예: w2c1k0l1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`,`concept_id`),
  KEY `idx_recommendation_reasons_concept` (`concept_id`),
  CONSTRAINT `fk_recommendation_reasons_concept` FOREIGN KEY (`concept_id`) REFERENCES `concepts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_recommendation_reasons_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token_hash` char(64) NOT NULL,
  `expires_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_tokens_hash` (`token_hash`),
  KEY `idx_refresh_tokens_user` (`user_id`),
  CONSTRAINT `fk_refresh_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `review_plans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `concept_id` bigint NOT NULL,
  `plan_date` date NOT NULL,
  `status` enum('TODO','DONE') NOT NULL DEFAULT 'TODO',
  `reason` enum('WRONG','WISH','MANUAL') NOT NULL,
  `done_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_plans_user_concept_date` (`user_id`,`concept_id`,`plan_date`),
  KEY `idx_review_plans_user_date` (`user_id`,`plan_date`),
  KEY `idx_review_plans_concept` (`concept_id`),
  CONSTRAINT `fk_review_plans_concept` FOREIGN KEY (`concept_id`) REFERENCES `concepts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_review_plans_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `team_analyses` (
  `team_id` bigint NOT NULL,
  `analysis_id` bigint NOT NULL,
  `shared_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`team_id`,`analysis_id`),
  KEY `idx_team_analyses_analysis` (`analysis_id`),
  CONSTRAINT `fk_team_analyses_analysis` FOREIGN KEY (`analysis_id`) REFERENCES `analyses` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_team_analyses_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `team_challenges` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `team_id` bigint NOT NULL,
  `analysis_id` bigint NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `target_rate` int NOT NULL COMMENT '목표 정답률 1~100 (%)',
  `created_by` bigint NOT NULL COMMENT '만든 팀장',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_team_challenges_team_analysis` (`team_id`,`analysis_id`,`end_date`),
  KEY `fk_team_challenges_creator` (`created_by`),
  CONSTRAINT `fk_team_challenges_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_team_challenges_share` FOREIGN KEY (`team_id`, `analysis_id`) REFERENCES `team_analyses` (`team_id`, `analysis_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `team_members` (
  `team_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role` enum('OWNER','MEMBER') NOT NULL,
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`team_id`,`user_id`),
  KEY `idx_team_members_user` (`user_id`),
  CONSTRAINT `fk_team_members_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_team_members_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `teams` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(30) NOT NULL,
  `description` varchar(200) DEFAULT NULL,
  `owner_id` bigint NOT NULL,
  `invite_code` char(8) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teams_invite_code` (`invite_code`),
  KEY `fk_teams_owner` (`owner_id`),
  CONSTRAINT `fk_teams_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(100) NOT NULL,
  `nickname` varchar(30) NOT NULL,
  `bio` varchar(300) DEFAULT NULL,
  `level` enum('BEGINNER','JUNIOR','SENIOR') NOT NULL DEFAULT 'BEGINNER',
  `role` enum('USER','ADMIN') NOT NULL DEFAULT 'USER',
  `status` enum('ACTIVE','WITHDRAWN') NOT NULL DEFAULT 'ACTIVE',
  `withdrawn_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_email` (`email`),
  UNIQUE KEY `uk_users_nickname` (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS=1;
