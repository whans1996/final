# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-26 22:47 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/issue-179-harness-quick-fixes

## 현재 기능

**이슈 정리 ① 하네스 빠른 수정** (#179·#182·#213·#231, `feature/issue-179-harness-quick-fixes`, 로드맵 항목 없음). HP-3 PR #230 머지됨

## 진행

- ✅ #179 `issues.mjs` `ISSUE_LIMIT` 200(+1로 잘림 판정)·배너/목록 경고 · #213 `roadmap.mjs` `normalizeItemId` → `branches.mjs` 로드맵 ID · #182 브랜치 보호 문구 · #231 `records.mjs` `undoMsysPathConv`(stamp·handoff, 작업 중 발견해 이 PR에서)
- ✅ /pr-review: verify all·harness 통과, reviewer 반드시 0·권장 5 + 무시 가능 2 반영. PR 설명 `.harness/pr-issue-179.md` (Closes 4개), 아직 push 전

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
