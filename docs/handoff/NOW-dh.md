# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-26 22:46 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/issue-179-harness-quick-fixes

## 현재 기능

**이슈 정리 ① 하네스 빠른 수정** (#179·#182·#213·#231, `feature/issue-179-harness-quick-fixes`, 로드맵 항목 없음). HP-3 PR #230 머지됨

## 진행

- ✅ #179 `scripts/lib/issues.mjs` 조회 상한 50 → `ISSUE_LIMIT` 200, 상한을 채우면 `truncated` → 배너 `⚠️ 200개에서 잘림`·모델 목록 경고 (session-start가 `res.truncated` 전달)
- ✅ #213 `roadmap.mjs` `normalizeItemId` → `branches.mjs`가 로드맵 ID(E-1·L-1a·HP-3)도 받음, `/spec` 스킬 설명 반영, peer.test E-1·E-10 경우
- ✅ #182 `codex-review.mjs:24` 주석·기획서 510·529줄 브랜치 보호 문구 (필수 체크 5개, Codex는 팀 규칙)
- ✅ /pr-review: verify all 통과, reviewer 반드시 0·권장 5 + 무시 가능 2 반영 (b66349b: 상한+1 판정, 상대 영역만이어도 잘림 경고, /progress·weekly 상한 1000, 기획서 33·507)
- ✅ #231 (작업 중 발견, 사용자: 새 이슈 남기지 말고 이 PR에서) `records.mjs` `undoMsysPathConv` → stamp·handoff 메모 인자 (f682294)
- PR 설명 `.harness/pr-issue-179.md` (Closes #179·#182·#213·#231). 다음: push → PR 생성

## 이슈 정리 순서 (11-26 사용자 승인)

① 지금 PR → K-3a 시드 검수(~11/28) → ② E-1 러너 #218·#219·#221·#223 (12/01 EV-1 전) → HP-4에 ④ #133·#134·#178·#196·#198·#224 흡수 → ③ A 버그 #168(500 먼저)·#200~#203·#155·#156·#150·#121, ⑤ 가드 우회 #188~#191·#193 한 PR. #163→F-27b, #204→K-1, #137→HP-5

## 바로 다음 할 일

1. push → `gh pr create --base develop --title "fix(harness): 이슈 정리 ① 조회 잘림·로드맵 ID·Git Bash 경로 변환·브랜치 보호 문구" --body-file .harness/pr-issue-179.md` → Codex 루프 → CI·Codex COMPLETE → 머지
2. 머지 뒤 `feature/K-3a-seed-check` 만들고 K-3a 시드 검수 스크립트 ①형식 ②예제 실행 (`/spec K-3a`부터, Opus/high)

## 막힌 점 / 결정 필요
- 사람: dh-home `git config user.name whans1996`, 노션 팀원 초대, F-05b liveTest
- 로드맵에 E-2(D·E 기준표·쌍 비교, 12/09 전) 행 추가 필요

## 주의
- Bash heredoc·`node -e` 안의 `\`가 줄어든다 → 역슬래시가 든 치환은 Edit 도구로 (이번에도 두 번 걸림)
- 새 follow-up은 되도록 지금 PR에서 해결 (이슈 정리 중 새 이슈를 늘리지 않는다, 11-26 사용자)
- 다음 마이그레이션은 V15
