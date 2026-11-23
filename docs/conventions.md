# 공통 컨벤션 (FE·BE 공용)

> FE와 BE가 이 문서를 기준으로 맞춘다. 바꾸려면 두 사람이 합의하고 `docs/결정로그.md`에 남긴다.
> `contract-checker` 에이전트가 이 문서를 기준으로 검사한다.

## 1. URL

- `/api/` 접두어. 리소스는 **복수형 명사, kebab-case**: `/api/review-plans`, `/api/concepts/{id}/reviews`
- 행위가 리소스로 표현되지 않을 때만 동사 하위 경로를 쓴다: `/api/auth/login`, `/api/users/{id}/follow`
- 관리자 API는 `/api/admin/**` (ROLE_ADMIN만 접근)

## 2. 요청·응답 본문

- JSON 필드는 **camelCase**. DB 컬럼은 snake_case이고 MyBatis `mapUnderscoreToCamelCase=true`로 변환한다.
- Enum 값은 **UPPER_SNAKE** 문자열: `"BEGINNER"`, `"DONE"`
- 날짜·시간은 **ISO-8601 + 오프셋**: `"2026-11-18T14:20:00+09:00"`. 날짜만 필요하면 `"2026-11-18"`
- ID는 숫자(`long`). 프론트에서 문자열로 바꾸지 않는다.

### 성공 응답

봉투(wrapper) 없이 **데이터를 그대로** 반환한다.

| 상황 | 상태 코드 | 본문 |
|---|---|---|
| 조회 | 200 | 객체 또는 목록/페이지 |
| 생성 | 201 | 생성된 리소스 (최소 `{ "id": 1 }`) |
| 수정 | 200 | 수정된 리소스 |
| 삭제, 본문 없는 성공 | 204 | 없음 |
| 비동기 작업 접수 | 202 | `{ "analysisId": 1, "status": "FETCHING" }` |

### 페이지 응답

요청: `?page=1&size=20` (page는 1부터, size 기본 20, 최대 100)

```json
{ "items": [ ... ], "page": 1, "size": 20, "total": 134 }
```

### 실패 응답

```json
{ "code": "CONCEPT_404", "message": "개념 카드를 찾을 수 없습니다.", "details": null }
```

- `message`는 **사용자에게 그대로 보여 줄 수 있는 한국어 문장**.
- `details`는 선택. 검증 실패 시 `{ "필드명": "사유" }`.
- BE는 `@RestControllerAdvice`의 `GlobalExceptionHandler` 한 곳에서 이 형식으로 변환한다. 스택 트레이스는 응답에 넣지 않는다.

## 3. 에러 코드

형식: `<도메인>_<HTTP 상태 or 3자리 번호>`

