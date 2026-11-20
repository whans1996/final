# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 13:59 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-17-community-board

---

## 현재 기능

F-17 커뮤니티 게시판 — spec **DONE (PR #61)**, CI 초록, **Codex ✅ 77ce03d** (이후 docs만). 사람이 GitHub에서 머지만 남음

## 진행

- ✅ develop(F-15) 합치기 5a7684d (헤더 메뉴 순서, 카드 상세 버튼 2개, conventions, openapi 재생성)
- ✅ contract-checker 0건 · reviewer 반드시 수정 0건, 권장 2건 반영 6223e8e · Codex P2 1건 반영 77ce03d (미리 채우기 중 폼 로딩)
- ✅ follow-up #60 (SC-07 "커뮤니티에 질문하기" 버튼, area:A, whans1996)

## 바로 다음 할 일

1. 사람: PR #61 머지 직전 `git fetch && git rev-list --count HEAD..origin/develop`가 0인지 확인 (아니면 `git merge origin/develop` → push → CI 초록) 후 GitHub에서 **Squash and merge**
2. 머지 뒤 `/clear` → `/pickup`. 다음 기능 후보: #57 F-12 찜 → `ReviewPlanService.addWish` 연동 (F-15 머지됨), 또는 F-18 댓글(`/spec F-18`, 패키지 `com.vibenote.post`)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 로컬 DB에 UI 점검용 f17ui@vibenote.test·글 1~3이 있다
- 동환 F-16이 `router/index.js`·헤더 nav(대시보드 order 1)를 바꾼다 → 머지 순서에 따라 `App.spec.js`·`navItems.spec.js` 헤더 기대값 충돌 가능
- `codex-review.mjs`가 Codex 실행 실패를 RUNNING으로 기다린다 (#48)
