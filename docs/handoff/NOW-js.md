# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 14:51 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/F-29-design-common

## 현재 기능

**F-29 디자인 공통**: PR 두 개로 나눴고 spec 2개 모두 APPROVED (11/27)
- F-29a 기반 `docs/specs/F-29a-design-foundation.md` (AC13, 이 브랜치 `feature/F-29-design-common`, ~11/30)
- F-29b 앱 셸 `docs/specs/F-29b-app-shell.md` (AC14, F-29a 머지 뒤 `feature/F-29b-app-shell`, 12/02). F-29a PR 본문은 `Roadmap: F-29 (일부)`, F-29b는 `Roadmap: F-29`

## 진행

- ✅ AC1~AC13 (597d574, e2fd902). `/pr-review` verify all 통과, reviewer 반영 1ad8592: system 테마→라이트(`FOLLOW_SYSTEM=false`, AC4 수정·결정로그·#243), h4~h6 굵기, BaseButton to+disabled, FormField required, ConfirmDialog 바깥 클릭 테스트
- ✅ `/dev/components` 스크린샷 4장 → 화면 설계서 2-1절 (765af74, push 완료)

## 바로 다음 할 일

1. **PR #244 (F-29a) CI 초록 + Codex ✅ 281ae65 → 사람이 GitHub에서 머지** (상대 PR이 먼저 머지되면 `git merge origin/develop` → push → CI 재확인). spec DONE 표시는 이미 함
2. 머지 뒤 develop에서 `feature/F-29b-app-shell` 생성 → `/implement F-29b` (`docs/specs/F-29b-app-shell.md` AC1부터, 옆 메뉴·하단 탭·`meta.nav.group`)

## 막힌 점 / 결정 필요
- 없음 (로컬 테스트 계정 `ui-f29a-…@example.com`). GMS 키 만료 12/09 18:00 → EV-2(12/09~12/12) 전에 재발급·연장 확인

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용(개념 이름·개수, 결함, forbidden, 채점 기준)을 적지 않는다.** 점수·개수·eval 커밋 SHA만
- 공통 코드 변경(PR @멘션): `main.css`, `components/common/*`, `router/index.js`(dev route), `main.js`, `index.html`, `vite.config.js`, package.json
- Figma는 공동 세션 전까지 건드리지 않는다. 김동환에게: 알림 T1 docs PR, SD-1 범위, F-29b가 A route 5개 `meta.nav`에 `group`·`icon` 추가 (미리 알림)
