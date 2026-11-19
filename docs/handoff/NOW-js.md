# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-19 15:46 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/common-harness-auto-merge

## 현재 기능

F-09 개념 카드 목록·검색 — 구현 완료, PR 전 (`feature/F-09-concept-list`, `/pr-review` 남음). 사이 작업: 하네스 자동 동기화 (`feature/common-harness-auto-merge`, 사용자 요청)

## 진행

- ✅ F-09: BE `33497a0`, FE `aab58f4`, 개수·SC-09 산출물 `971f725` (push). contract-checker·ui-verifier 0건
- ✅ 자동 동기화: `scripts/lib/sync.mjs#autoSync`(내 원격 → origin/develop 순 `git merge`, 미커밋 변경·병합 중이면 미룸, 충돌이면 abort, push 안 함, `git config vibenote.autoSync false`로 끔). `session-start.mjs`·`peer-watch.mjs`에 연결, 배너 `sessionBanner({ syncLines })` 🔄 줄. 테스트 `scripts/test/sync.test.mjs`(임시 저장소 11개)·`common.test.mjs` 배너. 문서: CLAUDE.md 작업 흐름, conventions 6절 develop 동기화, pickup 스킬, 결정로그

## 바로 다음 할 일

1. 자동 동기화 PR: `/pr-review` (base develop, 하네스=A 영역이라 @whans1996 멘션, 이유: 상대 머지분 자동 합치기). 머지 후 김동환은 develop을 합치면 바로 쓴다 (별도 설정 없음)
2. `git switch feature/F-09-concept-list` → `/pr-review` (F-09). PR 본문 @whans1996 멘션: `SecurityConfig`, `ConceptMapper`, `navItems.js`, `routes/concept.js`, `App.spec.js`·`navItems.spec.js`, CODEOWNERS

## 막힌 점 / 결정 필요

- F-09 헤더 메뉴 "개념 카드" order 5(맨 앞), 375px에서 "전체" select 2개 구분 — 사용자 확인 대기

## 주의

- develop에 PR #43(김동환 유스케이스) 머지됨 → 두 브랜치 모두 자동 동기화가 합칠 대상
- F-09 브랜치의 NOW-js.md는 `> 갱신:`(인용문) 형식이라 하네스가 못 읽는다 → F-09 PR 전에 이 파일 형식(맨 앞 `갱신:` 줄)으로 고친다
- 통합 테스트는 Docker Desktop 필요. F-09는 마이그레이션 없음
