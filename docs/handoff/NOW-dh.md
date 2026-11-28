# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-28 22:38 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/L-1b-wrong-notes

## 현재 기능

**L-1b 풀이 기록·오답 노트·선지 다시 섞기** `docs/specs/L-1b-wrong-notes.md` (**APPROVED 11-28**) · 브랜치 `feature/L-1b-wrong-notes`

## 진행

- ✅ I-1a PR #262 머지 (11/28 22:22 KST, 로드맵 날짜와 같아 재조정 없음)
- ✅ L-1b spec 승인: AC14개, API 3개(`GET /api/quizzes/{quizId}/attempts`·`/{attemptId}`, `GET /api/users/me/wrong-notes`), 새 에러 `QUIZ_ATTEMPT_404`, DB 변경 없음, 새 화면 SC-27 `/wrong-notes`
- ✅ 사용자 결정 4건 → 결정로그 11-28 행

## 바로 다음 할 일

1. `/clear` → `/implement L-1b` (Opus/high)
2. BE 먼저: `backend/src/test/java/com/vibenote/quiz/QuizAttemptHistoryTest`(AC1~4·9) → `WrongNoteTest`(AC5~9) red 확인 → `QuizController` GET 2개 + `/api/users/me/wrong-notes`, `ErrorCode.QUIZ_ATTEMPT_404`, `QuizMapper.xml` 오답 쿼리(ROW_NUMBER로 마지막 답)
3. FE: `utils/quizShuffle.js`(AC11) → `components/quiz/QuizAttemptHistory.vue`(AC10) → `views/WrongNoteView.vue`(AC12) → 진입 링크(AC13)

## 막힌 점 / 결정 필요
- 사람: 노션 초대, F-05b liveTest + #226 실제 LLM 점검(12/08 전), #251 js PC `--move-env`

## 주의
- 공통 파일 `utils/navIcons.js`에 `NotebookPen` 추가 → PR에서 김준섭 @멘션
- #250은 `AnalysisQuizTab.vue`·`QuizView.vue` 칸만 체크 (`Closes` 없음)
- 대시보드·복습 계획은 링크만(이전은 AM-1). 일정이 빠듯하면 AC10(SC-07 풀이 기록)을 먼저 뺀다
