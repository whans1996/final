# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 16:17 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/V-4-fe-latest-request

## 현재 기능

**V-4 FE 늦은 응답 무시·요청 취소** `docs/specs/V-4-fe-latest-request.md` (**APPROVED** 김준섭 11-27) · 브랜치 `feature/V-4-fe-latest-request` · 로드맵 V-4 (12/03~12/08, 의존 없음이라 앞당김)

## 진행

- ✅ F-29b PR #247 머지됨 (11-27)
- ✅ V-4 spec 초안: AC1~AC11, `composables/useLatestRequest.js`(begin → {signal,isCurrent}, invalidate, onScopeDispose), B·공통 23개 파일 분류(이관 14·새 가드 2·예외 1·대상 아님 6), api 3개에 `{ signal }` 두 번째 인자
- ✅ spec 승인 (김준섭 11-27), 결정 (사용자): 범위 B·공통만, 취소는 목록 4곳만, 기존 가드도 이관 → 결정로그 11-27 V-4 행

## 바로 다음 할 일

1. `/clear` 후 `/implement V-4` (Opus/high): AC1·AC2 `src/composables/__tests__/useLatestRequest.spec.js` red부터

## 막힌 점 / 결정 필요
- 없음. GMS 키 만료 12/09 18:00 → EV-2 전에 재발급·연장 확인. 내 이슈 #243(FOLLOW_SYSTEM)은 화면 이전 뒤

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용을 적지 않는다.** 점수·개수·SHA만
- V-4는 A 영역 FE 파일(analysis·quiz·challenge·team·reviewplan·dashboard)을 건드리지 않는다. A용 follow-up 이슈를 구현 PR에서 만든다
- 화면(View)은 `<main>`을 쓰지 않는다. 셸 전환은 afterEach에서만
