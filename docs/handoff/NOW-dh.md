# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신: 2026-11-24 13:52 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-21a-structure-graph

## 현재 기능

**F-21a 구조 시각화: 파일 의존 그래프** (P1) spec `docs/specs/F-21a-structure-graph.md` — APPROVED. AC1~AC17 green, `/pr-review` 중: 리뷰 반영 완료, PR 생성 확인 대기

## 진행

- ✅ BE AC1~AC13, FE AC14~AC17, contract-checker 불일치 0, 화면 점검·산출물(06 SC-07 구조 탭·스크린샷, 01, README)
- ✅ `/pr-review`: verify all 통과. reviewer 반드시 4건(계산량: 자기 참조 const 지수 계산, config 순환 StackOverflow, 정규식·`**` 백트래킹, 1MB 파일 제곱 경로) → 실패 테스트 11건 red 확인 후 `412c2f8`에서 상한·색인으로 수정. 권장 R1(계산을 트랜잭션 밖으로)·R3(XML 엔티티 10만 자)·R4(excerpt `"\\s+"`)·R5(mermaid `%`→`#37;`)·R6(spec 07 문구) 반영, R2 캐시 → #117. spec 5.1·결정로그에 상한 수치 기록
- PR 설명 `.harness/pr-F-21a.md` 작성 (공통 파일 build.gradle·application.yml·package.json·openapi.json, @JoonseobKim 멘션)

## 바로 다음 할 일

1. 사용자 확인 후 `gh pr create --base develop --title "feat(F-21a): 구조 탭 파일 의존 그래프" --body-file .harness/pr-F-21a.md`
2. `gh pr checks` 확인, `node scripts/codex-review.mjs <PR> --request --wait --status`(background)로 Codex COMPLETE까지 루프
3. COMPLETE + CI 초록이면 `docs(F-21a): 완료 표시 (spec DONE)` → 사람이 GitHub에서 머지

## 막힌 점 / 결정 필요

- 사람: 노션 팀원 초대, #77 담당(F-08 A·F-13 B 걸침)

## 주의

- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시) · 하네스 동결(11-23): 버그·작업 막는 문제만 고친다
- 이 PC의 Git Bash heredoc에서 python `'''` 문자열의 백슬래시가 한 번 풀린다 → Java 편집은 Write로 만든 스크립트 파일(raw 문자열)로
- 내 영역 이슈 #104·#105(F-25 집계)는 F-21 뒤 기능 사이에 처리
