# REST API 명세 — 바이브노트

> **자동 생성 문서다. 손으로 고치지 않는다.** 다시 만들기: `node scripts/export-api-doc.mjs` (주간 `/progress`와 제출 전).
> **정본은 Swagger**다. 실행 중이면 http://localhost:8080/swagger-ui/index.html, 파일은 [`docs/api/openapi.json`](../api/openapi.json) (테스트 `OpenApiExportTest`가 생성, CI가 코드와 일치 여부 검사).
> VibeNote API v1 · 엔드포인트 89개 · 스키마 116개

## 공통 규칙

- 응답·에러 형식, 에러 코드 체계, 페이지네이션, 인증 헤더: [`docs/conventions.md`](../conventions.md)
- 인증: `Authorization: Bearer <accessToken>` (Swagger의 Authorize 버튼에 Access 토큰 입력)
- 권한: **공개**는 토큰 없이, **로그인**은 Access 토큰, **ADMIN**은 관리자 계정 토큰이 필요하다. 에러 응답의 괄호 안이 에러 코드다

## 엔드포인트 목록

### 관리자

| 메서드 | URL | 요약 | 권한 |
|---|---|---|---|
| `GET` | /api/admin/challenges | 관리자 챌린지 목록 | ADMIN |
| `POST` | /api/admin/challenges | 챌린지 등록 | ADMIN |
| `GET` | /api/admin/challenges/{id} | 관리자 챌린지 상세 | ADMIN |
| `PUT` | /api/admin/challenges/{id} | 챌린지 수정 | ADMIN |
| `DELETE` | /api/admin/challenges/{id} | 챌린지 삭제 | ADMIN |
| `GET` | /api/admin/concepts | 관리자 카드 목록 | ADMIN |
| `POST` | /api/admin/concepts | 카드 생성 | ADMIN |
| `GET` | /api/admin/concepts/status-counts | 상태별 카드 수 | ADMIN |
| `GET` | /api/admin/concepts/{id} | 관리자 카드 상세 | ADMIN |
| `PUT` | /api/admin/concepts/{id} | 카드 수정·검수 | ADMIN |
| `DELETE` | /api/admin/concepts/{id} | 초안 삭제 | ADMIN |

### 회원

| 메서드 | URL | 요약 | 권한 |
|---|---|---|---|
| `GET` | /api/users/me | 내 프로필 조회 | 로그인 |
| `PATCH` | /api/users/me | 내 프로필 수정 | 로그인 |
| `DELETE` | /api/users/me | 회원 탈퇴 | 로그인 |
| `GET` | /api/users/{id} | 공개 프로필 조회 | 공개 |
| `GET` | /api/users/{id}/stats | 학습 통계 | 공개 |

### 커뮤니티

| 메서드 | URL | 요약 | 권한 |
|---|---|---|---|
| `GET` | /api/posts | 게시글 목록·검색 | 공개 |
| `POST` | /api/posts | 게시글 작성 | 로그인 |
| `GET` | /api/posts/{postId} | 게시글 상세 | 공개 |
| `PUT` | /api/posts/{postId} | 게시글 수정 | 로그인 |
| `DELETE` | /api/posts/{postId} | 게시글 삭제 | 로그인 |
| `GET` | /api/posts/{postId}/comments | 댓글 목록 | 공개 |
| `POST` | /api/posts/{postId}/comments | 댓글 작성 | 로그인 |
| `PUT` | /api/posts/{postId}/comments/{commentId} | 댓글 수정 | 로그인 |
| `DELETE` | /api/posts/{postId}/comments/{commentId} | 댓글 삭제 | 로그인 |

### 인증

| 메서드 | URL | 요약 | 권한 |
|---|---|---|---|
| `POST` | /api/auth/login | 로그인 | 공개 |
| `POST` | /api/auth/logout | 로그아웃 | 공개 |
| `POST` | /api/auth/refresh | 토큰 재발급 | 공개 |
| `POST` | /api/auth/signup | 회원가입 | 공개 |

### 분석

| 메서드 | URL | 요약 | 권한 |
|---|---|---|---|
| `GET` | /api/analyses | 내 분석 목록 | 로그인 |
| `POST` | /api/analyses | 분석 요청 (ZIP·저장소 URL) | 로그인 |
| `GET` | /api/analyses/{id} | 분석 상세 | 로그인 |
| `PATCH` | /api/analyses/{id} | 분석 제목·메모 수정 | 로그인 |
| `DELETE` | /api/analyses/{id} | 분석 삭제 | 로그인 |
| `GET` | /api/analyses/{id}/concepts | 분석의 개념 카드 | 로그인 |
| `GET` | /api/analyses/{id}/files | 분석 파일 목록 | 로그인 |
| `GET` | /api/analyses/{id}/files/{fileId} | 분석 파일 하나 | 로그인 |
| `GET` | /api/analyses/{id}/flows | 실행 흐름 목록 | 로그인 |
| `POST` | /api/analyses/{id}/flows | 실행 흐름 요청 | 로그인 |
| `POST` | /api/analyses/{id}/rerun | 실패한 저장소 분석 재실행 | 로그인 |
| `GET` | /api/analyses/{id}/status | 분석 진행 상태 | 로그인 |
| `GET` | /api/analyses/{id}/structure | 구조 그래프 | 로그인 |
| `PATCH` | /api/analyses/{id}/visibility | 분석 공개 설정 | 로그인 |

### 복습 계획

| 메서드 | URL | 요약 | 권한 |
|---|---|---|---|
| `GET` | /api/review-plans | 복습 계획 조회 | 로그인 |
| `POST` | /api/review-plans | 복습 계획 직접 추가 | 로그인 |
| `PATCH` | /api/review-plans/{id} | 복습 완료·되돌림 | 로그인 |
| `DELETE` | /api/review-plans/{id} | 복습 계획 삭제 | 로그인 |

### 피드

| 메서드 | URL | 요약 | 권한 |
|---|---|---|---|
| `GET` | /api/feed | 내 피드 | 로그인 |

### 팔로우

| 메서드 | URL | 요약 | 권한 |
|---|---|---|---|
| `GET` | /api/users/{id}/follow | 팔로우 상태 | 공개 |
| `POST` | /api/users/{id}/follow | 팔로우 | 로그인 |
| `DELETE` | /api/users/{id}/follow | 언팔로우 | 로그인 |
| `GET` | /api/users/{id}/followers | 팔로워 목록 | 공개 |
| `GET` | /api/users/{id}/followings | 팔로잉 목록 | 공개 |

### 개념 카드

| 메서드 | URL | 요약 | 권한 |
|---|---|---|---|
| `GET` | /api/concepts | 개념 카드 목록·검색 | 공개 |
| `GET` | /api/concepts/{conceptId}/reviews | 카드 리뷰 목록 | 공개 |
| `POST` | /api/concepts/{conceptId}/reviews | 리뷰 작성 | 로그인 |
| `GET` | /api/concepts/{conceptId}/reviews/me | 내 리뷰 조회 | 로그인 |
| `PUT` | /api/concepts/{conceptId}/reviews/me | 내 리뷰 수정 | 로그인 |
| `DELETE` | /api/concepts/{conceptId}/reviews/me | 내 리뷰 삭제 | 로그인 |
| `POST` | /api/concepts/{conceptId}/wish | 카드 찜 | 로그인 |
| `DELETE` | /api/concepts/{conceptId}/wish | 카드 찜 해제 | 로그인 |
| `GET` | /api/concepts/{id} | 개념 카드 상세 | 공개 |
| `GET` | /api/users/{userId}/wishes | 회원의 찜 목록 | 공개 |

### 퀴즈

| 메서드 | URL | 요약 | 권한 |
|---|---|---|---|
| `GET` | /api/analyses/{id}/quiz | 분석의 퀴즈 조회 | 로그인 |
| `POST` | /api/analyses/{id}/quiz | 퀴즈 다시 만들기 | 로그인 |
| `POST` | /api/quizzes/{quizId}/attempts | 퀴즈 풀이 제출·채점 | 로그인 |

### 챌린지

| 메서드 | URL | 요약 | 권한 |
|---|---|---|---|
| `GET` | /api/challenges | 챌린지 목록 | 공개 |
| `GET` | /api/challenges/{id} | 챌린지 상세 | 공개 |
| `POST` | /api/challenges/{id}/participation | 챌린지 참여 | 로그인 |
| `DELETE` | /api/challenges/{id}/participation | 챌린지 참여 취소 | 로그인 |
| `GET` | /api/users/me/challenges | 내 챌린지 | 로그인 |
| `GET` | /api/users/{userId}/challenges | 회원의 참여 챌린지 | 공개 |

### 추천

| 메서드 | URL | 요약 | 권한 |
|---|---|---|---|
| `GET` | /api/recommendations | 맞춤 추천 카드 조회 | 로그인 |

### 팀

| 메서드 | URL | 요약 | 권한 |
|---|---|---|---|
| `POST` | /api/teams | 팀 만들기 | 로그인 |
| `POST` | /api/teams/join | 초대 코드로 가입 | 로그인 |
| `GET` | /api/teams/{id} | 팀 상세 | 로그인 |
| `PUT` | /api/teams/{id} | 팀 정보 수정 (팀장) | 로그인 |
| `DELETE` | /api/teams/{id} | 팀 삭제 (팀장) | 로그인 |
| `GET` | /api/teams/{id}/analyses | 팀 공유 분석 목록 | 로그인 |
| `POST` | /api/teams/{id}/analyses | 팀에 분석 공유 | 로그인 |
| `DELETE` | /api/teams/{id}/analyses/{analysisId} | 팀 분석 공유 해제 (공유한 사람·팀장) | 로그인 |
| `GET` | /api/teams/{id}/analyses/{analysisId}/understanding | 팀 이해도 (모듈별·멤버별) | 로그인 |
| `GET` | /api/teams/{id}/challenges | 팀 목표 목록 | 로그인 |
| `POST` | /api/teams/{id}/challenges | 팀 목표 만들기 (팀장) | 로그인 |
| `DELETE` | /api/teams/{id}/challenges/{challengeId} | 팀 목표 삭제 (팀장) | 로그인 |
| `POST` | /api/teams/{id}/invite-code | 초대 코드 재발급 (팀장) | 로그인 |
| `DELETE` | /api/teams/{id}/members/me | 팀 탈퇴 (팀원) | 로그인 |
| `DELETE` | /api/teams/{id}/members/{userId} | 멤버 내보내기 (팀장) | 로그인 |
| `GET` | /api/users/me/teams | 내 팀 목록 | 로그인 |

## 엔드포인트 상세

### 관리자

#### `GET` /api/admin/challenges

**관리자 챌린지 목록** · 권한: ADMIN

