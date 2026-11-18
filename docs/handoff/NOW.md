# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 22:40 · 작업자: whans1996 (Claude, 집 PC) · 브랜치: feature/F-06-overview-explanation

## 현재 기능

F-06 개요·파일별 설명 — `docs/specs/F-06-overview-explanation.md` **APPROVED, BE(AC1~AC12) 완료·커밋, FE(AC13~15) 구현 완료**

## 진행

- ✅ BE: 8b09a86·58a6ccd·02630fb (BE 273개 green, contract-checker BE↔spec 일치)
- ✅ FE red 확인(F-06 25건만 실패, 의도한 이유)
- ✅ FE 구현: `api/analyses.js` 3함수, `utils/layers.js`, `components/analysis/AnalysisOverviewTab.vue`·`AnalysisFilesTab.vue`·`CodeViewer.vue`, `views/AnalysisDetailView.vue`(DONE이면 상세 1회·탭·쿼리). FE 132개 green

## 바로 다음 할 일

1. ui-verifier: BE(local) + FE dev 띄우고 DONE 분석의 `/analyses/{id}`·`?tab=files&file=…` 데스크톱·375px 확인
2. contract-checker(FE 포함) → 산출물(SC-07, 08 §1.3, WBS·간트) → `/pr-review`

## 막힌 점 / 결정 필요

- 사람: 이슈 #16(설정 키 키워드 확장 범위), 노션 대시보드 팀원 초대
- FE 커밋 때 Docker Desktop을 켜 둔다 (oxlint를 Docker로 실행, 꺼져 있으면 건너뜀)

## 주의

- 1.4 유스케이스(마감 11/23): `docs/산출물/02-유스케이스.md`에 분석 요청 UC 흐름 추가 필요
- 집 PC DB는 V1~V3 적용 상태(V4는 bootRun 때 적용). verify(Gradle)를 bootRun과 동시에 돌리지 않는다
