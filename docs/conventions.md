# 공통 컨벤션 (FE·BE 공용)

> FE와 BE가 이 문서를 기준으로 맞춘다. 바꾸려면 두 사람이 합의하고 `docs/기획서.md` 결정 로그에 남긴다.
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
| `COMMON` | 검증 실패(`COMMON_400`), 없는 주소(`COMMON_404`), 허용되지 않은 메서드(`COMMON_405`), 업로드 크기 초과(`COMMON_413`), 지원하지 않는 Content-Type(`COMMON_415`), 서버 오류(`COMMON_500`) |
| `AUTH` | 인증·토큰 (`AUTH_401` Access 토큰 없음/만료, `AUTH_403` 권한 없음, `AUTH_001` 로그인 실패, `AUTH_002` Refresh 무효·만료 → 다시 로그인) |
| `USER` | 회원, 프로필, 팔로우 (`USER_409` 이메일·닉네임 중복, `details`에 필드 · `USER_404` 없거나 탈퇴한 회원 · `USER_001` 탈퇴 시 비밀번호 불일치, `details.password`) |
| `ANALYSIS` | 분석 요청·파이프라인 (`ANALYSIS_413` 크기 초과, `ANALYSIS_415` 지원하지 않는 스택, `ANALYSIS_429` 일일 한도) |
| `CONCEPT` | 개념 카드, 리뷰, 찜 |
| `QUIZ` | 퀴즈 |
| `REVIEW_PLAN` | 복습 계획 |
| `POST` | 게시글, 댓글 |
| `LLM` | LLM 호출 실패·한도 (`LLM_503` 폴백까지 모두 응답 없음, 내부용 → 파이프라인 단계 FAILED) |
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
| 브랜치 | `feature/F-xx-slug`, `fix/F-xx-slug` | `feature/F-05-analysis-request` |

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
- **CI가 초록이고 Codex가 COMPLETE일 때만** 영역 오너가 머지한다. wip 커밋이 섞여 있으면 **Squash and merge**.
- **리뷰 지적 처리 기준**: 같은 PR에서 고치는 것은 spec 수용 기준이나 보안 기준(인증·인가, 비밀 누출, 경로 조작, 주입, 서비스 중단)을 깨는 지적만이다. 그 밖의 지적은 `follow-up` 라벨 이슈로 넘기고 PR 댓글로 이슈 번호를 답한다. 같은 주제로 재리뷰를 2회 했는데 또 지적이 나오면 사람이 정한다.
- **후속 작업은 GitHub Issues(`follow-up` 라벨)로 관리한다.** NOW.md·spec·PR 설명에는 이슈 번호만 적는다.
- **완료 표시(spec DONE, WBS·간트 실적)는 머지 전 마지막 커밋으로 PR 안에서 한다.** 머지 후 정리 PR은 만들지 않는다.
- API를 바꾼 PR은 `docs/api/openapi.json` 변경을 포함해야 한다 (CI가 불일치를 실패 처리).
- GitLab(lab.ssafy.com)은 최종 제출 전용이다. 개발 중에는 push하지 않는다.

## 8. API 문서 (Swagger)

- 모든 Controller에 `@Tag`, 모든 엔드포인트에 `@Operation(summary)`과 에러 `@ApiResponse`, 모든 요청·응답 DTO 필드에 `@Schema(description, example)`를 단다.
- Tag 이름은 한국어 도메인 이름으로 통일한다: `인증`, `회원`, `팔로우`, `피드`, `분석`, `개념 카드`, `퀴즈`, `복습 계획`, `추천`, `커뮤니티`, `챌린지`, `관리자`.
- 정본은 테스트가 추출하는 `docs/api/openapi.json`이다. 제출용 REST API 명세(`docs/산출물/07-REST-API-명세.md`)는 이 파일에서 생성한다.