상태(오늘 기준으로 계산)로 거르고 시작일 최신순(같으면 id 역순)으로 페이지 조회한다. 항목마다 참여자 수가 있다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `status` | query | 선택 | string: UPCOMING · ONGOING · ENDED | 상태 (없으면 전체). 오늘(Asia/Seoul)과 기간으로 계산한다 | `ONGOING` |
| `page` | query | 선택 | integer(int32) | 페이지 번호 (1부터) (기본 `1`, 범위 1~) | `1` |
| `size` | query | 선택 | integer(int32) | 페이지 크기 (최대 100) (기본 `20`, 범위 1~100) | `20` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [PageResponseAdminChallengeSummaryResponse](#pageresponseadminchallengesummaryresponse) |
| 400 | 상태·페이지 값 오류 (COMMON_400, details에 필드별 사유) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | ADMIN이 아님 (AUTH_403) | [ErrorResponse](#errorresponse) |

#### `POST` /api/admin/challenges

**챌린지 등록** · 권한: ADMIN

기간·목표·대표 이미지(선택)로 챌린지를 만든다. 종료일은 오늘 이후, 기간은 90일 이하이고 시작일은 과거여도 된다. 이미지는 /uploads/challenges/{UUID}.{ext}로 공개된다.

**요청 본문** (`multipart/form-data`, 선택): [AdminChallengeRequest](#adminchallengerequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 201 | 등록 성공 | [AdminChallengeCreateResponse](#adminchallengecreateresponse) |
| 400 | 입력값·이미지 오류 (COMMON_400, details에 필드별 사유, 이미지는 details.image) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | ADMIN이 아님 (AUTH_403) | [ErrorResponse](#errorresponse) |
| 413 | 요청 크기 한도 초과 (COMMON_413, 파일 10MB·요청 12MB) | [ErrorResponse](#errorresponse) |

#### `GET` /api/admin/challenges/{id}

**관리자 챌린지 상세** · 권한: ADMIN

설명·상태·참여자 수·등록·수정 시각을 포함한다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 챌린지 ID | `7` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [AdminChallengeResponse](#adminchallengeresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | ADMIN이 아님 (AUTH_403) | [ErrorResponse](#errorresponse) |
| 404 | 없는 챌린지 (CHALLENGE_404) | [ErrorResponse](#errorresponse) |

#### `PUT` /api/admin/challenges/{id}

**챌린지 수정** · 권한: ADMIN

전체 교체한다. image가 없고 removeImage가 false면 기존 이미지를 유지한다. 시작한(진행 중·종료) 챌린지는 목표 유형·목표값·시작일을 바꿀 수 없다. 종료일 ≥ 오늘은 종료일이 바뀔 때만 검사한다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 챌린지 ID | `7` |

**요청 본문** (`multipart/form-data`, 선택): [AdminChallengeRequest](#adminchallengerequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 수정 성공 | [AdminChallengeResponse](#adminchallengeresponse) |
| 400 | 입력값·이미지 오류, image와 removeImage=true를 함께 보냄 (COMMON_400, details에 필드별 사유) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | ADMIN이 아님 (AUTH_403) | [ErrorResponse](#errorresponse) |
| 404 | 없는 챌린지 (CHALLENGE_404) | [ErrorResponse](#errorresponse) |
| 409 | 시작한 챌린지의 목표 유형·목표값·시작일 변경 (CHALLENGE_001) | [ErrorResponse](#errorresponse) |
| 413 | 요청 크기 한도 초과 (COMMON_413, 파일 10MB·요청 12MB) | [ErrorResponse](#errorresponse) |

#### `DELETE` /api/admin/challenges/{id}

**챌린지 삭제** · 권한: ADMIN

언제든 실제로 지운다. 참여 기록도 함께 지워지고 대표 이미지 파일도 지운다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 챌린지 ID | `7` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 204 | 삭제 성공 | - |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | ADMIN이 아님 (AUTH_403) | [ErrorResponse](#errorresponse) |
| 404 | 없는 챌린지 (CHALLENGE_404) | [ErrorResponse](#errorresponse) |

#### `GET` /api/admin/concepts

**관리자 카드 목록** · 권한: ADMIN

상태와 상관없이 모든 카드를 상태·출처·키워드(이름·요약 부분 일치)로 거르고 수정일 최신순으로 페이지 조회한다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `status` | query | 선택 | string: DRAFT · PUBLISHED · HIDDEN | 상태 (없으면 전체) | `DRAFT` |
| `source` | query | 선택 | string: SEED · AI · ADMIN | 출처 (없으면 전체) | `AI` |
| `keyword` | query | 선택 | string | 이름·요약 부분 일치 검색어 (trim 후 50자 이하, 대소문자 무시) (길이 0~50자) | `락` |
| `page` | query | 선택 | integer(int32) | 페이지 번호 (1부터) (기본 `1`, 범위 1~) | `1` |
| `size` | query | 선택 | integer(int32) | 페이지 크기 (최대 100) (기본 `20`, 범위 1~100) | `20` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [PageResponseAdminConceptSummaryResponse](#pageresponseadminconceptsummaryresponse) |
| 400 | 조건·페이지 값 오류 (COMMON_400, details에 필드별 사유) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | ADMIN이 아님 (AUTH_403) | [ErrorResponse](#errorresponse) |

#### `POST` /api/admin/concepts

**카드 생성** · 권한: ADMIN

관리자가 카드를 직접 만든다 (출처 ADMIN). 상태는 DRAFT(초안)·PUBLISHED(바로 공개)만 받는다.

**요청 본문** (`application/json`, 필수): [AdminConceptRequest](#adminconceptrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 201 | 생성 성공 | [AdminConceptCreateResponse](#adminconceptcreateresponse) |
| 400 | 입력값 오류·HIDDEN으로 생성 (COMMON_400, details에 필드별 사유) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | ADMIN이 아님 (AUTH_403) | [ErrorResponse](#errorresponse) |
| 409 | 같은 이름의 카드가 있음 (CONCEPT_002, details.name) | [ErrorResponse](#errorresponse) |

#### `GET` /api/admin/concepts/status-counts

**상태별 카드 수** · 권한: ADMIN

검수 대기·공개·숨김 카드 수. 카드가 없는 상태도 0으로 준다.

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [AdminConceptStatusCountsResponse](#adminconceptstatuscountsresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | ADMIN이 아님 (AUTH_403) | [ErrorResponse](#errorresponse) |

#### `GET` /api/admin/concepts/{id}

**관리자 카드 상세** · 권한: ADMIN

상태와 상관없이 카드의 모든 내용과 연결된 분석 수·리뷰 수를 조회한다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 카드 ID | `91` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [AdminConceptDetailResponse](#adminconceptdetailresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | ADMIN이 아님 (AUTH_403) | [ErrorResponse](#errorresponse) |
| 404 | 없는 카드 (CONCEPT_404) | [ErrorResponse](#errorresponse) |

#### `PUT` /api/admin/concepts/{id}

**카드 수정·검수** · 권한: ADMIN

내용과 상태를 전체 교체한다. 초안 공개(DRAFT→PUBLISHED)·숨김·다시 공개에 쓴다. 출처·찜 수·평점은 바뀌지 않는다. 공개·숨김 카드는 DRAFT로 되돌릴 수 없고, 시드 카드의 이름은 바꿀 수 없다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 카드 ID | `91` |

**요청 본문** (`application/json`, 필수): [AdminConceptRequest](#adminconceptrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 수정 성공 | [AdminConceptDetailResponse](#adminconceptdetailresponse) |
| 400 | 입력값 오류·시드 카드 이름 변경 (COMMON_400, details에 필드별 사유) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | ADMIN이 아님 (AUTH_403) | [ErrorResponse](#errorresponse) |
| 404 | 없는 카드 (CONCEPT_404) | [ErrorResponse](#errorresponse) |
| 409 | 같은 이름의 카드가 있음 (CONCEPT_002) / 공개·숨김 카드를 DRAFT로 (CONCEPT_003) | [ErrorResponse](#errorresponse) |

#### `DELETE` /api/admin/concepts/{id}

**초안 삭제** · 권한: ADMIN

검수 대기(DRAFT) 카드와 그 분석 연결을 지운다. 공개 이력이 있는 카드의 찜·리뷰가 사라지지 않게 공개·숨김 카드는 지울 수 없다 (숨김을 쓴다).

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 카드 ID | `91` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 204 | 삭제 성공 | - |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | ADMIN이 아님 (AUTH_403) | [ErrorResponse](#errorresponse) |
| 404 | 없는 카드 (CONCEPT_404) | [ErrorResponse](#errorresponse) |
| 409 | 공개·숨김 카드 (CONCEPT_003) | [ErrorResponse](#errorresponse) |

### 회원

#### `GET` /api/users/me

**내 프로필 조회** · 권한: 로그인

email·role을 포함한 내 정보를 준다.

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [MyProfileResponse](#myprofileresponse) |
| 401 | 토큰 없음·만료·탈퇴 회원 (AUTH_401) | [ErrorResponse](#errorresponse) |

#### `PATCH` /api/users/me

**내 프로필 수정** · 권한: 로그인

닉네임·소개·학습 수준 중 보낸 필드만 바꾼다. null·생략은 그대로, 소개를 빈 문자열로 보내면 지운다.

**요청 본문** (`application/json`, 필수): [UpdateProfileRequest](#updateprofilerequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 수정 성공. 수정된 내 프로필 | [MyProfileResponse](#myprofileresponse) |
| 400 | 검증 실패 (COMMON_400, details에 필드별 사유) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료·탈퇴 회원 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 409 | 다른 회원이 쓰는 닉네임 (USER_409, details.nickname) | [ErrorResponse](#errorresponse) |

#### `DELETE` /api/users/me

**회원 탈퇴** · 권한: 로그인

현재 비밀번호를 확인한 뒤 비활성화(WITHDRAWN)하고 이메일·닉네임·소개를 마스킹한다. 모든 기기의 Refresh 토큰과 팔로우 관계, 팀 멤버 정보(팀장인 팀은 삭제)를 지우고, 작성한 글·댓글·리뷰와 퀴즈 기록은 남긴다. 같은 이메일로 다시 가입할 수 있다.

**요청 본문** (`application/json`, 필수): [WithdrawRequest](#withdrawrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 204 | 탈퇴 성공 | - |
| 400 | 비밀번호 누락 (COMMON_400) 또는 불일치 (USER_001), details.password | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료·이미 탈퇴 (AUTH_401) | [ErrorResponse](#errorresponse) |

#### `GET` /api/users/{id}

**공개 프로필 조회** · 권한: 공개

로그인 없이 볼 수 있다. email·role은 주지 않는다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [UserProfileResponse](#userprofileresponse) |
| 404 | 없는 회원 또는 탈퇴 회원 (USER_404) | [ErrorResponse](#errorresponse) |

#### `GET` /api/users/{id}/stats

**학습 통계** · 권한: 공개

완료한 분석 수(삭제 제외), 학습한 카드 수(퀴즈 정답 카드 ∪ 복습 완료 카드), 퀴즈 정답률(%, 풀이가 없으면 null). 로그인 없이 볼 수 있다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [UserStatsResponse](#userstatsresponse) |
| 404 | 없는 회원 또는 탈퇴 회원 (USER_404) | [ErrorResponse](#errorresponse) |

### 커뮤니티

#### `GET` /api/posts

**게시글 목록·검색** · 권한: 공개

게시판 구분 없이 또는 한 게시판의 글을 최신순(작성 시각 내림차순)으로 페이지 조회한다. keyword는 제목·본문 부분 일치. 비로그인도 쓸 수 있다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `boardType` | query | 선택 | string: QNA · REVIEW · FREE | 게시판 (QNA: 코드 이해 Q&A, REVIEW: 학습 후기, FREE: 자유). 없으면 전체 | `QNA` |
| `keyword` | query | 선택 | string | 제목·본문 부분 일치 검색어 (trim 후 50자 이하, 대소문자 무시) (길이 0~50자) | `트랜잭션` |
| `page` | query | 선택 | integer(int32) | 페이지 번호 (1부터) (기본 `1`, 범위 1~) | `1` |
| `size` | query | 선택 | integer(int32) | 페이지 크기 (최대 100) (기본 `20`, 범위 1~100) | `20` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [PageResponsePostSummaryResponse](#pageresponsepostsummaryresponse) |
| 400 | 게시판·검색어·페이지 값 오류 (COMMON_400, details에 필드별 사유) | [ErrorResponse](#errorresponse) |

#### `POST` /api/posts

**게시글 작성** · 권한: 로그인

게시판·제목(앞뒤 공백 제외 1~100자)·본문(1~5000자)으로 글을 쓴다. 내 공개 DONE 분석이나 공개 개념 카드 중 하나를 첨부할 수 있다. 팔로워 피드용 POST 활동을 함께 남긴다.

**요청 본문** (`application/json`, 필수): [PostRequest](#postrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 201 | 작성 성공 | [PostIdResponse](#postidresponse) |
| 400 | 검증 실패 (COMMON_400, details에 필드별 사유. 첨부 둘 다는 details.conceptId) 또는 첨부할 수 없는 분석 (POST_001)·카드 (POST_002) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요 (AUTH_401) | [ErrorResponse](#errorresponse) |

#### `GET` /api/posts/{postId}

**게시글 상세** · 권한: 공개

조회수를 1 올리고 글을 돌려준다(작성자 본인도 오른다. countView=false면 올리지 않는다). 비로그인도 쓸 수 있고, 유효한 토큰이 있으면 분석 첨부의 볼 수 있는지 판단(내 분석)에 쓴다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `postId` | path | 필수 | integer(int64) | 글 ID | `42` |
| `countView` | query | 선택 | boolean | false면 조회수를 올리지 않고 지금 값을 돌려준다 (글 수정 화면이 글을 불러올 때, #209) (기본 `true`) | `true` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [PostDetailResponse](#postdetailresponse) |
| 404 | 없는 글 (POST_404) | [ErrorResponse](#errorresponse) |

#### `PUT` /api/posts/{postId}

**게시글 수정** · 권한: 로그인

내 글의 게시판·제목·본문·첨부를 보낸 값으로 통째로 바꾼다(첨부를 없애려면 둘 다 null). 조회수·작성 시각은 그대로다. 검증은 작성과 같고, 글 존재(404)·작성자(403)를 먼저 본다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `postId` | path | 필수 | integer(int64) | 글 ID | `42` |

**요청 본문** (`application/json`, 필수): [PostRequest](#postrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 수정 성공 | [PostDetailResponse](#postdetailresponse) |
| 400 | 검증 실패 (COMMON_400) 또는 첨부할 수 없는 분석 (POST_001)·카드 (POST_002) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | 남의 글 (POST_403) | [ErrorResponse](#errorresponse) |
| 404 | 없는 글 (POST_404) | [ErrorResponse](#errorresponse) |

#### `DELETE` /api/posts/{postId}

**게시글 삭제** · 권한: 로그인

내 글과 그 댓글을 지운다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `postId` | path | 필수 | integer(int64) | 글 ID | `42` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 204 | 삭제 성공 | - |
| 401 | 로그인 필요 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | 남의 글 (POST_403) | [ErrorResponse](#errorresponse) |
| 404 | 없는 글 (POST_404) | [ErrorResponse](#errorresponse) |

#### `GET` /api/posts/{postId}/comments

**댓글 목록** · 권한: 공개

한 글의 댓글 중 가장 최근 200개를 오래된순(작성 시각, id 오름차순)으로 돌려준다. total은 전체 댓글 수. 비로그인도 쓸 수 있다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `postId` | path | 필수 | integer(int64) | 글 ID | `42` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [CommentListResponse](#commentlistresponse) |
| 404 | 없는 글 (POST_404) | [ErrorResponse](#errorresponse) |

#### `POST` /api/posts/{postId}/comments

**댓글 작성** · 권한: 로그인

글에 댓글(앞뒤 공백 제외 1~500자)을 쓴다. 남의 글·자기 글 모두 쓸 수 있고, 피드 활동은 남기지 않는다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `postId` | path | 필수 | integer(int64) | 글 ID | `42` |

**요청 본문** (`application/json`, 필수): [CommentRequest](#commentrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 201 | 작성 성공 | [CommentResponse](#commentresponse) |
| 400 | 검증 실패 (COMMON_400, details.content) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 글 (POST_404) | [ErrorResponse](#errorresponse) |

#### `PUT` /api/posts/{postId}/comments/{commentId}

**댓글 수정** · 권한: 로그인

내 댓글의 내용을 바꾼다. 같은 내용이면 바꾸지 않는다(edited 유지). 글(404) → 댓글(404) → 작성자(403)를 본 뒤 내용을 검증한다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `postId` | path | 필수 | integer(int64) | 글 ID | `42` |
| `commentId` | path | 필수 | integer(int64) | 댓글 ID | `101` |

**요청 본문** (`application/json`, 필수): [CommentRequest](#commentrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 수정 성공 | [CommentResponse](#commentresponse) |
| 400 | 검증 실패 (COMMON_400, details.content) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | 남의 댓글 (POST_004, 글 작성자여도) | [ErrorResponse](#errorresponse) |
| 404 | 없는 글 (POST_404) 또는 없는 댓글·그 글의 댓글이 아님 (POST_003) | [ErrorResponse](#errorresponse) |

#### `DELETE` /api/posts/{postId}/comments/{commentId}

**댓글 삭제** · 권한: 로그인

내 댓글을 지운다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `postId` | path | 필수 | integer(int64) | 글 ID | `42` |
| `commentId` | path | 필수 | integer(int64) | 댓글 ID | `101` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 204 | 삭제 성공 | - |
| 401 | 로그인 필요 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | 남의 댓글 (POST_004, 글 작성자여도) | [ErrorResponse](#errorresponse) |
| 404 | 없는 글 (POST_404) 또는 없는 댓글·그 글의 댓글이 아님 (POST_003) | [ErrorResponse](#errorresponse) |

### 인증

#### `POST` /api/auth/login

**로그인** · 권한: 공개

Access 토큰과 회원 정보를 본문으로, Refresh 토큰을 HttpOnly 쿠키 refresh_token(Path=/api/auth, 14일)으로 준다.

**요청 본문** (`application/json`, 필수): [LoginRequest](#loginrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 로그인 성공 | [LoginResponse](#loginresponse) |
| 400 | 이메일·비밀번호 누락 (COMMON_400) | [ErrorResponse](#errorresponse) |
| 401 | 이메일 없음·비밀번호 불일치·탈퇴 회원 (AUTH_001) | [ErrorResponse](#errorresponse) |

#### `POST` /api/auth/logout

**로그아웃** · 권한: 공개

이 기기의 Refresh 토큰을 폐기하고 쿠키를 지운다. 쿠키가 없어도 204.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `refresh_token` | cookie | 선택 | string | - | - |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 204 | 로그아웃 완료 | - |

#### `POST` /api/auth/refresh

**토큰 재발급** · 권한: 공개

refresh_token 쿠키로 새 Access 토큰을 받고, Refresh 토큰도 새로 바꾼다(회전). 앱 시작 시 세션 복원에도 쓴다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `refresh_token` | cookie | 선택 | string | - | - |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 재발급 성공 | [LoginResponse](#loginresponse) |
| 401 | 쿠키 없음·무효·만료·이미 사용·탈퇴 회원 (AUTH_002). 만료·탈퇴면 쿠키를 지우고, 없거나 이미 회전된 토큰이면 다른 탭이 받은 새 쿠키를 지키려고 그대로 둔다 | [ErrorResponse](#errorresponse) |

#### `POST` /api/auth/signup

**회원가입** · 권한: 공개

이메일·비밀번호·닉네임·학습 수준으로 가입한다. 로그인은 따로 호출한다.

**요청 본문** (`application/json`, 필수): [SignupRequest](#signuprequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 201 | 가입 성공 | [SignupResponse](#signupresponse) |
| 400 | 검증 실패 (COMMON_400, details에 필드별 사유) | [ErrorResponse](#errorresponse) |
| 409 | 이메일 또는 닉네임 중복 (USER_409, details에 해당 필드) | [ErrorResponse](#errorresponse) |

### 분석

#### `GET` /api/analyses

**내 분석 목록** · 권한: 로그인

삭제하지 않은 내 분석을 요청 시각 최신순으로 준다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `page` | query | 선택 | integer(int32) | 페이지 번호 (1부터) (기본 `1`, 범위 1~) | `1` |
| `size` | query | 선택 | integer(int32) | 페이지 크기 (최대 100) (기본 `20`, 범위 1~100) | `20` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [PageResponseAnalysisSummaryResponse](#pageresponseanalysissummaryresponse) |
| 400 | page·size 범위 밖 (COMMON_400) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |

#### `POST` /api/analyses

**분석 요청 (ZIP·저장소 URL)** · 권한: 로그인

ZIP 파일(file) 또는 공개 GitHub·GitLab(gitlab.com) 저장소 주소(sourceUrl) 중 하나를 접수하고 바로 202를 준다. 저장소는 기본 브랜치 아카이브를 백그라운드 FETCHING 단계에서 받는다. 코드 수집·필터·선별은 백그라운드에서 진행되며 상태는 /api/analyses/{id}/status로 확인한다. 하루(Asia/Seoul) 5회까지(ZIP·URL 합산)이며 FAILED는 세지 않는다.

**요청 본문** (`multipart/form-data`, 선택): object

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `consent` | string: true | 선택 | SSAFY GMS를 거친 외부 AI 전송 고지 동의. true여야 한다 | - |
| `file` | string(binary) | 선택 | 프로젝트 ZIP (.zip, 10MB 이하). sourceUrl과 둘 중 하나만 보낸다 | - |
| `level` | string: BEGINNER · JUNIOR · SENIOR | 선택 | 분석 수준. 생략하면 내 프로필 수준 | - |
| `sourceUrl` | string | 선택 | 공개 GitHub·GitLab(gitlab.com) 저장소 주소 (500자 이하, 브랜치·파일 경로 없이). file과 둘 중 하나만 보낸다 (길이 ~500자) | `https://github.com/octo/todo-app` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 202 | 접수됨 | [CreateAnalysisResponse](#createanalysisresponse) |
| 400 | 검증 실패 (COMMON_400, details.file·sourceUrl·consent·level) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 413 | 업로드 10MB 초과 (COMMON_413) | [ErrorResponse](#errorresponse) |
| 429 | 오늘 분석 요청 횟수를 다 씀 (ANALYSIS_429) | [ErrorResponse](#errorresponse) |

#### `GET` /api/analyses/{id}

**분석 상세** · 권한: 로그인

내 분석 또는 공개·완료된 남의 분석의 정보와 개요(요약·기술 스택·레이어·읽는 순서·API 목록). 진행 중이거나 개요 생성에 실패했으면 overview가 null이다. 남의 분석이면 mine = false, memo = null. LLM을 부르지 않는다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [AnalysisDetailResponse](#analysisdetailresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 분석, 삭제된 분석, 볼 수 없는 남의 분석(비공개·미완료·주인 탈퇴) (ANALYSIS_404) | [ErrorResponse](#errorresponse) |

#### `PATCH` /api/analyses/{id}

**분석 제목·메모 수정** · 권한: 로그인

내 분석의 제목(100자)·메모(1000자)를 보낸 값으로 바꾼다 (누락·빈 문자열은 null). 진행 중이어도 되고 파이프라인에는 영향이 없다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**요청 본문** (`application/json`, 필수): [UpdateAnalysisRequest](#updateanalysisrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 수정됨 (바뀐 상세) | [AnalysisDetailResponse](#analysisdetailresponse) |
| 400 | 제목 100자·메모 1000자 초과 (COMMON_400, details.title·memo) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 분석, 다른 회원의 분석(공개여도), 삭제된 분석 (ANALYSIS_404) | [ErrorResponse](#errorresponse) |

#### `DELETE` /api/analyses/{id}

**분석 삭제** · 권한: 로그인

끝난(DONE·FAILED) 내 분석을 지운다. 파일·실행 흐름·개념 연결은 지우고 퀴즈 풀이 기록과 일일 한도 집계는 남긴다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 204 | 삭제됨 | - |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 분석, 다른 회원의 분석(공개여도), 이미 삭제된 분석 (ANALYSIS_404) | [ErrorResponse](#errorresponse) |
| 409 | 진행 중(FETCHING~QUIZ)인 분석 (ANALYSIS_005) | [ErrorResponse](#errorresponse) |

#### `GET` /api/analyses/{id}/concepts

**분석의 개념 카드** · 권한: 로그인

내 분석 또는 공개·완료된 남의 분석의 코드에 쓰인 공개 개념 카드와 위치(파일·줄·맥락). 검수 전 AI 초안은 수(pendingCount)만 준다. 추출 전이거나 연결이 없으면 빈 목록이다. LLM을 부르지 않는다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [AnalysisConceptsResponse](#analysisconceptsresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 분석, 삭제된 분석, 볼 수 없는 남의 분석(비공개·미완료·주인 탈퇴) (ANALYSIS_404) | [ErrorResponse](#errorresponse) |

#### `GET` /api/analyses/{id}/files

**분석 파일 목록** · 권한: 로그인

내 분석 또는 공개·완료된 남의 분석에서 선별한 파일(최대 30개)을 선별 우선순위(id 오름차순)로 준다. 코드 내용은 단건 조회에서 받는다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 (파일 저장 전이면 빈 배열) | [AnalysisFileSummaryResponse](#analysisfilesummaryresponse)[] |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 분석, 삭제된 분석, 볼 수 없는 남의 분석(비공개·미완료·주인 탈퇴) (ANALYSIS_404) | [ErrorResponse](#errorresponse) |

#### `GET` /api/analyses/{id}/files/{fileId}

**분석 파일 하나** · 권한: 로그인

내 분석 또는 공개·완료된 남의 분석의 파일 역할·설명과 코드 내용. 키·비밀 설정값은 [REDACTED]로 가려져 있다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |
| `fileId` | path | 필수 | integer(int64) | - | - |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [AnalysisFileResponse](#analysisfileresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 분석·삭제된 분석·볼 수 없는 남의 분석 (ANALYSIS_404), 그 분석의 파일이 아님 (ANALYSIS_001) | [ErrorResponse](#errorresponse) |

#### `GET` /api/analyses/{id}/flows

**실행 흐름 목록** · 권한: 로그인

내 분석 또는 공개·완료된 남의 분석의 흐름 전체(id 오름차순). DONE이 아니면 summary·steps는 null. 화면은 PENDING이 있으면 3초마다 다시 부른다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 (흐름이 없으면 빈 배열) | [FlowResponse](#flowresponse)[] |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 분석, 삭제된 분석, 볼 수 없는 남의 분석(비공개·미완료·주인 탈퇴) (ANALYSIS_404) | [ErrorResponse](#errorresponse) |

#### `POST` /api/analyses/{id}/flows

**실행 흐름 요청** · 권한: 로그인

개요 apis에서 고른 API(1~3개)의 실행 흐름 생성을 접수하고 바로 202와 이 분석의 흐름 전체를 준다. 생성은 백그라운드에서 진행되며 GET으로 확인한다. 이미 PENDING·DONE인 API는 다시 만들지 않고, FAILED면 다시 만든다. 분석당 FAILED가 아닌 흐름은 3개까지.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**요청 본문** (`application/json`, 필수): [CreateFlowsRequest](#createflowsrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 202 | 접수됨 (흐름 전체, 방금 요청한 것은 PENDING) | [FlowResponse](#flowresponse)[] |
| 400 | endpoints 0개·4개 이상·중복·빈 값 (COMMON_400), 개요에 없는 API (ANALYSIS_002), 3개 초과 (ANALYSIS_003) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 분석, 다른 회원의 분석, 삭제된 분석 (ANALYSIS_404) | [ErrorResponse](#errorresponse) |
| 409 | 분석이 아직 DONE이 아님 (ANALYSIS_409) | [ErrorResponse](#errorresponse) |

#### `POST` /api/analyses/{id}/rerun

**실패한 저장소 분석 재실행** · 권한: 로그인

FAILED로 끝난 내 저장소 URL 분석을 같은 주소·수준·제목·메모로 새 분석으로 다시 접수하고 202를 준다. 실패한 분석은 삭제된다(F-19 삭제와 같음). 새 분석은 비공개이고 오늘 요청 1회로 센다. ZIP 분석은 원본을 보관하지 않아 다시 올려야 한다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 202 | 새 분석 접수됨 | [CreateAnalysisResponse](#createanalysisresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 분석, 다른 회원의 분석(공개·팀 공유여도), 삭제된 분석 (ANALYSIS_404) | [ErrorResponse](#errorresponse) |
| 409 | ZIP 분석이거나 FAILED가 아닌 분석 (ANALYSIS_008) | [ErrorResponse](#errorresponse) |
| 429 | 오늘 분석 요청 횟수를 다 씀 (ANALYSIS_429) | [ErrorResponse](#errorresponse) |

#### `GET` /api/analyses/{id}/status

**분석 진행 상태** · 권한: 로그인

내 분석 또는 공개·완료된 남의 분석의 단계·진행률·실패 사유·저장소 주소. 진행 화면이 2초마다 폴링한다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [AnalysisStatusResponse](#analysisstatusresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 분석, 삭제된 분석, 볼 수 없는 남의 분석(비공개·미완료·주인 탈퇴) (ANALYSIS_404) | [ErrorResponse](#errorresponse) |

#### `GET` /api/analyses/{id}/structure

**구조 그래프** · 권한: 로그인

선별 파일 사이의 의존 관계(Java 타입 참조·MyBatis XML·JS import·FE→API 호출)를 코드에서 확정한 것만 간선으로 준다. 확정하지 못한 연결은 사유와 함께 unresolved에 담는다. 요청 때 계산하며 DB에 저장하지 않는다(DONE 분석은 서버 메모리에 캐시). 파일이 없으면 세 배열이 비고, 개요가 없으면 FE→API 판정을 건너뛴다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [AnalysisStructureResponse](#analysisstructureresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 분석, 삭제된 분석, 볼 수 없는 남의 분석(비공개·미완료·주인 탈퇴) (ANALYSIS_404) | [ErrorResponse](#errorresponse) |
| 503 | 동시 계산 상한이 차서 기다려도 자리가 나지 않음 (ANALYSIS_007) | [ErrorResponse](#errorresponse) |

#### `PATCH` /api/analyses/{id}/visibility

**분석 공개 설정** · 권한: 로그인

공개하면 완료 때 기록된 피드 활동이 팔로워에게 보이고, 다른 회원이 읽기 전용으로 열람할 수 있다. 공개는 DONE일 때만 되고, 비공개는 상태와 관계없이 된다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**요청 본문** (`application/json`, 필수): [UpdateVisibilityRequest](#updatevisibilityrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 바뀜 | [VisibilityResponse](#visibilityresponse) |
| 400 | isPublic 누락·null (COMMON_400) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 분석, 다른 회원의 분석(공개여도), 삭제된 분석 (ANALYSIS_404) | [ErrorResponse](#errorresponse) |
| 409 | DONE이 아닌 분석을 공개로 바꾸려 함 (ANALYSIS_006) | [ErrorResponse](#errorresponse) |

### 복습 계획

#### `GET` /api/review-plans

**복습 계획 조회** · 권한: 로그인

기간(from~to, 생략하면 오늘부터 7일) 안의 내 계획과, 기간과 상관없이 오늘 이전 날짜의 TODO(밀린 복습). 공개되지 않은 카드의 계획은 뺀다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `from` | query | 선택 | string | 기간 시작 (YYYY-MM-DD, to와 함께) | `2026-11-20` |
| `to` | query | 선택 | string | 기간 끝 (YYYY-MM-DD, from과 함께, from부터 30일 뒤까지) | `2026-11-26` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [ReviewPlanListResponse](#reviewplanlistresponse) |
| 400 | 날짜 형식 오류, 한쪽만 줌, from > to, 31일 초과 (COMMON_400, details에 from·to) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |

#### `POST` /api/review-plans

**복습 계획 직접 추가** · 권한: 로그인

공개 카드를 planDate(생략하면 내일)의 TODO로 넣는다 (reason MANUAL). 한 카드의 TODO는 하나만 둔다.

**요청 본문** (`application/json`, 필수): [ReviewPlanCreateRequest](#reviewplancreaterequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 201 | 추가 성공 | [ReviewPlanItemResponse](#reviewplanitemresponse) |
| 400 | conceptId 없음·0 이하, planDate가 오늘 이전·오늘+365일 뒤 (COMMON_400, details에 필드) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 카드, 공개되지 않은 카드 (CONCEPT_404) | [ErrorResponse](#errorresponse) |
| 409 | 그 카드의 TODO가 이미 있음, 같은 날짜의 계획이 있음 (REVIEW_PLAN_409) | [ErrorResponse](#errorresponse) |

#### `PATCH` /api/review-plans/{id}

**복습 완료·되돌림** · 권한: 로그인

DONE이면 완료 시각을 지금으로, TODO면 완료 시각을 지운다. 이미 그 상태면 바꾸지 않는다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**요청 본문** (`application/json`, 필수): [ReviewPlanStatusRequest](#reviewplanstatusrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 변경 성공 | [ReviewPlanItemResponse](#reviewplanitemresponse) |
| 400 | status 없음, TODO·DONE이 아님 (COMMON_400) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 계획, 다른 회원의 계획, 공개되지 않은 카드의 계획 (REVIEW_PLAN_404) | [ErrorResponse](#errorresponse) |
| 409 | DONE→TODO인데 그 카드의 다른 TODO가 있음 (REVIEW_PLAN_409) | [ErrorResponse](#errorresponse) |

#### `DELETE` /api/review-plans/{id}

**복습 계획 삭제** · 권한: 로그인

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 204 | 삭제 성공 | - |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 계획, 다른 회원의 계획, 공개되지 않은 카드의 계획 (REVIEW_PLAN_404) | [ErrorResponse](#errorresponse) |

### 피드

#### `GET` /api/feed

**내 피드** · 권한: 로그인

팔로우한 사람이 공개한 분석·남긴 리뷰·찜한 카드·쓴 글·챌린지 참여/달성을 활동 시각 최신순(같으면 활동 id 내림차순)으로 페이지 조회한다. 내 활동은 넣지 않는다. 대상을 지금 볼 수 없으면(비공개·삭제된 분석, 지운 리뷰·글·챌린지, 해제한 찜, 공개가 아닌 카드) 목록과 total에서 뺀다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `page` | query | 선택 | integer(int32) | 페이지 번호 (1부터) (기본 `1`, 범위 1~) | `1` |
| `size` | query | 선택 | integer(int32) | 페이지 크기 (최대 100) (기본 `20`, 범위 1~100) | `20` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [PageResponseFeedItemResponse](#pageresponsefeeditemresponse) |
| 400 | 페이지 값 오류 (COMMON_400) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |

### 팔로우

#### `GET` /api/users/{id}/follow

**팔로우 상태** · 권한: 공개

회원의 팔로워·팔로잉 수와 로그인한 내가 팔로우 중인지 조회한다. 비로그인도 쓸 수 있고, 비로그인·내 프로필이면 following은 false다. 토큰이 깨졌거나 만료되면 비로그인과 같다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 회원 ID | `7` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [FollowStatusResponse](#followstatusresponse) |
| 404 | 없거나 탈퇴한 회원 (USER_404) | [ErrorResponse](#errorresponse) |

#### `POST` /api/users/{id}/follow

**팔로우** · 권한: 로그인

회원을 팔로우한다. 이미 팔로우 중이어도 200(멱등)이다. 수는 대상 회원 기준이다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 팔로우할 회원 ID | `7` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 팔로우 상태 | [FollowStatusResponse](#followstatusresponse) |
| 400 | 자기 자신 (USER_002) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없거나 탈퇴한 회원 (USER_404) | [ErrorResponse](#errorresponse) |

#### `DELETE` /api/users/{id}/follow

**언팔로우** · 권한: 로그인

팔로우를 푼다. 팔로우하지 않은 회원이어도 200(멱등)이다. 수는 대상 회원 기준이다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 언팔로우할 회원 ID | `7` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 팔로우 상태 | [FollowStatusResponse](#followstatusresponse) |
| 400 | 자기 자신 (USER_002) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없거나 탈퇴한 회원 (USER_404) | [ErrorResponse](#errorresponse) |

#### `GET` /api/users/{id}/followers

**팔로워 목록** · 권한: 공개

회원을 팔로우하는 사람을 팔로우한 시각 최신순(같으면 회원 id 내림차순)으로 페이지 조회한다. 비로그인도 쓸 수 있다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 회원 ID | `7` |
| `page` | query | 선택 | integer(int32) | 페이지 번호 (1부터) (기본 `1`, 범위 1~) | `1` |
| `size` | query | 선택 | integer(int32) | 페이지 크기 (최대 100) (기본 `20`, 범위 1~100) | `20` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [PageResponseFollowUserResponse](#pageresponsefollowuserresponse) |
| 400 | 페이지 값 오류 (COMMON_400) | [ErrorResponse](#errorresponse) |
| 404 | 없거나 탈퇴한 회원 (USER_404) | [ErrorResponse](#errorresponse) |

#### `GET` /api/users/{id}/followings

**팔로잉 목록** · 권한: 공개

회원이 팔로우하는 사람을 팔로우한 시각 최신순(같으면 회원 id 내림차순)으로 페이지 조회한다. 비로그인도 쓸 수 있다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 회원 ID | `7` |
| `page` | query | 선택 | integer(int32) | 페이지 번호 (1부터) (기본 `1`, 범위 1~) | `1` |
| `size` | query | 선택 | integer(int32) | 페이지 크기 (최대 100) (기본 `20`, 범위 1~100) | `20` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [PageResponseFollowUserResponse](#pageresponsefollowuserresponse) |
| 400 | 페이지 값 오류 (COMMON_400) | [ErrorResponse](#errorresponse) |
| 404 | 없거나 탈퇴한 회원 (USER_404) | [ErrorResponse](#errorresponse) |

### 개념 카드

#### `GET` /api/concepts

**개념 카드 목록·검색** · 권한: 공개

공개(PUBLISHED) 카드를 키워드(이름·요약 부분 일치)·카테고리·난이도로 거르고 인기순(찜 수→평점) 또는 최신순으로 페이지 조회한다. 비로그인도 쓸 수 있다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `keyword` | query | 선택 | string | 이름·요약 부분 일치 검색어 (trim 후 50자 이하, 대소문자 무시) (길이 0~50자) | `jwt` |
| `category` | query | 선택 | string | 카테고리 (java, spring, web, security, mybatis, vue). 없는 값이면 빈 목록 (길이 0~50자) | `security` |
| `difficulty` | query | 선택 | string: BEGINNER · JUNIOR · SENIOR | 난이도 | `JUNIOR` |
| `sort` | query | 선택 | string: POPULAR · LATEST | 정렬 (POPULAR: 찜 수→평점, LATEST: 최신 등록) (기본 `POPULAR`) | `POPULAR` |
| `page` | query | 선택 | integer(int32) | 페이지 번호 (1부터) (기본 `1`, 범위 1~) | `1` |
| `size` | query | 선택 | integer(int32) | 페이지 크기 (최대 100) (기본 `20`, 범위 1~100) | `20` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [PageResponseConceptSummaryResponse](#pageresponseconceptsummaryresponse) |
| 400 | 검색 조건·페이지 값 오류 (COMMON_400, details에 필드별 사유) | [ErrorResponse](#errorresponse) |

#### `GET` /api/concepts/{conceptId}/reviews

**카드 리뷰 목록** · 권한: 공개

공개 카드의 리뷰를 최신순(작성 시각 내림차순)으로 페이지 조회한다. 비로그인도 쓸 수 있다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `conceptId` | path | 필수 | integer(int64) | 카드 ID | `57` |
| `page` | query | 선택 | integer(int32) | 페이지 번호 (1부터) (기본 `1`, 범위 1~) | `1` |
| `size` | query | 선택 | integer(int32) | 페이지 크기 (최대 100) (기본 `20`, 범위 1~100) | `20` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [PageResponseConceptReviewResponse](#pageresponseconceptreviewresponse) |
| 400 | 페이지 값 오류 (COMMON_400) | [ErrorResponse](#errorresponse) |
| 404 | 없는 카드이거나 공개되지 않은 카드 (CONCEPT_404) | [ErrorResponse](#errorresponse) |

#### `POST` /api/concepts/{conceptId}/reviews

**리뷰 작성** · 권한: 로그인

별점(1~5)과 내용(앞뒤 공백 제외 1~200자)으로 리뷰를 쓴다. 카드당 한 번만 쓸 수 있고, 카드 평균 별점을 다시 계산해 함께 돌려준다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `conceptId` | path | 필수 | integer(int64) | 카드 ID | `57` |

**요청 본문** (`application/json`, 필수): [ConceptReviewRequest](#conceptreviewrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 201 | 작성 성공 | [ConceptReviewSaveResponse](#conceptreviewsaveresponse) |
| 400 | 별점·내용 검증 실패 (COMMON_400, details에 필드별 사유) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 카드이거나 공개되지 않은 카드 (CONCEPT_404) | [ErrorResponse](#errorresponse) |
| 409 | 이미 이 카드에 리뷰를 남김 (CONCEPT_409) | [ErrorResponse](#errorresponse) |

#### `GET` /api/concepts/{conceptId}/reviews/me

**내 리뷰 조회** · 권한: 로그인

이 카드에 내가 쓴 리뷰. 없으면 204(본문 없음).

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `conceptId` | path | 필수 | integer(int64) | 카드 ID | `57` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 내 리뷰 | [ConceptReviewResponse](#conceptreviewresponse) |
| 204 | 이 카드에 쓴 리뷰가 없음 | - |
| 401 | 로그인 필요 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 카드이거나 공개되지 않은 카드 (CONCEPT_404) | [ErrorResponse](#errorresponse) |

#### `PUT` /api/concepts/{conceptId}/reviews/me

**내 리뷰 수정** · 권한: 로그인

내 리뷰의 별점·내용을 바꾼다. 작성 시각과 목록 순서는 그대로이고, 카드 평균 별점을 다시 계산해 돌려준다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `conceptId` | path | 필수 | integer(int64) | 카드 ID | `57` |

**요청 본문** (`application/json`, 필수): [ConceptReviewRequest](#conceptreviewrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 수정 성공 | [ConceptReviewSaveResponse](#conceptreviewsaveresponse) |
| 400 | 별점·내용 검증 실패 (COMMON_400, details에 필드별 사유) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는·비공개 카드 (CONCEPT_404) 또는 작성한 리뷰가 없음 (CONCEPT_001) | [ErrorResponse](#errorresponse) |

#### `DELETE` /api/concepts/{conceptId}/reviews/me

**내 리뷰 삭제** · 권한: 로그인

내 리뷰를 지우고 다시 계산한 카드 평균 별점·리뷰 수를 돌려준다 (반올림된 평균을 화면이 다시 계산할 수 없어 200).

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `conceptId` | path | 필수 | integer(int64) | 카드 ID | `57` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 삭제 성공 | [ConceptRatingResponse](#conceptratingresponse) |
| 401 | 로그인 필요 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는·비공개 카드 (CONCEPT_404) 또는 작성한 리뷰가 없음 (CONCEPT_001) | [ErrorResponse](#errorresponse) |

#### `POST` /api/concepts/{conceptId}/wish

**카드 찜** · 권한: 로그인

공개 카드를 찜한다. 이미 찜했어도 200(멱등)이다. 새로 찜하면 내일 복습 계획(reason WISH)에 넣어 보고, 넣었으면 reviewPlanDate로 그 날짜를 돌려준다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `conceptId` | path | 필수 | integer(int64) | 카드 ID | `57` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 찜 상태 | [ConceptWishResponse](#conceptwishresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 카드이거나 공개되지 않은 카드 (CONCEPT_404) | [ErrorResponse](#errorresponse) |

#### `DELETE` /api/concepts/{conceptId}/wish

**카드 찜 해제** · 권한: 로그인

찜을 푼다. 찜하지 않은 카드여도 200(멱등)이고 바뀐 찜 수를 돌려준다. 복습 계획은 지우지 않는다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `conceptId` | path | 필수 | integer(int64) | 카드 ID | `57` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 찜 상태 | [ConceptWishResponse](#conceptwishresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 카드이거나 공개되지 않은 카드 (CONCEPT_404) | [ErrorResponse](#errorresponse) |

#### `GET` /api/concepts/{id}

**개념 카드 상세** · 권한: 공개

공개(PUBLISHED) 카드의 본문·예제 코드·출처와 관련 카드(최대 4개)를 조회한다. 비로그인도 쓸 수 있고, 로그인하면 내 분석(삭제되지 않은 DONE)에서 이 개념이 등장한 위치(최신 10개)를 함께 받는다. 토큰이 깨졌거나 만료되면 비로그인과 같다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 카드 ID | `57` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [ConceptDetailResponse](#conceptdetailresponse) |
| 404 | 없는 카드이거나 공개되지 않은 카드 (CONCEPT_404) | [ErrorResponse](#errorresponse) |

#### `GET` /api/users/{userId}/wishes

**회원의 찜 목록** · 권한: 공개

회원이 찜한 공개(PUBLISHED) 카드를 찜한 시각 최신순으로 페이지 조회한다. 비로그인도 쓸 수 있다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `userId` | path | 필수 | integer(int64) | 회원 ID | `3` |
| `page` | query | 선택 | integer(int32) | 페이지 번호 (1부터) (기본 `1`, 범위 1~) | `1` |
| `size` | query | 선택 | integer(int32) | 페이지 크기 (최대 100) (기본 `20`, 범위 1~100) | `20` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [PageResponseConceptSummaryResponse](#pageresponseconceptsummaryresponse) |
| 400 | 페이지 값 오류 (COMMON_400) | [ErrorResponse](#errorresponse) |
| 404 | 없거나 탈퇴한 회원 (USER_404) | [ErrorResponse](#errorresponse) |

### 퀴즈

#### `GET` /api/analyses/{id}/quiz

**분석의 퀴즈 조회** · 권한: 로그인

문항(정답·해설·개념 제외)과 요청자의 가장 최근 풀이 요약. 분석 주인과 그 분석을 공유받은 팀 멤버(F-20b)가 볼 수 있다. LLM을 부르지 않는다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [QuizResponse](#quizresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는·삭제된 분석, 내 분석도 내가 멤버인 팀에 공유된 분석도 아님 (ANALYSIS_404), 퀴즈가 없음 (QUIZ_404) | [ErrorResponse](#errorresponse) |

#### `POST` /api/analyses/{id}/quiz

**퀴즈 다시 만들기** · 권한: 로그인

생성에 실패한(quizStatus = FAILED) 내 완료 분석의 퀴즈를 같은 입력으로 다시 만든다. 바로 202를 주고 생성은 백그라운드에서 하며, 결과는 /api/analyses/{id}/status의 quizStatus(READY·FAILED)로 확인한다. 분석당 3회까지이고 실패해도 횟수는 돌아오지 않는다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | - | - |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 202 | 접수됨 (quizStatus = GENERATING) | [QuizRegenerationResponse](#quizregenerationresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는·삭제된 분석, 남의 분석(팀 공유 멤버 포함) (ANALYSIS_404) | [ErrorResponse](#errorresponse) |
| 409 | 분석이 DONE이 아니거나 퀴즈가 이미 있거나 만드는 중 (QUIZ_409) | [ErrorResponse](#errorresponse) |
| 429 | 재생성 횟수를 다 씀 (QUIZ_429) | [ErrorResponse](#errorresponse) |

#### `POST` /api/quizzes/{quizId}/attempts

**퀴즈 풀이 제출·채점** · 권한: 로그인

문항마다 한 번씩 답하면 채점해 저장하고, 틀린 문항의 공개 개념을 내일 복습 계획에 넣는다. 같은 퀴즈를 여러 번 풀 수 있다. 분석 주인과 그 분석을 공유받은 팀 멤버(F-20b)가 낼 수 있고, 풀이·복습 계획은 요청자의 것이다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `quizId` | path | 필수 | integer(int64) | - | - |

**요청 본문** (`application/json`, 필수): [QuizAttemptRequest](#quizattemptrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 201 | 채점 성공 | [QuizAttemptResponse](#quizattemptresponse) |
| 400 | answers 검증 실패 (COMMON_400), 문항마다 한 번씩 답하지 않음 (QUIZ_400) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 퀴즈, 삭제된 분석의 퀴즈, 내 분석도 내가 멤버인 팀에 공유된 분석도 아닌 퀴즈 (QUIZ_404) | [ErrorResponse](#errorresponse) |

### 챌린지

#### `GET` /api/challenges

**챌린지 목록** · 권한: 공개

상태(오늘 기준, 기본 진행 중)로 거른다. 진행 중은 종료일 오름차순, 예정은 시작일 오름차순, 종료는 종료일 내림차순이고 모두 다음 기준 id 역순이다. 비로그인도 쓸 수 있고, 로그인했으면 항목마다 참여 여부(joined)를 채운다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `status` | query | 선택 | string: ONGOING · UPCOMING · ENDED | 상태 (없으면 ONGOING). 오늘(Asia/Seoul)과 기간으로 계산한다 (기본 `ONGOING`) | `ONGOING` |
| `page` | query | 선택 | integer(int32) | 페이지 번호 (1부터) (기본 `1`, 범위 1~) | `1` |
| `size` | query | 선택 | integer(int32) | 페이지 크기 (최대 50) (기본 `12`, 범위 1~50) | `12` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [PageResponseChallengeSummaryResponse](#pageresponsechallengesummaryresponse) |
| 400 | 상태·페이지 값 오류 (COMMON_400, details에 필드별 사유) | [ErrorResponse](#errorresponse) |

#### `GET` /api/challenges/{id}

**챌린지 상세** · 권한: 공개

설명·상태·참여자 수와, 로그인해 참여한 회원이면 내 달성도(participation)를 준다. 비로그인도 쓸 수 있다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 챌린지 ID | `7` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [ChallengeDetailResponse](#challengedetailresponse) |
| 404 | 없는 챌린지 (CHALLENGE_404) | [ErrorResponse](#errorresponse) |

#### `POST` /api/challenges/{id}/participation

**챌린지 참여** · 권한: 로그인

예정·진행 중 챌린지에 참여한다. 이미 참여했어도 200(멱등)이다. 처음 참여하면 피드 활동을 남기고, 시작일부터의 활동을 바로 세어 목표에 닿았으면 달성 처리한다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 챌린지 ID | `7` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 참여 상태 | [ParticipationResponse](#participationresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 챌린지 (CHALLENGE_404) | [ErrorResponse](#errorresponse) |
| 409 | 종료된 챌린지 (CHALLENGE_002) | [ErrorResponse](#errorresponse) |

#### `DELETE` /api/challenges/{id}/participation

**챌린지 참여 취소** · 권한: 로그인

참여를 취소하고 피드의 참여 활동을 지운다. 참여하지 않았어도 200(멱등)이다. 달성했거나 종료된 챌린지는 취소할 수 없다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 챌린지 ID | `7` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 참여 상태 (joined false) | [ParticipationResponse](#participationresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 챌린지 (CHALLENGE_404) | [ErrorResponse](#errorresponse) |
| 409 | 종료된 챌린지 (CHALLENGE_002), 달성한 챌린지 (CHALLENGE_003) | [ErrorResponse](#errorresponse) |

#### `GET` /api/users/me/challenges

**내 챌린지** · 권한: 로그인

내가 참여한 챌린지와 달성도를 참여 시각 최신순으로 페이지 조회한다. 상태가 없으면 전체다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `status` | query | 선택 | string: ONGOING · UPCOMING · ENDED | 상태 (없으면 전체). 오늘(Asia/Seoul)과 기간으로 계산한다 | `ONGOING` |
| `page` | query | 선택 | integer(int32) | 페이지 번호 (1부터) (기본 `1`, 범위 1~) | `1` |
| `size` | query | 선택 | integer(int32) | 페이지 크기 (최대 50) (기본 `12`, 범위 1~50) | `12` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [PageResponseMyChallengeResponse](#pageresponsemychallengeresponse) |
| 400 | 상태·페이지 값 오류 (COMMON_400, details에 필드별 사유) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |

#### `GET` /api/users/{userId}/challenges

**회원의 참여 챌린지** · 권한: 공개

회원이 참여한 챌린지와 달성도를 참여 시각 최신순으로 페이지 조회한다. 상태가 없으면 전체다. 비로그인도 쓸 수 있다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `userId` | path | 필수 | integer(int64) | 회원 ID | `3` |
| `status` | query | 선택 | string: ONGOING · UPCOMING · ENDED | 상태 (없으면 전체). 오늘(Asia/Seoul)과 기간으로 계산한다 | `ONGOING` |
| `page` | query | 선택 | integer(int32) | 페이지 번호 (1부터) (기본 `1`, 범위 1~) | `1` |
| `size` | query | 선택 | integer(int32) | 페이지 크기 (최대 50) (기본 `12`, 범위 1~50) | `12` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [PageResponseMyChallengeResponse](#pageresponsemychallengeresponse) |
| 400 | 상태·페이지 값 오류 (COMMON_400, details에 필드별 사유) | [ErrorResponse](#errorresponse) |
| 404 | 없거나 탈퇴한 회원 (USER_404) | [ErrorResponse](#errorresponse) |

### 추천

#### `GET` /api/recommendations

**맞춤 추천 카드 조회** · 권한: 로그인

규칙 점수(min(오답,3)×3 + 내 분석 등장 2 + 찜한 분야 1 + 수준 적합도) 순으로 size개. 복습을 끝낸 카드, 오늘까지 복습할 카드, 찜한 카드는 뺀다. 이유는 캐시가 없거나 근거가 바뀐 카드만 LLM으로 만들고, LLM이 실패하면 규칙 문장(RULE)을 준다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `size` | query | 선택 | integer(int32) | 추천 개수 (1~10) (기본 `5`, 범위 1~10) | `5` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 (공개 카드가 없으면 빈 목록) | [RecommendationListResponse](#recommendationlistresponse) |
| 400 | size가 정수가 아님, 1 미만, 10 초과 (COMMON_400, details.size) | [ErrorResponse](#errorresponse) |
| 401 | 토큰 없음·만료 (AUTH_401) | [ErrorResponse](#errorresponse) |

### 팀

#### `POST` /api/teams

**팀 만들기** · 권한: 로그인

팀을 만들고 팀장이 된다. 초대 코드 8자를 함께 돌려준다.

**요청 본문** (`application/json`, 필수): [TeamRequest](#teamrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 201 | 생성 성공 | [TeamCreatedResponse](#teamcreatedresponse) |
| 400 | 이름·소개 형식 오류 (COMMON_400, details.name·description) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 409 | 이미 5개 팀에 속함 (TEAM_003) | [ErrorResponse](#errorresponse) |

#### `POST` /api/teams/join

**초대 코드로 가입** · 권한: 로그인

코드는 앞뒤 공백을 빼고 대문자로 바꿔 비교한다. 팀당 10명, 1인당 5개 팀까지다.

**요청 본문** (`application/json`, 필수): [JoinTeamRequest](#jointeamrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 가입 성공 | [JoinTeamResponse](#jointeamresponse) |
| 400 | 코드 없음·공백뿐 (COMMON_400, details.inviteCode) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 유효하지 않은 초대 코드 (TEAM_001) | [ErrorResponse](#errorresponse) |
| 409 | 이미 가입함 (TEAM_409), 팀 인원 가득 참 (TEAM_002), 이미 5개 팀에 속함 (TEAM_003) | [ErrorResponse](#errorresponse) |

#### `GET` /api/teams/{id}

**팀 상세** · 권한: 로그인

팀 정보와 멤버(팀장이 맨 앞, 나머지는 가입 순)를 준다. 초대 코드는 팀장에게만 값이 있다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 팀 ID | `12` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [TeamDetailResponse](#teamdetailresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 팀이거나 내가 멤버가 아닌 팀 (TEAM_404) | [ErrorResponse](#errorresponse) |

#### `PUT` /api/teams/{id}

**팀 정보 수정 (팀장)** · 권한: 로그인

이름·소개를 보낸 값으로 통째로 바꾼다. 검증은 팀장 확인 뒤에 한다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 팀 ID | `12` |

**요청 본문** (`application/json`, 필수): [TeamRequest](#teamrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 수정 성공 | [TeamDetailResponse](#teamdetailresponse) |
| 400 | 이름·소개 형식 오류 (COMMON_400) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | 팀장이 아님 (TEAM_403) | [ErrorResponse](#errorresponse) |
| 404 | 없는 팀이거나 내가 멤버가 아닌 팀 (TEAM_404) | [ErrorResponse](#errorresponse) |

#### `DELETE` /api/teams/{id}

**팀 삭제 (팀장)** · 권한: 로그인

팀과 모든 멤버 정보를 지운다. 초대 코드도 무효가 된다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 팀 ID | `12` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 204 | 삭제 성공 | - |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | 팀장이 아님 (TEAM_403) | [ErrorResponse](#errorresponse) |
| 404 | 없는 팀이거나 내가 멤버가 아닌 팀 (TEAM_404) | [ErrorResponse](#errorresponse) |

#### `GET` /api/teams/{id}/analyses

**팀 공유 분석 목록** · 권한: 로그인

삭제되지 않은 공유 분석을 공유 최신순으로 전부 준다 (최대 10개). solvedMemberCount는 지금 멤버 중 푼 사람 수, myLastAttempt는 내 최근 풀이다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 팀 ID | `12` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [TeamAnalysisResponse](#teamanalysisresponse)[] |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 팀이거나 내가 멤버가 아닌 팀 (TEAM_404) | [ErrorResponse](#errorresponse) |

#### `POST` /api/teams/{id}/analyses

**팀에 분석 공유** · 권한: 로그인

내 완료(DONE)·퀴즈 있는 분석을 팀에 공유한다. 팀원은 그 퀴즈만 풀 수 있다. 팀당 최대 10개.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 팀 ID | `12` |

**요청 본문** (`application/json`, 필수): [ShareTeamAnalysisRequest](#shareteamanalysisrequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 201 | 공유 성공 | [TeamAnalysisSharedResponse](#teamanalysissharedresponse) |
| 400 | analysisId 없음·1 미만 (COMMON_400, details.analysisId) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 팀이거나 내가 멤버가 아닌 팀 (TEAM_404), 없는·남의·삭제된 분석 (ANALYSIS_404) | [ErrorResponse](#errorresponse) |
| 409 | DONE 아님·퀴즈 없음 (TEAM_009), 이미 공유 (TEAM_007), 공유 10개 (TEAM_008) | [ErrorResponse](#errorresponse) |

#### `DELETE` /api/teams/{id}/analyses/{analysisId}

**팀 분석 공유 해제 (공유한 사람·팀장)** · 권한: 로그인

공유만 해제한다. 팀원의 풀이 기록은 남는다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 팀 ID | `12` |
| `analysisId` | path | 필수 | integer(int64) | 분석 ID | `42` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 204 | 해제 성공 | - |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | 공유한 사람도 팀장도 아님 (TEAM_010) | [ErrorResponse](#errorresponse) |
| 404 | 없는 팀이거나 내가 멤버가 아닌 팀 (TEAM_404), 팀에 공유되지 않았거나 삭제된 분석 (TEAM_006) | [ErrorResponse](#errorresponse) |

#### `GET` /api/teams/{id}/analyses/{analysisId}/understanding

**팀 이해도 (모듈별·멤버별)** · 권한: 로그인

지금 멤버마다 가장 최근 풀이로 레이어(모듈)별 정답률과 멤버 × 모듈 결과를 준다. 모듈 = 문항 개념이 연결된 파일의 레이어(없으면 other).

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 팀 ID | `12` |
| `analysisId` | path | 필수 | integer(int64) | 분석 ID | `42` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [TeamUnderstandingResponse](#teamunderstandingresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 팀이거나 내가 멤버가 아닌 팀 (TEAM_404), 팀에 공유되지 않았거나 삭제된 분석 (TEAM_006) | [ErrorResponse](#errorresponse) |

#### `GET` /api/teams/{id}/challenges

**팀 목표 목록** · 권한: 로그인

진행 중(종료일 ASC) → 예정(시작일 ASC) → 종료(종료일 DESC), 같으면 목표 id DESC. 달성은 지금 멤버마다 기간 안 가장 최근 풀이로 조회할 때 계산한다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 팀 ID | `12` |
| `analysisId` | query | 선택 | integer(int64) | 이 분석의 목표만 (선택) | `42` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [TeamChallengeResponse](#teamchallengeresponse)[] |
| 400 | analysisId가 숫자가 아니거나 1 미만 (COMMON_400) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 팀이거나 내가 멤버가 아닌 팀 (TEAM_404) | [ErrorResponse](#errorresponse) |

#### `POST` /api/teams/{id}/challenges

**팀 목표 만들기 (팀장)** · 권한: 로그인

공유 분석 하나에 기간(Asia/Seoul 날짜, 양끝 포함, 최대 30일)과 목표 정답률을 정한다. 분석마다 끝나지 않은 목표는 하나, 팀당 목표는 최대 20개.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 팀 ID | `12` |

**요청 본문** (`application/json`, 필수): [CreateTeamChallengeRequest](#createteamchallengerequest)

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 201 | 만들기 성공 | [TeamChallengeResponse](#teamchallengeresponse) |
| 400 | analysisId·startDate·endDate·targetRate 오류, 시작일이 오늘 전이거나 오늘 + 365일 뒤, 종료일이 시작일 전, 30일 초과 (COMMON_400, 필드별 details) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | 팀장이 아님 (TEAM_403) | [ErrorResponse](#errorresponse) |
| 404 | 없는 팀이거나 내가 멤버가 아닌 팀 (TEAM_404), 팀에 공유되지 않았거나 삭제된 분석 (TEAM_006) | [ErrorResponse](#errorresponse) |
| 409 | 끝나지 않은 목표가 이미 있음 (TEAM_011), 목표 20개 (TEAM_012) | [ErrorResponse](#errorresponse) |

#### `DELETE` /api/teams/{id}/challenges/{challengeId}

**팀 목표 삭제 (팀장)** · 권한: 로그인

상태와 상관없이 지운다. 풀이 기록은 남는다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 팀 ID | `12` |
| `challengeId` | path | 필수 | integer(int64) | 목표 ID | `7` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 204 | 삭제 성공 | - |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | 팀장이 아님 (TEAM_403) | [ErrorResponse](#errorresponse) |
| 404 | 없는 팀이거나 내가 멤버가 아닌 팀 (TEAM_404), 없는·다른 팀의·삭제된 분석의 목표 (TEAM_013) | [ErrorResponse](#errorresponse) |

#### `POST` /api/teams/{id}/invite-code

**초대 코드 재발급 (팀장)** · 권한: 로그인

새 코드를 발급한다. 이전 코드로는 더 가입할 수 없다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 팀 ID | `12` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 재발급 성공 | [InviteCodeResponse](#invitecoderesponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | 팀장이 아님 (TEAM_403) | [ErrorResponse](#errorresponse) |
| 404 | 없는 팀이거나 내가 멤버가 아닌 팀 (TEAM_404) | [ErrorResponse](#errorresponse) |

#### `DELETE` /api/teams/{id}/members/me

**팀 탈퇴 (팀원)** · 권한: 로그인

내 멤버 행을 지운다. 팀장은 탈퇴할 수 없고 팀을 삭제해야 한다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 팀 ID | `12` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 204 | 탈퇴 성공 | - |
| 400 | 팀장 (TEAM_004) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 404 | 없는 팀이거나 내가 멤버가 아닌 팀 (TEAM_404) | [ErrorResponse](#errorresponse) |

#### `DELETE` /api/teams/{id}/members/{userId}

**멤버 내보내기 (팀장)** · 권한: 로그인

팀원의 멤버 행을 지운다. 그 회원은 같은 코드로 다시 가입할 수 있다.

**파라미터**

| 이름 | 위치 | 필수 | 타입 | 설명 | 예 |
|---|---|---|---|---|---|
| `id` | path | 필수 | integer(int64) | 팀 ID | `12` |
| `userId` | path | 필수 | integer(int64) | 내보낼 회원 ID | `7` |

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 204 | 내보내기 성공 | - |
| 400 | 자기 자신 (TEAM_004) | [ErrorResponse](#errorresponse) |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |
| 403 | 팀장이 아님 (TEAM_403) | [ErrorResponse](#errorresponse) |
| 404 | 없는 팀이거나 내가 멤버가 아닌 팀 (TEAM_404), 대상이 그 팀 멤버가 아님 (TEAM_005) | [ErrorResponse](#errorresponse) |

#### `GET` /api/users/me/teams

**내 팀 목록** · 권한: 로그인

내가 속한 팀을 내가 가입한 시각 최신순(같으면 팀 id 내림차순)으로 전부 준다 (최대 5개).

**응답**

| 상태 | 설명 | 스키마 |
|---|---|---|
| 200 | 조회 성공 | [MyTeamResponse](#myteamresponse)[] |
| 401 | 로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401) | [ErrorResponse](#errorresponse) |

## 스키마 부록

### AdminChallengeCreateResponse

챌린지 등록 결과

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `id` | integer(int64) | 필수 | 새 챌린지 ID | `7` |

### AdminChallengeRequest

관리자 챌린지 등록·수정 요청 (multipart/form-data)

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `description` | string,null | 선택 | 설명 (2,000자 이하, 비면 null) (길이 0~2000자) | `분석한 프로젝트의 퀴즈를 12월 안에 10번 풀어 보세요.` |
| `endDate` | string(date) | 필수 | 종료일 yyyy-MM-dd. 시작일 이후, 기간(양 끝 포함) 90일 이하. 등록할 때와 종료일을 바꿀 때는 오늘 이후 (길이 1~자) | `2026-12-31` |
| `goalType` | string: REVIEW_COUNT · QUIZ_COUNT · ANALYSIS_COUNT | 필수 | 목표 유형 (REVIEW_COUNT 개념 복습, QUIZ_COUNT 퀴즈, ANALYSIS_COUNT 분석). 시작한 챌린지는 바꿀 수 없음 (길이 1~자) | `QUIZ_COUNT` |
| `goalValue` | string | 필수 | 목표값 (1~1,000). 시작한 챌린지는 바꿀 수 없음 (길이 1~자, 범위 1~1000) | `10` |
| `image` | string,null(binary) | 선택 | 대표 이미지 (jpg·png·webp, 2MB 이하, 확장자·MIME·내용이 일치). 수정 때 생략하면 기존 이미지를 유지한다 | - |
| `removeImage` | string: true · false | 선택 | 수정 전용. true면 기존 이미지를 지운다 (image와 함께 보낼 수 없음) (기본 `false`) | - |
| `startDate` | string(date) | 필수 | 시작일 yyyy-MM-dd (과거 허용). 시작한 챌린지는 바꿀 수 없음 (길이 1~자) | `2026-12-01` |
| `title` | string | 필수 | 제목 (앞뒤 공백 제거 후 1~100자) (길이 0~100자) | `12월 퀴즈 챌린지` |

### AdminChallengeResponse

관리자 챌린지 상세

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `createdAt` | string(date-time) | 필수 | 등록 시각 | `2026-11-24T08:50:00+09:00` |
| `description` | string,null | 필수 | 설명 (없으면 null) | `분석한 프로젝트의 퀴즈를 12월 안에 10번 풀어 보세요.` |
| `endDate` | string(date) | 필수 | 종료일 (포함) | `2026-12-31` |
| `goalType` | string: REVIEW_COUNT · QUIZ_COUNT · ANALYSIS_COUNT | 필수 | 목표 유형 (REVIEW_COUNT 개념 복습, QUIZ_COUNT 퀴즈, ANALYSIS_COUNT 분석) | `QUIZ_COUNT` |
| `goalValue` | integer(int32) | 필수 | 목표값 | `10` |
| `id` | integer(int64) | 필수 | 챌린지 ID | `7` |
| `imageUrl` | string,null | 필수 | 대표 이미지 공개 주소 (없으면 null) | `/uploads/challenges/3f2b9c1e-8a4d-4f7e-9b1a-2c5d6e7f8a90.png` |
| `participantCount` | integer(int64) | 필수 | 참여자 수 | `0` |
| `startDate` | string(date) | 필수 | 시작일 | `2026-12-01` |
| `status` | string: UPCOMING · ONGOING · ENDED | 필수 | 상태 (UPCOMING 예정, ONGOING 진행 중, ENDED 종료). 오늘(Asia/Seoul) 기준으로 계산 | `UPCOMING` |
| `title` | string | 필수 | 제목 | `12월 퀴즈 챌린지` |
| `updatedAt` | string(date-time) | 필수 | 수정 시각 | `2026-11-24T08:50:00+09:00` |

### AdminChallengeSummaryResponse

관리자 챌린지 목록 항목

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `endDate` | string(date) | 필수 | 종료일 (포함) | `2026-12-31` |
| `goalType` | string: REVIEW_COUNT · QUIZ_COUNT · ANALYSIS_COUNT | 필수 | 목표 유형 (REVIEW_COUNT 개념 복습, QUIZ_COUNT 퀴즈, ANALYSIS_COUNT 분석) | `QUIZ_COUNT` |
| `goalValue` | integer(int32) | 필수 | 목표값 | `10` |
| `id` | integer(int64) | 필수 | 챌린지 ID | `7` |
| `imageUrl` | string,null | 필수 | 대표 이미지 공개 주소 (없으면 null) | `/uploads/challenges/3f2b9c1e-8a4d-4f7e-9b1a-2c5d6e7f8a90.png` |
| `participantCount` | integer(int64) | 필수 | 참여자 수 | `0` |
| `startDate` | string(date) | 필수 | 시작일 | `2026-12-01` |
| `status` | string: UPCOMING · ONGOING · ENDED | 필수 | 상태 (UPCOMING 예정, ONGOING 진행 중, ENDED 종료). 오늘(Asia/Seoul) 기준으로 계산 | `UPCOMING` |
| `title` | string | 필수 | 제목 | `12월 퀴즈 챌린지` |

### AdminConceptCreateResponse

카드 생성 결과

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `id` | integer(int64) | 필수 | 새 카드 ID | `92` |

### AdminConceptDetailResponse

관리자 카드 상세

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `avgRating` | number(double) | 필수 | 평균 별점 (0.0~5.0, 리뷰가 없으면 0.0) | `0` |
| `body` | string,null | 필수 | 본문 (Markdown 원문) | `## 핵심<br>- 엔티티에 버전 컬럼을 두고 UPDATE 조건에 넣는다.` |
| `category` | string | 필수 | 카테고리 (java, spring, web, security, mybatis, vue) | `spring` |
| `createdAt` | string(date-time) | 필수 | 작성 시각 | `2026-11-20T14:20:00+09:00` |
| `difficulty` | string: BEGINNER · JUNIOR · SENIOR | 필수 | 난이도 | `SENIOR` |
| `exampleCode` | string,null | 필수 | 예제 코드 | `UPDATE orders SET status = ?, version = version + 1 WHERE id = ? AND version = ?` |
| `id` | integer(int64) | 필수 | 카드 ID | `91` |
| `linkedAnalysisCount` | integer(int64) | 필수 | 이 카드가 연결된 분석 수 (삭제된 분석은 연결이 지워져 빠진다) | `2` |
| `name` | string | 필수 | 개념 이름 | `낙관적 락` |
| `referenceUrl` | string,null | 필수 | 공식 문서 출처 링크 | `https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html` |
| `reviewCount` | integer(int64) | 필수 | 리뷰 수 | `0` |
| `source` | string: SEED · AI · ADMIN | 필수 | 출처 (SEED 시드, AI 분석 초안, ADMIN 관리자 작성) | `AI` |
| `status` | string: DRAFT · PUBLISHED · HIDDEN | 필수 | 상태 (DRAFT 검수 대기, PUBLISHED 공개, HIDDEN 숨김) | `DRAFT` |
| `summary` | string | 필수 | 한 줄 요약 | `충돌이 드물다고 보고 저장 시점에 버전으로 충돌을 확인하는 동시성 제어 방식이다.` |
| `updatedAt` | string(date-time) | 필수 | 수정 시각 | `2026-11-20T14:20:00+09:00` |
| `wishCount` | integer(int32) | 필수 | 찜 수 | `0` |

### AdminConceptRequest

관리자 카드 생성·수정 요청

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `body` | string,null | 선택 | 본문 Markdown (10,000자 이하, 비면 null) (길이 0~10000자) | `## 핵심<br>- 엔티티에 버전 컬럼을 두고 UPDATE 조건에 넣는다.` |
| `category` | string: java · spring · web · security · mybatis · vue | 필수 | 카테고리 (길이 1~자) | `spring` |
| `difficulty` | string: BEGINNER · JUNIOR · SENIOR | 필수 | 난이도 (길이 1~자) | `SENIOR` |
| `exampleCode` | string,null | 선택 | 예제 코드 (10,000자 이하, 비면 null) (길이 0~10000자) | `UPDATE orders SET status = ?, version = version + 1 WHERE id = ? AND version = ?` |
| `name` | string | 필수 | 개념 이름 (1~100자, 시드 카드는 바꿀 수 없음) (길이 0~100자) | `낙관적 락` |
| `referenceUrl` | string,null | 선택 | 공식 문서 출처 링크 (http·https만, 500자 이하, 비면 null) (길이 0~500자) | `https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html` |
| `status` | string: DRAFT · PUBLISHED · HIDDEN | 필수 | 상태. 생성은 DRAFT·PUBLISHED만, 공개·숨김 카드는 DRAFT로 되돌릴 수 없음 (길이 1~자) | `PUBLISHED` |
| `summary` | string | 필수 | 한 줄 요약 (1~500자) (길이 0~500자) | `충돌이 드물다고 보고 저장 시점에 버전으로 충돌을 확인하는 동시성 제어 방식이다.` |

### AdminConceptStatusCountsResponse

상태별 카드 수

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `DRAFT` | integer(int64) | 필수 | 검수 대기 | `3` |
| `HIDDEN` | integer(int64) | 필수 | 숨김 | `2` |
| `PUBLISHED` | integer(int64) | 필수 | 공개 | `85` |

### AdminConceptSummaryResponse

관리자 카드 목록 항목

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `avgRating` | number(double) | 필수 | 평균 별점 (0.0~5.0, 리뷰가 없으면 0.0) | `0` |
| `category` | string | 필수 | 카테고리 (java, spring, web, security, mybatis, vue) | `spring` |
| `difficulty` | string: BEGINNER · JUNIOR · SENIOR | 필수 | 난이도 | `SENIOR` |
| `id` | integer(int64) | 필수 | 카드 ID | `91` |
| `name` | string | 필수 | 개념 이름 | `낙관적 락` |
| `source` | string: SEED · AI · ADMIN | 필수 | 출처 (SEED 시드, AI 분석 초안, ADMIN 관리자 작성) | `AI` |
| `status` | string: DRAFT · PUBLISHED · HIDDEN | 필수 | 상태 (DRAFT 검수 대기, PUBLISHED 공개, HIDDEN 숨김) | `DRAFT` |
| `summary` | string | 필수 | 한 줄 요약 | `충돌이 드물다고 보고 저장 시점에 버전으로 충돌을 확인하는 동시성 제어 방식이다.` |
| `updatedAt` | string(date-time) | 필수 | 수정 시각 | `2026-11-20T14:20:00+09:00` |
| `wishCount` | integer(int32) | 필수 | 찜 수 | `0` |

### AnalysisConceptItem

분석 코드에 쓰인 개념 카드와 위치

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `concept` | [ConceptSummaryResponse](#conceptsummaryresponse) | 필수 | 카드 요약 | - |
| `contextNote` | string,null | 선택 | 이 코드에서 개념이 쓰인 방식 (없으면 null) | `생성자로 TodoMapper를 받아 필드에 저장합니다.` |
| `fileId` | integer,null(int64) | 선택 | 개념이 쓰인 분석 파일 ID (없으면 null) | `102` |
| `filePath` | string,null | 선택 | 그 파일 경로 (없으면 null) | `src/main/java/com/example/todo/TodoService.java` |
| `lineHint` | string,null | 선택 | 줄 위치 L12 또는 L12-L30 (없으면 null) | `L8-L15` |

### AnalysisConceptsResponse

분석에 연결된 개념 카드

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `items` | [AnalysisConceptItem](#analysisconceptitem)[] | 필수 | 공개(PUBLISHED) 카드. 파일이 있는 것 먼저 파일 ID 순, 그다음 카드 ID 순 | - |
| `pendingCount` | integer(int32) | 필수 | 관리자 검수를 기다리는 새 개념(AI 초안) 수 | `1` |

### AnalysisDetailResponse

분석 상세 (개요 포함)

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `createdAt` | string(date-time) | 필수 | 요청 시각 | `2026-11-24T14:20:00+09:00` |
| `errorCode` | string,null | 선택 | 실패 사유 코드. FAILED가 아니면 null | `LLM_503` |
| `errorMessage` | string,null | 선택 | 실패 사유 문구. FAILED가 아니면 null | - |
| `explainedFileCount` | integer(int32) | 필수 | 설명이 만들어진 파일 수 | `14` |
| `fileCount` | integer(int32) | 필수 | 선별해 저장한 파일 수 | `14` |
| `id` | integer(int64) | 필수 | 분석 ID | `42` |
| `isPublic` | boolean | 필수 | 공개 여부 | `false` |
| `level` | string: BEGINNER · JUNIOR · SENIOR | 필수 | 분석 수준 | `JUNIOR` |
| `liteMode` | boolean | 필수 | 간이 모드 여부. 성공한 LLM 호출 중 서버의 로컬 AI(Ollama)가 1건 이상이면 true. 처리 중이면 그 시점까지의 기록으로 계산한다 (F-27) | `false` |
| `memo` | string,null | 선택 | 메모. 주인이 아니면(mine = false) 항상 null | `JWT 필터 순서 다시 보기` |
| `mine` | boolean | 필수 | 요청한 회원의 분석인지 | `true` |
| `overview` | [AnalysisOverview](#analysisoverview) | 선택 | 프로젝트 개요. 개요 생성 전·실패 시 null | - |
| `owner` | [AnalysisOwnerResponse](#analysisownerresponse) | 필수 | 분석 주인 | - |
| `progress` | integer(int32) | 필수 | 진행률 (0~100) | `100` |
| `projectName` | string,null | 선택 | 프로젝트 이름 | `todo-app` |
| `quizRegenerationsLeft` | integer(int32) | 필수 | 퀴즈 다시 만들기 남은 횟수 (분석당 3회 − 사용, 최소 0, L-1a) (범위 0~) | `2` |
| `quizStatus` | string,null: GENERATING · READY · FAILED | 선택 | 퀴즈 상태. GENERATING 재생성 중, READY 퀴즈 있음, FAILED 생성 실패(다시 만들 수 있음). QUIZ 단계를 끝내기 전이면 null (L-1a) | `READY` |
| `sourceType` | string: URL · ZIP | 필수 | 입력 종류 | `ZIP` |
| `sourceUrl` | string,null | 선택 | 저장소 주소(정규화). ZIP 분석이면 null (F-05b AC12) | `https://github.com/octo/todo-app` |
| `status` | string: FETCHING · FILTERING · SELECTING · EXPLAINING · EXTRACTING · QUIZ · DONE · FAILED | 필수 | 진행 상태 | `DONE` |
| `title` | string,null | 선택 | 제목 | `todo-app` |

### AnalysisFileResponse

분석 파일 (코드 포함)

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `content` | string | 필수 | 코드 내용 | `package com.example.todo;` |
| `id` | integer(int64) | 필수 | 파일 ID | `101` |
| `layer` | string: controller · service · mapper · vue · js · config · domain · other | 필수 | 레이어 | `controller` |
| `path` | string | 필수 | 프로젝트 루트 기준 경로 | `src/main/java/com/example/todo/TodoController.java` |
| `role` | string,null | 선택 | 한 줄 역할. 설명을 만들지 못했으면 null | - |
| `summary` | string,null | 선택 | 파일 설명. 설명을 만들지 못했으면 null | - |

### AnalysisFileSummaryResponse

분석 파일 목록 항목

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `id` | integer(int64) | 필수 | 파일 ID | `101` |
| `layer` | string: controller · service · mapper · vue · js · config · domain · other | 필수 | 레이어 | `controller` |
| `path` | string | 필수 | 프로젝트 루트 기준 경로 | `src/main/java/com/example/todo/TodoController.java` |
| `role` | string,null | 선택 | 한 줄 역할. 설명을 만들지 못했으면 null | `할 일 목록·등록·완료 API를 제공하는 컨트롤러` |
| `summary` | string,null | 선택 | 파일 설명. 설명을 만들지 못했으면 null | `@RestController가 붙어 있어 메서드의 반환값이 JSON으로 응답됩니다.` |

### AnalysisOverview

프로젝트 개요 (AI 생성 + 규칙 기반 API 목록)

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `apis` | [ApiEndpoint](#apiendpoint)[] | 필수 | 컨트롤러 매핑 어노테이션으로 탐지한 API (path → method 순, 최대 50개) | - |
| `layers` | [LayerRole](#layerrole)[] | 필수 | 레이어별 역할 | - |
| `readingOrder` | [ReadingStep](#readingstep)[] | 필수 | 처음 읽을 파일 순서 (최대 5개) | - |
| `summary` | string | 필수 | 프로젝트 요약 (800자 이내) | `할 일을 등록하고 완료 표시하는 Todo 웹 서비스입니다.` |
| `techStack` | string[] | 필수 | 기술 스택 (최대 10개, 항목 40자 이내) | - |

### AnalysisOwnerResponse

분석 주인

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `id` | integer(int64) | 필수 | 회원 ID | `7` |
| `nickname` | string | 필수 | 닉네임 | `vibecoder` |

### AnalysisStatusResponse

분석 진행 상태

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `analysisId` | integer(int64) | 필수 | 분석 ID | `42` |
| `errorCode` | string,null: ANALYSIS_004 · ANALYSIS_400 · ANALYSIS_413 · ANALYSIS_415 · ANALYSIS_500 · ANALYSIS_502 · ANALYSIS_503 · LLM_502 · LLM_503 | 선택 | 실패 사유 코드. FAILED가 아니면 null. ANALYSIS_413은 저장소 아카이브 크기 초과(URL 분석)에서만 나오고, 코드 파일 수·합계로는 실패하지 않는다 | `ANALYSIS_413` |
| `errorMessage` | string,null | 선택 | 사용자에게 보여 줄 실패 사유. FAILED가 아니면 null | `저장소 아카이브가 30MB를 넘습니다. ZIP으로 필요한 코드만 올려 주세요.` |
| `progress` | integer(int32) | 필수 | 진행률 (0~100). FAILED면 실패 직전 값 | `15` |
| `quizStatus` | string,null: GENERATING · READY · FAILED | 선택 | 퀴즈 상태. GENERATING 재생성 중, READY 퀴즈 있음, FAILED 생성 실패(다시 만들 수 있음). QUIZ 단계를 끝내기 전이면 null (L-1a) | `READY` |
| `sourceUrl` | string,null | 선택 | 저장소 주소(정규화). ZIP 분석이면 null. FAILED 화면의 "저장소 보기" 링크 (F-19 AC12) | `https://github.com/octo/todo-app` |
| `status` | string: FETCHING · FILTERING · SELECTING · EXPLAINING · EXTRACTING · QUIZ · DONE · FAILED | 필수 | 진행 상태 | `FILTERING` |
| `updatedAt` | string(date-time) | 필수 | 마지막 상태 변경 시각 | `2026-11-24T14:20:01+09:00` |

### AnalysisStructureResponse

파일 의존 그래프

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `edges` | [StructureEdge](#structureedge)[] | 필수 | 코드에서 확정한 연결 (from → to → type 순, 상한까지) | - |
| `edgesTruncated` | boolean | 필수 | 연결이 상한을 넘어 잘렸는가 | `false` |
| `nodes` | [StructureNode](#structurenode)[] | 필수 | 모든 선별 파일 (fileId 오름차순) | - |
| `unresolved` | [UnresolvedLink](#unresolvedlink)[] | 필수 | 확인하지 못한 연결 (fileId → line 순, 상한까지) | - |
| `unresolvedTruncated` | boolean | 필수 | 확인하지 못한 연결이 상한을 넘어 잘렸는가 | `false` |

### AnalysisSummaryResponse

분석 목록 항목

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `createdAt` | string(date-time) | 필수 | 요청 시각 | `2026-11-24T14:20:00+09:00` |
| `id` | integer(int64) | 필수 | 분석 ID | `42` |
| `isPublic` | boolean | 필수 | 공개 여부 | `false` |
| `level` | string: BEGINNER · JUNIOR · SENIOR | 필수 | 분석 수준 | `JUNIOR` |
| `memo` | string,null | 선택 | 메모 (F-19) | `JWT 필터 순서 다시 보기` |
| `progress` | integer(int32) | 필수 | 진행률 (0~100) | `25` |
| `projectName` | string,null | 선택 | 프로젝트 이름 (ZIP 파일명에서 .zip을 뺀 이름) | `todo-app` |
| `sourceType` | string: URL · ZIP | 필수 | 입력 종류 | `ZIP` |
| `status` | string: FETCHING · FILTERING · SELECTING · EXPLAINING · EXTRACTING · QUIZ · DONE · FAILED | 필수 | 진행 상태 | `SELECTING` |
| `title` | string,null | 선택 | 제목 (처음에는 프로젝트 이름) | `todo-app` |

### ApiEndpoint

탐지한 API

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `fileId` | integer(int64) | 필수 | 매핑이 있는 파일 ID | `101` |
| `method` | string | 필수 | HTTP 메서드 (method 없는 @RequestMapping은 ANY) | `GET` |
| `path` | string | 필수 | 경로 | `/api/todos/{id}` |

### ChallengeDetailResponse

챌린지 상세

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `description` | string,null | 필수 | 설명 (없으면 null) | `분석한 프로젝트의 퀴즈를 기간 안에 10번 풀어 보세요.` |
| `endDate` | string(date) | 필수 | 종료일 (포함) | `2026-11-30` |
| `goalType` | string: REVIEW_COUNT · QUIZ_COUNT · ANALYSIS_COUNT | 필수 | 목표 유형 (REVIEW_COUNT 개념 복습, QUIZ_COUNT 퀴즈, ANALYSIS_COUNT 분석) | `QUIZ_COUNT` |
| `goalValue` | integer(int32) | 필수 | 목표값 | `10` |
| `id` | integer(int64) | 필수 | 챌린지 ID | `7` |
| `imageUrl` | string,null | 필수 | 대표 이미지 공개 주소 (없으면 null) | `/uploads/challenges/3f2b9c1e-8a4d-4f7e-9b1a-2c5d6e7f8a90.png` |
| `participantCount` | integer(int64) | 필수 | 참여자 수 | `2` |
| `participation` | [ChallengeProgressResponse](#challengeprogressresponse) | 필수 | 내 참여·달성도 (비로그인이거나 참여하지 않았으면 null) | - |
| `startDate` | string(date) | 필수 | 시작일 | `2026-11-20` |
| `status` | string: UPCOMING · ONGOING · ENDED | 필수 | 상태 (UPCOMING 예정, ONGOING 진행 중, ENDED 종료). 오늘(Asia/Seoul) 기준으로 계산 | `ONGOING` |
| `title` | string | 필수 | 제목 | `12월 퀴즈 챌린지` |

### ChallengeProgressResponse

내 챌린지 참여·달성도

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `completedAt` | string,null(date-time) | 필수 | 달성 시각 (미달성이면 null) | `2026-11-24T10:15:00+09:00` |
| `goalValue` | integer(int32) | 필수 | 목표값 | `10` |
| `joinedAt` | string(date-time) | 필수 | 참여 시각 | `2026-11-22T21:05:00+09:00` |
| `progress` | integer(int32) | 필수 | 달성도 (기간 안 활동 수, 목표값을 넘지 않는다) | `3` |

### ChallengeSummaryResponse

챌린지 목록 항목

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `endDate` | string(date) | 필수 | 종료일 (포함) | `2026-11-30` |
| `goalType` | string: REVIEW_COUNT · QUIZ_COUNT · ANALYSIS_COUNT | 필수 | 목표 유형 (REVIEW_COUNT 개념 복습, QUIZ_COUNT 퀴즈, ANALYSIS_COUNT 분석) | `QUIZ_COUNT` |
| `goalValue` | integer(int32) | 필수 | 목표값 | `10` |
| `id` | integer(int64) | 필수 | 챌린지 ID | `7` |
| `imageUrl` | string,null | 필수 | 대표 이미지 공개 주소 (없으면 null) | `/uploads/challenges/3f2b9c1e-8a4d-4f7e-9b1a-2c5d6e7f8a90.png` |
| `joined` | boolean | 필수 | 내가 참여했는지 (비로그인이면 false) | `true` |
| `participantCount` | integer(int64) | 필수 | 참여자 수 | `2` |
| `startDate` | string(date) | 필수 | 시작일 | `2026-11-20` |
| `status` | string: UPCOMING · ONGOING · ENDED | 필수 | 상태 (UPCOMING 예정, ONGOING 진행 중, ENDED 종료). 오늘(Asia/Seoul) 기준으로 계산 | `ONGOING` |
| `title` | string | 필수 | 제목 | `12월 퀴즈 챌린지` |

### CommentListResponse

댓글 목록

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `items` | [CommentResponse](#commentresponse)[] | 필수 | 가장 최근 200개를 오래된순(작성 시각, id 오름차순)으로 | - |
| `total` | integer(int64) | 필수 | 그 글의 전체 댓글 수 (items 수보다 크면 오래된 댓글은 빠진 것) | `2` |

### CommentRequest

댓글 작성·수정 요청

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `content` | string | 필수 | 내용 (앞뒤 공백 제외 1~500자, 글자 그대로 저장) (길이 0~500자) | `같은 클래스 안에서 호출하면 프록시를 거치지 않아서 트랜잭션이 안 걸려요.` |

### CommentResponse

댓글

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `authorId` | integer,null(int64) | 필수 | 작성자 ID (탈퇴한 회원이면 null) | `9` |
| `authorNickname` | string | 필수 | 작성자 현재 닉네임 (탈퇴한 회원이면 "탈퇴한 사용자") | `스프링러버` |
| `content` | string | 필수 | 내용 (글자 그대로. 화면은 HTML로 해석하지 않는다) | `같은 클래스 안에서 호출하면 프록시를 거치지 않아서 트랜잭션이 안 걸려요.<br>별도 빈으로 빼 보세요.` |
| `createdAt` | string(date-time) | 필수 | 작성 시각 | `2026-11-20T14:10:05+09:00` |
| `edited` | boolean | 필수 | 수정한 적이 있는지 (updatedAt이 createdAt보다 뒤) | `false` |
| `id` | integer(int64) | 필수 | 댓글 ID | `101` |
| `updatedAt` | string(date-time) | 필수 | 마지막 수정 시각 (수정한 적 없으면 작성 시각) | `2026-11-20T14:10:05+09:00` |

### ConceptAppearanceResponse

내 코드에서 이 개념이 등장한 위치

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `analysisId` | integer(int64) | 필수 | 분석 ID | `31` |
| `analysisTitle` | string | 필수 | 분석 제목 (없으면 프로젝트 이름, 둘 다 없으면 "제목 없는 분석") | `쇼핑몰 과제` |
| `contextNote` | string,null | 필수 | 이 코드에서 개념이 쓰인 맥락 | `로그인 성공 시 토큰을 만든다` |
| `fileId` | integer,null(int64) | 필수 | 등장한 파일 ID (파일을 특정하지 못했으면 null) | `412` |
| `filePath` | string,null | 필수 | 등장한 파일 경로 (fileId가 null이면 null) | `src/main/java/com/shop/security/JwtProvider.java` |
| `lineHint` | string,null | 필수 | 줄 위치 힌트 | `L24-L40` |

### ConceptDetailResponse

개념 카드 상세

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `appearances` | [ConceptAppearanceResponse](#conceptappearanceresponse)[] | 필수 | 내 분석(삭제되지 않은 DONE)에서 등장한 위치, 최신순 최대 10개. 비로그인은 빈 배열 | - |
| `avgRating` | number(double) | 필수 | 평균 별점 (0.0~5.0, 소수 1자리. 리뷰가 없으면 0.0) | `4.5` |
| `body` | string,null | 필수 | 쉬운 설명 본문 (Markdown 원문, 렌더링은 FE) | `## 핵심<br>- 서버는 **서명**과 만료만 검증한다.` |
| `category` | string | 필수 | 카테고리 (java, spring, security, mybatis, web, vue) | `security` |
| `difficulty` | string: BEGINNER · JUNIOR · SENIOR | 필수 | 난이도 | `SENIOR` |
| `exampleCode` | string,null | 필수 | 예제 코드 (언어 정보 없음) | `String token = Jwts.builder().subject(email).compact();` |
| `id` | integer(int64) | 필수 | 카드 ID | `57` |
| `name` | string | 필수 | 개념 이름 | `JWT 기반 인증` |
| `referenceUrl` | string,null | 필수 | 공식 문서 출처 링크 | `https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html` |
| `related` | [ConceptSummaryResponse](#conceptsummaryresponse)[] | 필수 | 관련 카드 최대 4개 (함께 등장한 횟수순, 모자라면 같은 카테고리 인기순) | - |
| `summary` | string | 필수 | 한 줄 요약 | `JWT는 사용자 정보와 만료 시간을 담고 서명한 토큰이다.` |
| `wishCount` | integer(int32) | 필수 | 찜 수 | `12` |
| `wished` | boolean | 필수 | 내가 찜했는지 (F-12). 비로그인·깨진 토큰이면 false | `false` |

### ConceptRatingResponse

카드 평점과 리뷰 수

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `avgRating` | number(double) | 필수 | 평균 별점 (소수 1자리, 리뷰가 없으면 0.0) | `4.2` |
| `reviewCount` | integer(int64) | 필수 | 리뷰 수 | `12` |

### ConceptReviewRequest

리뷰 작성·수정 요청

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `content` | string | 필수 | 리뷰 내용 (앞뒤 공백 제외 1~200자) (길이 0~200자) | `예제가 명확했어요` |
| `rating` | integer(int32) | 필수 | 별점 (정수 1~5) (범위 1~5) | `4` |

### ConceptReviewResponse

카드 리뷰

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `authorId` | integer,null(int64) | 필수 | 작성자 ID (탈퇴한 회원이면 null) | `7` |
| `authorNickname` | string | 필수 | 작성자 현재 닉네임 (탈퇴한 회원이면 "탈퇴한 사용자") | `코딩하는감자` |
| `content` | string | 필수 | 리뷰 내용 | `토큰 서명과 만료 검증 순서가 한눈에 들어왔어요` |
| `createdAt` | string(date-time) | 필수 | 작성 시각 | `2026-12-02T21:14:03+09:00` |
| `id` | integer(int64) | 필수 | 리뷰 ID | `301` |
| `rating` | integer(int32) | 필수 | 별점 (1~5) | `5` |

### ConceptReviewSaveResponse

리뷰 저장 결과

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `avgRating` | number(double) | 필수 | 평균 별점 (소수 1자리) | `4.3` |
| `review` | [ConceptReviewResponse](#conceptreviewresponse) | 필수 | 저장된 리뷰 | - |
| `reviewCount` | integer(int64) | 필수 | 리뷰 수 | `13` |

### ConceptSummaryResponse

개념 카드 요약

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `avgRating` | number(double) | 필수 | 평균 별점 (0.0~5.0, 소수 1자리. 리뷰가 없으면 0.0) | `4.5` |
| `category` | string | 필수 | 카테고리 (java, spring, security, mybatis, web, vue) | `security` |
| `difficulty` | string: BEGINNER · JUNIOR · SENIOR | 필수 | 난이도 | `JUNIOR` |
| `id` | integer(int64) | 필수 | 카드 ID | `12` |
| `name` | string | 필수 | 개념 이름 | `JWT 인증` |
| `summary` | string | 필수 | 한 줄 요약 | `서버가 서명한 토큰으로 요청한 사람을 확인하는 방식이다.` |
| `wishCount` | integer(int32) | 필수 | 찜 수 | `12` |

### ConceptWishResponse

찜 여부와 찜 수

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `reviewPlanDate` | string,null(date) | 필수 | 이번 찜으로 복습 계획이 새로 생겼으면 그 날짜(내일, Asia/Seoul). 이미 찜했거나 그 카드의 TODO·내일 계획이 있었거나 해제면 null | `2026-12-04` |
| `wishCount` | integer(int32) | 필수 | 요청 처리 후 카드의 찜 수 | `13` |
| `wished` | boolean | 필수 | 요청 후 내가 찜했는지 | `true` |

### CreateAnalysisResponse

분석 요청 접수 결과

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `analysisId` | integer(int64) | 필수 | 분석 ID | `42` |
| `status` | string: FETCHING · FILTERING · SELECTING · EXPLAINING · EXTRACTING · QUIZ · DONE · FAILED | 필수 | 현재 상태 (접수 직후 FETCHING) | `FETCHING` |

### CreateFlowsRequest

실행 흐름 요청

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `endpoints` | [FlowEndpoint](#flowendpoint)[] | 필수 | 흐름을 만들 API (개요 apis에서 1~3개, 중복 불가) (항목 1~3개) | - |

### CreateTeamChallengeRequest

팀 목표 만들기 요청

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `analysisId` | integer(int64) | 필수 | 팀에 공유된 분석 ID (범위 1~) | `42` |
| `endDate` | string(date) | 필수 | 종료일 (YYYY-MM-DD, 시작일과 같거나 그 뒤, 기간은 양끝 포함 최대 30일) | `2026-12-01` |
| `startDate` | string(date) | 필수 | 시작일 (YYYY-MM-DD, 오늘부터 오늘 + 365일까지) | `2026-11-25` |
| `targetRate` | integer(int32) | 필수 | 목표 정답률 (%) (범위 1~100) | `80` |

### ErrorResponse

실패 응답 (docs/conventions.md 2절)

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `code` | string | 선택 | 에러 코드 | `COMMON_400` |
| `details` | map<string, string> | 선택 | 검증 실패 시 필드별 사유. 없으면 null | `{"email":"이메일 형식이 아닙니다."}` |
| `message` | string | 선택 | 사용자에게 보여 줄 한국어 메시지 | `요청 값이 올바르지 않습니다.` |

### FeedActorResponse

활동한 회원

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `id` | integer(int64) | 필수 | 회원 ID | `7` |
| `nickname` | string | 필수 | 닉네임 | `java러버` |

### FeedItemResponse

피드 항목 (활동 하나)

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `actor` | [FeedActorResponse](#feedactorresponse) | 필수 | - | - |
| `createdAt` | string(date-time) | 필수 | 활동 시각 | `2026-11-20T13:40:00+09:00` |
| `id` | integer(int64) | 필수 | 활동 ID | `91` |
| `target` | [FeedTargetResponse](#feedtargetresponse) | 필수 | - | - |
| `type` | string: ANALYSIS · REVIEW · WISH · POST · CHALLENGE_JOIN · CHALLENGE_COMPLETE | 필수 | 활동 종류 | `REVIEW` |

### FeedTargetResponse

활동 대상. id는 링크할 대상의 id (REVIEW·WISH는 카드, ANALYSIS는 분석, POST는 글, CHALLENGE_*는 챌린지)

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `id` | integer(int64) | 필수 | 링크할 대상 ID. REVIEW는 리뷰 id가 아니라 카드 id | `12` |
| `rating` | integer,null(int32) | 필수 | REVIEW의 별점(1~5). 나머지 종류는 null | `5` |
| `title` | string | 필수 | 대상 이름. ANALYSIS는 분석 제목, 없으면 프로젝트명, 둘 다 없으면 "제목 없는 분석" | `JWT 기반 인증` |

### FlowEndpoint

API 하나 (개요 apis의 method·path 그대로)

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `method` | string | 필수 | HTTP 메서드 (개요 apis 값, ANY 포함) (길이 1~자) | `POST` |
| `path` | string | 필수 | 경로 (길이 1~자) | `/api/todos` |

### FlowResponse

실행 흐름

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `createdAt` | string(date-time) | 필수 | 요청 시각 | `2026-12-09T10:00:00+09:00` |
| `errorCode` | string,null: ANALYSIS_500 · LLM_502 · LLM_503 | 선택 | 실패 사유 코드. FAILED가 아니면 null | `LLM_503` |
| `errorMessage` | string,null | 선택 | 사용자에게 보여 줄 실패 사유. FAILED가 아니면 null | `AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.` |
| `id` | integer(int64) | 필수 | 흐름 ID | `7` |
| `method` | string | 필수 | HTTP 메서드 | `POST` |
| `path` | string | 필수 | 경로 | `/api/todos` |
| `status` | string: PENDING · DONE · FAILED | 필수 | 생성 상태 | `DONE` |
| `steps` | array,null | 선택 | 단계 (최대 12개). DONE이 아니면 null | - |
| `summary` | string,null | 선택 | 전체 흐름 요약 (500자 이내). DONE이 아니면 null | `새 할 일을 등록하면 컨트롤러가 입력을 검사하고, 서비스가 저장을 맡깁니다.` |

### FlowStep

실행 흐름 단계

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `description` | string | 필수 | 단계 설명 (600자 이내) | `@PostMapping이 붙은 create 메서드가 JSON 본문을 @RequestBody로 받습니다.` |
| `fileId` | integer,null(int64) | 선택 | 파일 ID. 분석에 없는 경로면 null | `101` |
| `layer` | string,null: controller · service · mapper · vue · js · config · domain · other | 선택 | 파일 layer. 분석에 없는 경로면 null | `controller` |
| `no` | integer(int32) | 필수 | 단계 번호 (1부터) | `1` |
| `path` | string | 필수 | 파일 경로 | `src/main/java/com/example/todo/TodoController.java` |
| `symbol` | string,null | 선택 | 클래스.메서드 (100자 이내) | `TodoController.create` |
| `title` | string | 필수 | 단계 이름 (60자 이내) | `요청 받기` |

### FollowStatusResponse

팔로우 여부와 대상 회원의 팔로워·팔로잉 수

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `followerCount` | integer(int64) | 필수 | 대상 회원을 팔로우하는 사람 수 | `12` |
| `following` | boolean | 필수 | 로그인한 내가 대상 회원을 팔로우 중인지. 비로그인·내 프로필이면 false | `true` |
| `followingCount` | integer(int64) | 필수 | 대상 회원이 팔로우하는 사람 수 | `3` |

### FollowUserResponse

팔로워·팔로잉 목록 항목

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `followedAt` | string(date-time) | 필수 | 팔로우한 시각 (follows.created_at) | `2026-11-20T15:02:11+09:00` |
| `id` | integer(int64) | 필수 | 회원 ID | `7` |
| `level` | string: BEGINNER · JUNIOR · SENIOR | 필수 | 학습 수준 | `JUNIOR` |
| `nickname` | string | 필수 | 닉네임 | `java러버` |

### InviteCodeResponse

초대 코드 재발급 응답

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `inviteCode` | string | 필수 | 새 초대 코드 8자 | `P4ZR8NWE` |

### JoinTeamRequest

초대 코드로 가입 요청

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `inviteCode` | string | 필수 | 초대 코드 8자 (대소문자·앞뒤 공백 무시) (길이 1~자) | `K7M2QX9P` |

### JoinTeamResponse

초대 코드로 가입 응답

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `teamId` | integer(int64) | 필수 | 가입한 팀 ID | `12` |

### LastAttemptResponse

내 가장 최근 풀이

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `attemptId` | integer(int64) | 필수 | 풀이 ID | `15` |
| `createdAt` | string(date-time) | 필수 | 제출 시각 | `2026-11-19T17:30:12+09:00` |
| `score` | integer(int32) | 필수 | 맞힌 문항 수 | `5` |
| `total` | integer(int32) | 필수 | 문항 수 | `6` |

### LayerRole

레이어 하나의 역할

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `layer` | string: controller · service · mapper · vue · js · config · domain · other | 필수 | 레이어 | `controller` |
| `role` | string | 필수 | 역할 (300자 이내) | `HTTP 요청을 받아 입력을 검사하고 Service를 부릅니다.` |

### LoginRequest

로그인 요청

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `email` | string | 필수 | 이메일 (길이 1~자) | `user@example.com` |
| `password` | string | 필수 | 비밀번호 (길이 1~자) | `vibe1234` |

### LoginResponse

로그인·토큰 재발급 응답

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `accessToken` | string | 필수 | Access 토큰 (Authorization: Bearer) | `eyJhbGciOiJIUzI1NiJ9...` |
| `expiresIn` | integer(int64) | 필수 | Access 토큰 유효 시간(초) | `1800` |
| `user` | [LoginUser](#loginuser) | 필수 | 로그인한 회원 | - |

### LoginUser

로그인한 회원 정보

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `id` | integer(int64) | 필수 | 회원 ID | `1` |
| `level` | string: BEGINNER · JUNIOR · SENIOR | 필수 | 학습 수준 | `BEGINNER` |
| `nickname` | string | 필수 | 닉네임 | `코딩초보` |
| `role` | string: USER · ADMIN | 필수 | 권한 | `USER` |

### MemberModuleResponse

멤버의 모듈별 결과

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `correct` | integer(int32) | 필수 | 맞힌 문항 수 | `1` |
| `layer` | string | 필수 | 레이어 | `controller` |
| `total` | integer(int32) | 필수 | 문항 수 | `2` |

### MemberUnderstandingResponse

멤버별 이해도

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `lastAttempt` | [LastAttemptResponse](#lastattemptresponse) | 선택 | 가장 최근 풀이 (안 풀었으면 null) | - |
| `modules` | [MemberModuleResponse](#membermoduleresponse)[] | 필수 | 레이어별 결과 (모듈 순서, 안 풀었으면 빈 배열) | - |
| `nickname` | string | 필수 | 닉네임 | `vibecoder` |
| `role` | string: OWNER · MEMBER | 필수 | 역할 | `OWNER` |
| `userId` | integer(int64) | 필수 | 회원 ID | `3` |

### ModuleUnderstandingResponse

모듈별 팀 이해도

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `correctRate` | integer(int32) | 선택 | 푼 멤버들의 정답률(0~100 정수, 반올림). 아무도 풀지 않았으면 null | `75` |
| `layer` | string: controller · service · mapper · vue · js · config · domain · other | 필수 | 레이어 | `controller` |
| `questionCount` | integer(int32) | 필수 | 이 레이어의 문항 수 | `2` |

### MyChallengeResponse

참여한 챌린지 항목

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `completedAt` | string,null(date-time) | 필수 | 달성 시각 (미달성이면 null) | `2026-11-24T10:15:00+09:00` |
| `endDate` | string(date) | 필수 | 종료일 (포함) | `2026-11-30` |
| `goalType` | string: REVIEW_COUNT · QUIZ_COUNT · ANALYSIS_COUNT | 필수 | 목표 유형 (REVIEW_COUNT 개념 복습, QUIZ_COUNT 퀴즈, ANALYSIS_COUNT 분석) | `QUIZ_COUNT` |
| `goalValue` | integer(int32) | 필수 | 목표값 | `10` |
| `id` | integer(int64) | 필수 | 챌린지 ID | `7` |
| `imageUrl` | string,null | 필수 | 대표 이미지 공개 주소 (없으면 null) | `/uploads/challenges/3f2b9c1e-8a4d-4f7e-9b1a-2c5d6e7f8a90.png` |
| `joinedAt` | string(date-time) | 필수 | 참여 시각 | `2026-11-22T21:05:00+09:00` |
| `progress` | integer(int32) | 필수 | 달성도 (기간 안 활동 수, 목표값을 넘지 않는다) | `3` |
| `startDate` | string(date) | 필수 | 시작일 | `2026-11-20` |
| `status` | string: UPCOMING · ONGOING · ENDED | 필수 | 상태 (UPCOMING 예정, ONGOING 진행 중, ENDED 종료). 오늘(Asia/Seoul) 기준으로 계산 | `ONGOING` |
| `title` | string | 필수 | 제목 | `12월 퀴즈 챌린지` |

### MyProfileResponse

내 프로필

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `bio` | string,null | 선택 | 소개. 없으면 null | `AI로 만든 Spring 프로젝트를 공부하고 있어요.` |
| `createdAt` | string(date-time) | 필수 | 가입 시각 | `2026-11-18T10:30:00+09:00` |
| `email` | string | 필수 | 이메일 | `user@example.com` |
| `id` | integer(int64) | 필수 | 회원 ID | `1` |
| `level` | string: BEGINNER · JUNIOR · SENIOR | 필수 | 학습 수준 | `BEGINNER` |
| `nickname` | string | 필수 | 닉네임 | `코딩초보` |
| `role` | string: USER · ADMIN | 필수 | 권한 | `USER` |

### MyTeamResponse

내 팀 목록 항목

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `description` | string,null | 선택 | 소개 | `F-05 분석 결과를 같이 공부합니다` |
| `id` | integer(int64) | 필수 | 팀 ID | `12` |
| `joinedAt` | string(date-time) | 필수 | 내가 가입한 시각 | `2026-11-24T10:00:00+09:00` |
| `memberCount` | integer(int32) | 필수 | 멤버 수 (팀장 포함) | `3` |
| `myRole` | string: OWNER · MEMBER | 필수 | 내 역할 | `OWNER` |
| `name` | string | 필수 | 팀 이름 | `스프링 스터디` |
| `ownerNickname` | string | 필수 | 팀장 닉네임 | `vibecoder` |

### PageResponseAdminChallengeSummaryResponse

페이지 응답

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `items` | [AdminChallengeSummaryResponse](#adminchallengesummaryresponse)[] | 선택 | 현재 페이지 항목 | - |
| `page` | integer(int32) | 선택 | 페이지 번호 (1부터) | `1` |
| `size` | integer(int32) | 선택 | 페이지 크기 | `20` |
| `total` | integer(int64) | 선택 | 전체 항목 수 | `134` |

### PageResponseAdminConceptSummaryResponse

페이지 응답

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `items` | [AdminConceptSummaryResponse](#adminconceptsummaryresponse)[] | 선택 | 현재 페이지 항목 | - |
| `page` | integer(int32) | 선택 | 페이지 번호 (1부터) | `1` |
| `size` | integer(int32) | 선택 | 페이지 크기 | `20` |
| `total` | integer(int64) | 선택 | 전체 항목 수 | `134` |

### PageResponseAnalysisSummaryResponse

페이지 응답

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `items` | [AnalysisSummaryResponse](#analysissummaryresponse)[] | 선택 | 현재 페이지 항목 | - |
| `page` | integer(int32) | 선택 | 페이지 번호 (1부터) | `1` |
| `size` | integer(int32) | 선택 | 페이지 크기 | `20` |
| `total` | integer(int64) | 선택 | 전체 항목 수 | `134` |

### PageResponseChallengeSummaryResponse

페이지 응답

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `items` | [ChallengeSummaryResponse](#challengesummaryresponse)[] | 선택 | 현재 페이지 항목 | - |
| `page` | integer(int32) | 선택 | 페이지 번호 (1부터) | `1` |
| `size` | integer(int32) | 선택 | 페이지 크기 | `20` |
| `total` | integer(int64) | 선택 | 전체 항목 수 | `134` |

### PageResponseConceptReviewResponse

페이지 응답

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `items` | [ConceptReviewResponse](#conceptreviewresponse)[] | 선택 | 현재 페이지 항목 | - |
| `page` | integer(int32) | 선택 | 페이지 번호 (1부터) | `1` |
| `size` | integer(int32) | 선택 | 페이지 크기 | `20` |
| `total` | integer(int64) | 선택 | 전체 항목 수 | `134` |

### PageResponseConceptSummaryResponse

페이지 응답

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `items` | [ConceptSummaryResponse](#conceptsummaryresponse)[] | 선택 | 현재 페이지 항목 | - |
| `page` | integer(int32) | 선택 | 페이지 번호 (1부터) | `1` |
| `size` | integer(int32) | 선택 | 페이지 크기 | `20` |
| `total` | integer(int64) | 선택 | 전체 항목 수 | `134` |

### PageResponseFeedItemResponse

페이지 응답

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `items` | [FeedItemResponse](#feeditemresponse)[] | 선택 | 현재 페이지 항목 | - |
| `page` | integer(int32) | 선택 | 페이지 번호 (1부터) | `1` |
| `size` | integer(int32) | 선택 | 페이지 크기 | `20` |
| `total` | integer(int64) | 선택 | 전체 항목 수 | `134` |

### PageResponseFollowUserResponse

페이지 응답

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `items` | [FollowUserResponse](#followuserresponse)[] | 선택 | 현재 페이지 항목 | - |
| `page` | integer(int32) | 선택 | 페이지 번호 (1부터) | `1` |
| `size` | integer(int32) | 선택 | 페이지 크기 | `20` |
| `total` | integer(int64) | 선택 | 전체 항목 수 | `134` |

### PageResponseMyChallengeResponse

페이지 응답

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `items` | [MyChallengeResponse](#mychallengeresponse)[] | 선택 | 현재 페이지 항목 | - |
| `page` | integer(int32) | 선택 | 페이지 번호 (1부터) | `1` |
| `size` | integer(int32) | 선택 | 페이지 크기 | `20` |
| `total` | integer(int64) | 선택 | 전체 항목 수 | `134` |

### PageResponsePostSummaryResponse

페이지 응답

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `items` | [PostSummaryResponse](#postsummaryresponse)[] | 선택 | 현재 페이지 항목 | - |
| `page` | integer(int32) | 선택 | 페이지 번호 (1부터) | `1` |
| `size` | integer(int32) | 선택 | 페이지 크기 | `20` |
| `total` | integer(int64) | 선택 | 전체 항목 수 | `134` |

### ParticipationResponse

챌린지 참여 상태

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `joined` | boolean | 필수 | 참여 여부 | `true` |
| `participantCount` | integer(int64) | 필수 | 참여자 수 | `3` |
| `participation` | [ChallengeProgressResponse](#challengeprogressresponse) | 필수 | 내 참여·달성도 (참여하지 않았으면 null) | - |

### PostAnalysisAttachment

분석 첨부

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `id` | integer(int64) | 필수 | 분석 ID | `15` |
| `projectName` | string,null | 필수 | 프로젝트명 (볼 수 없으면 null) | `shop-backend` |
| `title` | string,null | 필수 | 분석 제목 (볼 수 없거나 제목이 없으면 null) | `쇼핑몰 과제` |
| `viewable` | boolean | 필수 | 요청한 사람이 볼 수 있는지 (삭제되지 않았고, 내 분석이거나 탈퇴하지 않은 회원의 공개 분석) | `true` |

### PostConceptAttachment

개념 카드 첨부

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `category` | string,null | 필수 | 카테고리 (볼 수 없으면 null) | `security` |
| `difficulty` | string,null: BEGINNER · JUNIOR · SENIOR | 필수 | 난이도 (볼 수 없으면 null) | `JUNIOR` |
| `id` | integer(int64) | 필수 | 카드 ID | `12` |
| `name` | string,null | 필수 | 카드 이름 (볼 수 없으면 null) | `JWT` |
| `summary` | string,null | 필수 | 한 줄 요약 (볼 수 없으면 null) | `서버가 서명한 토큰으로 로그인 상태를 전달하는 방식` |
| `viewable` | boolean | 필수 | 볼 수 있는지 (공개 카드인지) | `true` |

### PostDetailResponse

게시글 상세

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `analysis` | [PostAnalysisAttachment](#postanalysisattachment) | 필수 | 분석 첨부 | - |
| `authorId` | integer,null(int64) | 필수 | 작성자 ID (탈퇴한 회원이면 null) | `7` |
| `authorNickname` | string | 필수 | 작성자 현재 닉네임 (탈퇴한 회원이면 "탈퇴한 사용자") | `코딩초보` |
| `boardType` | string: QNA · REVIEW · FREE | 필수 | 게시판 (QNA: 코드 이해 Q&A, REVIEW: 학습 후기, FREE: 자유) | `QNA` |
| `commentCount` | integer(int64) | 필수 | 댓글 수 | `3` |
| `concept` | [PostConceptAttachment](#postconceptattachment) | 필수 | 개념 카드 첨부 | - |
| `content` | string | 필수 | 본문 (글자 그대로. 화면은 HTML로 해석하지 않는다) | `같은 클래스 안에서 메서드를 호출하면 트랜잭션이 안 걸린다는데<br>왜 그런지 모르겠어요.` |
| `createdAt` | string(date-time) | 필수 | 작성 시각 | `2026-11-20T14:02:11+09:00` |
| `id` | integer(int64) | 필수 | 글 ID | `42` |
| `title` | string | 필수 | 제목 | `@Transactional이 안 먹는 이유가 뭔가요?` |
| `updatedAt` | string(date-time) | 필수 | 마지막 수정 시각 (조회수는 바꾸지 않는다) | `2026-11-20T14:02:11+09:00` |
| `viewCount` | integer(int32) | 필수 | 조회수 (상세 조회는 이번 조회를 더한 값) | `43` |

### PostIdResponse

만든 글 ID

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `id` | integer(int64) | 필수 | 글 ID | `42` |

### PostRequest

게시글 작성·수정 요청

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `analysisId` | integer,null(int64) | 선택 | 첨부할 내 분석 ID (공개·DONE이고 삭제되지 않은 내 분석, 비공개는 POST_001). conceptId와 함께 보낼 수 없다 | `15` |
| `boardType` | string: QNA · REVIEW · FREE | 필수 | 게시판 (QNA: 코드 이해 Q&A, REVIEW: 학습 후기, FREE: 자유) | `QNA` |
| `conceptId` | integer,null(int64) | 선택 | 첨부할 공개 개념 카드 ID. analysisId와 함께 보낼 수 없다 | `12` |
| `content` | string | 필수 | 본문 (앞뒤 공백 제외 1~5000자, 글자 그대로 저장) (길이 0~5000자) | `같은 클래스 안에서 메서드를 호출하면 트랜잭션이 안 걸린다는데<br>왜 그런지 모르겠어요.` |
| `title` | string | 필수 | 제목 (앞뒤 공백 제외 1~100자) (길이 0~100자) | `@Transactional이 안 먹는 이유가 뭔가요?` |

### PostSummaryResponse

게시글 요약 (목록 항목)

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `attachmentType` | string,null: ANALYSIS · CONCEPT | 필수 | 첨부 종류 (볼 수 있는지와 상관없이 알린다). 첨부가 없으면 null | `ANALYSIS` |
| `authorId` | integer,null(int64) | 필수 | 작성자 ID (탈퇴한 회원이면 null) | `7` |
| `authorNickname` | string | 필수 | 작성자 현재 닉네임 (탈퇴한 회원이면 "탈퇴한 사용자") | `코딩초보` |
| `boardType` | string: QNA · REVIEW · FREE | 필수 | 게시판 (QNA: 코드 이해 Q&A, REVIEW: 학습 후기, FREE: 자유) | `QNA` |
| `commentCount` | integer(int64) | 필수 | 댓글 수 | `3` |
| `createdAt` | string(date-time) | 필수 | 작성 시각 | `2026-11-20T14:02:11+09:00` |
| `id` | integer(int64) | 필수 | 글 ID | `42` |
| `title` | string | 필수 | 제목 | `@Transactional이 안 먹는 이유가 뭔가요?` |
| `viewCount` | integer(int32) | 필수 | 조회수 | `42` |

### QuizAnswerRequest

문항 하나의 답

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `questionId` | integer(int64) | 필수 | 문항 ID | `21` |
| `selectedIndex` | integer(int32) | 필수 | 고른 선택지 위치 (0~3) (범위 0~3) | `0` |

### QuizAttemptRequest

풀이 제출

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `answers` | [QuizAnswerRequest](#quizanswerrequest)[] | 필수 | 문항별 답 (문항마다 한 번씩) (항목 1~개) | - |

### QuizAttemptResponse

풀이 채점 결과

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `attemptId` | integer(int64) | 필수 | 풀이 ID | `16` |
| `createdAt` | string(date-time) | 필수 | 제출 시각 | `2026-11-19T17:42:05+09:00` |
| `results` | [QuizResultItem](#quizresultitem)[] | 필수 | 문항별 결과 (no 순서) | - |
| `reviewAdded` | [ReviewAddedItem](#reviewaddeditem)[] | 필수 | 틀린 문항의 공개 개념 중 새로 넣은 복습 계획 (conceptId 오름차순). 없으면 [] | - |
| `score` | integer(int32) | 필수 | 맞힌 문항 수 | `1` |
| `total` | integer(int32) | 필수 | 문항 수 | `2` |

### QuizQuestionResponse

퀴즈 문항 (정답·해설 없음)

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `choices` | string[] | 필수 | 선택지 4개 | `["작성자인지 확인한다","slug를 바꾼다","토큰을 새로 발급한다","글을 지우고 다시 만든다"]` |
| `id` | integer(int64) | 필수 | 문항 ID | `21` |
| `no` | integer(int32) | 필수 | 문항 번호 (1부터) | `1` |
| `question` | string | 필수 | 질문 (줄바꿈 포함 가능) | `ArticleApi의 updateArticle()이 수정 전에 하는 일은 무엇인가요?` |

### QuizRegenerationResponse

퀴즈 재생성 접수

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `analysisId` | integer(int64) | 필수 | 분석 ID | `42` |
| `quizRegenerationsLeft` | integer(int32) | 필수 | 이번 요청을 뺀 남은 재생성 횟수 (범위 0~) | `2` |
| `quizStatus` | string: GENERATING · READY · FAILED | 필수 | 퀴즈 상태 (접수 직후 항상 GENERATING) | `GENERATING` |

### QuizResponse

분석의 퀴즈

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `analysisId` | integer(int64) | 필수 | 분석 ID | `7` |
| `lastAttempt` | [LastAttemptResponse](#lastattemptresponse) | 선택 | 내 가장 최근 풀이. 풀이가 없으면 null | - |
| `questions` | [QuizQuestionResponse](#quizquestionresponse)[] | 필수 | 문항 5~10개 (no 순서) | - |
| `quizId` | integer(int64) | 필수 | 퀴즈 ID | `3` |

### QuizResultItem

문항별 채점 결과

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `answerIndex` | integer(int32) | 필수 | 정답 위치 | `1` |
| `choices` | string[] | 필수 | 선택지 4개 | - |
| `concept` | [ConceptSummaryResponse](#conceptsummaryresponse) | 선택 | 관련 개념 카드. 없거나 공개되지 않은 카드면 null | - |
| `correct` | boolean | 필수 | 맞혔는지 | `false` |
| `explanation` | string | 필수 | 해설 | `jwtService.getSubFromToken()으로 사용자 id를 꺼내 SecurityContextHolder에 넣습니다.` |
| `no` | integer(int32) | 필수 | 문항 번호 | `2` |
| `question` | string | 필수 | 질문 | `JwtTokenFilter가 Authorization 헤더에서 토큰을 꺼낸 뒤 하는 일로 알맞은 것은?` |
| `questionId` | integer(int64) | 필수 | 문항 ID | `22` |
| `selectedIndex` | integer(int32) | 필수 | 내가 고른 위치 | `3` |

### ReadingStep

읽는 순서 항목

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `fileId` | integer(int64) | 필수 | 파일 ID (analysis_files.id) | `101` |
| `path` | string | 필수 | 파일 경로 | `src/main/java/com/example/todo/TodoController.java` |
| `reason` | string | 필수 | 이 순서로 읽는 이유 (200자 이내) | `어떤 API가 있는지 먼저 봅니다.` |

### RecommendationItemResponse

추천 카드

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `concept` | [ConceptSummaryResponse](#conceptsummaryresponse) | 필수 | 카드 요약 | - |
| `reason` | string | 필수 | 추천 이유 한 줄 (100자 이하) | `퀴즈에서 2번 틀렸고 내 분석 코드에도 나온 JWT 인증이에요. 요청마다 사용자를 확인하는 흐름을 다시 보면 좋아요.` |
| `reasonSource` | string: AI · RULE | 필수 | 추천 이유 출처 | `AI` |
| `score` | integer(int32) | 필수 | 추천 점수 (min(오답,3)×3 + 내 코드 2 + 찜한 분야 1 + 수준 적합도) | `10` |
| `signals` | [RecommendationSignals](#recommendationsignals) | 필수 | 점수의 근거 신호 | - |

### RecommendationListResponse

맞춤 추천 목록

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `items` | [RecommendationItemResponse](#recommendationitemresponse)[] | 필수 | 추천 카드 (점수 → 찜 수 → 평점 → id 순, 최대 size개) | - |

### RecommendationSignals

추천 신호

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `inMyCode` | boolean | 필수 | 삭제되지 않은 내 분석에 이 카드가 나왔는지 | `true` |
| `levelFit` | integer(int32): -1 · 0 · 1 | 필수 | 회원 수준과 카드 난이도의 적합도 (같음 1, 한 단계 차이 0, 두 단계 차이 -1) | `1` |
| `wishedCategory` | boolean | 필수 | 내가 찜한 카드 중 같은 카테고리가 있는지 | `false` |
| `wrongCount` | integer(int32) | 필수 | 내 퀴즈 오답 중 이 카드를 다룬 문항 수 (상한 없는 실제 개수. 점수에는 3까지만 넣는다) | `2` |

### ReviewAddedItem

새로 추가한 복습 계획

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `conceptId` | integer(int64) | 필수 | 개념 카드 ID | `57` |
| `planDate` | string(date) | 필수 | 복습 날짜 (제출 다음 날, Asia/Seoul) | `2026-11-20` |

### ReviewPlanCreateRequest

복습 계획 직접 추가

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `conceptId` | integer(int64) | 필수 | 카드 ID | `12` |
| `planDate` | string,null(date) | 선택 | 복습할 날짜 (오늘 ~ 오늘+365일). 생략하면 내일 | `2026-11-21` |

### ReviewPlanItemResponse

복습 계획 항목

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `concept` | [ConceptSummaryResponse](#conceptsummaryresponse) | 필수 | 카드 요약 | - |
| `doneAt` | string,null(date-time) | 필수 | 완료 시각 (DONE일 때만, 아니면 null) | `2026-11-20T10:00:00+09:00` |
| `id` | integer(int64) | 필수 | 계획 ID | `31` |
| `planDate` | string(date) | 필수 | 복습할 날짜 | `2026-11-21` |
| `reason` | string: WRONG · WISH · MANUAL | 필수 | 추가된 이유 (WRONG 퀴즈 오답, WISH 찜, MANUAL 직접 추가) | `WRONG` |
| `status` | string: TODO · DONE | 필수 | 상태 (TODO 할 일, DONE 완료) | `TODO` |

### ReviewPlanListResponse

복습 계획 목록

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `from` | string(date) | 필수 | 기간 시작 (포함) | `2026-11-20` |
| `items` | [ReviewPlanItemResponse](#reviewplanitemresponse)[] | 필수 | 기간 안의 계획 (TODO·DONE, 날짜·id 오름차순) | - |
| `overdue` | [ReviewPlanItemResponse](#reviewplanitemresponse)[] | 필수 | 밀린 복습: 오늘 이전 날짜의 TODO (기간과 무관, 날짜·id 오름차순) | - |
| `to` | string(date) | 필수 | 기간 끝 (포함) | `2026-11-26` |
| `today` | string(date) | 필수 | 오늘 (서비스 시계, Asia/Seoul) | `2026-11-20` |

### ReviewPlanStatusRequest

복습 계획 상태 변경

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `status` | string: TODO · DONE | 필수 | 바꿀 상태 (TODO 되돌림, DONE 완료) | `DONE` |

### ShareTeamAnalysisRequest

팀에 분석 공유 요청

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `analysisId` | integer(int64) | 필수 | 공유할 내 분석 ID (DONE, 퀴즈 있음) (범위 1~) | `42` |

### SharedByResponse

공유한 사람

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `nickname` | string | 필수 | 닉네임 | `vibecoder` |
| `userId` | integer(int64) | 필수 | 회원 ID | `3` |

### SignupRequest

회원가입 요청

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `email` | string(email) | 필수 | 이메일 (앞뒤 공백 제거, 소문자로 저장) (길이 0~255자) | `user@example.com` |
| `level` | string: BEGINNER · JUNIOR · SENIOR | 필수 | 학습 수준 | `BEGINNER` |
| `nickname` | string | 필수 | 닉네임. 2~20자의 한글·영문·숫자·_ | `코딩초보` |
| `password` | string | 필수 | 비밀번호. 8~64자의 ASCII 영문·숫자·특수문자(공백 포함), 영문과 숫자를 각각 1자 이상 | `vibe1234` |

### SignupResponse

회원가입 결과

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `id` | integer(int64) | 필수 | 가입한 회원 ID | `1` |

### StructureEdge

파일 사이 연결 (코드에서 확정한 것만)

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `from` | integer(int64) | 필수 | 부르는 파일 ID | `101` |
| `line` | integer(int32) | 필수 | 근거가 처음 나온 from 파일의 줄 번호 (1부터) | `18` |
| `to` | integer(int64) | 필수 | 불리는 파일 ID | `102` |
| `type` | string: IMPORT · MYBATIS · HTTP | 필수 | 연결 종류 | `IMPORT` |

### StructureNode

선별 파일

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `fileId` | integer(int64) | 필수 | 파일 ID | `101` |
| `layer` | string: controller · service · mapper · vue · js · config · domain · other | 필수 | 레이어 (analysis_files.layer 소문자) | `controller` |
| `name` | string | 필수 | 파일 이름 | `TodoController.java` |
| `path` | string | 필수 | 저장소 기준 경로 | `src/main/java/com/example/todo/TodoController.java` |
| `role` | string,null | 선택 | 한 줄 역할 (F-06). 없으면 null | `할 일 API 요청을 받아 서비스로 넘깁니다` |

### TeamAnalysisResponse

팀 공유 분석

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `analysisId` | integer(int64) | 필수 | 분석 ID | `42` |
| `canUnshare` | boolean | 필수 | 내가 공유한 사람이거나 팀장이면 true | `true` |
| `level` | string: BEGINNER · JUNIOR · SENIOR | 필수 | 설명 수준 | `JUNIOR` |
| `myLastAttempt` | [LastAttemptResponse](#lastattemptresponse) | 선택 | 내 가장 최근 풀이 (안 풀었으면 null) | - |
| `projectName` | string | 필수 | 프로젝트 이름 | `spring-realworld` |
| `questionCount` | integer(int32) | 필수 | 퀴즈 문항 수 | `6` |
| `sharedAt` | string(date-time) | 필수 | 공유 시각 | `2026-11-25T14:20:00+09:00` |
| `sharedBy` | [SharedByResponse](#sharedbyresponse) | 필수 | 공유한 사람 | - |
| `solvedMemberCount` | integer(int32) | 필수 | 지금 멤버 중 한 번 이상 푼 사람 수 | `2` |
| `title` | string | 선택 | 분석 제목 (없으면 null) | `spring-realworld` |

### TeamAnalysisSharedResponse

팀 분석 공유 결과

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `analysisId` | integer(int64) | 필수 | 분석 ID | `42` |
| `sharedAt` | string(date-time) | 필수 | 공유 시각 | `2026-11-25T14:20:00+09:00` |

### TeamChallengeMemberResponse

팀 목표의 멤버별 진행

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `achieved` | boolean | 필수 | 달성 여부 (score × 100 ≥ 목표 정답률 × total) | `false` |
| `attempt` | [LastAttemptResponse](#lastattemptresponse) | 선택 | 기간 안의 가장 최근 풀이 (없으면 null) | - |
| `nickname` | string | 필수 | 닉네임 | `vibecoder` |
| `role` | string: OWNER · MEMBER | 필수 | 역할 | `OWNER` |
| `userId` | integer(int64) | 필수 | 회원 ID | `3` |

### TeamChallengeResponse

팀 목표

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `achieved` | boolean | 필수 | 팀 달성 (지금 멤버 전원 달성) | `false` |
| `achievedMemberCount` | integer(int32) | 필수 | 달성한 멤버 수 | `1` |
| `analysisId` | integer(int64) | 필수 | 분석 ID | `42` |
| `challengeId` | integer(int64) | 필수 | 목표 ID | `7` |
| `createdAt` | string(date-time) | 필수 | 만든 시각 | `2026-11-24T10:00:00+09:00` |
| `endDate` | string(date) | 필수 | 종료일 (Asia/Seoul, 포함) | `2026-11-30` |
| `memberCount` | integer(int32) | 필수 | 지금 멤버 수 | `3` |
| `members` | [TeamChallengeMemberResponse](#teamchallengememberresponse)[] | 필수 | 멤버별 진행 (팀장 먼저, 가입 순) | - |
| `projectName` | string | 필수 | 프로젝트 이름 | `spring-realworld` |
| `solvedMemberCount` | integer(int32) | 필수 | 기간 안에 푼 멤버 수 | `2` |
| `startDate` | string(date) | 필수 | 시작일 (Asia/Seoul, 포함) | `2026-11-24` |
| `status` | string: UPCOMING · ONGOING · ENDED | 필수 | 상태 | `ONGOING` |
| `targetRate` | integer(int32) | 필수 | 목표 정답률 (%) | `80` |
| `title` | string | 선택 | 분석 제목 (없으면 null) | `spring-realworld` |

### TeamCreatedResponse

팀 만들기 응답

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `id` | integer(int64) | 필수 | 팀 ID | `12` |
| `inviteCode` | string | 필수 | 초대 코드 8자 | `K7M2QX9P` |

### TeamDetailResponse

팀 상세

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `createdAt` | string(date-time) | 필수 | 만든 시각 | `2026-11-24T10:00:00+09:00` |
| `description` | string,null | 선택 | 소개 | `F-05 분석 결과를 같이 공부합니다` |
| `id` | integer(int64) | 필수 | 팀 ID | `12` |
| `inviteCode` | string,null | 선택 | 초대 코드. 팀원에게는 null | `K7M2QX9P` |
| `maxMembers` | integer(int32) | 필수 | 팀 최대 인원 | `10` |
| `memberCount` | integer(int32) | 필수 | 멤버 수 (팀장 포함) | `3` |
| `members` | [TeamMemberResponse](#teammemberresponse)[] | 필수 | 멤버 (팀장이 맨 앞, 나머지는 가입 순) | - |
| `myRole` | string: OWNER · MEMBER | 필수 | 요청한 사람의 역할 | `OWNER` |
| `name` | string | 필수 | 팀 이름 | `스프링 스터디` |

### TeamMemberResponse

팀 멤버

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `joinedAt` | string(date-time) | 필수 | 가입 시각 | `2026-11-24T11:30:00+09:00` |
| `level` | string: BEGINNER · JUNIOR · SENIOR | 필수 | 학습 수준 | `BEGINNER` |
| `nickname` | string | 필수 | 닉네임 | `learner` |
| `role` | string: OWNER · MEMBER | 필수 | 역할 | `MEMBER` |
| `userId` | integer(int64) | 필수 | 회원 ID | `7` |

### TeamRequest

팀 만들기·수정 요청

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `description` | string,null | 선택 | 소개 (앞뒤 공백 제외 200자 이하, 없거나 공백뿐이면 null) (길이 0~200자) | `F-05 분석 결과를 같이 공부합니다` |
| `name` | string | 필수 | 팀 이름 (앞뒤 공백 제외 1~30자, 중복 허용) (길이 0~30자) | `스프링 스터디` |

### TeamUnderstandingResponse

팀 이해도

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `analysisId` | integer(int64) | 필수 | 분석 ID | `42` |
| `memberCount` | integer(int32) | 필수 | 지금 멤버 수 | `3` |
| `members` | [MemberUnderstandingResponse](#memberunderstandingresponse)[] | 필수 | 멤버별 결과 (팀장 먼저, 가입 순) | - |
| `modules` | [ModuleUnderstandingResponse](#moduleunderstandingresponse)[] | 필수 | 레이어별 이해도 (controller, service, mapper, vue, js, config, domain, other 순서, 문항이 있는 레이어만) | - |
| `projectName` | string | 필수 | 프로젝트 이름 (제목이 없을 때 대신 보여 준다) | `spring-realworld` |
| `questionCount` | integer(int32) | 필수 | 퀴즈 문항 수 | `4` |
| `sharedBy` | [SharedByResponse](#sharedbyresponse) | 필수 | 공유한 사람 | - |
| `solvedMemberCount` | integer(int32) | 필수 | 푼 멤버 수 | `2` |
| `title` | string | 선택 | 분석 제목 (없으면 null) | `spring-realworld` |

### UnresolvedLink

확인하지 못한 연결

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `detail` | string | 필수 | FQN·경로·요청 원문 등 근거 | `com.example.todo.TodoValidator` |
| `fileId` | integer(int64) | 필수 | 연결이 나온 파일 ID | `102` |
| `kind` | string: IMPORT · PARSE · HTTP_CALL | 필수 | 종류 | `IMPORT` |
| `line` | integer(int32) | 필수 | 줄 번호 (1부터) | `5` |
| `reason` | string: NOT_SELECTED · PARSE_ERROR · URL_UNKNOWN · BASE_URL_UNKNOWN · NO_MATCHING_API · AMBIGUOUS | 필수 | 사유 | `NOT_SELECTED` |

### UpdateAnalysisRequest

분석 제목·메모 수정 요청

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `memo` | string,null | 선택 | 메모 (1000자 이하). 비우면 null (길이 0~1000자) | `JWT 필터 순서 다시 보기` |
| `title` | string,null | 선택 | 제목 (100자 이하). 비우면 null (길이 0~100자) | `할 일 앱 백엔드` |

### UpdateProfileRequest

내 프로필 수정 요청. 보낸 필드만 바뀐다

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `bio` | string,null | 선택 | 소개 (300자 이하). 빈 문자열이면 지운다 (길이 0~300자) | `AI로 만든 Spring 프로젝트를 공부하고 있어요.` |
| `level` | string,null: BEGINNER · JUNIOR · SENIOR | 선택 | 학습 수준 | `JUNIOR` |
| `nickname` | string,null | 선택 | 닉네임. 2~20자의 한글·영문·숫자·_ | `주니어개발자` |

### UpdateVisibilityRequest

분석 공개 설정 요청

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `isPublic` | boolean | 필수 | 공개 여부 | `true` |

### UserProfileResponse

공개 프로필

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `bio` | string,null | 선택 | 소개. 없으면 null | `AI로 만든 Spring 프로젝트를 공부하고 있어요.` |
| `createdAt` | string(date-time) | 필수 | 가입 시각 | `2026-11-18T10:30:00+09:00` |
| `id` | integer(int64) | 필수 | 회원 ID | `1` |
| `level` | string: BEGINNER · JUNIOR · SENIOR | 필수 | 학습 수준 | `BEGINNER` |
| `nickname` | string | 필수 | 닉네임 | `코딩초보` |

### UserStatsResponse

학습 통계

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `analysisCount` | integer(int64) | 필수 | 완료된 분석 수 (삭제한 분석 제외) | `3` |
| `learnedConceptCount` | integer(int64) | 필수 | 학습한 카드 수: 퀴즈에서 맞힌 카드와 복습을 완료한 카드 (중복 제외) | `12` |
| `quizAccuracy` | integer,null(int32) | 선택 | 퀴즈 정답률(%, 정수). 푼 퀴즈가 없으면 null | `75` |

### VisibilityResponse

분석 공개 설정 결과

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `id` | integer(int64) | 필수 | 분석 ID | `42` |
| `isPublic` | boolean | 필수 | 공개 여부 | `true` |

### WithdrawRequest

회원 탈퇴 요청

| 이름 | 타입 | 필수 | 설명 | 예 |
|---|---|---|---|---|
| `password` | string | 필수 | 현재 비밀번호 (길이 1~자) | `vibe1234` |