| 접두어 | 도메인 |
|---|---|
| `COMMON` | 검증 실패(`COMMON_400`), 없는 주소(`COMMON_404`), 허용되지 않은 메서드(`COMMON_405`), 요청 크기 초과(`COMMON_413`: multipart 파일 10MB·요청 전체 12MB, JSON 본문 1MB), 지원하지 않는 Content-Type(`COMMON_415`), 서버 오류(`COMMON_500`) |
| `AUTH` | 인증·토큰 (`AUTH_401` Access 토큰 없음/만료, `AUTH_403` 권한 없음, `AUTH_001` 로그인 실패, `AUTH_002` Refresh 무효·만료 → 다시 로그인) |
| `USER` | 회원, 프로필, 팔로우 (`USER_409` 이메일·닉네임 중복, `details`에 필드 · `USER_404` 없거나 탈퇴한 회원 · `USER_001` 탈퇴 시 비밀번호 불일치, `details.password` · `USER_002` 자기 자신 팔로우·언팔로우) |
| `ANALYSIS` | 분석 요청·파이프라인 (`ANALYSIS_413` 크기 초과, `ANALYSIS_415` 지원하지 않는 스택, `ANALYSIS_429` 일일 한도, `ANALYSIS_001` 그 분석의 파일이 아님(404), `ANALYSIS_002` 개요에 없는 API로 흐름 요청, `ANALYSIS_003` 분석당 흐름 3개 초과, `ANALYSIS_409` 분석이 끝나기 전 흐름 요청, `ANALYSIS_005` 진행 중인 분석 삭제(409), `ANALYSIS_006` 완료되지 않은 분석 공개(409), 저장소 URL 수집 실패 사유 `ANALYSIS_004` 저장소 없음·비공개(404)·`ANALYSIS_502` 저장소에서 받지 못함·`ANALYSIS_503` 저장소 서비스 요청 한도) |
| `CONCEPT` | 개념 카드, 리뷰, 찜 (`CONCEPT_404` 없거나 공개되지 않은 카드, `CONCEPT_001` 수정·삭제할 내 리뷰 없음, `CONCEPT_409` 같은 카드에 두 번째 리뷰, `CONCEPT_002` 관리자 카드 이름 중복, `CONCEPT_003` 공개·숨김 카드를 DRAFT로 되돌리거나 삭제) |
| `QUIZ` | 퀴즈 (`QUIZ_404` 없는 퀴즈·남의 분석 퀴즈·삭제된 분석 퀴즈, 내 분석이지만 퀴즈 없음 · `QUIZ_400` 문항마다 정확히 한 번씩 답하지 않은 제출) |
| `REVIEW_PLAN` | 복습 계획 (`REVIEW_PLAN_404` 없는 계획·남의 계획·공개되지 않은 카드의 계획 · `REVIEW_PLAN_409` 그 카드의 TODO가 이미 있음, 같은 날짜의 계획이 있음, 되돌릴 때 다른 TODO가 있음) |
| `POST` | 게시글, 댓글 (`POST_404` 없는 글, `POST_403` 남의 글 수정·삭제(형식 검증보다 먼저), `POST_001` 첨부할 수 없는 분석(남의·미완료·삭제·없음 구분 안 함), `POST_002` 첨부할 수 없는 개념 카드, `POST_003` 없는 댓글·경로의 글에 속하지 않는 댓글, `POST_004` 남의 댓글 수정·삭제(글 작성자여도, 형식 검증보다 먼저)) |
| `TEAM` | 팀, 초대 코드, 멤버 (`TEAM_404` 없는 팀·내가 멤버가 아닌 팀 · `TEAM_403` 팀원이 팀장 전용 작업(형식 검증보다 먼저) · `TEAM_001` 유효하지 않은 초대 코드(404) · `TEAM_409` 이미 가입한 팀 · `TEAM_002` 팀 인원 10명 가득 참(409) · `TEAM_003` 1인 5개 팀 초과(409, `TEAM_002`보다 먼저) · `TEAM_004` 팀장의 탈퇴·자기 내보내기(400) · `TEAM_005` 내보낼 대상이 그 팀 멤버가 아님(404)) |
| `LLM` | LLM 호출 실패·한도 (`LLM_503` 폴백까지 모두 응답 없음, `LLM_502` 재시도 후에도 응답 형식 오류. 둘 다 내부용 → 파이프라인 단계 FAILED) |
| `CHALLENGE` | 학습 챌린지, 참여 |

새 코드는 spec의 "API 계약 > 에러" 표에 먼저 적고 구현한다.

## 4. 인증

- 헤더: `Authorization: Bearer <accessToken>`
- Access 만료 시 401 + `AUTH_401` → FE 인터셉터가 `/api/auth/refresh` 후 1회 재시도 (`AUTH_001`·`AUTH_002`·`/api/auth/*` 요청은 재시도하지 않음)
- Access 토큰(30분)은 FE 메모리에만 둔다. Refresh 토큰(14일, 회전)은 **HttpOnly 쿠키** `refresh_token`(`SameSite=Strict; Path=/api/auth`)으로만 주고받고 DB에는 SHA-256 해시만 저장한다. 앱 시작 시 refresh로 세션을 복원한다. 상세는 `docs/specs/F-02-login-jwt.md` 1절

