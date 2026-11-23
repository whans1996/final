# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-23 16:00 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-20a-team

---

## 현재 기능

**F-20a 팀** (P2) `docs/specs/F-20a-team.md` APPROVED, `/implement` 진행 중. 이번 세션 목표: BE AC1~AC11 → FE AC12~AC15

## 진행

- ✅ BE AC1~AC11 green: `V8__teams.sql`, `team` 패키지(Controller·Service·Mapper·InviteCodeGenerator·dto·domain), `ErrorCode` TEAM_* 8개, `UserWithdrawalService`가 `TeamService.removeAllOf` 호출(AC10), openapi.json 갱신(팀 path만 추가, operationId 충돌 피하려고 메서드명 `createTeam` 등)
- ✅ 테스트: `team/TeamApiTest`·`TeamServiceTest`(코드 충돌 재시도)·`InviteCodeGeneratorTest`·`TeamJoinConcurrencyTest`, `user/WithdrawalApiTest#AC10_*`, 메타 테스트(FlywayMigrationTest·ErrorCodeTest)에 TEAM 추가
- ✅ conventions 3절 TEAM·8절 `팀` Tag, CODEOWNERS team 경로
- ✅ FE AC12~AC15 green: `api/teams.js`, `routes/team.js`(+`router/index.js` 합치기), `views/team/TeamListView`·`TeamDetailView`, `components/team/TeamCard`·`TeamForm`·`InviteCodeBox`·`TeamMemberList`·`teamLabels.js`, 헤더 메뉴 테스트(App·navItems)에 "팀" 추가

## 바로 다음 할 일

1. contract-checker (spec 3절 ↔ openapi.json ↔ `src/api/teams.js`)
2. BE(local)·FE dev 서버 띄우고 ui-verifier: `/teams`·`/teams/{id}` 1280·375 (헤더 메뉴 9개라 375 가로 넘침 주의)
3. 산출물: `docs/산출물/` 화면 설계서 SC-22·23, ERD(teams·team_members), 요구사항 정의서 F-20 행 → `/pr-review`

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경: `UserWithdrawalService`·`UserController`(탈퇴 설명)·`ErrorCode`·conventions·`router/index.js` → PR에서 @whans1996 멘션
- 로컬 DB에 화면 점검용 `ui-admin@example.test`(ADMIN)·초안 카드 id 171이 남아 있다
