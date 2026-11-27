# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 15:48 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/F-29b-app-shell

## 현재 기능

**F-29b 앱 셸** `docs/specs/F-29b-app-shell.md` (APPROVED, AC14 모두 green) · 브랜치 `feature/F-29b-app-shell` · **PR #247** (head b4f29a3, `Roadmap: F-29`, 김동환 @멘션) · ~12/02 넘겨주기

## 진행

- ✅ AC1~AC14 구현·테스트 (e9acb91 … 1256182), `/pr-review`: verify all 통과, reviewer 반드시 수정 1건 반영 fe99fa1 (App.vue 셸 전환을 `router.afterEach`로 → 로그아웃 시 화면 재마운트·401→/login 방지, 회귀 App.spec AC11) + 권장 4건(닉네임 메뉴 접근 이름, viewport-fit=cover, dev 갤러리 중첩 main 제거, 사이드바 z-20, FE CLAUDE.md)
- ✅ 화면 설계서 2-1절 구현 반영·스크린샷 7장 (ac88f2b), PR 본문 `.harness/pr-F-29b.md`
- ✅ Codex 1차 FINDINGS 2건 반영 b4f29a3: P1 `App.vue` afterEach에서 `failure`(취소된 이동)면 셸 유지(회귀 App.spec AC11 취소), P2 `HeaderSearch.vue` `maxlength="50"`(API @Size 50)
- 🟡 CI 재실행 중, Codex 재리뷰 1회차 RUNNING (`node scripts/codex-review.mjs 247 --request --wait --status` 백그라운드)

## 바로 다음 할 일

1. Codex 판정 확인: `node scripts/codex-review.mjs 247`. FINDINGS면 `/pr-review` 8단계(AC·보안 깨는 것만 고치고 P2는 follow-up 이슈 → `--resolve`). COMPLETE + CI 초록이면 10단계: spec 헤더 `DONE (PR #247)`·체크리스트 마지막 칸 → `docs(F-29b): 완료 표시` 커밋·push
2. 사람이 GitHub에서 #247 머지 (상대 PR이 먼저 머지되면 `git merge origin/develop` → push → CI 재확인)
3. 머지 뒤 다음 로드맵: `node scripts/roadmap.mjs --next --after F-29` (LP-1 랜딩·CD-1·V-4, 12/03~)

## 막힌 점 / 결정 필요
- 없음. 미확인: ADMIN `/admin` 화면, `/analyses/:id` 1440px 화면 (단위 테스트로만). GMS 키 만료 12/09 18:00 → EV-2 전에 재발급·연장 확인

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용을 적지 않는다.** 점수·개수·SHA만
- 셸 전환은 내비게이션 경계(afterEach)에서만: 로그인 뒤 테스트는 `router.push`까지 해야 회원 셸이 된다
- 화면(View)은 `<main>`을 쓰지 않는다. 본문 상하 여백은 각 화면 개편 PR에서
