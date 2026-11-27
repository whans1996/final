# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 11:37 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/HP-4-verify-ci-bugs

## 현재 기능

**HP-4 하네스 PR ④ 검증·CI·버그** (로드맵 11/28~11/29, `Roadmap: HP-4`). spec `docs/specs/HP-4-verify-ci-bugs.md` APPROVED, 브랜치 `feature/HP-4-verify-ci-bugs`

## 진행

- ✅ AC1~13 전부 green (하네스 전체 테스트 통과). PR 전
- PR에 적을 것: AC5 기준에 "파일 mtime도 NOW보다 늦음" 추가(평소 흐름 오탐 방지), AC10은 head 리뷰 지적이 있으면 FINDINGS 유지(conventions 7절), AC13은 전송을 node `--send`로 옮김
- AC8 dry run: #237 브랜치 a85d6f3 → "직전 성공 f299cb5 이후 문서만 변경" 판정

## 이슈 정리 순서 (11-26 사용자 승인)

HP-4 🔨 → ③ A 버그 #168(500 먼저)·#200~#203·#155·#156·#150·#121, ⑤ 가드 우회 #188~#191·#193 한 PR. #163→F-27b, #204→K-1, #137→HP-5

## 바로 다음 할 일

1. `/clear` 후 `/pr-review` (Opus/high): Closes #165 #166 #133 #134 #178 #196 #198 #224, @JoonseobKim 멘션(ci.yml·스킬·CLAUDE.md·conventions)
2. PR 열린 뒤 NOW·작업로그만 push해 AC8 실제 확인 (Backend·Frontend skipped, 필수 체크 통과) → spec 체크리스트 칸

## 막힌 점 / 결정 필요
- 사람: 노션 팀원 초대, F-05b liveTest · 로드맵에 E-2(D·E 기준표·쌍 비교, 12/09 전) 행 추가 필요
## 주의
- 공통: `ci.yml`·CODEOWNERS·스킬·CLAUDE.md를 바꾸므로 PR에 @JoonseobKim · 다음 마이그레이션 V15
