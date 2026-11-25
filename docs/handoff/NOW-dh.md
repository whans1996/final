# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 10:56 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/common-harness-revamp

## 현재 기능

**H-1 하네스 개편** (WBS 6.1, 11/26~28, 기획서 6절 H-1). spec 없음(하네스): 기획서 H-1 행이 범위다. 단위마다 커밋하고, 끝나면 PR 1개(`Closes #29 #129`) → 하네스 재동결.

## 진행

- ✅ ① #129 docs만 바꾼 PR은 codex-loop 건너뜀 (`loopSkipReason`, 3d57f6b)
- ✅ ② 커밋 hook 빠른 검사(`verify.mjs --staged`, `scripts/lib/related.mjs`) + `.githooks/pre-push` 전체 검증 + `.harness/gradle.lock`(`scripts/lib/lock.mjs`) + `.harness/verified.json` 재사용 (d78a032, push 확인함)
- ⬜ ③ `.claude/hooks/guard-bash.mjs` 셸 토큰 파서 (따옴표·`&&`·`;`·`|` 분리, cd 추적) + 테스트 `scripts/test/guard-bash.test.mjs`
- ⬜ ④ `scripts/stamp.mjs --draft`: NOW·작업로그·진행현황 자동 초안 + 메모 3줄
- ⬜ ⑤ `scripts/doctor.mjs` ⑥ 안전 명령 허용 목록(fewer-permission-prompts 스킬) ⑦ 주간 지표(Codex 라운드·가드 차단·토큰) ⑧ #29 owners·peer ⑨ 결정로그·README·환경설정 문서

## 바로 다음 할 일

1. ③ guard-bash: 규칙을 `scripts/lib/guard.mjs`(순수 함수 `checkCommand(cmd, {branchOf})`)로 옮기고 테스트 먼저. 지금 정규식의 오탐(따옴표 안·heredoc)과 누락(`cd a && git -C b commit`)을 테스트 케이스로
2. 이어서 ④~⑨, 마지막에 `/pr-review`

## 막힌 점 / 결정 필요

- 사람: GMS 키 수령(12/01 전), 노션 팀원 초대, F-05b liveTest, 제출 zip 이름(지역·반)

## 주의

- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20) · 하네스는 H-1 동안만 동결을 해제하고, 끝나면 다시 동결한다
- Codex: 3회차부터 P2는 follow-up 이관 (11-25 결정) · 진행현황·WBS·간트는 `/progress` docs PR에서만
- 이 PC의 Git Bash heredoc에서 python `'''`가 깨진다 → 편집은 Edit 도구나 Write로 만든 스크립트 파일로