## 5. 네이밍

| 대상 | 규칙 | 예 |
|---|---|---|
| DB 테이블·컬럼 | snake_case, 테이블 복수형 | `concept_reviews.user_id` |
| Java 클래스 | PascalCase + 역할 접미어 | `ConceptController`, `ConceptService`, `ConceptMapper`, `ConceptResponse` |
| Java DTO | `XxxRequest` / `XxxResponse` (record) | `SignupRequest` |
| Vue 컴포넌트 | PascalCase, 화면은 `XxxView.vue` | `ConceptCard.vue`, `ConceptDetailView.vue` |
| FE API 함수 | 동사 + 리소스, camelCase | `fetchConcepts`, `createReview` |
| Pinia store | `useXxxStore`, 파일은 `xxx.js` | `useAuthStore` in `stores/auth.js` |
| 브랜치 | `feature/F-xx-slug`, `fix/F-xx-slug`, follow-up 이슈만 처리하면 `feature/issue-<번호>-slug` | `feature/F-05-analysis-request`, `feature/issue-26-endpoint-case` |

## 6. 커밋 메시지

`타입(범위): 요약` — git `commit-msg` hook이 검사한다.

- 타입: `feat` `fix` `refactor` `test` `wip` (범위 필수) / `docs` `chore` (범위 생략 가능)
- 범위: `F-xx` (기능 ID. 분할 spec은 `F-05a`·`F-05b`처럼 소문자 접미사), `common` (여러 기능에 걸친 공통 코드), `harness` (하네스·도구)
- 예: `feat(F-05): 분석 요청 API 추가`, `wip(F-05): 교대 체크포인트`, `chore(common): Spotless 설정`
- `wip`가 아닌 커밋은 변경된 영역(backend/frontend)의 테스트가 통과해야 커밋된다.

## 7. Pull Request (GitHub)

