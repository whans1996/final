# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 14:53 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-00b-common-foundation

## 현재 기능

F-00b 공통 기반 — `docs/specs/F-00b-common-foundation.md` APPROVED, `/implement` 중. AC1~12 모두 green (AC12 ui-verifier 1440·375px 문제 0건)

## 진행

- ✅ BE AC1~6 (`a637dbd`): concept 도메인·`ConceptQueryService`·`ConceptSummaryResponse`, ErrorCode·ActivityType·`PUBLIC_GET_PATHS` 도메인 블록, `AdminBootstrap`. 로컬 실기동으로 WARN·승격 둘 다 확인
- ✅ FE AC7~11 (`58b4a28`): `router/routes/*.js`, `utils/navItems.js`, `AdminLayout`, `ConceptCard`·`PageNav`·`EmptyState`. BE·FE verify 통과
- ✅ 문서 (`fd38f99`): CODEOWNERS, conventions 9절, frontend/CLAUDE.md, 환경설정 3절, 화면 설계서
- ✅ 기존 간헐 실패 수정 (`48ac033`): `AnalysisTestSupport.zip()` 엔트리 시각 고정 (2초 경계에서 `AnalysisControllerTest` AC1 실패)

## 바로 다음 할 일

1. `/clear` 뒤 `/pr-review` (Opus/high) → PR 본문에 `@JoonseobKim` 멘션 (공통 코드 전부. `node scripts/owners.mjs --diff origin/develop`)
2. 1.4 유스케이스(마감 11/23) `docs/산출물/02-유스케이스.md`

## 막힌 점 / 결정 필요

- ui 점검 계정 `ui-admin@vibe.dev`/`vibe1234`(로컬 DB에만, ADMIN). storageState `.harness/ui-admin.json`. 분석 0건이라 PageNav 실화면은 단위 테스트로만 확인
- 김준섭: F-09는 F-00b 머지 뒤 시작. `ConceptSummaryResponse` 필드 = F-09 spec 3절. 헤더 메뉴 `show`는 guest·user·admin 3종뿐이라 비로그인에게도 보일 "개념 카드" 메뉴는 F-09에서 `show: 'all'` 추가가 필요할 수 있음
- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부

## 주의

- Gradle 테스트를 bootRun과 동시에 돌리지 않는다. 통합 테스트는 Docker Desktop 필요. V1~V5 수정 금지
- Git Bash heredoc·sed에서 `\n`·`\s`·`\|`가 깨진다 → 정규식·이스케이프·표(`|`)가 든 수정은 Edit 도구로 쓴다. 긴 heredoc 여러 개를 한 명령에 넣으면 따옴표 파싱이 깨진다 (파일마다 Write)
