# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 17:49 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-14-code-quiz

## 현재 기능

F-14 코드 이해 퀴즈 — `docs/specs/F-14-code-quiz.md` APPROVED, `/implement` 진행 중. AC1~AC11 모두 green

## 진행

- ✅ `bd9a7e9` BE 생성 AC1~AC4·AC11 / `df139ec` FE AC9·AC10 (퀴즈 탭·SC-08)
- ✅ BE API AC5~AC8 green (`QuizService`·`QuizController`·dto 8개, `reviewplan/ReviewPlanService`·Mapper, ErrorCode QUIZ_404·QUIZ_400, `AnalysisService.checkOwned`), openapi.json 갱신, 산출물(UC30·ERD·화면설계서 SC-07 퀴즈 탭·SC-08·08·09·datasets DS-06) 반영 — 커밋 직전
- 복습 INSERT는 `INSERT ... SELECT ... WHERE NOT EXISTS + ODKU`(Connector/J CLIENT_FOUND_ROWS 때문에 spec의 ODKU 영향 행 수 방식은 중복도 1) → PR에 적기

## 바로 다음 할 일

1. 커밋 `feat(F-14): AC5~AC8 퀴즈 조회·채점·오답 복습 API` push
2. `contract-checker`(spec 3절 ↔ openapi.json ↔ `frontend/src/api/quizzes.js`) → BE local + FE dev 띄워 `ui-verifier`(SC-07 `?tab=quiz`, SC-08 데스크톱·375px) → 스크린샷 `SC-07-quiz`·`SC-08` 을 `docs/산출물/screens/`에 넣고 06-화면설계서 표에 추가
3. `/pr-review` 제안

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부

## 주의

- 공통 파일 변경: `application.yml`(quiz 블록), `docs/conventions.md`(QUIZ 행), `ErrorCode`(QUIZ 2개), `AnalysisService.checkOwned` 추가 → PR에 @JoonseobKim 멘션
- FE는 테스트와 구현을 함께 쓴 뒤 돌렸다 (red 단계 생략). PR 본문에 적는다
- B 파일(`ConceptService`·`ConceptController`·`api/concepts.js`)과 공용 `ConceptCard.vue`·`EmptyState.vue`는 고치지 않는다. Gradle 테스트를 bootRun과 동시에 돌리지 않는다
