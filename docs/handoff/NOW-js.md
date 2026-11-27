# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 13:30 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/F-29-design-common

## 현재 기능

**F-29 디자인 공통** — 두 PR로 나눔 (결정로그 11-27 마지막 줄). spec 2개 **APPROVED (11/27)**
- F-29a 기반 `docs/specs/F-29a-design-foundation.md` (AC13, 이 브랜치 `feature/F-29-design-common`, ~11/30)
- F-29b 앱 셸 `docs/specs/F-29b-app-shell.md` (AC14, F-29a 머지 뒤 `feature/F-29b-app-shell`, 12/02). 둘 다 PR 본문 `Roadmap: F-29`

## 진행

- ✅ Figma 세션 결정 5줄·EV-1(#240) 끝. spec 질문 답: 두 PR 분할, preflight + `@layer base` 보정·기존 변수 별칭, 하단 탭 '분석' = `/analyses`, 알림은 `#notifications` 슬롯만
- 🟡 spec 초안 작성 완료 (커밋 전). 다크 토큰 값은 F-29a 5.2절에서 새로 정함 (결정로그에 없음)

## 바로 다음 할 일

1. (완료) spec 승인 커밋·push
2. 새 세션(Opus/high) `/implement F-29a`: `npm i tailwindcss @tailwindcss/vite reka-ui lucide-vue-next pretendard @fontsource-variable/jetbrains-mono` → AC별 red 테스트(`useTheme.spec.js`, `components/common/__tests__/BaseButton.spec.js` …) → `main.css` `@theme` 토큰
3. Figma 프레임(토큰·컴포넌트 7개·셸 3폭)은 spec과 병행. 값이 어긋나면 spec 5.2절 표를 고친다
4. 김동환에게: 알림 T1 로드맵·기획서 docs PR, SD-1 범위 반영, F-29b가 A route 파일 5개 `meta.nav`에 `group`·`icon`을 더함 (미리 알림)

## 막힌 점 / 결정 필요
- GMS 키 만료 12/09 18:00 → EV-2(12/09~12/12) 전에 재발급·연장 확인

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용(개념 이름·개수, 결함, forbidden, 채점 기준)을 적지 않는다.** 점수·개수·eval 커밋 SHA만
- **Figma는 공동 세션 전까지 건드리지 않는다** (김동환 요청)