- 대상: `feature/*` → `develop`. 마일스톤 마감 때 `develop` → `main`.
- **제목은 커밋 메시지와 같은 형식** `타입(F-xx): 요약` (CI가 검사한다).
- 본문은 `.github/pull_request_template.md`를 채운다 (`/pr-review`가 생성).
- **리뷰는 reviewer 에이전트(`/pr-review`)와 Codex 봇이 한다.** 사람 리뷰 승인은 받지 않는다.
- **CI가 초록이고 Codex가 COMPLETE일 때만** 영역 오너(PR 작성자 본인)가 머지한다. 예외: `docs/`만 바꾸는 주간 `/progress` PR은 Codex 재리뷰를 기다리지 않고 CI 초록이면 머지한다(자동 리뷰에 지적이 달리면 읽고 판단한다). head 커밋에 남은 Codex 지적이 모두 P2이고 각각 `follow-up` 이슈로 넘겨 스레드에 이슈 번호를 답했으면 `node scripts/codex-review.mjs <PR> --resolve`로 이관 완료를 표시한다(PR 설명 "후속 작업"에 기록). wip 커밋이 섞여 있으면 **Squash and merge**.
- **공통 코드**(SecurityConfig, GlobalExceptionHandler, 공통 응답·ErrorCode, FE 라우터·레이아웃·공용 컴포넌트·api 클라이언트, 빌드 설정)를 바꾼 PR은 본문에서 상대를 @멘션한다 (A `@whans1996`, B `@JoonseobKim`).
- **develop 동기화**: 상대 PR이 먼저 머지되면 `git merge origin/develop`으로 받는다 (rebase·force push 금지). Claude 세션에서는 세션 시작 hook과 `peer-watch` hook(10분 간격)이 fetch 직후 자동으로 합친다 (`scripts/lib/sync.mjs`): ① 다른 PC가 push한 내 브랜치 커밋(보호 브랜치는 fast-forward만) ② 기능 브랜치(`feature/`·`fix/`)면 origin/develop ③ 체크아웃하지 않은 로컬 `develop`·`main`을 origin으로 fast-forward(작업 트리와 무관해 커밋 안 된 변경이 있어도 한다. 로컬에만 있는 커밋이 있으면 건드리지 않고 알린다). fetch는 `--prune`으로 지워진 원격 브랜치를 정리한다. 추적 파일에 커밋 안 된 변경이 있거나 병합 중이면 미루고 알리며, 충돌하면 `merge --abort`로 되돌리고 충돌 파일을 알린다. 병합 커밋은 push하지 않는다(다음 push 때 함께). PC별로 끄려면 `git config vibenote.autoSync false`. 머지 전에 한 번 더 합치고 CI 초록을 확인한다. `docs/api/openapi.json`이 충돌하면 한쪽을 고른 뒤 BE 테스트(`OpenApiExportTest`)로 다시 생성한다. Flyway 번호가 겹치면(CI `check-migrations`) develop에 없는 내 파일만 `git mv`로 다음 번호로 바꾼다.
- **리뷰 지적 처리 기준**: 같은 PR에서 고치는 것은 spec 수용 기준이나 보안 기준(인증·인가, 비밀 누출, 경로 조작, 주입, 서비스 중단)을 깨는 지적만이다. 그 밖의 지적은 `follow-up` 라벨 이슈로 넘기고 PR 댓글로 이슈 번호를 답한다. 같은 주제로 재리뷰를 2회 했는데 또 지적이 나오면 사람이 정한다.
- **후속 작업은 GitHub Issues(`follow-up` 라벨)로 관리한다.** NOW 파일·spec·PR 설명에는 이슈 번호만 적는다. 만들 때 `area:*` 라벨과 담당자를 함께 단다(9절, 본문 형식은 `.github/ISSUE_TEMPLATE/follow-up.md`).
  - **차단**: 누군가의 작업(커밋·머지·다음 기능)을 막는 이슈에는 `blocker` 라벨을 더한다(`follow-up`과 함께). 세션 시작 배너는 영역과 무관하게 차단 이슈를 🔴로 맨 앞에 보여 주고, `/pickup`·`/spec`은 차단 이슈가 열려 있으면 멈추고 사용자에게 묻는다. 해결되면 닫고, 막지 않게 되면 라벨만 뗀다.
  - **순환**: 세션 시작 배너와 `/pickup`이 차단·내 영역·공통·분류 필요 이슈를 보여 주고, `/spec`은 그 기능에 흡수할 이슈를 spec "관련 이슈"에 적고, `/progress`는 영역별 열림·7일 넘음을 점검한다. 기능 사이 남는 시간에는 내 영역 이슈를 `feature/issue-<번호>-slug` 브랜치에서 처리한다. 이슈 브랜치는 spec 대신 이슈 본문 "제안"을 수용 기준으로 삼고(실패하는 테스트부터), 커밋 범위에는 이슈의 원래 기능 F-xx(없으면 `common`·`harness`)를 쓰며, `/pr-review`의 PR 설명 파일은 `.harness/pr-issue-<번호>.md`로 한다.
  - **닫기**: 해결 PR 본문 "해결 이슈"에 이슈마다 `Closes #n`을 적는다. develop은 기본 브랜치가 아니라 GitHub가 닫지 않으므로 `close-issues` 워크플로가 develop 머지 때 닫는다. 고치지 않기로 하면 이유를 댓글로 남기고 `gh issue close <n> --reason "not planned"`.
- **완료 표시(spec DONE)는 머지 전 마지막 커밋으로 PR 안에서 한다.** 머지 후 정리 PR은 만들지 않는다. 진행현황·WBS·간트 실적은 기능 PR에서 고치지 않고 주간 `/progress` docs PR이 develop 기준으로 맞춘다 (두 PR이 같은 표를 고치면 충돌한다, 11-19 결정).
- API를 바꾼 PR은 `docs/api/openapi.json` 변경을 포함해야 한다 (CI가 불일치를 실패 처리).
- GitLab(lab.ssafy.com)은 최종 제출 전용이다. 개발 중에는 push하지 않는다.

