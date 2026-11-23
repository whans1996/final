# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-23 16:09 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-20a-team

---

## 현재 기능

**F-20a 팀** (P2) `docs/specs/F-20a-team.md` APPROVED. **AC1~AC15 전부 green**, 구현 마무리 완료 → 다음은 `/pr-review`

## 진행

- ✅ BE AC1~AC11 (0fa7c27): `V8__teams.sql`, `team` 패키지, `ErrorCode` TEAM_* 8개, 탈퇴 연동(AC10), openapi.json(팀 path만 추가, 메서드명 `createTeam` 등으로 operationId 충돌 회피), 메타 테스트에 TEAM
- ✅ FE AC12~AC15 (bea4a16): `api/teams.js`, `routes/team.js`+`router/index.js`, `views/team/*`, `components/team/*`, 헤더 메뉴 테스트에 "팀"
- ✅ contract-checker 불일치 0 / ui-verifier 문제 0 (5화면 × 1280·375, 가로 스크롤 없음)
- ✅ 산출물: 화면 설계서 SC-22·23(와이어프레임+스크린샷), ERD teams·team_members, 요구사항 정의서 F-20 행(a·b)

## 바로 다음 할 일

1. `/clear` 후 새 세션 `/pr-review` (Opus/high): verifier all → reviewer → PR 본문에 @whans1996 멘션(공통 파일) → Codex 루프
2. 머지 후 spec 상태 DONE, F-20b `/spec` (A 영역 퀴즈·분석 권한 변경 필요 → 상대와 협의)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경: `UserWithdrawalService`·`UserController`(탈퇴 설명)·`ErrorCode`·conventions·`router/index.js`·CODEOWNERS → PR에서 @whans1996 멘션
- 8080(옛 빌드 BE, 14:09 시작)·5173은 이 세션이 끄지 못했다(권한 거부). 화면 점검은 18080 BE + 5180 Vite(스크래치 설정)로 했다. 로컬 DB에 V8 적용됨, 점검용 계정 `ui-team-owner@`·`ui-team-member@example.test`(비번 vibe1234)·팀 id 1
- 로컬 DB에 `ui-admin@example.test`(ADMIN)·초안 카드 id 171도 남아 있다
