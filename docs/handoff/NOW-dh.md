# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-28 23:33 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/L-1b-wrong-notes

## 현재 기능

**L-1b 풀이 기록·오답 노트·선지 다시 섞기** `docs/specs/L-1b-wrong-notes.md` (APPROVED 11-28, /pr-review 중) · 브랜치 `feature/L-1b-wrong-notes`

## 진행

- ✅ BE AC1~AC9 (0c92b46), FE AC10~AC14 (bbe812e). 에러 코드는 형식 규칙대로 `QUIZ_001`(결정로그 11-28)
- ✅ contract-checker 불일치 0, CODEOWNERS 추가. 화면 점검: 5174(FE)·8081(BE) 임시 서버, 9개 화면 라이트·다크·375px 넘침 0·콘솔 에러 0 (`.harness/l1b-final.mjs`)
- ✅ 산출물 01·02(UC34)·06(SC-27·스크린샷)·07 재생성 (93786f9, 19fc743), #250 칸 체크
- ⏳ /pr-review 2단계: verifier(all)·reviewer 실행 중 → PR 아직 없음

## 바로 다음 할 일

1. reviewer 지적 분류 → `반드시 수정`은 `fix(L-1b): ...` 커밋, 권장은 사용자에게 묻기
2. `.harness/pr-L-1b.md` 작성(템플릿, `Roadmap: L-1b`, AC 표, `node scripts/pr-screens.mjs SC-07-quiz-history SC-08-reshuffle SC-27-wrong-notes` 출력, 공통 파일 @JoonseobKim) → `node scripts/roadmap.mjs --check-body .harness/pr-L-1b.md` → 사용자 확인 뒤 `gh pr create --base develop`
3. Codex 루프: `node scripts/codex-review.mjs <PR> --request --wait --status` (background)

## 막힌 점 / 결정 필요
- 유스케이스 PNG는 PlantUML jar가 없어 `.puml`만 고침 (I-1a도 PNG 미반영)
- 사람: 노션 초대, F-05b liveTest + #226 실제 LLM 점검(12/08 전), #251 js PC `--move-env`

## 주의
- 공통 파일 → PR에서 @JoonseobKim: `ErrorCode.java`(QUIZ_001), `utils/navIcons.js`(NotebookPen), 테스트 `App.spec.js`·`navItems.spec.js` 메뉴 목록, `CODEOWNERS`. 8080·5173 옛 서버는 건드리지 않음, 8081·5174는 PR 뒤 끄기
