# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 17:28 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-14-code-quiz

## 현재 기능

F-14 코드 이해 퀴즈 — `docs/specs/F-14-code-quiz.md` **APPROVED (whans1996 11-19)**, 구현 전 (AC 11개, Flyway 없음, 새 에러 QUIZ_404·QUIZ_400)

## 진행

- ✅ F-08 PR #50 머지 후 정리 (develop `7f9e2fc`)
- ✅ F-14 spec 초안 + 결정로그 한 줄 (오답 → 내일 복습 자동 추가, 생성 실패 시 퀴즈 없이 DONE·재생성 없음, 재풀이 허용, #49 흡수 안 함)
- ✅ spec 승인 커밋 `docs(F-14): spec 승인`, `feature/F-14-code-quiz` push

## 바로 다음 할 일

1. `/clear` 후 새 세션(Opus / high)에서 `/implement F-14`: 먼저 `quiz/QuizResponseParserTest`(AC3)·`analysis/AnalysisQuizPipelineTest`(AC1, `AnalysisExtractingPipelineTest` 본뜸) red부터. `llm-fixtures/quiz.json`이 없으면 mock 파이프라인에서 QUIZ가 항상 비게 됨

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부

## 주의

- B 파일(`ConceptService`·`ConceptController`·`api/concepts.js`·`routes/concept.js`)과 공용 `ConceptCard.vue`·`EmptyState.vue`는 고치지 않는다. Gradle 테스트를 bootRun과 동시에 돌리지 않는다. V1~V5 수정 금지
- Git Bash heredoc·sed에서 한글·`\n`이 깨질 수 있다 → 한글 수정은 Edit 도구로
