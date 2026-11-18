# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 22:45 · 작업자: whans1996 (Claude, 집 PC) · 브랜치: feature/F-06-overview-explanation

## 현재 기능

F-06 개요·파일별 설명 — **PR #20 머지 대기** (spec DONE 표시 완료, Codex ✅ bc1f1f3 (P2 1건 → #21), CI 초록)

## 진행

- ✅ AC1~AC15 + spec 8절 fixture 검증(`MainFixturesTest`). reviewer 반드시 1건·권장 4건 반영(16d3cc5·8bbff55)
- ✅ Codex 1차 P2 2건: 개요 입력 목록 상한(AC3) a46a198 수정, 문자열 연결 매핑 경로 → #21 이관(스레드 답변)
- ✅ FE CI 실패 수정 bc1f1f3: `run-oxlint.mjs`가 Linux에서 `oxlint/bin/oxlint`를 exports 제한으로 못 찾음(로컬은 Docker 경로라 안 보였음)
- ✅ 완료 표시: spec DONE, `04-WBS` 3.3 ✅, `05-간트` done + 실적 막대, `진행현황.md` F-06 ✅

## 바로 다음 할 일

1. 영역 오너(팀원 A)가 GitHub에서 PR #20 머지 (wip 없음, Merge 또는 Squash)
2. 머지 후 develop pull → 다음 spec: A 영역 F-07(실행 흐름, #19·#21 먼저 처리 권장) 또는 F-05b(URL 수집). `/spec F-07`
3. 1.4 유스케이스(마감 11/23): `docs/산출물/02-유스케이스.md`에 분석 요청·결과 조회 UC 흐름 추가

## 막힌 점 / 결정 필요

- 사람: 이슈 #16(설정 키 키워드 확장 범위), 노션 대시보드 팀원 초대
- 하네스: `codex-review.mjs`는 영수증("Didn't find any major issues")이 있어도 요약의 `has_unresolved_findings`(이관·수정한 옛 스레드)가 남으면 FINDINGS로 판정한다(a46a198에서 발생). 판정 우선순위 수정 필요

## 주의

- 커밋 메시지에 `.env` 글자가 있으면 guard-bash가 막는다 → 메시지를 파일로 써서 `git commit -F`
- 다음 프롬프트 평가 때 `prompts/file/senior.md`의 "근거가 있을 때만" 완화(미평가) 확인
- FE 커밋 때 Docker Desktop을 켜 둔다 (oxlint Docker 실행). verify(Gradle)를 bootRun과 동시에 돌리지 않는다
