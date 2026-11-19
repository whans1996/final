# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 13:22 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/common-session-banner

## 현재 기능

하네스 (common) 세션 시작 배너 — `feature/common-session-banner`, **PR #38** (Codex·CI 대기, reviewer 반드시 1건 반영)

## 진행

- ✅ PR #35·#36 머지됨, secret `MATTERMOST_WEBHOOK_URL_JS` 등록 확인
- ✅ 원인: SessionStart hook이 `additionalContext`(모델 전용)만 내보내 사용자는 누구·어느 PC인지 못 봄
- ✅ `sessionBanner()`(scripts/lib/common.mjs) + hook `systemMessage`: 사람·PC·브랜치 / 환경 / ⚠️ 주의 첫 문장 / 내·상대 기능. 테스트 2개 추가(37/37), /pickup 보고 첫 줄에 사람 표시

## 바로 다음 할 일

1. 이 PR Codex COMPLETE·CI 초록 → GitHub에서 본인 머지 → `git switch develop && git pull` (새 세션 시작 때 배너가 보이는지 확인)
2. `/clear` 뒤 `/spec`으로 **공통 기반** spec (concept 도메인·Mapper·카드 DTO, ErrorCode·ActivityType 블록, 라우터·nav 분리, ConceptCard·Pagination, 관리자 레이아웃)
3. 1.4 유스케이스(마감 11/23) `docs/산출물/02-유스케이스.md`

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부, 이 PC `node scripts/setup.mjs` 전체 점검 1회

## 주의

- Gradle 테스트를 bootRun과 동시에 돌리지 않는다. 통합 테스트는 Docker Desktop 필요. V1~V5 수정 금지
- Git Bash heredoc 안의 `\n`·`\s`가 깨진다 → 정규식·이스케이프가 든 코드는 Edit 도구로 쓴다
