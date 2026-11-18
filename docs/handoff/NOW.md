# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-05a-analysis-zip

## 현재 기능

F-05a 분석 요청(ZIP) — AC1~16 모두 green, `/pr-review` 직전 (push 안 됨)

## 진행

- ✅ BE AC1~13(0c298c3·4cb7633), FE AC14~16(bdf1ec8), verify fe 106개 통과
- ✅ contract-checker 불일치 0 / ui-verifier 데스크톱·375px 통과(실 서버에서 ZIP → DONE, python-only → FAILED ANALYSIS_415 확인). 모바일 헤더 로고 줄바꿈 수정
- ✅ 산출물: 06 화면설계서 SC-05·06·07(스크린샷 `screens/SC-05~07-*`), 04-WBS 3.2a·3.2b, 01 F-05 행. 03-ERD error_code는 이미 반영돼 있었음
- ✅ 하네스: `scripts/ui-check.mjs`가 회전된 refresh 쿠키를 다음 viewport·`--storage` 파일로 넘기고, 로그인 화면으로 튕기면 문제로 센다

## 바로 다음 할 일

1. 새 세션에서 `/pr-review` (reviewer 보안 중점: `analysis/pipeline/ZipArchiveReader` Zip Slip·zip bomb, `SecretRedactor` 키 마스킹, `AnalysisPipeline` 임시 파일 삭제) → PR(develop) 생성 → Codex 리뷰 확인
2. PR 설명 스크린샷: `node scripts/pr-screens.mjs SC-05 SC-06 SC-07`

## 막힌 점 / 결정 필요

- 없음. 해석: 상세 화면은 404 외 에러면 문구를 보이고 폴링 계속. 목록 페이지는 로컬 상태(쿼리 미사용)

## 주의

- 로컬 DB에 화면 점검용 회원 `ui-check@vibenote.test`/`uicheck1234`(분석 1·2) 있음. `.harness/auth.json`은 이 회원의 세션(gitignore)
- Git Bash curl로 한글 JSON을 보내면 인코딩이 깨져 400이 난다(서버 버그 아님). `-F file=@`에는 `cygpath -m` 경로
- verify(Gradle)를 bootRun과 동시에 돌리지 않는다. V1·V2·V3 수정 금지
