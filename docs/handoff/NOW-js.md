# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-29 05:51 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/NT-1-header-notifications

## 현재 기능

**NT-1 헤더 알림 기반 + 소셜 이벤트** `docs/specs/NT-1-header-notifications.md` (**APPROVED 11-29**) · 브랜치 `feature/NT-1-header-notifications` · 로드맵 NT-1 (11/28~12/02, 12/02 B→A 넘겨주기)

## 진행

- ✅ spec 승인: AC 16개(알림 API 5 · 공통 서비스 4 · 소셜 이벤트 3 · FE 4), API 4개, V18 `notifications`, 결정로그 11-29 행
- ⬜ 구현 시작 전

## 바로 다음 할 일

1. `/clear` → `/implement NT-1` (Opus/high)
2. `node scripts/next-migration.mjs`로 V18 재확인 → `V18__notifications.sql` (spec 4절 DDL)
3. red부터: `NotificationControllerTest#AC1_내_알림만_최신순으로_돌려준다` → `NotificationService.notify`(AC6·7) → 소셜 이벤트 AC10~12 → FE `NotificationBell.spec.js`
4. 12/02 넘겨주기가 빠듯하면 BE(AC1~12)를 먼저 PR로 내고 FE(AC13~16)는 뒤 PR (spec 요약에서 제안, 아직 미결)

## 막힌 점 / 결정 필요
- 없음. GMS 키 만료 12/09 18:00 → EV-2 전에 재발급·연장 확인. `.env` js-home은 `~/.vibenote/.env`로 이전 완료(11-29), js-ssafy는 남음. `feature/issue-241-fe-deps-check`(harness #241) develop 미머지

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용을 적지 않는다.** 점수·개수·SHA만
- AC12는 A 파일 `ChallengeProgressService.recount`에 한 줄, AC9는 공통 `UserWithdrawalService` → PR에서 @whans1996 멘션
