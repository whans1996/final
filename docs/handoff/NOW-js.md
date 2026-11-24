# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 14:14 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/issue-44-sync-tests

---

## 현재 기능

**#44 하네스 자동 동기화 다듬기** (`feature/issue-44-sync-tests`, develop 0d7d2e6에서 분기). 이슈 브랜치, 이슈 "제안"이 수용 기준

## 진행

- ✅ #77 PR #119 (`feature/issue-77-concept-link-fk`): CI 초록, Codex ✅ 750f215 → **사람이 GitHub에서 머지 대기**
- ✅ #44 d3c96bf: `sync.test.mjs` 실제 저장소 테스트 7개(ff 실패·갈라짐·detached·병합 중·autoSync off/no/0·merge.ff only/false), `autoSyncEnabled` `--type=bool`, `mergeOne` `-c merge.ff=true`, session-start "## 자동 동기화"를 Git 블록 뒤로. 하네스 테스트 93개 통과
- reviewer: 반드시 수정 없음. 권장 3건(`--ff` 명시로 `branch.<name>.mergeOptions`까지 막기, 불리언 아닌 값 경고, detached 테스트 `deepEqual(lines, [])`) 사용자 확인 중
- PR 설명 `.harness/pr-issue-44.md` (A 영역 하네스 파일 3개 → @whans1996)

## 바로 다음 할 일

1. 권장 반영 여부 결정 → push → `gh pr create --base develop --title "chore(harness): 자동 동기화 경계 테스트 추가와 설정 해석 다듬기" --body-file .harness/pr-issue-44.md` → Codex 루프
2. PR #119·#44 PR 머지는 사람이 GitHub에서
3. #77·#44 뒤에는 멈춘다 (사용자 지시 11-24). Phase 6은 나중에

## 막힌 점 / 결정 필요

- 없음

## 주의

- 최종 ERD 이미지(`diagrams/erd.png`, Workbench Reverse Engineer)는 W5에 붙인다
