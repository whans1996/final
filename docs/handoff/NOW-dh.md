# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-28 23:07 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/L-1b-wrong-notes

## 현재 기능

**L-1b 풀이 기록·오답 노트·선지 다시 섞기** `docs/specs/L-1b-wrong-notes.md` (APPROVED 11-28, 구현 중) · 브랜치 `feature/L-1b-wrong-notes`

## 진행

- ✅ BE AC1~AC9 (0c92b46): `QuizController` GET attempts·attempts/{id}·`/api/users/me/wrong-notes`, `WrongNoteService`, `QuizMapper.xml` `wrongNotes`(ROW_NUMBER), 테스트 `QuizAttemptHistoryTest`·`WrongNoteTest` green, openapi 갱신
- ✅ 에러 코드 `QUIZ_ATTEMPT_404` → **`QUIZ_001`** (형식 규칙 `ErrorCodeTest`, 결정로그·spec·conventions 반영)
- ✅ FE AC10~AC14 (bbe812e): `QuizAttemptHistory.vue`, `utils/quizShuffle.js`, `QuizView` 다시 섞기·`?from=wrong-notes`, `WrongNoteView`·`WrongNoteItem`, 라우트·메뉴·대시보드/복습 링크, Tailwind·useLatestRequest 이관(ADOPTED 4개)
- ⏳ contract-checker 실행 중

## 바로 다음 할 일

1. contract-checker 결과 반영
2. ui-verifier: BE(local)·FE dev 띄우고 SC-07 퀴즈 탭 풀이 기록 펼침, SC-08 다시 풀기, SC-27 `/wrong-notes` (데스크톱·모바일·다크)
3. 산출물: 06 화면 설계서(SC-07·SC-08 바뀐 부분, SC-27 새 행), 01 요구사항(오답 노트 행), 02 유스케이스, 07은 `node scripts/export-api-doc.mjs`
4. #250 본문 `AnalysisQuizTab.vue`·`QuizView.vue` 칸 체크 → `/pr-review`

## 막힌 점 / 결정 필요
- 사람: 노션 초대, F-05b liveTest + #226 실제 LLM 점검(12/08 전), #251 js PC `--move-env`

## 주의
- 공통 파일 변경 → PR에서 김준섭 @멘션: `ErrorCode.java`(QUIZ_001), `utils/navIcons.js`(NotebookPen), 테스트 `App.spec.js`·`navItems.spec.js` 메뉴 목록에 "오답 노트"
- #250은 `AnalysisQuizTab.vue`·`QuizView.vue` 칸만 체크 (`Closes` 없음)
- 대시보드·복습 계획은 링크만(이전은 AM-1)
