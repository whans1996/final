# F-24 학습 챌린지 관리 (관리자 등록·조회·수정·삭제, 대표 이미지)

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-24 |
| 우선순위 | P0 |
| 영역 오너 | A 김동환 (AI·분석·학습 루프) |
| 관련 필수 요구사항 | ⑫ 추가(계획/챌린지) · 명세서 "추가 · 챌린지 정보 CRD(기간·설명·이미지)" |
| 의존 spec | F-00b 공통 기반(관리자 레이아웃 `AdminLayout`, `PageQuery`·`PageResponse`, `ClockConfig`), F-02 로그인(ADMIN 역할). F-25 참여·달성도가 이 spec의 `challenges`·`ChallengeStatus`를 쓴다 |
| 관련 이슈 | 없음 |
| 승인 | 김동환 2026-11-24 |

## 1. 목적

관리자가 "12월 한 달 퀴즈 10회", "이번 주 개념 복습 20회" 같은 학습 챌린지를 등록하고 관리하게 한다. 회원은 F-25에서 이 챌린지에 참여하고, 기간 안의 학습 활동으로 달성도를 채운다. 이 spec은 **관리자 쪽 CRUD와 대표 이미지 업로드·제공**만 다룬다 (UC51, SC-21).

**규칙 (11-24 결정)**:
- 챌린지 = 제목(1~100자) · 설명(선택, 2,000자 이하) · 기간(시작일~종료일, Asia/Seoul 날짜, 양 끝 포함) · 목표 유형(`REVIEW_COUNT` 개념 복습 n회 / `QUIZ_COUNT` 퀴즈 n회 / `ANALYSIS_COUNT` 분석 n회) · 목표값(1~1,000) · 대표 이미지(선택).
- 기간 검증은 등록·수정 모두 같다. 종료일 ≥ 시작일, 기간은 양 끝을 포함해 **90일 이하**다. 등록할 때와 종료일을 바꿀 때는 **종료일 ≥ 오늘**이어야 한다. 시작일은 과거여도 된다 (시연 때 이미 진행 중인 챌린지를 만들 수 있다).
- 상태는 저장하지 않고 오늘(Asia/Seoul)과 비교해 계산한다. 오늘 < 시작일이면 `UPCOMING`(예정), 시작일 ≤ 오늘 ≤ 종료일이면 `ONGOING`(진행 중), 오늘 > 종료일이면 `ENDED`(종료)다. F-20c 팀 목표와 같은 기준이다.
- **시작한 챌린지(진행 중·종료)는 목표 유형·목표값·시작일을 바꿀 수 없다.** 참여자의 달성도 기준이 중간에 바뀌지 않게 하기 위해서다. 제목·설명·이미지·종료일은 바꿀 수 있다.
- **삭제는 언제든 실제 삭제한다.** 참여 기록(`challenge_participants`)은 FK CASCADE로 함께 지워지고, 피드의 `CHALLENGE_*` 활동은 F-04 쿼리가 이미 거른다(`LEFT JOIN challenges`).
- 이미지는 jpg·png·webp, 2MB 이하이고 확장자·MIME·매직 바이트가 모두 맞아야 한다. 파일은 `{app.upload-dir}/challenges/{UUID}.{ext}`에 저장하고 `/uploads/challenges/{UUID}.{ext}`로 누구나 받을 수 있다. 교체·제거·챌린지 삭제 때는 이전 파일을 **트랜잭션 커밋 뒤에** 지운다. 실패해도 응답은 성공이고 로그만 남긴다.
- **정적 제공 범위는 `/uploads/challenges/**`뿐이다.** `{app.upload-dir}`에는 분석 ZIP 임시 파일(`analysis-tmp/`)도 있으므로 폴더 전체를 열지 않는다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_관리자가_이미지와_함께_챌린지를_등록하면_201을_받는다()`
> BE 테스트는 다른 테스트 데이터에 기대지 않는다. 관리자·회원·챌린지·참여 행을 테스트마다 직접 넣는다. 오늘은 `@TestBean Clock`으로 **2026-11-24(Asia/Seoul)**에 고정한다.
> 판단 순서: **401 → 403(`/api/admin/**` URL 규칙) → 404 `CHALLENGE_404` → 400 검증 → 409 `CHALLENGE_001`**.

### 백엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 관리자 M이 | `POST /api/admin/challenges`(multipart)로 `title` "12월 퀴즈 챌린지", `startDate` 2026-12-01, `endDate` 2026-12-31, `goalType` `QUIZ_COUNT`, `goalValue` 10, `image`(png)를 보내면 | 201과 `{id}`를 받는다. 행의 `created_by` = M이다. 파일이 `{upload-dir}/challenges/{UUID}.png`에 생기고, 상세의 `imageUrl`은 `/uploads/challenges/{UUID}.png`다. 그 주소를 **비로그인**으로 GET하면 200과 같은 바이트를 받는다. 이미지 없이 보내면 201이고 `imageUrl`은 `null`이다 |
| AC2 | 관리자가 | 본문이 잘못되면 | 400 `COMMON_400`이고 `details`에 필드별 사유가 있다. `title` 없음·공백·101자(앞뒤 공백 제거 후 1~100), `description` 2,001자, `startDate`·`endDate` 없음·형식 오류, `endDate` < `startDate`, `endDate` < 오늘(11-23), 기간 91일(12-01~03-01), `goalType` 없음·허용값 밖, `goalValue` 없음·0·1,001. 경계값 제목 1·100자, 설명 2,000자, 기간 90일, `goalValue` 1·1,000, `endDate` = 오늘, 시작일 과거(11-20)는 통과한다 |
| AC3 | 관리자가 | 이미지가 규칙에 맞지 않으면 | 400 `COMMON_400`, `details.image`에 사유가 있다. gif(`.gif`, GIF 바이트), 확장자와 내용 불일치(`.png`인데 JPEG 바이트), MIME 불일치(`.png`·PNG 바이트인데 `text/plain`), 빈 파일, 2MB + 1바이트가 해당한다. **정확히 2MB(2,097,152바이트)는 통과**한다. 실패하면 행도 파일도 남지 않는다. 10MB를 넘는 요청은 전역 한도로 413 `COMMON_413`이다 |
| AC4 | 챌린지 C1(11-01~11-10 종료), C2(11-20~11-30 진행 중, 참여 2명), C3(11-24~11-24 진행 중), C4(12-01~12-31 예정)가 있을 때 | `GET /api/admin/challenges`를 요청하면 | 200 `PageResponse`, `items`는 `[C4, C3, C2, C1]`이다 (`start_date DESC, id DESC`). 항목마다 `status`(`ENDED`·`ONGOING`·`UPCOMING`)와 `participantCount`(C2 = 2, 나머지 0)가 있다. `?status=ONGOING`이면 `[C3, C2]`이고 `total` 2다. `?status=DONE`이나 잘못된 `page`·`size`는 400이다. `size`는 기본 20, 최대 100이다 |
| AC5 | 챌린지 C가 있을 때 | `GET /api/admin/challenges/{C}` | 200과 3절 `AdminChallengeResponse`(설명, `createdAt`, `updatedAt`, `status`, `participantCount` 포함)를 받는다. 없는 id → 404 `CHALLENGE_404` |
| AC6 | 예정 챌린지 C(이미지 P)가 있을 때 | `PUT /api/admin/challenges/{C}`(multipart) | 모든 필드를 바꿀 수 있고 200과 상세를 받는다. 새 `image`를 보내면 `imageUrl`이 바뀌고 P 파일이 지워진다. `image` 없이 보내면 기존 이미지를 유지한다. `removeImage=true`면 `imageUrl`이 `null`이 되고 P 파일이 지워진다. `image`와 `removeImage=true`를 함께 보내면 400(`details.image`)이다. 검증은 AC2·AC3과 같다. 없는 id → 404 |
| AC7 | 진행 중 챌린지 C(11-20~11-30, `QUIZ_COUNT` 10)와 종료 챌린지 E(11-01~11-10)가 있을 때 | C를 수정하면 | `goalType`·`goalValue`·`startDate` 중 하나라도 **다른 값**이면 409 `CHALLENGE_001`이고 아무것도 바뀌지 않는다. 같은 값과 함께 제목·설명·이미지·종료일(→ 12-10)을 바꾸면 200이다. 종료일을 오늘보다 앞(11-23)으로 바꾸면 400 `details.endDate`다. E는 종료일을 그대로 두면 제목·설명·이미지를 고칠 수 있다(200). E의 종료일을 12-05로 바꾸면 다시 진행 중이 되고 200이다 (단, 기간 90일 검증은 그대로 적용) |
| AC8 | 참여자 2명과 이미지 P가 있는 챌린지 C가 있을 때 | `DELETE /api/admin/challenges/{C}` | 204. `challenges`·`challenge_participants` 행이 사라지고 P 파일도 지워진다. 다시 GET하면 404 `CHALLENGE_404`이다. 없는 id를 DELETE하면 404다 |
| AC9 | 비로그인 / 회원(USER) | 위 5개 엔드포인트를 요청하면 | 401 `AUTH_401` / 403 `AUTH_403`. 행·파일이 생기거나 바뀌지 않는다 |
| AC10 | `{upload-dir}/analysis-tmp/1.zip`과 `{upload-dir}/secret.txt`가 있을 때 | 비로그인이 `GET /uploads/analysis-tmp/1.zip`, `/uploads/secret.txt`, `/uploads/challenges/..%2fsecret.txt`, `/uploads/challenges/%2e%2e/secret.txt`를 요청하면 | 모두 파일 내용을 받지 못한다 (404 또는 400). `/uploads/challenges/없는파일.png`는 404다 |

### 프론트엔드 (SC-21, 관리자 레이아웃의 자식 화면)

| ID | Given | When | Then |
|---|---|---|---|
| AC11 | 관리자가 | `/admin/challenges?status=&page=`를 열면 | 상태 탭(전체·예정·진행 중·종료, 쿼리에 반영)과 표(썸네일(이미지가 없으면 목표 유형별 기본 그림) · 제목 · 기간 "2026.12.01 ~ 12.31" · 목표 "퀴즈 10회" · 상태 배지 · 참여자 수 · 수정·삭제 버튼)가 보인다. 목록이 비면 `EmptyState` "등록된 챌린지가 없습니다" + 등록 버튼이 보이고, 페이지는 `PageNav`로 넘긴다. 사이드 메뉴에 "챌린지"(`meta.adminNav` order 20)가 있다. 로딩·에러 상태가 있다 |
| AC12 | 목록에서 | 삭제를 누르면 | 확인 대화상자(브라우저 `confirm` 금지, 화면 안 모달)에 "참여자 n명의 참여 기록도 함께 지워집니다"가 나온다. 확인하면 `DELETE` 후 목록을 다시 불러온다. 실패하면 목록 위에 메시지를 띄운다 |
| AC13 | 관리자가 | `/admin/challenges/new` 또는 `/admin/challenges/:id/edit`에서 저장하면 | 클라이언트 검증(필수·길이·기간 90일·종료일 ≥ 오늘·목표값 1~1,000·이미지 형식·2MB)을 통과해야 요청을 보낸다. 이미지는 미리보기·교체·제거를 할 수 있다. 서버 400 `details`는 해당 필드 아래에 표시한다. 시작한 챌린지의 수정 화면은 목표 유형·목표값·시작일 입력이 비활성이고 "이미 시작한 챌린지는 목표와 시작일을 바꿀 수 없습니다"를 보여 준다(409를 받으면 같은 문구). 저장에 성공하면 목록으로 돌아간다. 없는 id는 "챌린지를 찾을 수 없습니다"를 보여 준다 |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: `관리자` (F-13과 같다). 클래스 단위 401·403 `@ApiResponse`.
> 날짜는 `yyyy-MM-dd`, 시각은 F-00b 공통 형식이다. `status` 값: `UPCOMING`, `ONGOING`, `ENDED` (`ChallengeStatus`). `goalType` 값: `REVIEW_COUNT`, `QUIZ_COUNT`, `ANALYSIS_COUNT` (`ChallengeGoalType`).

### `GET /api/admin/challenges?status=&page=&size=`

- 권한: ADMIN
- 응답 `200`

```json
{
  "items": [
    {
      "id": 7,
      "title": "12월 퀴즈 챌린지",
      "imageUrl": "/uploads/challenges/3f2b9c1e-8a4d-4f7e-9b1a-2c5d6e7f8a90.png",
      "startDate": "2026-12-01",
      "endDate": "2026-12-31",
      "goalType": "QUIZ_COUNT",
      "goalValue": 10,
      "status": "UPCOMING",
      "participantCount": 0
    }
  ],
  "page": 1,
  "size": 20,
  "total": 1
}
```

### `GET /api/admin/challenges/{id}`

- 권한: ADMIN
- 응답 `200` `AdminChallengeResponse`

```json
{
  "id": 7,
  "title": "12월 퀴즈 챌린지",
  "description": "분석한 프로젝트의 퀴즈를 12월 안에 10번 풀어 보세요.",
  "imageUrl": "/uploads/challenges/3f2b9c1e-8a4d-4f7e-9b1a-2c5d6e7f8a90.png",
  "startDate": "2026-12-01",
  "endDate": "2026-12-31",
  "goalType": "QUIZ_COUNT",
  "goalValue": 10,
  "status": "UPCOMING",
  "participantCount": 0,
  "createdAt": "2026-11-24T08:50:00+09:00",
  "updatedAt": "2026-11-24T08:50:00+09:00"
}
```

### `POST /api/admin/challenges` (multipart/form-data)

- 권한: ADMIN
- 요청 파트(form 필드):

| 이름 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `title` | text | ✅ | 앞뒤 공백 제거 후 1~100자 |
| `description` | text | | 2,000자 이하. 빈 문자열은 `null`로 저장 |
| `startDate` | text `yyyy-MM-dd` | ✅ | 과거 허용 |
| `endDate` | text `yyyy-MM-dd` | ✅ | ≥ `startDate`, ≥ 오늘, 기간(양 끝 포함) ≤ 90일 |
| `goalType` | text | ✅ | `REVIEW_COUNT`·`QUIZ_COUNT`·`ANALYSIS_COUNT` |
| `goalValue` | text(정수) | ✅ | 1~1,000 |
| `image` | file | | jpg(`.jpg`·`.jpeg`, `image/jpeg`, `FF D8 FF`) · png(`.png`, `image/png`, `89 50 4E 47 0D 0A 1A 0A`) · webp(`.webp`, `image/webp`, `RIFF....WEBP`), 1바이트~2MB |

- 응답 `201`: `{ "id": 7 }`

### `PUT /api/admin/challenges/{id}` (multipart/form-data)

- 권한: ADMIN
- 요청: POST와 같은 필드(전체 교체)에 `removeImage`(text `true`/`false`, 기본 `false`)를 더한다. `image`가 없고 `removeImage`가 `false`면 기존 이미지를 유지한다. `endDate ≥ 오늘`은 종료일이 **바뀔 때만** 검사한다.
- 응답 `200`: `AdminChallengeResponse`

### `DELETE /api/admin/challenges/{id}`

- 권한: ADMIN
- 응답 `204`

### `GET /uploads/challenges/{fileName}`

- 권한: 공개 (정적 리소스, Swagger 문서 대상 아님). 다른 `/uploads/**` 경로는 제공하지 않는다.

### 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | 검증 실패 (`details`에 필드별 사유. 이미지 문제는 `details.image`), 잘못된 `status`·`page`·`size` |
| 401 | AUTH_401 | 토큰 없음·만료 |
| 403 | AUTH_403 | ADMIN이 아님 |
| 404 | CHALLENGE_404 | 없는 챌린지 (조회·수정·삭제) — **새 코드** "챌린지를 찾을 수 없습니다." |
| 409 | CHALLENGE_001 | 시작한 챌린지의 목표 유형·목표값·시작일 변경 — **새 코드** "이미 시작한 챌린지는 목표와 시작일을 바꿀 수 없습니다." |
| 413 | COMMON_413 | multipart 전역 한도(파일 10MB·요청 12MB) 초과 |

> `docs/conventions.md` 3절 `CHALLENGE` 행에 두 코드를 적는다.

## 4. DB 변경

- Flyway: **없음** (`challenges`·`challenge_participants`는 `V1__init.sql`에 있다. `title VARCHAR(100)`, `description TEXT`, `image_url VARCHAR(500)`, `CHECK (end_date >= start_date)`, `CHECK (goal_value > 0)`, participants FK `ON DELETE CASCADE`)
- 사용 테이블: `challenges`(읽기·쓰기), `challenge_participants`(수 세기, CASCADE 삭제)
- `image_url`에는 공개 경로 `/uploads/challenges/{UUID}.{ext}`를 저장한다. 파일 삭제 때는 이 경로에서 파일 이름만 꺼내 `{upload-dir}/challenges/` 아래로 다시 정규화해 확인한다.

## 5. 구현 메모

### 백엔드 (`com.vibenote.challenge`)

| 항목 | 내용 |
|---|---|
| 공용 (F-25 재사용) | `ChallengeGoalType` enum, `ChallengeStatus` enum + `of(startDate, endDate, today)`, `ChallengeMapper`(+ `mapper/challenge/ChallengeMapper.xml`), `Challenge` 도메인 |
| 관리자 | `challenge.admin.AdminChallengeController`(`/api/admin/challenges`, Tag `관리자`), `AdminChallengeService`, `dto/AdminChallengeRequest`(`@ModelAttribute` + Bean Validation, 교차 검증은 서비스), `AdminChallengeSearchRequest`, `AdminChallengeSummaryResponse`, `AdminChallengeResponse`, `AdminChallengeCreateResponse`. 목록은 `PageQuery`·`PageResponse.of` 재사용 |
| 이미지 | `ChallengeImageStorage`: `@Value("${app.upload-dir}")`로 `{dir}/challenges`를 잡는다(`AnalysisTempFiles` 방식). 확장자·MIME·매직 바이트를 검사한다(`CreateAnalysisRequest` ZIP 검사 방식). 파일 이름은 `UUID.randomUUID()` + 정규화한 확장자(`.jpeg` → `.jpg`)다. 크기 상한 2MB는 직접 센다(전역 multipart 10MB와 별개). 저장 실패는 `COMMON_500` |
| 파일 정리 | 새 파일은 DB 저장 전에 쓰고, 롤백되면 새 파일을 지운다. 이전 파일은 커밋 뒤 지운다(`TransactionSynchronization.afterCompletion`). 삭제에 실패하면 `warn` 로그만 남긴다 |
| 정적 제공 | `common.config.WebConfig`(`WebMvcConfigurer.addResourceHandlers`)에서 `/uploads/challenges/**` → `file:{upload-dir}/challenges/`만 매핑한다. `SecurityConfig.PUBLIC_PATHS`의 `/uploads/**`는 그대로 둔다(핸들러가 없는 경로는 404) |
| 에러 코드 | `ErrorCode`의 `// ── CHALLENGE (A F-24·F-25) ──` 블록에 `CHALLENGE_404`, `CHALLENGE_001` |
| 시각 | `ClockConfig.ZONE`(Asia/Seoul)과 `Clock` 빈으로 오늘을 계산한다 |

### 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `routes/challenge.js` `adminChildren`: `challenges`(`admin-challenges`, `meta.adminNav {label:'챌린지', order:20}`) → `views/admin/challenge/AdminChallengeListView.vue`, `challenges/new`(`admin-challenge-new`)·`challenges/:id(\\d+)/edit`(`admin-challenge-edit`) → `AdminChallengeEditView.vue` |
| 컴포넌트 | `components/challenge/admin/AdminChallengeTable.vue`, `AdminChallengeForm.vue`(이미지 미리보기 `URL.createObjectURL`, 해제 처리), `AdminChallengeStatusTabs.vue`, `labels.js`(상태·목표 유형 문구, 기본 그림), 공용 `PageNav`·`EmptyState`. F-13 `AdminConcept*` 구조를 따른다 |
| store | 없음 (화면 로컬 상태) |
| API 함수 | `src/api/adminChallenges.js`: `fetchAdminChallenges(params)`, `fetchAdminChallenge(id)`, `createAdminChallenge(form)`, `updateAdminChallenge(id, form)`, `deleteAdminChallenge(id)`. `FormData`로 보내고 Content-Type은 axios에 맡긴다(`analyses.js` `createAnalysis`와 같다) |
| 상태별 UI | 로딩 / 빈 목록 / 에러 메시지 / 저장 중 버튼 비활성 / 시작한 챌린지 잠금 안내 |

## 6. LLM (AI 기능만)

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1~AC9 | `challenge/admin/AdminChallengeApiTest` (`multipart(...)`, PUT은 `multipart(HttpMethod.PUT, url)`, 파일은 `build/test-uploads`에서 확인) | MockMvc 통합 |
| AC2·AC7 교차 검증 | `challenge/admin/AdminChallengeServiceTest` 또는 `dto/AdminChallengeRequestTest` | 단위 |
| AC3 | `challenge/ChallengeImageStorageTest` (매직 바이트·경계 2MB·확장자 정규화·UUID 파일명) | 단위 |
| AC4 상태 계산 | `challenge/ChallengeStatusTest` (시작일 = 오늘, 종료일 = 오늘 경계) | 단위 |
| AC10 | `common/config/UploadResourceTest` | MockMvc 통합 |
| AC11·AC12 | `views/admin/challenge/__tests__/AdminChallengeListView.spec.js`, `layouts/__tests__/navItems.spec.js` | Vitest |
| AC13 | `components/challenge/admin/__tests__/AdminChallengeForm.spec.js`, `AdminChallengeEditView.spec.js` | Vitest |
| API 모듈 | `api/__tests__/adminChallenges.spec.js` (FormData 필드) | Vitest |

검증: `node scripts/verify.mjs all`, contract-checker(spec ↔ openapi.json ↔ `api/adminChallenges.js`), ui-verifier(데스크톱·모바일), 로그인이 필요한 SC-21은 연결된 Chrome으로 직접 확인하고 스크린샷을 `docs/산출물/screens/SC-21-*.png`로 남긴다.

## 8. 범위 밖

- 회원용 챌린지 목록·상세(SC-12·13), 참여·취소, 달성도 집계, `CHALLENGE_JOIN`·`CHALLENGE_COMPLETE` 활동 기록, 대시보드·프로필 챌린지 절 → **F-25**
- 시연용 챌린지 시드 데이터, 이미지 리사이즈·썸네일 생성, 외부 스토리지(S3 등), 상태별 챌린지 수 API, 챌린지 복제
- 팀 기간 목표(`team_challenges`) → F-20c(B)

## 9. 결정 필요

- 없음 (11-24 사용자 결정: 실제 삭제 · 시작 후 목표·시작일 잠금 · 종료일 ≥ 오늘·90일 이하·시작일 과거 허용 · 이미지 선택)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-24-challenge-admin`
- [x] Flyway 마이그레이션 (없음 확인)
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] BE 구현 (green)
- [x] FE 구현 (green)
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [x] `docs/conventions.md` 3절 CHALLENGE 에러 코드
- [x] contract-checker 통과
- [x] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [x] 산출물 반영 (화면 설계서 SC-21 절·스크린샷, 유스케이스 UC51 대안 흐름)
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
