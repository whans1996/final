# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 13:48 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/common-issue-loop

## 현재 기능

하네스 (common) follow-up 이슈 순환 — `feature/common-issue-loop` (커밋 c116f4c, PR 전)

## 진행

- ✅ PR #38(세션 배너) 머지됨
- ✅ `scripts/lib/issues.mjs`(분류·배너·`closingRefs`) + SessionStart 📌 줄·이슈 섹션, `close-issues.yml`, 이슈·PR 템플릿, pickup·spec·progress·pr-review·handoff 스킬, conventions 7·9절, 결정로그. 하네스 테스트 43/43
- ⛔ `.claude/settings.json`에 `gh issue edit`·`gh issue close` 허용 추가는 auto mode 분류기가 막음 → 사람이 직접 추가

## 바로 다음 할 일

1. `/pr-review`로 reviewer → PR (본문 @JoonseobKim 멘션, 공통 코드) → Codex 루프 → 머지. 머지 뒤 close-issues 워크플로가 도는지 확인
2. (완료) 기존 이슈 8개 분류: #16·#23·#24·#26 area:A, #17·#29·#32·#39 area:common, 담당 whans1996
3. `/clear` 뒤 `/spec`으로 **공통 기반** spec (concept 도메인·Mapper·카드 DTO, ErrorCode·ActivityType 블록, 라우터·nav 분리, ConceptCard·Pagination, 관리자 레이아웃)
4. 1.4 유스케이스(마감 11/23) `docs/산출물/02-유스케이스.md`

## 막힌 점 / 결정 필요

- 사람: settings.json 허용 목록에 `Bash(gh issue edit:*)`·`Bash(gh issue close:*)` 추가, 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부, 이 PC `node scripts/setup.mjs` 전체 점검 1회

## 주의

- Gradle 테스트를 bootRun과 동시에 돌리지 않는다. 통합 테스트는 Docker Desktop 필요. V1~V5 수정 금지
- Git Bash heredoc 안의 `\n`·`\s`가 깨진다 → 정규식·이스케이프가 든 코드는 Edit 도구로 쓴다
