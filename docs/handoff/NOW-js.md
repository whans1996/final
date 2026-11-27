# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 13:52 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/F-29-design-common

## 현재 기능

**F-29 디자인 공통** — 두 PR. spec 2개 APPROVED (11/27)
- F-29a 기반 `docs/specs/F-29a-design-foundation.md` (AC13, 이 브랜치 `feature/F-29-design-common`, ~11/30)
- F-29b 앱 셸 `docs/specs/F-29b-app-shell.md` (AC14, F-29a 머지 뒤 `feature/F-29b-app-shell`, 12/02). 둘 다 `Roadmap: F-29`

## 진행

- ✅ AC1·AC2·AC4~AC12 (597d574): Tailwind v4 `main.css` 토큰·별칭·base 보정·폰트, `useTheme`+`index.html` 스크립트, 공통 컴포넌트 7개, EmptyState·PageNav 이전(`.empty`·`.pager` 클래스 유지 — AdminChallengeListView 테스트가 씀), `/dev/components`, NOTICE, `frontend/CLAUDE.md`. FE verify 통과. 토큰 조정: 라이트 success `#3a7535`, `on-solid` 추가(spec 5.2절)
- ✅ AC3·AC5·AC13 화면 (ui-verifier, BE 없이): `/dev/components` 라이트·다크×1280·375 문제 0, `/`·`/login`·`/signup`·`/concepts`·`/community` develop과 색·폰트만 다름

## 바로 다음 할 일

1. (BE를 띄울 수 있으면) 데이터가 있는 `/concepts`·`/community`·`/analyses/:id`도 develop과 비교
2. `/pr-review` → PR (`Roadmap: F-29`, 공통 컴포넌트·main.css·router/index.js 변경이라 @whans1996 멘션)

## 막힌 점 / 결정 필요
- js-ssafy BE `bootRun` 실패: MySQL `Access denied for user 'vibenote'@'localhost'` → 로컬 DB 계정·`.env` 비밀번호 확인 필요 (사람)
- GMS 키 만료 12/09 18:00 → EV-2(12/09~12/12) 전에 재발급·연장 확인

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용(개념 이름·개수, 결함, forbidden, 채점 기준)을 적지 않는다.** 점수·개수·eval 커밋 SHA만
- 공통 코드 변경(PR @멘션): `main.css`, `components/common/*`, `router/index.js`(dev route), `main.js`, `index.html`, `vite.config.js`, package.json
- Figma는 공동 세션 전까지 건드리지 않는다. 김동환에게: 알림 T1 docs PR, SD-1 범위, F-29b가 A route 5개 `meta.nav`에 `group`·`icon` 추가 (미리 알림)
