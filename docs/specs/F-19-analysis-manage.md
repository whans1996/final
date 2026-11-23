# F-19 분석 관리 (수정·공개 설정·삭제 + 공개 분석 열람)

| 항목 | 값 |
|---|---|
| 상태 | DONE (PR #93) <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-19 |
| 우선순위 | P0 |
| 영역 오너 | A 김동환 (AI·분석·학습 루프) |
| 관련 필수 요구사항 | ③ ⑥ ⑦ (명세서 필수 · 핵심 데이터 F1303 수정·F1304 삭제) |
| 의존 spec | F-05a (분석·목록 SC-06, 진행 상태 SC-07, 일일 한도), F-05b (`sourceUrl`·"저장소 보기"), F-06·F-07·F-08 (결과 API·탭), F-14 (퀴즈는 주인만), F-03 (분석 수·퀴즈 정답률·`users.status`), F-04 (피드 AC7 `ANALYSIS` 가시성), F-17 (게시글 분석 첨부 표시) |
| 관련 이슈 | #60 (SC-07 "커뮤니티에 질문하기" 버튼), #81 (FAILED 때도 "저장소 보기" 링크). 구현 PR에 `Closes #60`, `Closes #81` |
| 승인 | 김동환 2026-11-23 |

## 1. 목적

회원이 자기 분석의 제목·메모를 고치고, 공개 여부를 정하고, 필요 없는 분석을 지운다. 지금 피드(F-04)의 "새 분석을 공개했어요"와 게시글(F-17)의 "분석 보기"는 `/analyses/{id}`로 연결되지만 주인이 아니면 404가 난다. 그래서 공개한 분석은 **다른 회원도 읽기 전용으로** 개요·파일·개념·실행 흐름을 보게 한다 (F-05a·06·07·08·14 spec이 "공개 분석의 타인 조회는 F-19"로 미뤄 둔 것).
삭제는 soft delete다 (V2, 결정로그 11-18). 파일·흐름·개념 연결은 지우고 퀴즈 풀이 기록은 남겨 통계·추천·챌린지·일일 한도가 깨지지 않게 한다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_주인이_제목과_메모를_고치면_200과_바뀐_상세를_준다()`
> "볼 수 있는 분석" = 미삭제 AND (내 분석 OR (`is_public = TRUE` AND `status = DONE` AND 주인 `users.status = ACTIVE`)). 볼 수 없으면 존재를 드러내지 않도록 404 `ANALYSIS_404`.

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 주인 U에게 분석 A가 있을 때 (상태 무관: 진행 중·DONE·FAILED) | `PATCH /api/analyses/{A}` `{title, memo}` | 200과 바뀐 상세(AC11 형식). 앞뒤 공백을 자르고, 빈 문자열·공백만이면 **null**로 저장한다 (목록·피드·첨부는 기존처럼 프로젝트명 → "제목 없는 분석"으로 대체 표시). 두 필드 모두 보낸 값으로 바꾼다(누락 = null). 파이프라인 진행에는 영향이 없다 |
| AC2 | U가 | `title` 101자 이상 또는 `memo` 1001자 이상으로 PATCH | 400 `COMMON_400`, `details.title` / `details.memo`. 저장되지 않는다 (경계: 100자·1000자는 200) |
| AC3 | 남의 분석 / 삭제된 분석 / 없는 id에 | U가 `PATCH /{id}`, `PATCH /{id}/visibility`, `DELETE /{id}` | 셋 다 404 `ANALYSIS_404`. 남의 분석이 **공개**여도 같다 |
| AC4 | U의 DONE 분석 A가 비공개이고 팔로워 F가 있을 때 | U가 `PATCH /{A}/visibility` `{"isPublic": true}` | 200 `{id, isPublic: true}`. F의 `GET /api/feed`에 A 완료 때 기록된 `ANALYSIS` 활동이 **원래 시각 그대로** 보인다 (F-04 AC7). 새 활동은 만들지 않는다. 다시 `false`로 바꾸면 피드에서 빠진다 |
| AC5 | U의 분석이 DONE이 아닐 때 (진행 중·FAILED) | `{"isPublic": true}` | 409 `ANALYSIS_006`, 바뀌지 않음. `{"isPublic": false}`는 상태와 관계없이 200. `isPublic` 누락·null → 400 `COMMON_400` (`details.isPublic`) |
| AC6 | U의 DONE 분석 A에 파일·흐름·개념 연결·퀴즈와 U의 풀이 기록이 있을 때 | `DELETE /api/analyses/{A}` | 204. `analyses.deleted_at`이 채워지고 `analysis_concepts`·`analysis_flows`·`analysis_files`의 A 행이 지워진다. `quizzes`·`quiz_questions`·`quiz_attempts`·`quiz_answers`는 남고 F-03 `quizAccuracy`는 그대로다. A는 내 목록(`GET /api/analyses`)·F-03 `analysisCount`·팔로워 피드에서 빠지고, A를 첨부한 게시글은 "볼 수 없는 분석"(F-17 첨부 `viewable = false`)으로 보인다. 일일 한도 집계는 그대로(돌아오지 않음). 다시 DELETE하면 404 |
| AC7 | U의 분석이 진행 중(FETCHING~QUIZ)일 때 | `DELETE` | 409 `ANALYSIS_005`, 아무것도 지우지 않는다. DONE·FAILED 판단은 `UPDATE … WHERE status IN ('DONE','FAILED') AND deleted_at IS NULL` 한 문장으로 해서 파이프라인 상태 갱신과 경쟁하지 않는다 (0행이면 소유·존재를 다시 보고 404/409를 고른다). FAILED 분석은 204 |
| AC8 | U의 DONE 분석에 생성 중(`PENDING`) 실행 흐름이 있을 때 | `DELETE` 후 그 흐름의 생성이 끝나 `FlowService.complete`(또는 `markFailed`)가 불리면 | 삭제는 204. 늦게 온 결과는 행이 없어 0행 갱신으로 끝나고 예외·로그 오류 없이 저장되지 않는다 |
| AC9 | U의 A가 공개·DONE이고 다른 회원 W가 로그인했을 때 | W가 `GET /{A}`, `/{A}/status`, `/{A}/files`, `/{A}/files/{fileId}`, `/{A}/concepts`, `/{A}/flows` | 모두 200 (주인과 같은 내용). 상세의 `mine = false`, `owner = {id: U, nickname}`, `memo = null` (메모는 주인만) |
| AC10 | W에게 A가 비공개 / DONE 아님 / 삭제됨 / 주인 U가 탈퇴(`WITHDRAWN`)일 때 | AC9의 6개 GET | 모두 404 `ANALYSIS_404`. 또 A가 **공개여도** W의 `POST /{A}/flows`는 404 `ANALYSIS_404`, `GET /{A}/quiz`는 404 `ANALYSIS_404`, `POST /api/quizzes/{Q}/attempts`는 404 `QUIZ_404` (주인만, F-07·F-14 그대로) |
| AC11 | 주인 U가 | `GET /{A}` | `mine = true`, `owner = {id: U, nickname}`, `memo`가 저장된 값으로 온다. `GET /api/analyses` 목록 항목에도 `memo`가 온다 |
| AC12 (#81) | URL 분석이 수집 단계에서 실패(FAILED, `ANALYSIS_004`·`502`·`503`)했을 때 | `GET /{id}/status` | 응답에 `sourceUrl`(정규화한 저장소 주소, ZIP 분석이면 `null`)이 있다. SC-07은 FAILED 화면에서도 이 값으로 "저장소 보기" 링크(새 탭)를 보여 준다 |
| AC13 (FE 주인) | U가 내 분석 SC-07을 열었을 때 (상태 무관) | 화면 | 제목 아래 "관리" 영역(진행 중에도 보여 제목·메모를 고칠 수 있다): **수정**(제목·메모 폼, 글자 수 카운터 100/1000, 저장 중 버튼 비활성, 400·404는 서버 `message`), **공개 토글**(DONE일 때만 켜짐, "공개하면 팔로워 피드와 게시글 첨부에서 보이고, 로그인한 다른 회원도 이 분석의 코드와 설명을 읽을 수 있습니다" 안내), **삭제**(DONE·FAILED일 때만, `window.confirm('이 분석을 삭제할까요? 퀴즈 풀이 기록은 남습니다.')` 확인 후 성공하면 `/analyses`로 이동, 409는 서버 문구). DONE이면 "커뮤니티에 질문하기" 버튼 → `/community/write?analysisId={id}` (#60). 메모가 있으면 제목 아래에 줄바꿈을 살려 보여 준다 |
| AC14 (FE 타인) | W가 공개 분석 SC-07을 열었을 때 | 화면 | 제목 아래 "{닉네임}님의 분석"(`/users/{id}` 링크). 관리 영역·질문 버튼·메모가 없고, 탭은 개요·파일·실행 흐름·개념만 (퀴즈 탭 없음, `?tab=quiz`로 들어와도 개요). 실행 흐름 탭은 읽기 전용: API 선택·"실행 흐름 만들기"·"다시 만들기"를 숨기고 흐름 카드만 보인다 (흐름이 없으면 "아직 만든 실행 흐름이 없습니다.") |
| AC15 (FE 목록) | SC-06 내 분석 목록에서 | 카드 | 공개 분석에 "공개" 배지, 메모가 있으면 첫 줄(한 줄 말줄임)을 보여 준다 |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: `분석` (기존 `AnalysisController`). 모두 로그인 필요.

### `PATCH /api/analyses/{id}` — 제목·메모 수정

- 권한: 주인만 (남의 분석·삭제·없음 → 404)
- 요청 (`UpdateAnalysisRequest`)

```json
{ "title": "할 일 앱 백엔드", "memo": "JWT 필터 순서 다시 보기" }
```

- 응답 `200` — `AnalysisDetailResponse` (아래 "응답 변경", 주인이므로 `mine: true`)
- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | `title` 100자 초과, `memo` 1000자 초과 (`details`에 필드) |
| 404 | ANALYSIS_404 | 남의 분석, 삭제된 분석, 없는 id |

### `PATCH /api/analyses/{id}/visibility` — 공개 설정

- 권한: 주인만
- 요청 (`UpdateVisibilityRequest`, `isPublic` 필수 `@NotNull Boolean`)

```json
{ "isPublic": true }
```

- 응답 `200` (`VisibilityResponse`)

```json
{ "id": 42, "isPublic": true }
```

- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | `isPublic` 누락·null |
| 404 | ANALYSIS_404 | 남의 분석, 삭제된 분석, 없는 id |
| 409 | ANALYSIS_006 | DONE이 아닌 분석을 공개로 바꾸려 함 |

### `DELETE /api/analyses/{id}` — 삭제

- 권한: 주인만
- 응답 `204` (본문 없음)
- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 404 | ANALYSIS_404 | 남의 분석, 이미 삭제됨, 없는 id |
| 409 | ANALYSIS_005 | 진행 중(FETCHING~QUIZ)인 분석 |

### 권한이 바뀌는 기존 API (주인 → "볼 수 있는 분석")

`GET /api/analyses/{id}`, `/status`, `/files`, `/files/{fileId}`, `/concepts`, `/flows`. 요청·에러는 그대로이고 `@Operation` description을 "내 분석 또는 공개·완료된 남의 분석"으로 고친다. `POST /flows`와 퀴즈 API는 주인만(변경 없음).

### 응답 변경

- `AnalysisDetailResponse`에 추가

```json
{
  "memo": "JWT 필터 순서 다시 보기",
  "mine": true,
  "owner": { "id": 7, "nickname": "vibecoder" }
}
```

  `memo`는 `mine = false`이면 항상 `null`. `owner`는 새 record `AnalysisOwnerResponse(long id, String nickname)`.
- `AnalysisSummaryResponse`(내 목록)에 `memo` (nullable) 추가
- `AnalysisStatusResponse`에 `sourceUrl` (nullable, ZIP이면 null) 추가 (#81)

### 새 에러 코드 (`ErrorCode`, `docs/conventions.md` 3절 ANALYSIS 행에 추가)

| 코드 | 상태 | 메시지 |
|---|---|---|
| `ANALYSIS_005` | 409 | 진행 중인 분석은 삭제할 수 없습니다. 분석이 끝난 뒤 다시 시도해 주세요. |
| `ANALYSIS_006` | 409 | 완료된 분석만 공개할 수 있습니다. |

## 4. DB 변경

- Flyway: **없음** (`analyses.title`·`memo`·`is_public`은 V1, `deleted_at`은 V2에 있다)
- 사용 테이블: `analyses`, `analysis_files`, `analysis_flows`, `analysis_concepts`, `users`(주인 닉네임·status), 읽기만 `quizzes`·`quiz_attempts`(AC6 검증)
- `AnalysisMapper.xml`
  - `columns`에 `memo` 추가
  - `findViewable(id, userId)` / `findViewableDetail(id, userId)`: `analyses a JOIN users u ON u.id = a.user_id WHERE a.id = #{id} AND a.deleted_at IS NULL AND (a.user_id = #{userId} OR (a.is_public = TRUE AND a.status = 'DONE' AND u.status = 'ACTIVE'))`, 상세는 `u.nickname AS owner_nickname` 포함
  - `updateInfo(id, userId, title, memo, now)`, `updateVisibility(id, userId, isPublic, now)` (공개는 `AND status = 'DONE'` 조건)
  - `softDelete(id, userId, now)`: `UPDATE analyses SET deleted_at = #{now}, updated_at = #{now} WHERE id AND user_id AND deleted_at IS NULL AND status IN ('DONE','FAILED')`
  - 자식 삭제: `deleteConceptLinks` → `deleteFlows` → `deleteFiles` (`analysis_concepts.file_id` FK가 파일을 참조하므로 개념 연결을 먼저). 같은 `@Transactional` 안에서 `softDelete`가 1행일 때만
- 서비스: `AnalysisService.checkViewable(userId, analysisId)`(없으면 `ANALYSIS_404`)를 두고 `getStatus`·`getDetail`·`listFiles`·`getFile`, `FlowService.list`, `AnalysisConceptService.getConcepts`가 이것을 쓴다. `checkOwned`·`findOwned`는 `POST /flows`·퀴즈 등 주인 전용 경로에 그대로 둔다. `QuizService`는 고치지 않는다 (F-20b가 같은 메서드를 바꾸므로 충돌 방지)

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | 기존 `/analyses` → `AnalysisListView.vue` (SC-06), `/analyses/:id` → `AnalysisDetailView.vue` (SC-07). 라우트 변경 없음 |
| 컴포넌트 | 새 `components/analysis/AnalysisManagePanel.vue` (props: `detail`, emits: `updated(detail)`, `deleted`) — 수정 폼·공개 토글·삭제. `AnalysisFlowsTab.vue`에 `readonly` prop (선택·만들기·다시 만들기 숨김). `AnalysisDetailView`: `detail.mine`으로 주인/타인 분기, 타인이면 `TABS`에서 퀴즈 제외, 주인 DONE이면 "커뮤니티에 질문하기" `RouterLink` (#60), FAILED 상태 영역의 "저장소 보기"는 status `sourceUrl` 사용 (#81) |
| store | 없음 (화면 지역 상태) |
| API 함수 | `src/api/analyses.js`: `updateAnalysis(id, {title, memo})`, `updateVisibility(id, isPublic)`, `deleteAnalysis(id)` (JSDoc 타입에 `memo`·`mine`·`owner`·`sourceUrl` 추가) |
| 상태별 UI | 저장·삭제 중 버튼 비활성 / 실패는 서버 `message` / 404 "분석을 찾을 수 없습니다."(기존) / 공개 토글 실패 시 이전 값으로 되돌림 |
| 모바일 | 375px에서 관리 영역 버튼이 줄바꿈되고 페이지 가로 스크롤 없음 |

## 6. LLM (AI 기능만)

해당 없음 (LLM 호출 없음).

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1~AC3, AC11 | `analysis/AnalysisManageApiTest#AC1_…`~`AC3_…`, `#AC11_…` (공백→null, 100·1000자 경계, 남의 공개 분석 404 포함) | MockMvc 통합 |
| AC4, AC5 | `analysis/AnalysisManageApiTest#AC4_…`(팔로워 피드에 원래 활동 등장·비공개 시 빠짐), `#AC5_…`(FAILED·진행 중 409, false는 200, null 400) | MockMvc 통합 |
| AC6~AC8 | `analysis/AnalysisManageApiTest#AC6_…`(자식 3테이블 0행, 퀴즈·풀이 행 유지, 목록·stats·피드·게시글 `analysisDeleted`, 재삭제 404), `#AC7_…`(진행 중 409·행 그대로, FAILED 204), `#AC8_…`(PENDING 흐름 삭제 후 `FlowService.complete`·`markFailed` 0행) | MockMvc 통합 |
| AC9, AC10 | `analysis/AnalysisPublicViewApiTest#AC9_…`(6개 GET 200, mine·owner·memo null), `#AC10_…`(비공개·FAILED·삭제·탈퇴 주인 6개 404, 공개여도 POST flows·quiz·attempts 404) | MockMvc 통합 |
| AC12 | `analysis/AnalysisPublicViewApiTest#AC12_…`(URL FAILED → sourceUrl, ZIP → null) + `AnalysisDetailView.spec.js` FAILED 링크 | MockMvc + Vitest |
| AC13 | `components/analysis/__tests__/AnalysisManagePanel.spec.js`(수정 저장·카운터·공개 토글 DONE만·되돌림·confirm 취소/확인·409 문구), `views/__tests__/AnalysisDetailView.spec.js`(질문 버튼 링크, 삭제 후 `/analyses`) | Vitest |
| AC14 | `views/__tests__/AnalysisDetailView.spec.js`(타인: 닉네임 링크, 관리·퀴즈 탭 없음, `?tab=quiz` → 개요), `components/analysis/__tests__/AnalysisFlowsTab.spec.js`(readonly) | Vitest |
| AC15 | `views/__tests__/AnalysisListView.spec.js`(공개 배지, 메모 첫 줄) | Vitest |
| 계약 | `OpenApiExportTest` → `docs/api/openapi.json`, contract-checker | — |
| 화면 | ui-verifier: SC-06, SC-07 주인(관리 영역)·타인(읽기 전용), 데스크톱·모바일 | Playwright |

## 8. 범위 밖

- 다른 회원의 공개 분석 퀴즈 풀이 (주인만. 팀 공유 풀이는 F-20b. 필요하면 F-20b 머지 뒤 별도 판단)
- 공개 분석 모아 보기·검색, 프로필(SC-18)의 공개 분석 목록 → 2차 기획 `idea`
- 삭제 취소·휴지통, 분석 신고, 비로그인 열람
- 목록(SC-06)에서 바로 수정·삭제·공개 전환 (관리는 SC-07 한 곳. 화면 설계서 SC-06 설명을 구현 PR에서 고친다)
- 회원 탈퇴 시 분석 실제 삭제 (9절)
- 분석 다시 실행·수준 변경

## 9. 결정 필요

- `docs/산출물/09-데이터셋.md` 5절은 "회원 탈퇴(F-03) 시 관련 파일·결과를 삭제한다"고 적었지만, F-03은 비활성화만 한다. 이 spec은 탈퇴 회원의 공개 분석을 남에게 404로 막기만 한다 (AC10). 탈퇴 시 분석 파일을 실제로 지울지는 구현 PR에서 follow-up 이슈(area:F-03·A)로 남기고 문서 문구를 맞춘다
- F-17 게시글 첨부(`PostAnalysisAttachment.viewable`, B 영역)는 주인 탈퇴 여부를 보지 않아, 탈퇴 회원의 공개 분석 첨부는 제목이 보이지만 "분석 보기"를 누르면 404가 된다. 이 spec은 B 코드를 고치지 않고 구현 PR "후속 작업"에 적어 김준섭에게 알린다

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-19-analysis-manage`
- [x] Flyway 마이그레이션 (없음)
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] BE 구현 (green)
- [x] FE 구현 (green)
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [x] `docs/conventions.md` 3절 `ANALYSIS_005`·`ANALYSIS_006`
- [x] contract-checker 통과
- [x] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일)
- [x] reviewer 지적 사항 반영
- [x] 산출물 반영 (`06-화면설계서.md` SC-06·SC-07, `02-유스케이스.md` UC13 대안 흐름, `01-요구사항정의서.md` F-19 행 "공개 분석 열람")
- [x] PR CI 초록 (`Closes #60`, `Closes #81`)
- [x] 상태 DONE (PR #93)
