# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 16:41 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-18-comments

---

## 현재 기능

F-18 댓글 — **PR #68** 머지 대기 (CI 초록, Codex ✅ 50e8608, spec DONE)

## 진행

- ✅ BE AC1~10, FE AC11~13, 산출물 UC41·SC-15, ui-verifier
- ✅ /pr-review: verifier(BE 617·FE 419), contract-checker 0, reviewer 반드시 수정 0, 권장 3건 50e8608
- ✅ PR #68 CI 초록, Codex COMPLETE 50e8608 (재리뷰 1회), 완료 표시 커밋

## 바로 다음 할 일

1. 사람: PR #68을 GitHub에서 머지 (wip 없음, 일반 머지 가능). 그 전에 상대 PR이 develop에 먼저 머지됐으면 `git merge origin/develop` → push → CI 재확인
2. 머지 후 새 세션 `/pickup` → 다음 B 영역 기능 또는 내 이슈(#46·#51·#64) 선택

## 막힌 점 / 결정 필요

- 없음

## 주의

- `codex-review.mjs`가 Codex 실행 실패를 RUNNING으로 기다린다 (#48)
