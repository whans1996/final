# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/common-fe-tooling-upgrade

## 현재 기능

환경 최신화 PR ② (FE 도구·하네스·문서). PR ① #12(BE Boot 4.1·Java 21)는 11-18 develop 머지 완료.

## 진행

- ✅ FE: vitest 5.1·jsdom 31·oxlint/eslint-plugin-oxlint 1.94·eslint-plugin-vue 10.12·prettier 3.10.1 고정. verify fe 통과(76 테스트), 설정 변경 불필요
- ✅ setup.mjs JDK 안내, CLAUDE.md·backend/CLAUDE.md(Boot 4 채택, Jackson 3 import 규칙, JSON 컬럼 null 필드 래퍼)·README·환경설정.md 1절·5절·기획서 202행 + 결정 로그, 작업로그
- ⏳ `/pr-review` → PR ② 생성

## 바로 다음 할 일

1. `/pr-review` (reviewer, ui-check `/`), PR 본문 후속 작업: 시드 카드 Boot 3 테스트 API·"빌드는 17" 문구 갱신, Lombok 제거 검토, npm audit 19건(업그레이드 전과 동일, picomatch 수정 버전 없음)
2. (사람) PR ② 머지 → `/clear` → F-05 분석 요청 `/spec`(A), F-09·F-10 `/spec`(B, category 필터 6종)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 집 PC: `git pull` → `node -v`가 24.15 미만이면 Node 24 최신 설치 → `node scripts/setup.mjs` → `cd frontend && npm install` → 첫 BE 빌드에서 JDK 21 자동 다운로드(몇 분). STS는 JDK 21 설치 필요
- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금). V1·V2 수정 금지
