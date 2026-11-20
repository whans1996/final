# ER Diagram — 바이브노트

> 작성 상태: 🟡 초안 · 설계 정본은 `docs/기획서.md` 3절 "ERD 초안"(mermaid)이고, **DDL 정본은 `backend/src/main/resources/db/migration/V1__init.sql`**(이후 V2+)이다.
> **최종본**은 구현이 끝난 뒤 실제 DB(Flyway 적용 후)에서 MySQL Workbench Reverse Engineer로 만든 이미지(`diagrams/erd.png`)와 아래 테이블 정의로 대체한다 (W5).

<!-- 최종: ![ERD](diagrams/erd.png) -->

## 테이블 목록

| 테이블 | 설명 | 주요 관계 | 기능 |
|---|---|---|---|
| users | 회원 (수준, 역할, 상태 ACTIVE/WITHDRAWN) | - | F-01~03 |
| refresh_tokens | Refresh 토큰 | users 1:N | F-02 |
| follows | 팔로우 (follower, following 복합 PK) | users N:M users | F-04 |
| activities | 공개 활동 기록(피드) | users 1:N | F-04, F-25 |
| analyses | 분석 요청과 결과 개요(JSON: summary·techStack·layers·readingOrder·apis, F-06 5.3절), 상태·진행률·실패 사유(error_code·error_message, V3), 공개 여부, 제목·메모, 삭제 시각(soft delete) | users 1:N | F-05a, F-06, F-19 |
| analysis_files | 선별한 주요 파일(최대 30개, 키 마스킹된 content), 한 줄 역할(role, V4)과 설명(summary) | analyses 1:N | F-05a, F-06 |
| analysis_flows | API 실행 흐름(endpoint `"{METHOD} {path}"`, 상태 PENDING·DONE·FAILED, 요약·단계 JSON 캐시·실패 코드, V5) | analyses 1:N | F-07 |
| concepts | 개념 카드 (상태 DRAFT/PUBLISHED/HIDDEN, 출처 SEED/AI). AI 초안은 분석의 EXTRACTING 단계가 `DRAFT·AI`로 만들고 예제 코드·출처는 비워 둔다(관리자 검수 F-13에서 채움). `name` UNIQUE라 같은 이름이면 기존 카드를 쓴다 | - | F-08~13 |
| analysis_concepts | 분석 ↔ 개념 연결과 코드 위치(파일 `file_id`, 줄 `line_hint` 예: L12-L30, 맥락 `context_note`). EXTRACTING 단계(F-08)가 채우고, PK `(analysis_id, concept_id)`라 한 분석에서 개념 하나는 위치 하나다. 분석당 최대 15개 | analyses N:M concepts | F-08, F-10 |
| concept_reviews | 카드 리뷰 | concepts 1:N, users 1:N | F-11 |
| concept_wishes | 카드 찜 | concepts N:M users | F-12 |
| quizzes, quiz_questions | 분석별 퀴즈와 문항(선택지 4개 JSON 문자열 배열, 정답 위치 0부터, 해설, 관련 카드 `concept_id` NULL 가능). 분석 파이프라인 QUIZ 단계(F-14)가 분석당 1번 만들고(`analysis_id` UNIQUE, 재생성 없음) 문항 번호는 id 순서다. 생성이 실패하면 행이 없다 | analyses 1:1, concepts 1:N | F-14 |
| quiz_attempts, quiz_answers | 풀이 기록(`score` 맞힌 수, `total` 문항 수)과 문항별 답안. 같은 퀴즈를 다시 풀면 새 풀이로 쌓인다. 분석을 soft delete해도 남는다(통계·챌린지가 읽음) | users·quizzes 1:N | F-14 |
| review_plans | 날짜별 복습 계획. 퀴즈 제출 때 틀린 공개 개념이 `reason=WRONG`(F-14), 찜할 때 `WISH`(F-12가 `ReviewPlanService.addWish` 호출), 카드 상세 "복습에 추가"로 `MANUAL`(F-15)이 들어간다. 자동 추가는 내일 날짜, 모두 TODO로 시작하고 한 회원·카드의 TODO는 하나만 둔다. 완료하면 `status=DONE`·`done_at`(학습 통계·챌린지가 센다). 공개되지 않은 카드의 계획은 목록·API에서 빠진다(행은 둔다). UNIQUE `(user_id, concept_id, plan_date)` | users·concepts 1:N | F-14, F-15 |
| recommendation_reasons | 추천 이유 캐시. PK `(user_id, concept_id)`, 이유를 만들 때의 신호 키 `signal_key`(예: `w2c1k0l1`, V6)가 지금 신호와 다르면 다시 만든다. LLM이 실패해 규칙 문장을 준 카드는 저장하지 않는다 | users·concepts | F-16 |
| posts | 게시글 (board_type QNA/REVIEW/FREE, 분석·카드 첨부) | users 1:N | F-17 |
| comments | 댓글 | posts 1:N, users 1:N | F-18 |
| challenges | 학습 챌린지 (기간, 이미지, 목표 유형·값) | users(작성 관리자) 1:N | F-24 |
| challenge_participants | 챌린지 참여와 달성도 | challenges N:M users | F-25 |
| llm_usage_logs | LLM 호출 사용량 | users 1:N | NF-07 |

## 정규화 메모

- 조회·집계 대상(파일, 개념 연결, 퀴즈 문항, 리뷰, 찜, 복습)은 테이블로 정규화하고, 화면에 통째로 보여 주는 분석 개요와 실행 흐름 단계는 JSON 컬럼으로 저장했다 (기획서 결정 로그 "결과 저장: 테이블과 JSON 혼합").
- `concepts.wish_count`, `concepts.avg_rating`은 목록 정렬 성능을 위한 의도적인 비정규화(반정규화) 컬럼이며, 찜·리뷰 변경 시 같은 트랜잭션에서 갱신한다.
- 챌린지 달성도는 `challenge_participants.progress`에 저장하되 원천은 review_plans·quiz_attempts·analyses이며, 활동 발생 시 갱신한다.
