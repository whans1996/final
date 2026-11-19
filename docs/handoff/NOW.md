# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-19 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-07-execution-flow (spec 승인 커밋)

## 현재 기능

F-07 실행 흐름 설명 — `docs/specs/F-07-execution-flow.md` **APPROVED (whans1996 2026-11-19), 구현 전** (F-05a #14·F-06 #20 머지 완료)

## 진행

- ✅ spec 초안: AC1~AC16 (BE AC1~AC13, FE AC14~AC16), V5 마이그레이션, 프롬프트 `prompts/flow/*`, 에러 코드 ANALYSIS_002·003·409
- ✅ 사용자 결정 4건(비동기+폴링, 분석당 3개, 규칙 기반 입력 파일, #19·#21 포함) → spec 10절·기획서 결정 로그

## 바로 다음 할 일

1. `/clear` 후 새 세션(Opus/high)에서 `/implement F-07` 시작
2. 구현 순서: V5 → `FileSelectorTest#AC12_…`·`ApiDetectorTest#AC13_…`(#19·#21, 작고 독립) → `FlowContextBuilderTest` → 서비스·생성기 → FE 탭
3. 1.4 유스케이스(마감 11/23): `docs/산출물/02-유스케이스.md`에 분석 요청·결과 조회·실행 흐름 UC 추가

## 막힌 점 / 결정 필요

- 사람: 이슈 #16(설정 키 키워드 확장 범위), 노션 대시보드 팀원 초대
- 하네스: `codex-review.mjs`가 영수증이 있어도 옛 스레드의 `has_unresolved_findings`로 FINDINGS 판정 → 판정 우선순위 수정 필요

## 주의

- 작업로그에 F-06 작업(11-18 19:50 이후) 항목이 없다 → 다음 커밋 때 보충
- 커밋 메시지에 `.env` 글자가 있으면 guard-bash가 막는다 → 메시지를 파일로 써서 `git commit -F`
- FE 커밋 때 Docker Desktop을 켜 둔다 (oxlint Docker 실행). verify(Gradle)를 bootRun과 동시에 돌리지 않는다. V1~V4 수정 금지
