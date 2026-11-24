# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 09:43 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/issue-95-team-challenge-date-range

---

## 현재 기능

**#95** F-20c 팀 목표 시작일 상한 (follow-up, `feature/issue-95-team-challenge-date-range`). `/pr-review` 중

## 진행

- ✅ BE 상한(오늘 + 365일) fb6acd9, 400 Swagger 설명 60a8211, FE 시작일 `:max` 2f0b2b9 (+ SC-24 화면 설계서)
- ✅ verifier 전체 통과(FE 625), contract-checker 불일치는 60a8211로 해소, reviewer 반드시 수정 0·권장 2 모두 반영
- ✅ PR 설명 `.harness/pr-issue-95.md` (Closes #95)

## 바로 다음 할 일

1. 사용자 확인 뒤 `gh pr create --base develop --title "fix(F-20c): 팀 목표 시작일 상한을 오늘 + 365일로 (#95)" --body-file .harness/pr-issue-95.md`
2. `node scripts/codex-review.mjs <PR> --request --wait --status` (백그라운드) → COMPLETE까지 루프
3. 다음 이슈: #96 (숫자 필드 타입 오류가 팀원에게 403보다 400 먼저), 그다음 #92

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일은 `docs/api/openapi.json` 설명 문구 2곳뿐 (PR에서 @whans1996 멘션)
