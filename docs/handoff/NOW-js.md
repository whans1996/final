# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-19 11:48 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/common-codex-account-notice

## 현재 기능

없음 — 착수 전. B 영역: F-04 팔로우·피드, F-09 카드 목록·검색, F-10 카드 상세, F-11 리뷰, F-12 찜, F-13 관리자 카드, F-17 게시판, F-18 댓글

## 진행

- ✅ 착수 체크리스트 js-home(macOS): setup `--pc js-home` 전부 ✅, `verify all` 통과, bootRun(local) Flyway·Swagger 확인, PR #28(pcs.json 주의 사항) 머지 (11-19)
- 🟡 착수 체크리스트 js-ssafy: 진행 중 (교육장 PC Claude가 환경설정 6절대로 구성 중)
- ✅ Codex 리뷰: JoonseobKim 계정 연결·저장소 환경 생성(11-19) 후 PR #34 `@codex review` → Codex ✅ 4a5ca2c (지적 없음). Mattermost 알림 CI 단계 통과
- 🟡 PR #34 `codex-review.mjs` 계정 미연결 안내 ERROR 판정 — CI 초록, Codex ✅, 머지 대기 (#33은 환경 생성 전이라 닫음)

## 바로 다음 할 일

1. js-ssafy 착수 체크리스트 마무리 → `node scripts/setup.mjs --pc js-ssafy`, `/pickup`으로 이 파일 주입 확인
2. PR #34 머지 (하네스 변경이라 PR 본문에서 @whans1996 멘션함)
3. 첫 기능 `/spec F-09` (카드 목록·검색, W2 11/24~30). 시드 카드 85개(`R__seed_concepts.sql`)와 `concepts` 테이블(V1)이 이미 있다
4. W2 산출물: `docs/산출물/06-화면설계서.md` 초안 (B 담당, 11/24~28)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 코드(SecurityConfig, GlobalExceptionHandler, 라우터, 레이아웃, 공용 컴포넌트)를 바꾸면 PR 본문에서 @whans1996 멘션
- 마이그레이션 만들기 전 `node scripts/next-migration.mjs`. 통합 테스트는 Docker Desktop 필요
