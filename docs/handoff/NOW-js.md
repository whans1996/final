# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 16:37 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/V-4-fe-latest-request

## 현재 기능

**V-4 FE 늦은 응답 무시·요청 취소** `docs/specs/V-4-fe-latest-request.md` (**APPROVED** 김준섭 11-27) · 브랜치 `feature/V-4-fe-latest-request` · 로드맵 V-4 (12/03~12/08, 앞당김)

## 진행

- ✅ AC1~AC11 전부 green (FE verify 통과), `frontend/CLAUDE.md` 규칙 한 줄, ui-verifier 공개 화면 5개 문제 0건
- ✅ composable `composables/useLatestRequest.js`, B·공통 16개 파일 이관·새 가드, api 3개 `{ signal }`
- ✅ A 영역 이관 follow-up 이슈 #250 (area:A, 담당 김동환, A 파일 26개 목록·적용법)
- ✅ origin/develop 합침 (#246 가드 우회 묶음 머지분)

## 바로 다음 할 일

1. `/clear` 후 `/pr-review` (Opus/high): reviewer·verifier → PR 생성 (`Roadmap: V-4`, 공통 파일 변경이라 본문에 @whans1996 멘션, A 이관은 #250)
2. spec 체크리스트 남은 것: reviewer 지적 반영, PR CI 초록, DONE(머지 후)

## 막힌 점 / 결정 필요
- 없음. GMS 키 만료 12/09 18:00 → EV-2 전에 재발급·연장 확인. 내 이슈 #243(FOLLOW_SYSTEM)은 화면 이전 뒤

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용을 적지 않는다.** 점수·개수·SHA만
- 공통 파일: `composables/useLatestRequest.js`·`useMoreList.js`, `api/concepts.js`·`adminConcepts.js`, 공통 화면(ConceptList/Detail, AdminConceptList, AdminChallengeList/Edit, UserProfile, UserWishes, WishButton), `views/__tests__/viewTestUtils.js`(도우미 `deferred`·`untilAborted`·`canceledError`)
- 테스트에서 `wrapper.unmount()`하면 vue-router가 현재 위치를 '/'로 돌린다 → 이동 여부는 `router.replace` spy로 본다
