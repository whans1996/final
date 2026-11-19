# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 14:37 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-00b-common-foundation

## 현재 기능

F-00b 공통 기반 — `docs/specs/F-00b-common-foundation.md` **APPROVED** (김동환 11-19), 브랜치 `feature/F-00b-common-foundation` (spec 승인 커밋만, 구현 전)

## 진행

- ✅ PR #41 머지, 이 PC `setup.mjs` 전체 점검 통과(11-19)
- ✅ spec 초안 AC1~12: concept 도메인·`ConceptQueryService`·`ConceptSummaryResponse`, ErrorCode 도메인 블록·끝 쉼표·`CONCEPT_404`, `AdminBootstrap`(`app.admin.bootstrap-email`), route 파일 분리·`meta.nav`, `AdminLayout`, `ConceptCard`·`PageNav`·`EmptyState`. 새 API·마이그레이션 없음
- ✅ 결정: ID F-00b, nav는 route `meta.nav`, 관리자는 설정값 승격 (결정로그 2줄)

## 바로 다음 할 일

1. `/clear` 뒤 `/implement F-00b` (Opus/high). 순서: BE AC5 `ErrorCodeTest` → AC1~4 concept → AC6 → FE AC7~11 → ui-verifier AC12
2. 1.4 유스케이스(마감 11/23) `docs/산출물/02-유스케이스.md`

## 막힌 점 / 결정 필요

- 김준섭: NOW-js.md 끝 `<!-- [임시] ... -->` 줄은 본인이 지울 것. "머지 금지" PR 가드 필요 여부 결정. F-09 spec APPROVED(`feature/F-09-concept-list`), 구현은 F-00b 머지 뒤. F-09 spec 3절 `ConceptSummaryResponse` 필드 = F-00b AC4와 일치(확인함)
- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부

## 주의

- Gradle 테스트를 bootRun과 동시에 돌리지 않는다. 통합 테스트는 Docker Desktop 필요. V1~V5 수정 금지
- Git Bash heredoc·sed에서 `\n`·`\s`·`\|`가 깨진다 → 정규식·이스케이프·표(`|`)가 든 수정은 Edit 도구로 쓴다
