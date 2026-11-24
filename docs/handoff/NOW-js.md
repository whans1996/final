# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 14:23 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/issue-44-sync-tests

---

## 현재 기능

**#44 하네스 자동 동기화 다듬기** (`feature/issue-44-sync-tests`, develop 합침). 이슈 브랜치, 이슈 "제안"이 수용 기준

## 진행

- ✅ #77 PR #119 머지 (11-24), #77 닫힘
- ✅ #44 d3c96bf: `sync.test.mjs` 실제 저장소 테스트 7개(ff 실패·갈라짐·detached·병합 중·autoSync off/no/0·merge.ff only/false), `autoSyncEnabled` `--type=bool`, session-start "## 자동 동기화"를 Git 블록 뒤로. 하네스 테스트 93개 통과
- ✅ reviewer 권장 반영: `mergeOne`에 `--ff` 명시(`merge.ff`·`branch.<name>.mergeOptions`보다 우선), detached 테스트 `deepEqual(lines, [])`. 불리언 아닌 값 경고는 하지 않음(사용자 결정)
- PR 설명 `.harness/pr-issue-44.md` (A 영역 하네스 파일 3개 → @whans1996)

## 바로 다음 할 일

1. #44 PR #120: Codex ✅ f1ade0d, Backend CI 진행 중(나머지 초록) → CI 초록 확인 후 사람이 GitHub에서 머지 (머지되면 #44는 close-issues가 닫음)
2. #77·#44 뒤에는 멈춘다 (사용자 지시 11-24). 남은 후보: WBS 1.8 화면 설계서 챌린지 SC-12·13·21(기한 11/28), Phase 6은 나중에

## 막힌 점 / 결정 필요

- 없음

## 주의

- 최종 ERD 이미지(`diagrams/erd.png`, Workbench Reverse Engineer)는 W5에 붙인다
