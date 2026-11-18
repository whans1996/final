# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-05a-analysis-zip

## 현재 기능

F-05a 분석 요청(ZIP) — spec APPROVED, `/implement` 진행 중. 이번 세션 목표: FE AC14~16

## 진행

- ✅ (커밋 0c298c3·4cb7633, push 안 됨) BE AC1~13 전부 green, openapi.json
- ✅ FE 테스트 작성·red 확인(30개 중 28 실패, 모두 미구현 사유): `api/__tests__/analyses.spec.js`, `views/__tests__/AnalysisRequestView·AnalysisDetailView·AnalysisListView.spec.js`
- ✅ FE AC14~16 구현, verify fe green(106개), 커밋: `api/analyses.js`, `composables/usePolling.js`(generation으로 이전 폴링 응답 무시), `utils/analysisStatus.js`(5.1절 라벨), `components/analysis/AnalysisStatusBadge.vue`, 화면 3개, 라우트 3개(`analyses`·`analysis-new`·`analysis-detail`), `App.vue` 헤더 링크·줄바꿈

## 바로 다음 할 일

1. contract-checker(spec 3절 ↔ openapi.json ↔ `src/api/analyses.js`) → BE(local)+FE dev 띄우고 ui-verifier(`/analyses/new`, `/analyses`, `/analyses/{id}`, 375px)
2. 산출물: 03-ERD(error_code), 06 화면설계서 SC-05·06·07, 04-WBS 3.2a·3.2b → spec 체크리스트 갱신 → `/pr-review` (reviewer 보안 중점: Zip Slip·zip bomb·키 마스킹)

## 막힌 점 / 결정 필요

- 없음. 해석: 상세 화면은 404 외 에러면 메시지를 보이고 폴링을 계속한다. 목록 페이지는 로컬 상태(쿼리 미사용)

## 주의

- `AnalysisTestSupport`는 `@f05a.test` 회원 DELETE로 정리. 테스트 키·PEM 문자열은 이어 붙여 만든다
- FE 폴링 테스트는 `vi.useFakeTimers({ toFake: ['setTimeout','clearTimeout'] })` (flushPromises가 멈추지 않게)
- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다. V1·V2·V3 수정 금지
