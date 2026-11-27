# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 11:30 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/HP-4-verify-ci-bugs

## 현재 기능

**HP-4 하네스 PR ④ 검증·CI·버그** (로드맵 11/28~11/29, `Roadmap: HP-4`). spec `docs/specs/HP-4-verify-ci-bugs.md` APPROVED, 브랜치 `feature/HP-4-verify-ci-bugs`

## 진행

- ✅ AC1 `verify be --tests` · AC2 시작 HEAD 기준 기록(#133) · AC3 설정·NOW → harness 영역 · AC4 `stamp --write-now`
- ✅ AC5 lib/checkpoint.mjs (커밋 후 미기록: 파일 mtime도 NOW보다 늦을 때만. 평소 흐름 오탐 방지, PR에 적기) · AC6 merge-base
- ✅ AC7 ci.yml be·fe 필터 = import 닫힘(ci-filter.test) · AC8 `scripts/ci-changes.mjs`(#237 브랜치 dry run: f299cb5 이후 문서만 판정) · AC9 pr-review 문구
- ⬜ AC10~13 Codex·알림

## 이슈 정리 순서 (11-26 사용자 승인)

HP-4 🔨 → ③ A 버그 #168(500 먼저)·#200~#203·#155·#156·#150·#121, ⑤ 가드 우회 #188~#191·#193 한 PR. #163→F-27b, #204→K-1, #137→HP-5

## 바로 다음 할 일

1. AC10 (#165): `scripts/lib/codex.mjs` 판정에 "PR 전체(merge-base 대비)가 문서만이면 COMPLETE 문서만 PR, Codex 생략" + `codex-status.yml` success. `codex.test.mjs` red부터
2. AC11 (#198) 영수증 B 이후 docs만 · AC12 (#178) `.claude/hooks/codex-loop.mjs` `skipRecheck` 원격 sha · AC13 (#196) `roadmap.yml` 전송 실패 시 exit 1
3. 그 뒤 PR에서 AC8 실제 확인(docs만 push → Backend·Frontend skipped)

## 막힌 점 / 결정 필요
- 사람: 노션 팀원 초대, F-05b liveTest · 로드맵에 E-2(D·E 기준표·쌍 비교, 12/09 전) 행 추가 필요
## 주의
- 공통: `ci.yml`·CODEOWNERS·스킬·CLAUDE.md를 바꾸므로 PR에 @JoonseobKim
- Bash heredoc·`node -e` 안의 `\`가 줄어든다 → 역슬래시가 든 내용은 Write·Edit 도구로 · 다음 마이그레이션 V15