## 8. API 문서 (Swagger)

- 모든 Controller에 `@Tag`, 모든 엔드포인트에 `@Operation(summary)`과 에러 `@ApiResponse`, 모든 요청·응답 DTO 필드에 `@Schema(description, example)`를 단다.
- Tag 이름은 한국어 도메인 이름으로 통일한다: `인증`, `회원`, `팔로우`, `피드`, `분석`, `개념 카드`, `퀴즈`, `복습 계획`, `추천`, `커뮤니티`, `팀`, `챌린지`, `관리자`.
- 정본은 테스트가 추출하는 `docs/api/openapi.json`이다. 제출용 REST API 명세(`docs/산출물/07-REST-API-명세.md`)는 이 파일에서 생성한다.

## 9. 병렬 개발 경계 (11-19)

- **영역 정본은 `.github/CODEOWNERS`**다. 한 사람만 적힌 경로는 그 사람 영역, 두 사람이 적힌 경로는 공통이다. 새 도메인 패키지나 FE 폴더를 만들면 같은 PR에서 한 줄 추가한다. FE views·components는 도메인 폴더(`views/quiz/`, `components/community/` 등)에 둔다.
- 파일 영역 확인: `node scripts/owners.mjs <경로...>`, PR 전 `node scripts/owners.mjs --diff origin/develop`. Claude가 상대 영역·공통 파일을 고치면 `area-notice` hook이 알리고(차단하지 않음), 상대 NOW 파일은 `guard-files`가 막는다.
- **공통 기반은 A가 먼저 만든다** (결정로그 11-19): concept 도메인·Mapper·카드 응답 DTO, ErrorCode·ActivityType 도메인 블록, 라우터·nav 영역별 분리, 공용 컴포넌트, 관리자 레이아웃. B는 공통 기반 PR이 develop에 머지된 뒤 그 위에서 작업한다.
- **공통 enum·배열은 도메인 블록 안에만 추가한다** (ErrorCode, ActivityType, `PUBLIC_GET_PATHS`, `ConceptMapper` Java·XML). 블록은 `// ── 도메인 (영역 기능) ──` 주석으로 나누고 모든 항목 끝에 쉼표를 단다(enum의 `;`는 맨 아래 단독 줄). 파일 끝에 이어 붙이면 두 사람의 추가가 같은 줄에서 충돌한다.
- **FE 라우트·메뉴는 도메인 파일과 route 메타로 나눈다** (F-00b): route는 `frontend/src/router/routes/<도메인>.js`, 헤더 메뉴는 `meta.nav`, 관리자 메뉴는 `/admin` 자식의 `meta.adminNav`. 상세는 `frontend/CLAUDE.md`.
- **상대 테이블**은 상대 도메인의 Mapper·Service를 통해 읽는다. 쓰기(INSERT·UPDATE)는 그 테이블 주인의 Service만 한다. 필요한 메서드가 없으면 상대에게 요청하거나 PR에서 @멘션하고 최소로 추가한다.
- **push 시점**: `/implement` 체크포인트 커밋, spec 승인 커밋, `/handoff` 뒤에 feature 브랜치를 push한다. 상대의 세션 시작·peer-watch hook과 Mattermost 알림은 원격의 NOW 파일을 본다.
- **알림 경로**: Claude 세션 안(세션 시작 hook, 10분 간격 `peer-watch` hook), Mattermost(`.github/workflows/notify.yml`, PR 열림·머지·NOW push). 급한 합의(공통 파일 동시 수정 등)는 Mattermost에서 직접 말한다.
- 이슈 라벨: `area:A`, `area:B`, `area:common`을 `follow-up`과 함께 단다. 이슈 담당은 라벨의 영역 오너다 (공통은 만든 사람). 만드는 명령: `gh issue create --label follow-up --label area:<A|B|common> --assignee @me` (내 영역 라벨은 `node scripts/stamp.mjs --area`). 세션 시작 배너는 area 라벨이 없는 이슈를 "분류 필요"로 센다.
