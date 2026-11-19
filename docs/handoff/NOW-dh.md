# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-19 09:53 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-07-execution-flow

## 현재 기능

F-07 실행 흐름 — **PR #25 열림, develop 충돌(기획서 결정 로그) 해소, CI 초록, Codex ✅ 1243034 (P2 1건 → #26), spec DONE 표시 완료. 머지 대기**

## 진행

- ✅ /pr-review: verifier 통과(BE 316+·FE 151), contract-checker 불일치 0, reviewer 반드시 2·권장 4 반영(`a8ddb15`, `1243034`)
- ✅ 후속 이슈: #23 프롬프트 구분자 주입, #24 흐름이 분석 실행기 독점, #26 endpoint UNIQUE 대소문자
- ✅ 완료 표시: spec DONE, WBS 3.11·간트 t311/a311 실적, 진행현황 F-07 행

## 바로 다음 할 일

1. 영역 오너가 GitHub에서 PR #25 머지 (Closes #19·#21). 머지 후 `git switch develop && git pull`
2. 1.4 유스케이스(마감 11/23): `docs/산출물/02-유스케이스.md`에 분석 요청·결과 조회·실행 흐름 UC
3. 다음 기능 `/spec`: A 영역 F-05b(URL 수집·SSRF) 또는 F-08(개념 추출)

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대. 하네스: `codex-review.mjs` 옛 스레드 FINDINGS 판정

## 주의

- 작업로그에 F-06·F-07 작업 항목이 없다 → handoff 때 보충
- Gradle 테스트를 bootRun과 동시에 돌리지 않는다. 통합 테스트는 Docker Desktop 필요. V1~V5 수정 금지
