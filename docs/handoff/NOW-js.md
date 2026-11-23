# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-23 15:49 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-20a-team

---

## 현재 기능

**F-20a 팀** (P2, F-20 팀 챌린지의 1/2) `docs/specs/F-20a-team.md` **APPROVED (김준섭 11-23)**, `feature/F-20a-team`에 승인 커밋·push. 구현 시작 전. PR #79 머지·#46·#76 닫힘 확인 완료, 내 P0 전부 DONE

## 진행

- ✅ 11-23 사용자 결정: F-20을 a(팀·초대 코드·멤버, B만)·b(공동 분석 이해도·기간 목표, A 영역 퀴즈 권한 변경 필요)로 분할, 초대 코드 8자 방식, 팀당 10명·1인 5팀·OWNER/MEMBER·위임 없음 → 결정로그 1줄 추가
- ✅ spec AC1~AC15 (BE 11: 생성·검증·내 팀·상세·가입·상한/동시 가입·수정/재발급·탈퇴/내보내기·삭제·회원 탈퇴 연동·401 / FE 4: SC-22 `/teams`·SC-23 `/teams/:id`), API 9개, `V{n}__teams.sql`(teams·team_members), 에러 `TEAM_*` 8개, Tag `팀`

## 바로 다음 할 일

1. `/clear` 후 새 세션 `/implement F-20a` (Opus/high). 시작 때 `git merge origin/develop` → `node scripts/next-migration.mjs`로 V 번호(현재 V8) 확인 → `V8__teams.sql`(spec 4절 DDL) → `team/TeamApiTest#AC1_팀을_만들면_201과_초대_코드를_받고_팀장이_된다`부터 red

## 막힌 점 / 결정 필요

- 없음

## 주의

- AC10은 공통 파일 `user/UserWithdrawalService`를 고친다 → PR에서 @whans1996 멘션. conventions 3절 `TEAM`·8절 `팀` Tag, CODEOWNERS 추가도 같은 PR
- 로컬 DB에 화면 점검용 `ui-admin@example.test`(ADMIN)·초안 카드 id 171이 남아 있다
