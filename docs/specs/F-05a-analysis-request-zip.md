# F-05a 분석 요청 (ZIP 업로드 · 수집·필터·선별 파이프라인)

| 항목 | 값 |
|---|---|
| 상태 | DONE (PR #14 머지, 2026-11-18) <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-05a (F-05를 a·b로 분할. F-05b = 공개 저장소 URL 수집) |
| 우선순위 | P0 |
| 영역 오너 | 팀원 A (AI·분석) |
| 관련 필수 요구사항 | ⑬ AI 활용 분석의 입력. 명세서 "필수 · 핵심 데이터"의 작성(F1301)·목록 조회(F1302), NF-03·NF-05·NF-07 |
| 의존 spec | F-01 회원가입, F-02 로그인·JWT (DONE). F-06이 이 파이프라인 뒤에 EXPLAINING 이후 단계를 붙인다 |
| 승인 | whans1996 2026-11-18 |

## 1. 목적

회원이 AI로 만든 프로젝트를 **ZIP으로 올리면** 서버가 1초 안에 접수(202)하고, 백그라운드에서 코드를 검사해 **지원 스택의 주요 파일만 골라 비밀 정보를 지운 뒤 저장**한다.
사용자는 진행 상태를 폴링 화면에서 보고, 내 분석 목록에서 이력을 확인한다. 이 spec은 LLM을 호출하지 않는다. 설명(F-06)·개념(F-08)·퀴즈(F-14)는 여기서 저장한 `analysis_files`를 입력으로 쓴다.
URL 입력은 F-05b에서 같은 엔드포인트와 같은 파이프라인에 "수집기" 하나를 더하는 방식으로 붙인다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_ZIP을_올리면_202와_analysisId를_받는다()`

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 로그인한 회원이 (수준 JUNIOR) | `POST /api/analyses`에 `file`(올바른 .zip), `consent=true`를 보내면 (`level` 생략) | 202와 `{analysisId, status:"FETCHING"}`을 받는다. `analyses` 행: user_id=본인, source_type=`ZIP`, source_url NULL, project_name=`title`=업로드 파일명에서 `.zip`을 뺀 이름, level=`JUNIOR`(내 수준), status=`FETCHING`, progress=0, is_public=false. 임시 파일 `{app.upload-dir}/analysis-tmp/{id}.zip`이 생기고, 커밋 뒤 파이프라인 실행이 1회 요청된다. `level=SENIOR`를 보내면 그 값이 저장된다 |
| AC2 | 로그인한 회원이 | 잘못된 요청을 보내면 | 400 `COMMON_400`, `details`에 필드별 사유. `file` 없음·빈 파일 → `details.file`. 확장자가 `.zip`이 아니거나 첫 4바이트가 ZIP 시그니처(`PK\x03\x04`)가 아님 → `details.file`. `consent`가 없거나 `true`가 아님 → `details.consent`. 허용되지 않은 `level` → `details.level`. 10MB 초과 파일 → 413 `COMMON_413` (기존 multipart 처리). 어느 경우든 `analyses` 행과 임시 파일이 생기지 않는다 |
| AC3 | 토큰이 없는 사용자가 | `POST /api/analyses`, `GET /api/analyses`, `GET /api/analyses/{id}/status`를 호출하면 | 401 `AUTH_401` |
| AC4 | 오늘(Asia/Seoul 0시 이후) 만든 분석 중 `FAILED`가 아닌 것이 5개인 회원이 (soft delete된 분석도 셈) | 6번째로 요청하면 | 429 `ANALYSIS_429`. 행이 생기지 않는다. 오늘 분석 5개 중 1개가 `FAILED`이거나, 5개 중 1개가 어제 23:59에 만들어졌으면 6번째는 202다. 한도는 설정값 `analysis.daily-limit`(기본 5) |
| AC5 | Spring 프로젝트 ZIP(최상위 폴더 1개, controller·service·mapper java·mapper xml·vue·`application.yml`, `node_modules/`·`build/` 폴더, `.env`, `id_rsa`, `logo.png` 포함)으로 만든 분석이 | 파이프라인이 실행되면 | status가 FETCHING → FILTERING → SELECTING → `DONE`, progress 100, error_code NULL. `analysis_files`에 선별 파일이 저장된다: path는 **공통 최상위 폴더를 뗀** 상대 경로(`src/main/java/.../UserController.java`), layer는 5.2절 규칙의 값. `node_modules/`·`build/`·`.env`·`id_rsa`·`logo.png`는 저장되지 않는다. `activities`에 (user, `ANALYSIS`, analysisId) 1행이 생긴다 |
| AC6 | 선별될 파일 내용에 키가 들어 있을 때 (`AIza`+35자, `sk-`+20자 이상, `ghp_`/`github_pat_` 토큰, `AKIA`+16자, `-----BEGIN ... PRIVATE KEY-----` 블록, `application.yml`의 `password: hunter2`·`jwt.secret=abc`·`api-key: xyz`) | 파이프라인이 끝나면 | 저장된 `content`에서 그 값이 모두 `[REDACTED]`로 바뀌어 있고 원래 문자열은 어디에도 없다. `${DB_PASSWORD}`처럼 기본값 없는 환경변수 참조는 그대로 두고, `${DB_PASSWORD:hunter2}`는 기본값만 `[REDACTED]`로 바꾼다. 나머지 줄은 바뀌지 않는다 |
| AC7 | 지원 파일이 많은 ZIP일 때 | 파이프라인이 실행되면 | 필터 후 지원 파일이 201개 이상이거나 읽은 지원 파일 합계가 10MB를 넘으면 status `FAILED`, error_code `ANALYSIS_413`, error_message는 5.3절 문구. 200개 정확히면 `DONE`. 지원 파일이 30개를 넘으면 5.2절 우선순위로 30개(`analysis.max-selected-files`)만 저장한다. 1MB를 넘는 단일 파일은 읽지 않고 건너뛴다(실패 아님) ⚠️ **F-26a가 대체** (#155): 지원 파일 수·합계로 실패하지 않고 인덱스 상한 2,000개·30MB를 넘는 파일은 버리고 계속한다 (`dropped_files`). |
| AC8 | `.java`·`.js`·`.mjs`·`.vue` 파일이 하나도 없는 ZIP(예: Python만, yml만)일 때 | 파이프라인이 실행되면 | `FAILED`, `ANALYSIS_415`. `analysis_files`는 비어 있다 |
| AC9 | 위험하거나 깨진 ZIP일 때 (`../../evil.java`·`/etc/passwd`·`C:\x.java` 같은 루트 밖·절대 경로 엔트리, 또는 손상된 아카이브) | 파이프라인이 실행되면 | `FAILED`, `ANALYSIS_400`. 디스크의 `analysis-tmp` 밖에 어떤 파일도 생기지 않는다 (압축을 디스크에 풀지 않는다) |
| AC10 | 파이프라인이 `DONE`이든 `FAILED`든 (예기치 못한 예외 포함) | 끝나면 | 임시 ZIP `{id}.zip`이 삭제되어 있다. 예기치 못한 예외는 `FAILED`, `ANALYSIS_500`이고 그 전 단계의 progress는 그대로 남는다 |
| AC11 | 서버가 꺼질 때 진행 중(FETCHING·FILTERING·SELECTING·EXPLAINING·EXTRACTING·QUIZ)이던 분석이 있으면 | 애플리케이션이 시작되면 | 그 분석들은 `FAILED`, `ANALYSIS_500`, 재시작 문구가 된다. `DONE`·`FAILED`·soft delete된 행은 그대로다. `analysis-tmp`에 남은 파일은 지워진다 |
| AC12 | 내 분석 3개(그중 1개 soft delete)와 다른 회원의 분석 2개가 있을 때 | `GET /api/analyses?page=1&size=20` 하면 | 200과 페이지 응답. 내 미삭제 분석 2개만 `created_at` 최신순(같으면 id 내림차순)으로 온다. 항목 필드는 3절. `size`가 1~100 밖이거나 `page<1`이면 400 `COMMON_400` (`PageQuery` 규칙) |
| AC13 | 로그인한 회원이 | `GET /api/analyses/{id}/status` 하면 | 내 분석이면 200 `{analysisId, status, progress, errorCode, errorMessage, updatedAt}` (성공 중이면 errorCode·errorMessage `null`). 없는 id, **다른 회원의 분석**, soft delete된 분석이면 404 `ANALYSIS_404` |
| AC14 | 로그인한 회원이 | `/analyses/new` 화면(SC-05)에서 요청하면 | 수준 라디오는 내 수준으로 선택되어 있다. ZIP 파일 선택과 "Gemini 전송 고지" 동의 체크가 모두 있어야 "분석 요청" 버튼이 활성화된다. 10MB 넘는 파일을 고르면 업로드 전에 필드 아래 안내를 보여 주고 버튼을 막는다. 202면 `/analyses/{analysisId}`로 이동한다. 429·413·400 응답의 `message`(`details`는 해당 필드 아래)를 보여 준다. 요청 중에는 버튼이 비활성화된다 |
| AC15 | 로그인한 회원이 | `/analyses/:id` 화면(SC-07 진행 상태 부분)을 열면 | 상태를 바로 1회, 이후 2초마다 조회하며 단계 이름(5.1절 한국어 라벨)과 진행률 막대를 보여 준다. `DONE`이면 "분석이 끝났습니다" 안내, `FAILED`면 `errorMessage`와 "다시 요청하기"(`/analyses/new`) 링크를 보여 주고 둘 다 **폴링을 멈춘다**. 화면을 떠나도 폴링이 멈춘다. 404면 "분석을 찾을 수 없습니다."를 보여 주고 멈춘다 |
| AC16 | 로그인한 회원이 | `/analyses` 화면(SC-06 목록 부분)을 열면 | 내 분석을 카드로 보여 준다(제목, 수준, 상태 배지, 진행 중이면 진행률, 요청일). 카드를 누르면 `/analyses/{id}`로 간다. 목록이 비면 "아직 분석이 없습니다"와 "첫 분석 요청하기"(`/analyses/new`) 버튼을 보여 준다. 20개 넘으면 이전·다음 페이지로 이동한다. 로그인 헤더에 "분석 요청"·"내 분석" 링크가 있다 |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: `분석`.

### `POST /api/analyses`

- 권한: 로그인 필요
- 요청 `multipart/form-data`

| 파트 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `file` | 파일 | ✅ | `.zip`, 10MB 이하 |
| `level` | 문자열 | | `BEGINNER`·`JUNIOR`·`SENIOR`. 생략하면 내 프로필 수준 |
| `consent` | 문자열 | ✅ | `true`여야 한다 (주요 파일이 Google Gemini 무료 등급으로 전송된다는 고지에 대한 동의, `09-데이터셋.md` 6절) |

> F-05b는 `file` 대신 `sourceUrl` 파트를 받도록 이 엔드포인트를 확장한다 (둘 중 정확히 하나).

- 응답 `202`

```json
{ "analysisId": 42, "status": "FETCHING" }
```

- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | `details.file`(없음·빈 파일·ZIP 아님), `details.consent`(동의 안 함), `details.level`(허용되지 않은 값) |
| 401 | AUTH_401 | 토큰 없음·만료 |
| 413 | COMMON_413 | 업로드 10MB 초과 (multipart 한도, `GlobalExceptionHandler` 기존 처리) |
| 429 | ANALYSIS_429 | 오늘 분석 요청 횟수(기본 5회)를 다 씀 |

```json
{ "code": "ANALYSIS_429", "message": "오늘 분석 요청 횟수(5회)를 모두 사용했습니다. 내일 다시 시도해 주세요.", "details": null }
```

### `GET /api/analyses?page=1&size=20`

- 권한: 로그인 필요 (내 분석만)
- 응답 `200`

```json
{
  "items": [
    {
      "id": 42,
      "title": "todo-app",
      "projectName": "todo-app",
      "sourceType": "ZIP",
      "level": "JUNIOR",
      "status": "SELECTING",
      "progress": 25,
      "isPublic": false,
      "createdAt": "2026-11-24T14:20:00+09:00"
    }
  ],
  "page": 1,
  "size": 20,
  "total": 1
}
```

- 에러: 400 `COMMON_400`(page·size 범위), 401 `AUTH_401`

### `GET /api/analyses/{id}/status`

- 권한: 로그인 필요 (본인 분석만. 공개 분석의 타인 조회는 F-19에서 결정)
- 응답 `200` (진행 중)

```json
{ "analysisId": 42, "status": "FILTERING", "progress": 15, "errorCode": null, "errorMessage": null, "updatedAt": "2026-11-24T14:20:01+09:00" }
```

- 응답 `200` (실패)

```json
{ "analysisId": 43, "status": "FAILED", "progress": 15, "errorCode": "ANALYSIS_413", "errorMessage": "지원하는 코드 파일이 312개입니다. 200개 이하로 줄여서 다시 올려 주세요.", "updatedAt": "2026-11-24T14:21:00+09:00" }
// ⚠️ F-26a 이후 이 실패는 없다. 지금 ANALYSIS_413은 저장소 아카이브 크기 초과뿐이다 (#155)
```

- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 401 | AUTH_401 | 토큰 없음·만료 |
| 404 | ANALYSIS_404 | 없는 id, 다른 회원의 분석, soft delete된 분석 (존재 여부를 드러내지 않으려고 403 대신 404) |

### 새 에러 코드 (`ErrorCode` enum)

| 코드 | HTTP | 메시지 | 쓰임 |
|---|---|---|---|
| `ANALYSIS_404` | 404 | 분석을 찾을 수 없습니다. | 응답 |
| `ANALYSIS_429` | 429 | 오늘 분석 요청 횟수({limit}회)를 모두 사용했습니다. 내일 다시 시도해 주세요. | 응답 |
| `ANALYSIS_400` | 400 | 5.3절 | 파이프라인 FAILED 사유 (`analyses.error_code`) |
| `ANALYSIS_413` | 413 | 5.3절 | 파이프라인 FAILED 사유 |
| `ANALYSIS_415` | 415 | 5.3절 | 파이프라인 FAILED 사유 |
| `ANALYSIS_500` | 500 | 5.3절 | 파이프라인 FAILED 사유 |

## 4. DB 변경

- Flyway: `V3__analysis_error_code.sql` — `ALTER TABLE analyses ADD COLUMN error_code VARCHAR(30) NULL AFTER progress;` (V1·V2는 수정하지 않는다)
- 사용 테이블: `analyses`(INSERT, 상태·progress·error UPDATE, 목록·한도 집계), `analysis_files`(INSERT), `activities`(INSERT `ANALYSIS`), `users`(수준 조회, 한도 집계 직렬화를 위한 `SELECT ... FOR UPDATE`)
- 일일 한도 집계: `SELECT COUNT(*) FROM analyses WHERE user_id=? AND created_at >= ? AND status <> 'FAILED'` (`deleted_at` 조건 없음, V2 주석의 규칙). 기존 `idx_analyses_user_created` 사용. 오늘 0시는 `Clock`(Asia/Seoul)으로 계산해 경계 테스트가 가능하게 한다
- 산출물: `03-ERD.md`의 `analyses`에 `error_code` 추가

## 5. 파이프라인 (BE)

- 패키지 `com.vibenote.analysis`. 요청 트랜잭션(한도 확인 → 행 INSERT → 임시 파일 저장)이 **커밋된 뒤** 파이프라인을 시작한다 (`@TransactionalEventListener(AFTER_COMMIT)` 등). 다른 스레드가 커밋 전 행을 못 보는 문제를 막는다.
- `@EnableAsync` + 분석 전용 `ThreadPoolTaskExecutor`(core 2, max 2, queue 50). 큐가 차서 거절되면 `FAILED`·`ANALYSIS_500`.
- ZIP은 **디스크에 풀지 않는다.** `java.util.zip`으로 엔트리를 스트리밍하며 필요한 파일만 메모리로 읽는다 (Zip Slip 원천 차단, 압축 해제 폴더 삭제 불필요). 엔트리 크기 헤더를 믿지 않고 실제로 읽은 바이트로 1MB·10MB 한도를 센다 (zip bomb 방어).

### 5.1 단계와 진행률

| status | progress (진입 시) | 화면 라벨 | 담당 spec |
|---|---|---|---|
| FETCHING | 5 | 코드 수집 중 | F-05a (URL은 F-05b) |
| FILTERING | 15 | 파일 검사 중 | F-05a |
| SELECTING | 25 | 주요 파일 선별 중 | F-05a |
| EXPLAINING | 40 | 설명 생성 중 | F-06 |
| EXTRACTING | 70 | 개념 추출 중 | F-08 |
| QUIZ | 85 | 퀴즈 생성 중 | F-14 |
| DONE | 100 | 분석 완료 | |
| FAILED | 직전 값 유지 | 분석 실패 | |

F-05a에서는 SELECTING 다음이 곧 `DONE`이다. 이후 spec은 단계를 끼워 넣기만 하면 되도록 단계 목록을 한 곳(예: `AnalysisPipeline`의 단계 리스트)에 둔다. `DONE`이 될 때 `activities(ANALYSIS)`를 기록한다.

### 5.2 필터·선별 규칙

| 구분 | 규칙 |
|---|---|
| 경로 | `\`를 `/`로 바꾸고 정규화. 절대 경로(`/`, `C:` 등)나 `..`로 루트를 벗어나면 즉시 `ANALYSIS_400`. `__MACOSX/` 엔트리와 디렉터리 엔트리는 무시. 모든 파일이 최상위 폴더 하나 아래에 있으면 그 폴더를 경로에서 뗀다. 파일명을 UTF-8로 읽을 수 없으면 MS949(Windows 기본 압축 도구)로 다시 읽는다. 최상위 폴더를 뗀 경로가 500자(`analysis_files.path`)를 넘으면 건너뛴다 (리뷰 반영 11-18) |
| 제외 폴더 (읽지 않음) | 경로 조각 중 하나가 `node_modules`, `build`, `dist`, `target`, `out`, `bin`, `.git`, `.gradle`, `.idea`, `.vscode`, `.mvn`, `coverage`, 또는 `.`으로 시작하는 폴더 |
| 비밀 파일 (읽지 않음) | 파일명이 `.env` 또는 `.env.*`, 확장자 `.pem`·`.key`·`.p12`·`.pfx`·`.jks`·`.keystore`, 파일명이 `id_rsa`·`id_ed25519`로 시작 |
| 지원 파일 | 확장자 `.java`, `.xml`, `.js`, `.mjs`, `.vue`, `.yml`, `.yaml`, `.properties`, `.sql`, 그리고 파일명 `build.gradle`·`build.gradle.kts`·`package.json`. 그 외(이미지, `.ts`, `.py`, `.class`, `.jar` 등)는 무시 |
| 크기 | 단일 파일 1MB 초과는 건너뜀. 지원 파일 수 > 200 또는 읽은 합계 > 10MB면 `ANALYSIS_413` |
| 스택 확인 | 지원 파일 중 `.java`·`.js`·`.mjs`·`.vue`가 0개면 `ANALYSIS_415` |
| 키 패턴 마스킹 | 정규식으로 `AIza[0-9A-Za-z_-]{35}`, `sk-[A-Za-z0-9_-]{20,}`, `gh[pousr]_[A-Za-z0-9]{36,}`, `github_pat_[A-Za-z0-9_]{22,}`, `AKIA[0-9A-Z]{16}`, `-----BEGIN [A-Z ]*PRIVATE KEY-----` ~ `-----END ... -----` 블록 → `[REDACTED]`. `.yml`·`.yaml`·`.properties`에서 키 이름에 `password`·`secret`·`api-key`·`apikey`·`api_key`·`token`·`credential`·`privatekey`·`accesskey`가 들어가거나 **마지막 단어가 `key`**(`key`·`private-key`·`ACCESS_KEY`·`aws.key`·`signingKey`·`JWTKey`, 단어 경계 기준이라 `monkey` 제외)인 항목의 값 → `[REDACTED]` (`cache-key`·`routing-key`·`partition-key`·`sort-key`·`foreign-key`·`public-key`는 마지막 단어 기준 정확히 일치할 때만 제외. YAML 키의 따옴표는 벗겨 비교. `primary-key`는 Azure 계정 키 이름이라 가림. 이 key 규칙으로만 비밀인 값은 그 줄에서 가리고, 2차 본문 전체 치환은 8자 이상 토큰만 하고 짧은 토큰이 든 YAML 값은 그 값 범위(파서 노드 위치)를 공백만 남기고 통째로 가린다(`key: id`가 파일 곳곳의 `id`를 가리지 않게, 다음 줄 값·하위 매핑도 원래 자리는 가림, Codex P1), #16 11-23) (값이 비어 있거나 기본값 없는 `${NAME}`이면 그대로, `${NAME:기본값}`이면 기본값만 가림. YAML 블록 스칼라(`|`·`>`)는 지시자를 남기고 이어지는 더 깊은 줄을 가림. YAML 흐름 매핑(`{password: x}`) 안쪽 항목과 `.properties` 줄 이음(``) 뒤 줄도 가림. 2차로 SnakeYAML·`java.util.Properties`로 해석해 비밀 키 아래(하위 포함)의 문자열 값을 모아 본문 전체에서 단어 경계 기준으로 가림(숫자·불리언 제외, 토큰 200개 초과 또는 해석한 값이 원문에 글자 그대로 없으면(이스케이프·줄 이음) 파일 전체를 가림, 해석 실패 시 1차 결과만. `.properties` 1차는 공백 구분자도 인식). 리뷰·Codex 반영 11-18) |
| layer 판정 | `controller`(파일명 `*Controller.java`, 또는 주석 밖에 `@RestController`·`@Controller`가 있는 `.java` — F-07 AC12·#19), `service`(`*Service.java`·`*ServiceImpl.java`), `mapper`(`*Mapper.java`·`*Mapper.xml`·`*Dao.java`·`mapper/` 아래 xml), `vue`(`.vue`), `js`(`router/`·`store`·`stores/`·`api/` 아래 js 및 기타 js), `config`(`*Config.java`·`application*.yml|properties`·`build.gradle*`·`package.json`·`pom.xml`), `domain`(`domain/`·`entity/`·`dto/`·`model/` 아래 java), `other`(나머지) |
| 선별 우선순위 | controller → service → mapper → vue → js → config → domain → other, 같은 layer 안에서는 경로 오름차순. 상위 30개(`analysis.max-selected-files`)를 저장 |

### 5.3 FAILED 문구 (`analyses.error_message`)

| error_code | 원인 | error_message |
|---|---|---|
| ANALYSIS_400 | 깨진 ZIP, 루트 밖·절대 경로 | ZIP 파일을 읽을 수 없습니다. 손상되었거나 허용되지 않는 경로가 들어 있습니다. |
| ANALYSIS_413 | ~~지원 파일 200개 초과~~ (F-26a가 대체, #155) | 지원하는 코드 파일이 {n}개입니다. 200개 이하로 줄여서 다시 올려 주세요. |
| ANALYSIS_413 | ~~지원 파일 합계 10MB 초과~~ (F-26a가 대체, #155) | 코드 파일 합계가 10MB를 넘습니다. 크기를 줄여서 다시 올려 주세요. |
| ANALYSIS_415 | 지원 코드 없음 | 분석할 수 있는 코드가 없습니다. Java·Spring·MyBatis·JavaScript·Vue 프로젝트만 지원합니다. |
| ANALYSIS_500 | 예기치 못한 오류, 실행기 거절 | 분석 중 오류가 발생했습니다. 다시 요청해 주세요. |
| ANALYSIS_500 | 서버 재시작 (AC11) | 서버가 다시 시작되어 분석이 중단되었습니다. 다시 요청해 주세요. |

설정값(`application.yml`): `analysis.daily-limit: 5`, `analysis.max-files: 200`, `analysis.max-total-bytes: 10MB`, `analysis.max-file-bytes: 1MB`, `analysis.max-selected-files: 30`. 임시 폴더는 기존 `app.upload-dir` 아래 `analysis-tmp/`. (⚠️ `max-files`·`max-total-bytes`는 F-26a가 `max-index-files: 2000`·`max-index-bytes: 30MB`로 대체, #155)

## 6. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `/analyses/new` → `AnalysisRequestView.vue` (SC-05), `/analyses/:id(\d+)` → `AnalysisDetailView.vue` (SC-07의 진행 상태 부분, 탭은 F-06부터), `/analyses` → `AnalysisListView.vue` (SC-06의 목록 부분, 수정·삭제·공개는 F-19). 셋 다 `meta: { requiresAuth: true }`. 가입 직후 이동하는 `/analyses/new`(F-01 기존 테스트)가 이제 실제 화면이 된다 (NF-01) |
| 컴포넌트 | 상태 배지·진행률 막대는 목록과 상세가 같이 쓰므로 `components/analysis/AnalysisStatusBadge.vue` 하나로 둔다 (라벨은 5.1절) |
| store | 없음 (화면 로컬 상태). 기본 수준은 `useAuthStore().level` |
| API 함수 | `src/api/analyses.js`: `createAnalysis({ file, level, consent })`(FormData), `fetchAnalyses({ page, size })`, `fetchAnalysisStatus(id)` |
| 고지 문구 | "분석을 위해 주요 파일이 Google Gemini(무료 등급)로 전송되며, 무료 등급 입력은 모델 개선에 쓰일 수 있습니다. 비공개·민감한 코드는 올리지 마세요. `.env`와 키 패턴은 전송 전에 지웁니다." + 동의 체크 |
| 제한 안내 | "하루 5회 · 지원 스택 Java/Spring/MyBatis/JavaScript/Vue · 코드 파일 200개 · 10MB 이하" → #155에서 "… · ZIP 10MB 이하 · 큰 프로젝트는 중요한 파일 30개를 골라 설명합니다"로 바꿈 (F-26a가 대체) |
| 상태별 UI | 요청: 제출 중 비활성화, 필드 아래 `details`, 상단 `message`. 상세: 로딩 / 진행 중(라벨·막대) / 완료 / 실패(사유·다시 요청) / 404. 목록: 로딩 / 빈 목록 CTA / 에러 메시지 / 페이지 이동 |
| 반응형 | 375px~1440px 가로 스크롤 없음 (NF-02) |

## 7. LLM

이 spec은 LLM을 호출하지 않는다. 선별 결과(`analysis_files`)가 F-06 개요·파일 설명 프롬프트의 입력이 된다. Gemini 전송 동의는 여기서 받는다.

## 8. 테스트 계획

테스트용 ZIP은 테스트 코드에서 `ZipOutputStream`으로 만든다 (외부·eval 데이터 미사용, CLAUDE.md 규칙 11). 파이프라인 결과는 동기적으로 검증하고 Awaitility는 도입하지 않는다.

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1~AC4, AC12, AC13 | `analysis/AnalysisControllerTest` | MockMvc 통합 (`IntegrationTestBase`). 파이프라인 시작은 `@MockitoBean`으로 대체해 호출 여부만 확인 |
| AC4 경계 | `analysis/AnalysisControllerTest#AC4_...` | 고정 `Clock` 또는 `created_at`을 직접 넣은 행으로 어제 23:59·FAILED·삭제 케이스 |
| AC5~AC10 | `analysis/AnalysisPipelineTest` | 통합. 행을 넣고 파이프라인 `run(analysisId)`을 직접 호출한 뒤 DB 확인 |
| AC6 | `analysis/pipeline/SecretRedactorTest` | 단위 (패턴별, `${...}` 유지) |
| AC7, AC9 | `analysis/pipeline/ZipArchiveReaderTest` | 단위 (경로 이탈·절대 경로·손상·1MB·10MB·200개·최상위 폴더 제거) |
| AC5, AC7 | `analysis/pipeline/FileSelectorTest` | 단위 (layer 판정, 우선순위, 30개 상한) |
| AC11 | `analysis/AnalysisRecoveryTest` | 통합 (복구 메서드 직접 호출) |
| AC14 | `src/views/__tests__/AnalysisRequestView.spec.js` | Vitest |
| AC15 | `src/views/__tests__/AnalysisDetailView.spec.js` | Vitest (fake timers로 2초 폴링·중지·unmount) |
| AC16 | `src/views/__tests__/AnalysisListView.spec.js` | Vitest |
| API 함수 | `src/api/__tests__/analyses.spec.js` | Vitest (FormData 파트 이름, 쿼리) |

## 9. 범위 밖

- 공개 저장소 URL 수집(GitHub·GitLab 아카이브, 허용 호스트·SSRF 방어) → **F-05b**
- 설명(EXPLAINING)·개념 추출(EXTRACTING)·퀴즈(QUIZ) 단계와 분석 결과 조회 `GET /api/analyses/{id}` → F-06·F-08·F-14
- 제목·메모 수정, 삭제, 공개 설정, 타인의 공개 분석 조회 → F-19
- 남은 일일 횟수 조회 API (안내는 고정 문구로 대신), 분석 취소
- TypeScript·Python 등 지원 스택 밖 코드, 비공개 저장소, 업로드 원본 보관
- 동의 여부 DB 저장 (요청 시 `consent=true` 검증만)

## 10. 결정 필요

- 없음 (11-18 사용자 결정: F-05 a·b 분할, 일일 한도 5회 · FAILED 제외 · soft delete 포함)
- (후속, **11-23 결정 #16**: `key`로 끝나는 키도 가리고 비밀이 아닌 이름만 예외, 5.2절) 설정 키 키워드 확장: `private-key`·`access-key`처럼 `key`로 끝나는 키를 가릴지. `key`만으로 넓히면 `cache-key` 같은 일반 설정도 가려진다. 11-23부터 5.2절 규칙 (11-18 reviewer 지적)

## 11. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-05a-analysis-zip`
- [x] Flyway `V3__analysis_error_code.sql`
- [x] AC별 실패 테스트 작성 (red 확인) — BE AC1~13, FE AC14~16
- [x] BE 구현 (green) — AC1~13
- [x] FE 구현 (green) — AC14~16
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [x] contract-checker 통과
- [x] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일)
- [x] reviewer 지적 사항 반영 (반드시 2·권장 7, Codex P2 6건)
- [x] 산출물 반영: `03-ERD.md`(error_code), `06-화면설계서.md` SC-05·06·07 스크린샷, `04-WBS.md` 3.2를 3.2a(F-05a)·3.2b(F-05b)로 분리, `01-요구사항정의서`의 F-05 행 확인
- [x] PR CI 초록 (PR #14 head bcfaa2f: Backend·Frontend·PR 제목 통과)
- [x] 상태 DONE (PR #14 머지, 2026-11-18)
