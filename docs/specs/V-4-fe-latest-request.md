# V-4 FE 늦은 응답 무시·요청 취소 공통 패턴

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | V-4 (FE 부분. BE 동시 갱신 규칙·활성 회원 확인은 A) |
| 우선순위 | T1 |
| 영역 오너 | B 김준섭 (공통 composable·공통 화면 포함, 공통 PR로 김동환 @멘션) |
| 관련 필수 요구사항 | - (품질·검증 트랙, 기획서 6절 V-4) |
| 의존 spec | F-29b (앱 셸, 머지됨) |
| 관련 이슈 | 없음 (#104·#105는 BE, 이미 닫힘). A 영역 이관은 새 follow-up 이슈로 남긴다 |
| 승인 | 김준섭 2026-11-27 |

## 1. 목적

Codex 지적의 14%가 FE 늦은 응답·상태 경합이다 (11-25 감사). 화면마다 `let lastRequest = 0`·`generation`·`id !== props.id` 같은 가드를 손으로 쓰고, 빠진 곳은 리뷰가 하나씩 잡았다. 순번 비교 + 이전 요청 취소 + 화면을 떠날 때 무효화를 **composable 하나**로 묶고, B·공통 파일을 거기로 옮기고, 빠진 곳 두 군데(글 수정 화면, 찜 버튼)를 메운다. 사용자 입장에서는 "카드·글을 빠르게 넘기거나 검색어를 바꿔도 이전 결과가 끼어들지 않고, 떠난 화면이 나를 다른 곳으로 끌고 가지 않는다".

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `it('AC1 다시 begin하면 이전 요청은 현재가 아니고 취소된다', ...)`

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | `useLatestRequest()`로 `begin()`한 요청이 진행 중일 때 | `begin()`을 다시 부르면 | 이전 요청의 `isCurrent()`는 false, `signal.aborted`는 true이고 새 요청의 `isCurrent()`는 true다 |
| AC2 | 요청이 진행 중일 때 | `invalidate()`를 부르거나 composable을 쓴 컴포넌트(effect scope)가 사라지면 | 그 요청은 `isCurrent()` false·`signal.aborted` true다. 사라진 뒤 `begin()`한 요청도 처음부터 `isCurrent()` false다 (늦은 watch가 다시 불러도 반영하지 않는다) |
| AC3 | `fetchConcepts`·`fetchAdminConcepts`·`fetchPosts`에 | 두 번째 인자 `{ signal }`을 주면 / 주지 않으면 | 그 signal이 axios 요청 설정에 실린다 / 지금과 같은 요청을 보낸다 (기존 호출부 변경 없음) |
| AC4 | 카드 목록·게시글 목록·관리자 카드 목록·첨부 카드 검색에서 | 조건(검색어·필터·쪽)을 응답 전에 다시 바꾸면 | 이전 요청의 signal이 취소되고, 취소된 요청 때문에 에러 문구나 빈 목록이 보이지 않으며, 마지막 조건의 결과만 보인다 |
| AC5 | 목록 화면(카드·게시글·관리자 카드·관리자 챌린지)에서 요청이 진행 중일 때 | 화면을 떠나면 | 늦게 온 응답이 범위 보정 `router.replace`를 부르지 않는다 (기존 동작 유지, composable로 이관) |
| AC6 | 카드 상세·게시글 상세·프로필·팔로우 목록 머리·팔로우 요약·찜 목록·댓글 목록·관리자 챌린지 수정에서 | 대상 id를 응답 전에 바꾸면 | 이전 id의 응답(성공·실패 모두)은 화면에 반영되지 않고 로딩도 끄지 않는다 |
| AC7 | 위 AC6 화면에서 요청이 진행 중일 때 | 화면을 떠나면(unmount) | 늦은 응답이 상태를 바꾸거나 이동을 일으키지 않는다 (지금은 상세 화면들이 unmount 무효화를 하지 않는다) |
| AC8 | 남의 글 수정 주소(`/community/:id/edit`)에 들어가 글 응답을 기다리는 중 | 다른 화면으로 떠나면 | 늦게 온 응답이 상세 화면으로 `router.replace`하지 않는다 (지금은 떠난 뒤 끌려간다) |
| AC9 | 카드 A에서 찜 버튼을 눌러 응답을 기다리는 중 | 관련 카드를 눌러 `conceptId`가 B로 바뀌면 | A의 응답(성공·실패 모두)이 B의 찜 상태·개수·복습 예정일·에러를 바꾸지 않고 `change`를 emit하지 않으며, 버튼 잠금(pending)은 풀린다 |
| AC10 | `useMoreList`(피드·팔로우 목록)가 | 내부를 `useLatestRequest`로 바꾼 뒤 | 기존 테스트(겹친 id 거르기, reset 중 늦은 응답 버리기)가 그대로 통과하고, 컴포넌트가 사라진 뒤 온 응답은 반영하지 않는다 |
| AC11 | 5절 "이관" 대상 파일에 | 이관 후 | 직접 쓴 순번 가드(`++lastRequest`·`++lastHead`·`++lastSearch`·`id !== challengeId.value` 비교)가 남지 않는다 (소스를 읽는 가드 테스트, 예외는 5절 표에 적은 파일만) |

## 3. API 계약

BE API 변경 없음. `docs/api/openapi.json` 변경 없음.

FE `src/api` 함수 시그니처만 넓힌다 (URL·메서드·파라미터 이름은 그대로, contract-checker 대상 아님):

```js
// before
export function fetchConcepts({ keyword, category, difficulty, sort, page, size } = {})
// after: 두 번째 인자는 axios 요청 설정 일부. 지금은 signal만 받는다
export function fetchConcepts({ keyword, category, difficulty, sort, page, size } = {}, { signal } = {})
```

같은 방식: `adminConcepts.js` `fetchAdminConcepts`, `posts.js` `fetchPosts`. `adminChallenges.js`는 A 영역이라 손대지 않는다 (관리자 챌린지 목록은 무시만).

`http.js` 변경 없음: 취소는 `begin()`·`invalidate()`가 순번을 올린 뒤에만 일어나므로 취소된 요청은 항상 `isCurrent()` false이고, 호출부가 에러 문구를 보이기 전에 걸러진다. 인터셉터는 응답 없는 에러(취소 포함)를 그대로 reject한다 (401 재발급 재시도 중 취소되면 같은 signal로 재시도가 바로 취소된다).

## 4. DB 변경

- Flyway: 없음

## 5. 프론트엔드

### 5.1 composable `src/composables/useLatestRequest.js`

```js
const latest = useLatestRequest()

async function load() {
  const req = latest.begin() // 이전 요청 abort + 순번 증가
  loading.value = true
  try {
    // signal은 받는 api(3절 3개)에만 넘긴다. 나머지는 무시만 한다
    const result = await fetchConcepts(conditions.value, { signal: req.signal })
    if (!req.isCurrent()) return
    items.value = result.items
  } catch (error) {
    if (!req.isCurrent()) return
    loadError.value = errorMessage(error)
  } finally {
    if (req.isCurrent()) loading.value = false
  }
}
```

- `begin()` → `{ signal, isCurrent }`. `invalidate()`는 진행 중 요청을 무효로 하고 취소한다.
- 정리는 `onScopeDispose`(effect scope가 있을 때)로 한다. 컴포넌트에서도, `useMoreList` 같은 composable 안에서도 쓸 수 있다. scope 밖에서 부르면 자동 정리 없이 동작한다.
- 화면 하나에 독립된 요청 흐름이 둘이면 인스턴스를 둘 만든다 (예: UserFollowsView 머리 `head`와 목록).
- 흐름을 바꾸지 않는다: 지금 각 화면의 `try/catch/finally` 구조와 `redirecting` 같은 분기는 그대로 두고 순번 변수만 바꾼다.
- `usePolling`은 자기 `generation`·`isCurrent`가 있고 폴링 의미(멈춤·재시작)가 달라 이번에 바꾸지 않는다.

### 5.2 B·공통 파일 처리 (API를 부르는 23개)

| 파일 | 영역 | 처리 |
|---|---|---|
| `views/concept/ConceptListView.vue` | 공통 | 이관 + signal (AC4·AC5) |
| `views/community/PostListView.vue` | B | 이관 + signal (AC4·AC5) |
| `views/admin/concept/AdminConceptListView.vue` | 공통 | 이관 + signal (AC4·AC5) |
| `components/community/AttachmentPicker.vue` | B | `searchConcepts` 이관 + signal (AC4). `loadAnalyses`는 한 번만 불러 그대로 |
| `views/admin/challenge/AdminChallengeListView.vue` | 공통 | 이관, 무시만 (api가 A 영역, AC5) |
| `views/admin/challenge/AdminChallengeEditView.vue` | 공통 | 이관 (id 비교 → composable, AC6·AC7) |
| `views/concept/ConceptDetailView.vue` | 공통 | 이관 (AC6·AC7) |
| `views/community/PostDetailView.vue` | B | 이관 (AC6·AC7) |
| `views/UserProfileView.vue` | 공통 | 이관 (AC6·AC7) |
| `views/UserFollowsView.vue` | B | 머리 `lastHead` 이관 (AC6·AC7), 목록은 `useMoreList` |
| `components/follow/FollowSummary.vue` | B | `load` 이관 (AC6·AC7). `toggle`의 `id === props.userId` 비교는 사용자 동작 결과라 그대로 |
| `components/concept/UserWishes.vue` | 공통 | 이관 (AC6·AC7) |
| `components/community/PostComments.vue` | B | 이관 (AC6·AC7) |
| `composables/useMoreList.js` | 공통 | 이관 (AC10). 피드(FeedView)·팔로우 목록이 이것으로 해결된다 |
| `views/community/PostWriteView.vue` | B | **새 가드** `loadPost`·`prefillAttachment` (AC8) |
| `components/concept/WishButton.vue` | 공통 | **새 가드** `toggle` (AC9) |
| `components/concept/ConceptReviews.vue` | 공통 | **그대로** (예외). 읽기·쓰기를 한 번에 하나로 직렬화하는 `generation`이라 단순 최신 요청 패턴이 아니다. 이유를 파일 주석에 이미 적었다 |
| `views/community/FeedView.vue` | B | 대상 아님: 한 번 부르고 목록은 `useMoreList` |
| `components/community/CommentItem.vue` | B | 대상 아님: 사용자 동작(수정·삭제)이 같은 댓글에만 적용 |
| `views/admin/concept/AdminConceptEditView.vue` | 공통 | 대상 아님: 한 번 부르고(`conceptId` 고정) 늦은 응답이 이동을 일으키지 않는다 |
| `views/MySettingsView.vue` | 공통 | 대상 아님: 한 번 부르고 로딩이 끝나야 폼이 보인다 |
| `views/LoginView.vue`·`SignupView.vue` | 공통 | 대상 아님: 제출 중 버튼 잠금(`submitting`) |
| `stores/auth.js` | 공통 | 대상 아님: refresh 동시 호출 1회는 이미 처리, 화면 상태 없음 |

### 5.3 그 밖

| 항목 | 내용 |
|---|---|
| 화면/라우트 | 변경 없음 (보이는 UI 변화 없음) |
| 규칙 문서 | `frontend/CLAUDE.md` 규칙에 "대상이 바뀌거나 화면을 떠날 수 있는 조회는 `useLatestRequest`로" 한 줄, composables 폴더 설명에 추가 |
| A 영역 | A 파일 26개(analysis·quiz·challenge·team·reviewplan·dashboard)는 바꾸지 않는다. 목록과 적용법을 follow-up 이슈(`area:A`, 담당 김동환)로 남겨 A가 자기 PR에서 옮긴다 |

## 6. LLM (AI 기능만)

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1·AC2 | `src/composables/__tests__/useLatestRequest.spec.js` | Vitest (effectScope로 정리 확인) |
| AC3 | `src/api/__tests__/concepts.spec.js`·`posts.spec.js`·`adminConcepts.spec.js` (없으면 새로) | Vitest (`http.defaults.adapter`로 설정 확인) |
| AC4 | `ConceptListView.spec.js`·`PostListView.spec.js`·`AdminConceptListView.spec.js`·`AttachmentPicker.spec.js` | Vitest (api mock이 받은 signal의 aborted 확인, 취소 reject 뒤 에러 문구 없음) |
| AC5 | 위 목록 spec의 기존 "떠난 뒤 replace 안 함" 테스트 + `AdminChallengeListView.spec.js` | Vitest (기존 테스트 유지) |
| AC6·AC7 | 각 화면·컴포넌트 spec (`ConceptDetailView`·`PostDetailView`·`UserProfileView`·`UserFollowsView`·`FollowSummary`·`UserWishes`·`PostComments`·`AdminChallengeEditView`). 기존 테스트가 있으면 유지하고 없는 것만 추가 | Vitest (deferred promise, `wrapper.unmount()` 뒤 resolve) |
| AC8 | `PostWriteView.spec.js` | Vitest |
| AC9 | `WishButton.spec.js` | Vitest (`setProps({ conceptId })` 뒤 resolve·reject) |
| AC10 | `src/composables/__tests__/useMoreList.spec.js` | Vitest (기존 + unmount) |
| AC11 | `src/composables/__tests__/latestRequestAdoption.spec.js` | Vitest (5.2 "이관" 파일 소스를 읽어 직접 가드 패턴이 없음을 확인) |

화면 변화가 없으므로 ui-verifier는 카드 목록·게시글 목록·카드 상세 3화면 콘솔 에러 확인만 한다.

## 8. 범위 밖

- A 영역 FE 파일 26개 이관 (follow-up 이슈로 A에게)
- BE 동시 갱신 규칙·활성 회원 확인 (V-4 BE, A)
- `usePolling` 재작성, `ConceptReviews` 직렬화 재작성
- 위 3개 외 api 함수의 signal 연결 (상세 조회는 무시만으로 충분하다)
- lint 규칙(ESLint 커스텀 룰)으로 강제하기. AC11 가드 테스트로 대신한다
- 화면 디자인 변경 (각 화면 개편 PR)

## 9. 결정 필요

- 없음 (11-27: 적용 범위 B·공통만, 취소는 연달아 바뀌는 목록 4곳만, 기존 가드도 이관 — 사용자 선택)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/V-4-fe-latest-request`
- [x] AC1·AC2 composable 테스트 red → `useLatestRequest.js` green
- [x] AC3 api signal
- [x] AC4·AC5 목록 4+1곳 이관
- [x] AC6·AC7 상세·컴포넌트 이관
- [x] AC8 PostWriteView, AC9 WishButton 새 가드
- [x] AC10 useMoreList, AC11 가드 테스트
- [x] `frontend/CLAUDE.md` 규칙 한 줄
- [ ] A 영역 follow-up 이슈 (`area:A`, 파일 목록·적용 예시)
- [ ] ui-verifier 통과 (콘솔 에러만, 데스크톱)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영: 없음 (화면·ERD·요구사항 변화 없음)
- [ ] PR CI 초록, 공통 파일 변경이라 김동환 @멘션
- [ ] 상태 DONE (PR 머지 후)
