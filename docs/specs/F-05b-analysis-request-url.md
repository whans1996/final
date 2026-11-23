# F-05b 분석 요청 (공개 저장소 URL 수집)

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-05b (F-05를 a·b로 분할. F-05a = ZIP 업로드와 수집·필터·선별 파이프라인, DONE) |
| 우선순위 | P0 |
| 영역 오너 | A 김동환 (AI·분석·학습 루프) |
| 관련 필수 요구사항 | ⑬ AI 활용 분석의 입력. 명세서 "필수 · 핵심 데이터"의 작성(F1301), NF-05(SSRF 방어) |
| 의존 spec | F-05a (엔드포인트·파이프라인·일일 한도·임시 파일·재시작 복구를 그대로 쓴다), F-06 (상세 응답에 `sourceUrl` 추가) |
| 관련 이슈 | 없음 (#24 실행기 스레드 독점은 흡수하지 않음, 11-23 결정) |
| 승인 | whans1996 2026-11-23 |

## 1. 목적

회원이 AI로 만든 프로젝트의 **공개 GitHub·GitLab(gitlab.com) 저장소 주소**를 입력하면, 서버가 ZIP 업로드와 같은 엔드포인트로 접수(202)하고 백그라운드 FETCHING 단계에서 **기본 브랜치의 아카이브 ZIP을 내려받아** F-05a 파이프라인(필터·선별·마스킹 → 설명·개념·퀴즈)에 넘긴다.
git clone 없이 아카이브 API만 쓰고(기획서 아키텍처 "저장소 수집"), 사용자가 입력한 주소로 직접 요청하지 않고 **고정된 API 주소를 서버가 조립**해 SSRF를 막는다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_저장소_URL을_보내면_202와_analysisId를_받는다()`

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 로그인한 회원이 (수준 JUNIOR) | `POST /api/analyses`에 `sourceUrl=https://github.com/octo/todo-app.git/`, `consent=true`를 보내면 | 202와 `{analysisId, status:"FETCHING"}`. `analyses` 행: source_type=`URL`, source_url=`https://github.com/octo/todo-app`(4.1절 정규화), project_name=title=`todo-app`, level=`JUNIOR`, status=`FETCHING`. 요청 시점에는 외부 요청도 임시 파일도 없고, 커밋 뒤 파이프라인 실행이 1회 요청된다. `https://gitlab.com/group/sub/my-app`이면 source_url 그대로, project_name `my-app` |
| AC2 | 로그인한 회원이 | 잘못된 `sourceUrl` 또는 입력 조합을 보내면 | 400 `COMMON_400`. `file`·`sourceUrl` 둘 다 → `details.sourceUrl` "ZIP 파일과 저장소 주소 중 하나만 보내세요." 둘 다 없음 → `details.file` "ZIP 파일을 올리거나 저장소 주소를 입력하세요." 4.1절 규칙 위반(허용되지 않은 호스트 `lab.ssafy.com`·`github.io`, `ftp:` 스킴, 브랜치·하위 경로 `/tree/dev`·`/blob/main/a.java`·`/-/tree/dev`, 쿼리·프래그먼트, 사용자 정보·포트, 저장소 이름에 허용되지 않은 문자·`..`, 500자 초과, 공백만) → `details.sourceUrl` (4.1절 문구). 어느 경우든 행이 생기지 않는다. `consent`·`level` 검증은 F-05a AC2 그대로 함께 모은다 |
| AC3 | 오늘 ZIP 분석 5개(FAILED 아님)를 만든 회원이, 또는 토큰 없는 사용자가 | URL로 요청하면 | 각각 429 `ANALYSIS_429`, 401 `AUTH_401`. 일일 한도는 ZIP·URL을 합쳐 센다 (F-05a AC4 규칙 그대로) |
| AC4 | GitHub URL 분석이 있고, 가짜 GitHub 서버가 `GET {github-api-base}/repos/octo/todo-app/zipball`에 허용 호스트로 302를 주고 그 주소가 최상위 폴더 `octo-todo-app-1a2b3c4/`가 든 Spring 프로젝트 ZIP을 줄 때 | 파이프라인이 실행되면 | FETCHING 단계에서 아카이브를 `{app.upload-dir}/analysis-tmp/{id}.zip`에 받은 뒤 F-05a와 같은 규칙으로 처리해 `DONE`이 된다. `analysis_files.path`는 최상위 폴더를 뗀 경로(`src/main/java/.../UserController.java`). 요청에는 `User-Agent: VibeNote`가 붙는다. `analysis.fetch.github-token`이 설정되어 있으면 `Authorization: Bearer <token>`을 **api 호스트 요청에만** 붙이고 리다이렉트된 다운로드 요청에는 붙이지 않는다. 끝나면 임시 파일이 지워진다 |
| AC5 | GitLab URL `https://gitlab.com/group/sub/my-app` 분석이 있을 때 | 파이프라인이 실행되면 | `GET {gitlab-api-base}/projects/group%2Fsub%2Fmy-app/repository/archive.zip`으로 받고 `DONE`이 된다 (프로젝트 경로는 URL 인코딩한 한 조각). 토큰 헤더는 붙이지 않는다 |
| AC6 | 저장소가 없거나 비공개이거나 비어 있어 서버가 404(GitHub·GitLab), 401, 403(한도 초과가 아님), 그 밖의 4xx를 줄 때 | 파이프라인이 실행되면 | `FAILED`, `ANALYSIS_004`, 5.2절 문구. 일일 한도를 깎지 않는다(FAILED 제외 규칙). 임시 파일이 남지 않는다 |
| AC7 | 서버가 요청 한도 초과를 알릴 때 (429, 또는 403이면서 `x-ratelimit-remaining: 0`) | 파이프라인이 실행되면 | `FAILED`, `ANALYSIS_503`, 5.2절 문구 |
| AC8 | 서버가 5xx를 주거나, 연결이 안 되거나, 받기 시작부터 끝까지 `analysis.fetch.timeout`(기본 60초, 테스트는 짧게)을 넘기거나, 200 응답 본문이 ZIP 시그니처(`PK\x03\x04`)로 시작하지 않을 때 | 파이프라인이 실행되면 | `FAILED`, `ANALYSIS_502`, 5.2절 문구. 쓰다 만 임시 파일이 남지 않는다 |
| AC9 | 아카이브가 `analysis.fetch.max-archive-bytes`(기본 30MB)를 넘을 때 (`Content-Length`가 넘거나, 헤더 없이 실제로 받은 바이트가 넘음) | 파이프라인이 실행되면 | 상한을 넘는 순간 읽기를 멈추고 `FAILED`, `ANALYSIS_413`, "저장소 아카이브가 30MB를 넘습니다. …"(5.2절). 정확히 30MB면 계속 진행한다 |
| AC10 | 다운로드 응답이 리다이렉트일 때 | 파이프라인이 실행되면 | 리다이렉트는 **시작 요청과 같은 스킴이고 `analysis.fetch.allowed-hosts`에 있는 호스트일 때만** 최대 3번 따라간다. 허용되지 않은 호스트(예: `169.254.169.254`, `evil.example`)·스킴이 바뀐 주소·`Location` 없음·4번째 리다이렉트는 그 주소로 요청하지 않고 `FAILED`, `ANALYSIS_502` |
| AC11 | 내려받은 아카이브에 지원 코드가 없거나(README·이미지만) 지원 파일이 200개를 넘을 때 | 파이프라인이 실행되면 | F-05a와 같은 `ANALYSIS_415`·`ANALYSIS_413` (FETCHING 뒤 단계는 입력 종류와 무관) |
| AC12 | 내 URL 분석과 ZIP 분석이 있을 때 | `GET /api/analyses/{id}` 하면 | 응답에 `sourceUrl`이 있다: URL 분석은 정규화한 주소, ZIP 분석은 `null`. 목록 `GET /api/analyses` 항목은 바뀌지 않는다 |
| AC13 | 로그인한 회원이 | `/analyses/new`(SC-05)에서 | 입력 방식 선택("ZIP 업로드" 기본 / "저장소 주소")이 있다. 저장소 주소를 고르면 주소 입력칸(placeholder `https://github.com/owner/repo`)과 안내 "공개 GitHub·GitLab(gitlab.com) 저장소의 기본 브랜치를 분석합니다. 저장소 주소만 입력하세요."가 보인다. 주소(공백 제외)와 동의가 있어야 버튼이 활성화되고, 요청에는 `sourceUrl`만 보낸다(`file` 파트 없음). 방식을 바꾸면 다른 방식의 입력값을 비운다. 400의 `details.sourceUrl`은 주소 칸 아래에 보인다. 202면 `/analyses/{analysisId}`로 이동한다. ZIP 방식은 F-05a AC14 그대로 |
| AC14 | 로그인한 회원이 | URL 분석의 `/analyses/:id`(SC-07)를 열면 | 제목 근처에 "저장소 보기" 링크(`href`=sourceUrl, `target="_blank"`, `rel="noopener noreferrer"`)가 있다. ZIP 분석에는 없다. 실패하면 F-05a AC15처럼 `errorMessage`(ANALYSIS_004·502·503 문구)와 "다시 요청하기"를 보여 준다 |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: `분석`.

### `POST /api/analyses` (확장)

- 권한: 로그인 필요
- 요청 `multipart/form-data` — **`file`과 `sourceUrl` 중 정확히 하나**

| 파트 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `file` | 파일 | 둘 중 하나 | `.zip`, 10MB 이하 (F-05a) |
| `sourceUrl` | 문자열 | 둘 중 하나 | 공개 저장소 주소. 4.1절 형식, 500자 이하 |
| `level` | 문자열 | | `BEGINNER`·`JUNIOR`·`SENIOR`. 생략하면 내 프로필 수준 |
| `consent` | 문자열 | ✅ | `true` |

```
sourceUrl=https://github.com/octo/todo-app
level=SENIOR
consent=true
```

- 응답 `202`

```json
{ "analysisId": 57, "status": "FETCHING" }
```

- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | `details.file`(없음·ZIP 아님, 둘 다 없음), `details.sourceUrl`(형식·호스트·경로 위반, 둘 다 보냄), `details.consent`, `details.level` |
| 401 | AUTH_401 | 토큰 없음·만료 |
| 413 | COMMON_413 | 업로드 10MB 초과 (ZIP) |
| 429 | ANALYSIS_429 | 오늘 분석 요청 횟수를 다 씀 (ZIP·URL 합산) |

```json
{
  "code": "COMMON_400",
  "message": "요청 값이 올바르지 않습니다.",
  "details": { "sourceUrl": "저장소 주소만 입력하세요. 브랜치·파일 경로(/tree/…, /blob/…)는 지원하지 않습니다." }
}
```

- Swagger summary는 "분석 요청 (ZIP·저장소 URL)"으로 바꾼다.

### `GET /api/analyses/{id}` (필드 추가)

`AnalysisDetailResponse`에 `sourceUrl` (string, nullable)을 `sourceType` 다음에 추가한다.

```json
{ "id": 57, "title": "todo-app", "projectName": "todo-app", "sourceType": "URL", "sourceUrl": "https://github.com/octo/todo-app", "level": "SENIOR", "status": "DONE", "...": "F-06 필드 그대로" }
```

### 새 에러 코드 (`ErrorCode` enum, 파이프라인 FAILED 사유 전용)

| 코드 | HTTP | 메시지 (5.2절) | 쓰임 |
|---|---|---|---|
| `ANALYSIS_004` | 404 | 저장소를 찾을 수 없습니다. … | `analyses.error_code` |
| `ANALYSIS_502` | 502 | 저장소에서 코드를 받지 못했습니다. … | `analyses.error_code` |
| `ANALYSIS_503` | 503 | 저장소 서비스의 요청 한도를 넘었습니다. … | `analyses.error_code` |

`docs/conventions.md` 3절 `ANALYSIS` 행에 세 코드를 추가한다.

## 4. 입력 규칙과 DB

### 4.1 저장소 주소 규칙 (`RepositoryUrl.parse`, 요청 때 동기 검증)

| 구분 | 규칙 |
|---|---|
| 전처리 | 앞뒤 공백 제거. 비면 "저장소 주소를 입력하세요." 500자 초과면 "저장소 주소는 500자 이하로 입력하세요." |
| 스킴 | `https` 또는 `http` (대소문자 무관, 저장은 `https`). 그 밖·스킴 없음 → "https://로 시작하는 GitHub·GitLab 저장소 주소를 입력하세요." |
| 호스트 | `github.com`·`www.github.com`·`gitlab.com`만 (대소문자 무관). 사용자 정보(`user@`)·포트가 있으면 위반. 그 밖 → "GitHub(github.com)나 GitLab(gitlab.com)의 공개 저장소만 분석할 수 있습니다." |
| 쿼리·프래그먼트 | 있으면 위반 (아래 경로 문구) |
| GitHub 경로 | 끝의 `/`와 `.git`을 하나씩 뗀 뒤 정확히 `/{owner}/{repo}`. owner `[A-Za-z0-9-]{1,39}`, repo `[A-Za-z0-9._-]{1,100}`이고 `.`·`..`이 아님 |
| GitLab 경로 | 끝의 `/`와 `.git`을 뗀 뒤 `/{ns}/…/{project}` 2~20조각. 각 조각 `[A-Za-z0-9._-]{1,255}`, `.`·`..`·`-`가 아님 (`/-/tree/…`는 `-` 조각이라 위반) |
| 경로 위반 문구 | 조각 수가 많거나 `tree`·`blob`·`-` 등이 섞임, 쿼리·프래그먼트 → "저장소 주소만 입력하세요. 브랜치·파일 경로(/tree/…, /blob/…)는 지원하지 않습니다." 문자·조각 수 부족 → "저장소 주소 형식이 올바르지 않습니다. 예: https://github.com/owner/repo" |
| 정규화 저장 | `https://github.com/{owner}/{repo}`, `https://gitlab.com/{ns}/…/{project}` (호스트 소문자, `www.` 제거, 경로 대소문자는 입력 그대로) |
| 이름 | project_name·title = 마지막 조각(repo·project), 100자로 자름 (F-05a `MAX_NAME_LENGTH`) |

브랜치는 항상 **기본 브랜치**다 (11-23 결정). 존재·공개 여부는 요청 때 확인하지 않고 FETCHING에서 판정한다 (요청 202를 1초 안에 주고, 외부 API 호출을 요청 스레드에서 하지 않음).

### 4.2 DB 변경

- Flyway: **없음**. `analyses.source_type ENUM('URL','ZIP')`·`source_url VARCHAR(500)`(V1)을 그대로 쓴다.
- 사용 테이블: `analyses`(INSERT with `source_type=URL`·`source_url`, 상세 조회에 `source_url` 추가). 산출물 `03-ERD.md` 변경 없음.

## 5. 수집기 (BE)

- 패키지 `com.vibenote.analysis.fetch`: `RepositoryUrl`(파싱·정규화, 호스트 종류 GITHUB·GITLAB), `RepositoryArchiveFetcher`(다운로드), `FetchProperties`(`analysis.fetch.*`).
- `AnalysisService.requestZip`을 입력 종류별로 나누되 한도 확인 → INSERT → (ZIP만) 임시 파일 저장 → 이벤트 발행 순서와 트랜잭션은 F-05a 그대로. `Analysis.newUrl(userId, repositoryUrl, level, now)` 추가.
- `AnalysisPipeline.fetch`: ZIP이면 지금처럼 임시 파일 확인, URL이면 `RepositoryArchiveFetcher.download(url, tempFiles.path(id))`. 이후 단계는 그대로이며 `finally`의 임시 파일 삭제와 재시작 복구(F-05a AC10·AC11)가 다운로드 파일에도 적용된다.
- HTTP는 JDK `java.net.http.HttpClient`(`Redirect.NEVER`, 리다이렉트는 직접 따라가며 4.1·AC10 검사)로 하고 응답 본문을 스트리밍으로 임시 파일에 쓰며 바이트를 센다. 새 의존성 없음.

### 5.1 다운로드 주소와 설정

| 종류 | 첫 요청 | 헤더 |
|---|---|---|
| GitHub | `{github-api-base}/repos/{owner}/{repo}/zipball` (기본 브랜치, 보통 `codeload.github.com`으로 302) | `User-Agent: VibeNote`, `Accept: application/vnd.github+json`, 토큰 있으면 api 호스트에만 `Authorization: Bearer …` |
| GitLab | `{gitlab-api-base}/projects/{URL 인코딩한 경로}/repository/archive.zip` | `User-Agent: VibeNote` |

```yaml
analysis:
  fetch:
    github-api-base: https://api.github.com
    gitlab-api-base: https://gitlab.com/api/v4
    allowed-hosts: [api.github.com, codeload.github.com, gitlab.com]
    github-token: ${GITHUB_TOKEN:}   # 선택. 공개 저장소 읽기 권한이면 충분 (비인증 60회/시 → 5,000회/시)
    max-archive-bytes: 30MB
    connect-timeout: 5s
    timeout: 60s                     # 첫 요청부터 본문 끝까지
    max-redirects: 3
```

- `.env.example`에 `GITHUB_TOKEN=`(주석: 선택, 교육장 공용 IP 한도 대비)을 추가한다. 토큰 값은 로그·예외 메시지에 남기지 않는다.
- 테스트는 base·allowed-hosts를 JDK `HttpServer`로 띄운 `http://localhost:{port}`로 바꾼다 (외부 네트워크 없음).

### 5.2 FAILED 문구 (`analyses.error_message`)

| error_code | 원인 | error_message |
|---|---|---|
| ANALYSIS_004 | 404·401·403(한도 아님)·그 밖 4xx | 저장소를 찾을 수 없습니다. 공개 저장소인지, 주소가 맞는지, 기본 브랜치에 코드가 있는지 확인해 주세요. |
| ANALYSIS_503 | 429, 403 + `x-ratelimit-remaining: 0` | 저장소 서비스의 요청 한도를 넘었습니다. 잠시 뒤 다시 요청하거나 ZIP으로 올려 주세요. |
| ANALYSIS_502 | 5xx, 연결 실패, 시간 초과, ZIP이 아닌 본문, 허용되지 않은 리다이렉트 | 저장소에서 코드를 받지 못했습니다. 잠시 뒤 다시 요청하거나 ZIP으로 올려 주세요. |
| ANALYSIS_413 | 아카이브 30MB 초과 | 저장소 아카이브가 30MB를 넘습니다. ZIP으로 필요한 코드만 올려 주세요. |

F-05a 5.3절 문구(ANALYSIS_400·413·415·500)는 그대로다.

## 6. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `/analyses/new` `AnalysisRequestView.vue`(SC-05)에 입력 방식 선택과 주소 입력칸 추가, `/analyses/:id` `AnalysisDetailView.vue`(SC-07)에 "저장소 보기" 링크 |
| 컴포넌트 | 새 컴포넌트 없음 (방식 선택은 라디오 2개) |
| store | 없음 |
| API 함수 | `src/api/analyses.js`의 `createAnalysis({ file, sourceUrl, level, consent })`: `file`이 있으면 `file` 파트, 아니면 `sourceUrl` 파트를 붙인다 |
| 제한 안내 | 기존 문구 뒤에 "· 저장소 아카이브 30MB 이하"(주소 방식일 때) |
| 상태별 UI | 요청 중 버튼 비활성화, `details.sourceUrl`은 주소 칸 아래, 그 밖 F-05a 그대로 |
| 반응형 | 375px~1440px 가로 스크롤 없음 (NF-02) |
| 산출물 | `06-화면설계서.md` SC-05(주소 방식 스크린샷)·SC-07(링크) 갱신 |

## 7. LLM

이 spec은 LLM을 호출하지 않는다. 내려받은 코드는 F-05a 선별·마스킹을 거친 뒤 F-06 이후 단계에 들어간다. Gemini 전송 동의는 F-05a와 같은 `consent`로 받는다.

## 8. 테스트 계획

외부 네트워크를 쓰지 않는다. 가짜 GitHub·GitLab 서버는 JDK `com.sun.net.httpserver.HttpServer`로 테스트 안에서 띄우고, ZIP은 `ZipOutputStream`으로 만든다 (CLAUDE.md 규칙 11, eval 데이터 미사용).

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1~AC3, AC12 | `analysis/AnalysisControllerTest` | MockMvc 통합 (파이프라인 시작은 기존처럼 `@MockitoBean`) |
| AC2 형식 | `analysis/fetch/RepositoryUrlTest` | 단위 (허용·거부 주소 표, 정규화, 이름) |
| AC4~AC10 | `analysis/fetch/RepositoryArchiveFetcherTest` | 단위 + 가짜 서버 (리다이렉트·토큰 헤더·상태 코드·한도·시간 초과·30MB 경계·ZIP 시그니처) |
| AC4, AC6, AC11 | `analysis/AnalysisPipelineTest` | 통합 (URL 분석 행 + 가짜 서버로 `run(id)` → DONE·FAILED·임시 파일 삭제) |
| 실제 연결 | `analysis/fetch/RepositoryArchiveFetcherLiveTest` | `@Tag("live")`, 사람이 `liveTest`로 수동 실행. DS-02-01(`gothinkster/spring-boot-realworld-example-app`, split dev) 다운로드 |
| AC13 | `src/views/__tests__/AnalysisRequestView.spec.js` | Vitest |
| AC14 | `src/views/__tests__/AnalysisDetailView.spec.js` | Vitest |
| API 함수 | `src/api/__tests__/analyses.spec.js` | Vitest (`sourceUrl` 파트, `file` 파트 없음) |

## 9. 범위 밖

- 브랜치·태그·커밋 지정, 하위 폴더만 분석 → 기본 브랜치 전체만 (11-23 결정)
- 비공개 저장소, 사용자별 GitHub·GitLab 토큰·OAuth, `lab.ssafy.com` 등 설치형 GitLab·Bitbucket
- DNS 해석 결과(사설·링크로컬 IP) 검사: 요청 호스트는 사용자 입력이 아니라 설정의 고정 API 주소와 허용 호스트 목록이라 대상이 바뀌지 않는다. 허용 호스트 밖으로의 리다이렉트는 AC10이 막는다
- 같은 저장소 재분석 캐시·중복 감지, 요청 때 저장소 존재 미리 확인
- 다운로드 진행률 표시 (FETCHING 동안 progress 5 유지)
- 분석 실행기 스레드 분리 (#24, 다운로드도 최대 60초 실행기 스레드를 쓴다는 점을 이슈에 덧붙인다)

## 10. 결정 필요

- 없음 (11-23 사용자 결정: 기본 브랜치만·뒤 경로 400, 아카이브 30MB, 선택적 `GITHUB_TOKEN`, #24 흡수 안 함)

## 11. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-05b-analysis-url`
- [x] Flyway 마이그레이션 — 없음
- [ ] AC별 실패 테스트 작성 (red 확인) — BE AC1~12 ✅, FE AC13~14
- [x] BE 구현 (green) — `RepositoryUrl`, `RepositoryArchiveFetcher`, `AnalysisService`·`AnalysisPipeline.fetch`, `ErrorCode` 3개, 상세 `sourceUrl`
- [ ] FE 구현 (green)
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [x] `.env.example`(`GITHUB_TOKEN`), `docs/conventions.md` 3절 에러 코드
- [ ] contract-checker 통과
- [ ] ui-verifier 통과 (데스크톱·모바일)
- [ ] reviewer 지적 사항 반영 (SSRF·토큰 누출 중점)
- [ ] 산출물 반영: `06-화면설계서.md` SC-05·SC-07, `01-요구사항정의서` F-05 행 확인, `08-외부API-생성형AI-활용.md`에 GitHub·GitLab 아카이브 API 사용
- [ ] liveTest 수동 확인 (DS-02-01)
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
