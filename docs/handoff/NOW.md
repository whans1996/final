# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 19:45 · 작업자: whans1996 (Claude, 집 PC) · 브랜치: feature/F-05a-wrapup

## 현재 기능

없음 — F-05a 완료(PR #14 머지, spec DONE). 머지 후 정리(진행현황·WBS·간트·spec) 브랜치 `feature/F-05a-wrapup`

## 진행

- ✅ PR #14 머지(CI 초록, head bcfaa2f). `/progress` W1 점검: W1 대비 앞섬, 재계획 불필요 (`docs/진행현황.md`)
- ✅ WBS 3.2a ✅(PR #14), 간트 3.2를 3.2a(done)·3.2b로 분리하고 실적 막대 추가

## 바로 다음 할 일

1. `feature/F-05a-wrapup` push → develop PR(docs만) → 영역 오너 머지
2. `/clear` 후 다음 기능 `/spec`: A 영역 F-06(개요·파일별 설명, LLM 첫 사용) 또는 F-05b(URL 수집·SSRF), B 영역 F-09·F-10
3. 1.4 유스케이스(마감 11/23): `docs/산출물/02-유스케이스.md`에 분석 요청(F-05a) UC 흐름 추가

## 막힌 점 / 결정 필요

- F-05a spec 10절 미결: 설정 키 키워드 확장(`private-key`·`access-key` 등 `key`로 끝나는 키)
- 후속: 다른 도메인 created_at/updated_at도 DB 세션 시간대 의존(UTC MySQL이면 9시간 차이). 분석은 서비스 Clock으로 고침

## 주의

- 집 PC: 스마트 앱 컨트롤이 oxlint 네이티브 바인딩을 막아 FE lint만 실패(테스트·빌드는 통과). FE 커밋 전 해결 필요 (wasm 바인딩 `--no-save` 설치 또는 SAC 끄기, 사람 결정)
- 집 PC DB는 새로 만들어 비어 있음(V1~V3 적용). 화면 점검용 회원은 필요할 때 가입
- `.env`는 properties 형식으로 읽힌다: 따옴표·끝 공백이 값에 포함됨
- verify(Gradle)를 bootRun과 동시에 돌리지 않는다. V1·V2·V3 수정 금지
