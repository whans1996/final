# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-19 16:27 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-09-concept-list

## 현재 기능

F-09 개념 카드 목록·검색 — **PR #47 머지 대기** (CI 초록, Codex ✅ `51fa2f3`, spec DONE `d8186cf`). 사람이 GitHub에서 머지. 하네스 자동 동기화 **PR #45는 11-19 develop 머지됨** (이 브랜치에도 합침)

## 진행

- ✅ F-09 `/pr-review`: verifier all 통과, contract-checker 0건, reviewer 반드시 수정 0건. 권장 반영: 공개 범위 401 테스트 `ConceptListApiTest#AC1_공개는_GET_목록뿐이고_POST는_토큰_없으면_401` (`8dda4bf`), PR 본문에 F-10 spec·B 화면 초안 포함 명시. 권장 1(범위 밖 page UX) → #46
- ✅ 사용자 결정(11-19 "다 진행해"): 헤더 nav "개념 카드" order 5, 375px "전체" select 2개는 현재 구현대로
- ⚠️ Codex 첫 리뷰(`8dda4bf`, PR 생성 시)가 Codex 쪽 오류로 `status: failed` → `@codex review` 재요청 후 COMPLETE. `codex-review.mjs`가 `failed`를 RUNNING으로 보고 계속 기다리는 문제 → #48

## 바로 다음 할 일

1. PR #47 머지 확인(`gh pr view 47 --json state`) → `git switch develop && git pull`
2. 김동환에게: F-08 브랜치에서 `git merge origin/develop` 1회 하면 이후 자동 동기화가 돈다
3. 다음 기능: F-10 개념 카드 상세 (`docs/specs/F-10-concept-detail.md` APPROVED) → `/implement F-10` (Opus/high 새 세션)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 통합 테스트는 Docker Desktop 필요. F-09는 마이그레이션 없음
- 로컬 `feature/common-harness-auto-merge`(머지 완료)는 지워도 된다 (push 안 한 NOW 커밋 `96ef5f0`만 남음)
