# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-24 13:56 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-21a-structure-graph

## 현재 기능

**F-21a 구조 시각화: 파일 의존 그래프** (P1) spec `docs/specs/F-21a-structure-graph.md` — APPROVED. PR #118 열림, CI·Codex 확인 중

## 진행

- ✅ BE AC1~AC13, FE AC14~AC17, contract-checker 불일치 0, 화면 점검·산출물(06 SC-07 구조 탭·스크린샷, 01, README)
- ✅ `/pr-review`: verify all 통과. reviewer 반드시 4건(자기 참조 const 지수 계산, config 순환 StackOverflow, 정규식·`**` 백트래킹, 1MB 파일 제곱 경로) → 실패 테스트 11건 red 확인 후 `412c2f8`에서 상한·색인으로 수정. 권장 R1·R3~R6 반영, R2 캐시 → #117
- PR #118 (`.harness/pr-F-21a.md`, @JoonseobKim 멘션). 첫 CI에서 하네스 테스트가 NOW 헤더(`갱신:` 줄이 `> "` 뒤에 붙음)로 실패 → 헤더 복구

## 바로 다음 할 일

1. `gh pr checks 118` 초록 확인. Codex: `node scripts/codex-review.mjs 118 --request --wait --status`(background)로 COMPLETE까지 루프
2. COMPLETE + CI 초록이면 `docs(F-21a): 완료 표시 (spec DONE)` → 사람이 GitHub에서 머지

## 막힌 점 / 결정 필요

- 사람: 노션 팀원 초대, #77 담당(F-08 A·F-13 B 걸침)

## 주의

- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시) · 하네스 동결(11-23): 버그·작업 막는 문제만 고친다
- 이 PC의 Git Bash heredoc에서 python `'''` 문자열의 백슬래시가 한 번 풀린다 → Java 편집은 Write로 만든 스크립트 파일(raw 문자열)로
- 내 영역 이슈 #104·#105(F-25 집계)는 F-21 뒤 기능 사이에 처리
