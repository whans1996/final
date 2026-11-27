# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 16:33 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/V-4-fe-latest-request

## 현재 기능

**V-4 FE 늦은 응답 무시·요청 취소** `docs/specs/V-4-fe-latest-request.md` (**APPROVED** 김준섭 11-27) · 브랜치 `feature/V-4-fe-latest-request` · 로드맵 V-4 (12/03~12/08, 앞당김)

## 진행

- ✅ AC1·AC2 `composables/useLatestRequest.js` (begin/invalidate/onScopeDispose), AC3 api 3개 `{ signal }`
- ✅ AC4·AC5 목록 4곳(취소)+관리자 챌린지 목록(무시) 이관, AC6·AC7 상세 8곳 이관 (unmount 테스트는 `wrapper.vm`으로 확인)
- ✅ AC8 PostWriteView·AC9 WishButton 새 가드 (a9b2ef8)
- 테스트 도우미: `views/__tests__/viewTestUtils.js` `untilAborted`·`canceledError`·`deferred`

## 바로 다음 할 일

1. AC10 `useMoreList.js` 이관 + `src/composables/__tests__/useMoreList.spec.js` 새로(겹친 id·reset 중 늦은 응답·effectScope stop 뒤 응답 무시)
2. AC11 `src/composables/__tests__/latestRequestAdoption.spec.js` (spec 5.2 이관 파일 소스에 `++lastRequest` 등 없음)
3. `frontend/CLAUDE.md` 규칙 한 줄, A 영역 follow-up 이슈, ui-verifier 3화면, `/pr-review`

## 막힌 점 / 결정 필요
- 없음. GMS 키 만료 12/09 18:00 → EV-2 전에 재발급·연장 확인. 내 이슈 #243(FOLLOW_SYSTEM)은 화면 이전 뒤

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용을 적지 않는다.** 점수·개수·SHA만
- V-4는 A 영역 FE 파일을 건드리지 않는다. 공통 파일(composable·api·공통 화면) 변경이라 PR에 @whans1996 멘션
- 테스트에서 `wrapper.unmount()`하면 vue-router가 현재 위치를 '/'로 돌린다 → 이동 여부는 `router.replace` spy로 본다
