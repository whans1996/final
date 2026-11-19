# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 11:07 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/common-harness-sync

## 현재 기능

하네스 병렬 동기화·알림·영역 경계 (common) — 구현·단위 테스트 25개 통과, 커밋 후 `/pr-review` 전

## 진행

- ✅ 상대 NOW 원격 읽기(`lib/peer.mjs` latestPeerNow, session-start 출처 표시·스냅샷), `peer-watch` UserPromptSubmit hook(10분, develop·NOW·PR 변화)
- ✅ Mattermost: `lib/notify.mjs`·`notify-message.mjs`·`.github/workflows/notify.yml` (secret 없으면 건너뜀)
- ✅ `.github/CODEOWNERS`(정본), `lib/owners.mjs`·`scripts/owners.mjs`, guard-files 상대 NOW 차단, `area-notice` PostToolUse 경고
- ✅ 스킬(implement·spec push, pr-review owners), 결정로그 3줄, conventions 9절, 환경설정 6·7절, CLAUDE.md, PR 템플릿, 라벨 area:A·B·common 생성
- ✅ develop(PR #28 js-home macOS) 합침. 김준섭 PR #28이 peer-watch 실측에서 잡힘

## 바로 다음 할 일

1. `/pr-review` → PR(`chore(harness): 병렬 동기화·알림·영역 경계`) → Codex·CI → 본인 머지. PR에 @JoonseobKim 멘션(CODEOWNERS·알림 안내)
2. 사람: `docs/환경설정.md` 7절대로 Mattermost 웹훅 → secret `MATTERMOST_WEBHOOK_URL` 등록, `.claude/settings.json` allow에 `Bash(node scripts/owners.mjs:*)` 추가(자동 모드가 막음)
3. `/clear` 뒤 `/spec`으로 **공통 기반** spec (concept 도메인·Mapper·카드 DTO, ErrorCode·ActivityType 블록, 라우터·nav 분리, ConceptCard·Pagination, 관리자 레이아웃). 결정 필요: analysis_concepts·AI 초안 형식, DTO 공유, 공개 GET, ADMIN 시드, /api/users/me/challenges, 업로드 유틸, F-01~03 유지보수, 공동 산출물 분담
4. 1.4 유스케이스(마감 11/23) `docs/산출물/02-유스케이스.md`

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부
- 김준섭 js-home은 macOS — hook은 Node라 동작하지만 첫 세션에서 peer-watch·area-notice 출력 확인 필요

## 주의

- Gradle 테스트를 bootRun과 동시에 돌리지 않는다. 통합 테스트는 Docker Desktop 필요. V1~V5 수정 금지
- 이 PC는 `node scripts/setup.mjs` 전체 점검 기록 없음
