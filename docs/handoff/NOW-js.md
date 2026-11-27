# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 14:30 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/F-29-design-common

## 현재 기능

**F-29 디자인 공통**: PR 두 개로 나눴고 spec 2개 모두 APPROVED (11/27)
- F-29a 기반 `docs/specs/F-29a-design-foundation.md` (AC13, 이 브랜치 `feature/F-29-design-common`, ~11/30)
- F-29b 앱 셸 `docs/specs/F-29b-app-shell.md` (AC14, F-29a 머지 뒤 `feature/F-29b-app-shell`, 12/02). F-29a PR 본문은 `Roadmap: F-29 (일부)`, F-29b는 `Roadmap: F-29`

## 진행

- ✅ AC1~AC13 (597d574, e2fd902). `/pr-review` verify all 통과, reviewer 반영 1ad8592: system 테마→라이트(`FOLLOW_SYSTEM=false`, AC4 수정·결정로그·#243), h4~h6 굵기, BaseButton to+disabled, FormField required, ConfirmDialog 바깥 클릭 테스트
- ✅ `/dev/components` 스크린샷 4장 → 화면 설계서 2-1절 (765af74, push 완료)

## 바로 다음 할 일

1. (PR 설명 `.harness/pr-F-29a.md` 준비 완료·사용자 확인 대기) `gh pr create --base develop --title "feat(F-29a): Tailwind v4 토큰·다크 테마·공통 컴포넌트 7개" --body-file .harness/pr-F-29a.md`
2. `gh pr checks` 확인 → `node scripts/codex-review.mjs <PR> --request --wait --status` (background)로 Codex 루프
3. CI 초록 + Codex COMPLETE가 되면 spec 헤더 `DONE (PR #n)`과 체크리스트 마지막 칸을 고쳐 한 커밋으로 push, 머지는 사람이 함

## 막힌 점 / 결정 필요
- 없음 (로컬 테스트 계정 `ui-f29a-…@example.com`). GMS 키 만료 12/09 18:00 → EV-2(12/09~12/12) 전에 재발급·연장 확인

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용(개념 이름·개수, 결함, forbidden, 채점 기준)을 적지 않는다.** 점수·개수·eval 커밋 SHA만
- 공통 코드 변경(PR @멘션): `main.css`, `components/common/*`, `router/index.js`(dev route), `main.js`, `index.html`, `vite.config.js`, package.json
- Figma는 공동 세션 전까지 건드리지 않는다. 김동환에게: 알림 T1 docs PR, SD-1 범위, F-29b가 A route 5개 `meta.nav`에 `group`·`icon` 추가 (미리 알림)
