# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 10:26 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/K-3a-seed-check

## 현재 기능

**K-3a 시드 카드 원본 형식 + 검수 ①형식 ②예제 실행** (`docs/specs/K-3a-seed-check.md`, DONE (PR #234), `Roadmap: K-3a`, 11/26~11/28)

## 진행

- ✅ AC1~AC11 구현, /pr-review에서 verifier·reviewer 지적(반드시 3·권장 전부, 결정로그) 반영. 85장 오류 0·경고 437, 수정 카드 1건(navigation-guards)
- ✅ **PR #234 CI 초록 + Codex ✅ eaf14e7** (Codex P2 3건 반영: vue script·setup 경로, js checker 문법, vue 매크로 실제 타입). 완료 표시 커밋 완료 → **머지 대기 (GitHub에서 직접)**
- ✅ **PR #236** (`feature/issue-235-codex-head-lag`, Closes #235, push 직후 codex-review가 이전 head로 판정하던 하네스 버그): Codex ✅ 0c63e83, Backend CI 대기

## 이슈 정리 순서 (11-26 사용자 승인)

① ✅ → K-3a ✅(PR #234) → ② E-1 러너 #218·#219·#221·#223 (12/01 EV-1 전) → HP-4에 ④ #133·#134·#178·#196·#198·#224 흡수 → ③ A 버그 #168(500 먼저)·#200~#203·#155·#156·#150·#121, ⑤ 가드 우회 #188~#191·#193 한 PR. #163→F-27b, #204→K-1, #137→HP-5

## 바로 다음 할 일

1. 사람: PR #234 GitHub에서 머지 (상대 PR이 먼저 머지됐으면 `git merge origin/develop` → push로 CI 재확인)
2. PR #236: `gh pr checks 236`로 Backend 초록 확인 → GitHub에서 머지
3. 11/29부터 HP-4 (#165·#166 + ④ 묶음)

## 막힌 점 / 결정 필요
- 사람: 노션 팀원 초대, F-05b liveTest · 로드맵에 E-2(D·E 기준표·쌍 비교, 12/09 전) 행 추가 필요
## 주의
- 공통: `R__seed_concepts.sql`(생성본)·`SeedConceptsTest`·CODEOWNERS(`/data/seed/`·`/backend/src/test/resources/seed/` A)·`frontend/package.json`·`ci.yml` → PR에 @JoonseobKim
- Bash heredoc·`node -e` 안의 `\`가 줄어든다 → 역슬래시가 든 내용은 Write·Edit 도구로 · 다음 마이그레이션 V15
