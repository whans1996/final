# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 17:41 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-14-code-quiz

## 현재 기능

F-14 코드 이해 퀴즈 — `docs/specs/F-14-code-quiz.md` APPROVED, `/implement` 진행 중

## 진행

- ✅ BE 생성: AC1~AC4·AC11 green (`quiz/QuizPrompts`·`QuizResponseParser`·`QuizGenerator`·`QuizSaveService`·`QuizMapper`, 파이프라인 QUIZ(85), 프롬프트 3개, `quiz.json`, 기존 파이프라인 테스트 순서 기대값 수정)
- ✅ FE(테스트 green, 아직 미커밋): `api/quizzes.js`, `AnalysisQuizTab.vue`, `components/quiz/QuizQuestion.vue`, `QuizView.vue`, `utils/quizFormat.js`, route `analysis-quiz`, 상세 퀴즈 탭
- ⏳ BE API: AC5~AC8 (`QuizService`·`QuizController`·dto 8개, `reviewplan/ReviewPlanService`·Mapper, ErrorCode QUIZ_404·QUIZ_400). `test/.../quiz/QuizTestSupport.java`(고정 Clock 11-19 17:30)만 작성됨

## 바로 다음 할 일

1. `quiz/QuizApiTest`(AC5~AC8)·`reviewplan/ReviewPlanServiceTest`(AC7) red → 구현. 복습 INSERT는 `ON DUPLICATE KEY UPDATE id=id`만으로는 Connector/J 기본(CLIENT_FOUND_ROWS)에서 중복도 1행을 돌려주므로 `INSERT ... SELECT ... WHERE NOT EXISTS(같은 날짜)`로 판단 (spec과 다름, PR에 적기)
2. OpenApiExportTest로 openapi.json 갱신 → contract-checker → ui-verifier → 산출물(UC30·ERD·화면설계서·08·09·datasets DS-06)

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부

## 주의

- 공통 파일 변경: `application.yml`(quiz 블록), `docs/conventions.md`(QUIZ 행), ErrorCode(예정) → PR에 @JoonseobKim 멘션
- B 파일(`ConceptService`·`ConceptController`·`api/concepts.js`·`routes/concept.js`)과 공용 `ConceptCard.vue`·`EmptyState.vue`는 고치지 않는다. Gradle 테스트를 bootRun과 동시에 돌리지 않는다. V1~V5 수정 금지
- FE는 테스트와 구현을 함께 쓴 뒤 돌렸다 (red 단계 생략). PR 본문에 적는다
