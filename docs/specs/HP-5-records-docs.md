# HP-5 하네스 PR ⑤ 기록·문서

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | HP-5 (로드맵 트랙 4 하네스, 11/29) |
| 우선순위 | 하네스 (기능 동결 대상 아님) |
| 영역 오너 | A 김동환 (하네스 주도). 공통 기록(`docs/작업로그.md`)의 보관 위치와 `/progress` 절차를 바꾸므로 PR에 @JoonseobKim |
| 관련 필수 요구사항 | 없음. 제출물 "REST API 명세"(산출물 07)·"DB Schema.sql"·"외부 API·생성형 AI 활용"(산출물 08)을 준비한다 |
| 의존 spec | 없음. 범위 근거는 `docs/기획서.md` 7.7절 (HX-9, HX-10, 보류 H-14 #180) |
| 관련 이슈 | #137 #180 (구현 PR에 `Closes`) |
| 승인 | 김동환 2026-11-27 |

## 1. 목적

- **기록 (HX-9)**: 두 사람이 함께 쓰는 `docs/작업로그.md`가 열흘 만에 857줄이 됐다(하루 약 95줄). 지난 주 항목을 `docs/기록/`으로 옮겨 AI가 읽고 grep하는 파일과 union 머지 범위를 작게 유지한다. 결정로그(185줄)는 결정을 한 곳에서 찾는 편이 중요해 그대로 둔다 (11-27 결정).
- **문서 (HX-10)**: 제출물 산출물 07 REST API 명세와 DB Schema.sql을 정본(`docs/api/openapi.json`, Flyway `V*.sql`)에서 **생성**해 손으로 옮기지 않는다.
- 갱신은 주간 `/progress` docs PR에서 세 가지(보관·07·Schema.sql)를 한 번에 하고 제출 전에 한 번 더 한다. CI는 최신인지 검사하지 않는다. 기능 PR에 부담을 더하지 않기 위해서다 (11-27 결정).
- 함께 처리: 산출물 08에 H-1 가드·pre-push 전체 검증 사례 반영(#137), `.harness/logs` 정리 규칙(#180, 이 PC에 413개·7.7MB).

## 2. 수용 기준 (Acceptance Criteria)

> 하네스 테스트는 `scripts/test/*.test.mjs`(node:test). 테스트 이름 앞에 `HP-5 ACn`을 붙인다. 파일 시스템·git·Docker를 건드리는 부분은 얇게 두고 판정은 순수 함수로 테스트한다.

### 작업로그 주차별 보관 (HX-9)

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 작업로그에 W1(11/17~11/23)·W2(11/24~11/30) 항목이 섞여 있고 오늘이 11/29(W2)일 때 | `node scripts/archive-worklog.mjs`를 실행하면 | 지금 주보다 앞선 주(W1)의 항목을 `docs/기록/작업로그-W1.md`로 옮기고, `docs/작업로그.md`에는 머리말(제목·안내·형식)과 W2 이후 항목만 원래 순서대로 남긴다. 주는 기획서 마일스톤 표(W1 11/17, W2 11/24, W3 12/01, W4 12/08, W5 12/15, 버퍼 12/21)로 나누고 항목 헤더의 날짜(`## YYYY-MM-DD`)로 판정한다. union 머지로 문서 순서가 시간순이 아니어도 날짜로 판정한다. 보관 파일 머리에는 `# 작업로그 W1 (11/17~11/23)`과 원래 파일로 돌아가는 링크를 둔다 |
| AC2 | 보관할 항목이 없거나 이미 보관한 뒤일 때 | 다시 실행하면 | 파일을 바꾸지 않고 "보관할 항목 없음"을 알린다 (exit 0). `--dry-run`은 옮길 주·항목 수만 출력하고 파일을 바꾸지 않는다. `--today YYYY-MM-DD`로 기준일을 줄 수 있다 (테스트·제출 전 정리용) |
| AC3 | 상대 브랜치가 보관 전 develop에서 갈라져 W1 항목 근처를 건드려, union 머지 뒤 이미 보관한 항목이 `docs/작업로그.md`에 다시 나타났을 때 | 다시 실행하면 | 보관 파일에 같은 항목(헤더 줄부터 다음 항목 전까지 앞뒤 공백 제외 동일)이 있으면 보관 파일에 또 붙이지 않고 작업로그에서만 지운다. 보관 파일에 없는 같은 주의 새 항목은 날짜 순서에 맞춰 보관 파일 끝에 붙인다 |
| AC4 | 날짜를 읽을 수 없는 헤더(`## YYYY-MM-DD`가 아님)나 W1 이전 날짜 항목이 있을 때 | 보관하면 | 그 항목은 작업로그에 그대로 두고 경고에 헤더 첫 줄을 보여 준다 (자료를 잃지 않는다) |
| AC5 | 내 작업로그 항목이 이번 주에 없고 보관 파일에만 있을 때 | SessionStart hook이 "내 작업로그 마지막 항목"과 직전 작업 PC를 고르거나, `stamp --draft`가 "내 마지막 기록 뒤 커밋"을 계산하면 | 작업로그와 가장 최근 보관 파일을 함께 읽어 내 마지막 항목을 찾는다 (`common.mjs`에 `readWorklog()` 하나로 모으고 `session-start.mjs`·`stamp.mjs`가 쓴다). 항목 추가(`--append-log`, `handoff.mjs`)는 계속 `docs/작업로그.md` 끝에 한다 |

### 산출물 07 REST API 명세 생성 (HX-10)

| ID | Given | When | Then |
|---|---|---|---|
| AC6 | `docs/api/openapi.json`이 있을 때 | `node scripts/export-api-doc.mjs`를 실행하면 | `docs/산출물/07-REST-API-명세.md`를 통째로 다시 쓴다. 구성: 머리말(정본·생성 명령·"손으로 고치지 않는다", API 제목·버전·엔드포인트 수) → 공통 규칙(지금 파일의 문장 유지) → **엔드포인트 목록**(openapi `tags` 순서대로 Tag별 표: 메서드 · URL · 요약 · 권한) → **엔드포인트 상세**(설명, 파라미터 표 이름·위치·필수·타입·설명·예, 요청 본문 스키마, 응답 표 상태·설명·스키마) → **스키마 부록**(components.schemas별 필드 표 이름·타입·필수·설명·예, `enum`·`$ref`·배열 표기). Tag 안은 경로 → 메서드(GET·POST·PUT·PATCH·DELETE) 순 |
| AC7 | 권한 판정 | 엔드포인트마다 | `security: []`이면 **공개**, 아니고 응답 403 설명에 `ADMIN`이 있으면 **ADMIN**, 그 밖은 **로그인**. 에러 응답 설명의 코드(`(AUTH_401)` 등)는 응답 표에 그대로 보인다 |
| AC8 | 같은 openapi.json으로 | 두 번 실행하면 | 결과가 바이트 단위로 같다 (시각·실행 환경을 쓰지 않는다). openapi.json이 없거나 JSON이 아니면 파일을 바꾸지 않고 exit 1. 지원하지 않는 스키마 형태(예: `oneOf`)는 `(복합 스키마)`로 적고 멈추지 않는다 |

### Schema.sql 추출 (HX-10)

| ID | Given | When | Then |
|---|---|---|---|
| AC9 | Docker가 켜져 있을 때 | `node scripts/export-schema.mjs`를 실행하면 | 테스트와 같은 `mysql:8.4` 임시 컨테이너를 띄워 `V*.sql`을 **버전 번호 순**(V2 < V10)으로 적용하고 `mysqldump --no-data`로 최종 스키마를 `docs/산출물/Schema.sql`에 쓴다. 반복 마이그레이션 `R__*`(시드 데이터)는 넣지 않는다. 컨테이너는 성공·실패와 관계없이 지운다. 비밀번호는 쓰지 않는다(빈 root 비밀번호 임시 컨테이너, 외부 포트를 열지 않음) |
| AC10 | 덤프 결과 | 파일로 쓰기 전에 정리하면 | 머리 주석(생성 명령, 반영한 마지막 V 번호, "손으로 고치지 않는다"), `CREATE DATABASE IF NOT EXISTS vibenote …; USE vibenote;`, `SET FOREIGN_KEY_CHECKS=0;` … `=1;` 사이에 테이블별 `CREATE TABLE`이 남는다. `AUTO_INCREMENT=<n>`, 덤프 시각, `/*!40101 SET …*/` 같은 세션 변수 줄은 지운다. 같은 V 파일로 두 번 실행하면 결과가 같다 (정리 함수는 순수 함수로 테스트) |
| AC11 | 만든 Schema.sql을 | 같은 컨테이너의 빈 DB에 적용하면 | 모든 테이블의 `SHOW CREATE TABLE`이 V 파일로 만든 DB와 같아야 파일을 쓴다. 다르면 파일을 바꾸지 않고 다른 테이블 이름과 함께 exit 1 |
| AC12 | Docker가 꺼져 있거나 V 파일 적용이 실패할 때 | 실행하면 | 기존 Schema.sql을 바꾸지 않고 원인(Docker 꺼짐 / 실패한 V 파일 이름과 MySQL 오류 첫 줄)을 알린 뒤 exit 1 |

### 절차·문서

| ID | Given | When | Then |
|---|---|---|---|
| AC13 | 주간 `/progress`를 돌릴 때 | 진행현황을 쓰는 단계 뒤에 | `/progress` 스킬이 `archive-worklog.mjs` → `export-api-doc.mjs` → `export-schema.mjs`를 실행하고 결과를 같은 docs PR에 커밋한다. Docker가 꺼져 Schema.sql을 못 만들면 건너뛰었다고 보고에 적는다. CLAUDE.md 명령어 표와 문서 지도("작업 이력"에 `docs/기록/`), README 제출 허브의 Schema.sql 링크, 07 머리말을 맞춘다 |
| AC14 (#137) | 산출물 08 3절(개발 과정의 생성형 AI 활용) | 하네스 가드를 설명하는 곳에 | H-1 가드 구조(셸 명령을 토큰으로 나눠 판정, force push·`--no-verify`·`.env` 읽기·보호 브랜치 push 거부), pre-push 전체 검증, reviewer가 가드 퇴행을 재현해 잡은 사례(작업로그·PR에 남은 수치 그대로)를 추가한다. 수치는 작업로그·PR에서 확인한 것만 쓴다 |
| AC15 (#180) | `.harness/logs/`에 `verify-*.log`가 50개를 넘을 때 | `verify.mjs`가 새 로그를 쓴 뒤 | 가장 최근 50개만 남기고 오래된 `verify-*.log`를 지운다. 방금 쓴 로그와 `verify-` 아닌 파일(`bootrun-*.log` 등)은 지우지 않는다. 고르는 판정(`logsToPrune(names, keep)`)은 순수 함수로 테스트한다 |

## 3. API 계약

없음 (하네스·문서). API 명세 문서는 openapi.json에서 생성할 뿐 API를 바꾸지 않는다.

## 4. DB 변경

- Flyway: 없음 (V 파일을 읽기만 한다)
- 사용 테이블: 전체 (Schema.sql 추출 대상)

## 5. 프론트엔드

없음

## 6. LLM (AI 기능만)

없음

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1~AC4 | `scripts/test/archive-worklog.test.mjs` (`splitByWeek`·`mergeArchive` 순수 함수, 임시 디렉터리에서 CLI 1회) | node:test |
| AC5 | `scripts/test/common.test.mjs` (`readWorklog`·보관 파일에만 있는 내 항목), `records.test.mjs` | node:test |
| AC6~AC8 | `scripts/test/export-api-doc.test.mjs` (작은 openapi fixture: 공개·로그인·ADMIN, 파라미터·본문·배열·enum·`$ref`·`oneOf`, 두 번 렌더 동일) | node:test |
| AC9·AC11·AC12 | 수동 1회 실측 (Docker 켠 상태, 결과를 작업로그에), Docker 없음 경로는 `docker` 실행 실패를 주입해 단위 테스트 | 실측 + node:test |
| AC10 | `scripts/test/export-schema.test.mjs` (`orderMigrations`, `cleanDump` 샘플 덤프 문자열) | node:test |
| AC13·AC14 | reviewer 확인 (문서) | 리뷰 |
| AC15 | `scripts/test/verify-logs.test.mjs` (`logsToPrune`) | node:test |

## 8. 범위 밖

- 결정로그 보관 (11-27 결정: 그대로 둔다)
- CI에서 07·Schema.sql 최신 여부 검사 (11-27 결정: 주간 `/progress`와 제출 전에 갱신)
- 시드 데이터(`R__seed_concepts.sql`)를 Schema.sql에 넣기. 시드는 R__ 파일이 정본이고 `docs/산출물/09-데이터셋.md`에서 가리킨다
- ERD(산출물 03) 그림 자동 생성
- 다른 `.harness/` 파일(guard.jsonl, token 기록 등) 정리. 이번에는 verify 로그만 다룬다
- 남은 공통 하네스 이슈(#131·#140·#181 등). 각자 이슈로 둔다

## 9. 결정 필요

- 없음 (11-27 질문 4개 답 반영: 작업로그만 보관, MySQL 덤프 정리본, 주간 `/progress` 갱신, #137·#180 흡수)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/HP-5-records-docs`
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] 보관 스크립트·`readWorklog` (AC1~5, green)
- [x] 07 생성 스크립트 + 07 재생성 (AC6~8)
- [x] Schema.sql 스크립트 + 실측 1회 (AC9~12)
- [x] `/progress`·CLAUDE.md·README·08·verify 로그 정리 (AC13~15)
- [ ] reviewer 지적 사항 반영
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
