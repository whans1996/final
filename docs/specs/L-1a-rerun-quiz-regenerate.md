# L-1a 실패 분석 재실행(URL)·퀴즈 재생성

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | L-1a (로드맵 트랙 2, 2차 점검 J2-01·Q-02) |
| 우선순위 | T0 (기획서 7.4b) |
| 영역 오너 | A 김동환 (AI·분석·학습 루프) |
| 관련 필수 요구사항 | ⑫ ⑬ (F-14 퀴즈·분석 흐름 보강) |
| 의존 spec | F-05b(URL 분석), F-14(퀴즈), F-19(분석 삭제) |
| 관련 이슈 | 없음 |
| 승인 | 김동환 2026-11-26 |

## 1. 목적

분석이 저장소 수집·AI 응답 실패로 끝나면 지금은 "다시 요청하기" 링크로 요청 화면에 돌아가 주소·수준을 다시 넣어야 하고, 퀴즈 생성이 실패하면 "퀴즈를 만들지 못했습니다"에서 멈춰 학습 루프(분석 → 퀴즈 → 복습)가 끊긴다 (2차 점검 J2-01·Q-02).
이 기능은 ① 실패한 **GitHub URL 분석**을 같은 설정으로 한 번에 다시 실행하고, ② 퀴즈 없이 끝난 분석의 **퀴즈만 다시 만들게** 한다.
ZIP 분석은 원본을 보관하지 않으므로 "ZIP을 다시 올려 주세요"로 안내한다 (결정로그 11-26 L-1 범위 ①, F-14의 "재생성 없음" 결정을 대체).

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_실패한_URL_분석을_재실행하면_새_분석이_202로_시작된다()`

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 내 URL 분석(id 41)이 `FAILED`이고 오늘 한도가 남았을 때 | `POST /api/analyses/41/rerun` | 202와 새 `analysisId`(예: 57), `status: FETCHING`. 새 행은 41의 `source_type=URL`·`source_url`·`project_name`·`level`·`title`·`memo`를 그대로 쓰고 `is_public=false`, `quiz_status=NULL`, `quiz_regenerations=0`이다. 커밋 뒤 파이프라인이 시작된다(`AnalysisRequestedEvent`). 41은 같은 트랜잭션에서 F-19 삭제와 같이 soft delete되고 파일·흐름·개념 연결이 지워진다(`AnalysisDeletedEvent`) |
| AC2 | 오늘 분석 요청이 이미 한도(`analysis.daily-limit`)만큼일 때 | 재실행을 요청하면 | 429 `ANALYSIS_429`(기존 문구). 41은 그대로 `FAILED`·미삭제이고 새 행이 없다. 재실행이 성공하면 새 행이 오늘 요청 1회로 세어진다 |
| AC3 | 재실행할 수 없는 분석 | 재실행을 요청하면 | ZIP 분석이거나 `FAILED`가 아님(진행 중·`DONE`) → 409 `ANALYSIS_008`. 없는 분석·남의 분석(공개·팀 공유 포함)·삭제된 분석 → 404 `ANALYSIS_404`. 두 행 모두 그대로다. 같은 분석에 재실행 요청 2개가 동시에 오면 하나만 202이고 다른 하나는 404이며, 새 행은 1개다 |
| AC4 | 분석의 퀴즈 상태 | 파이프라인 QUIZ 단계가 끝나거나 상태를 조회하면 | QUIZ 단계에서 문항을 저장하면 `analyses.quiz_status=READY`, 생성이 빈 결과(형식 오류 2회·LLM_503)면 `FAILED`가 퀴즈 저장과 같은 트랜잭션에서 기록된다. 분석이 QUIZ 단계를 끝내기 전(진행 중, 그 전 단계에서 `FAILED`)에는 `NULL`이다. `GET /api/analyses/{id}/status`와 `GET /api/analyses/{id}`가 `quizStatus`를, 상세는 `quizRegenerationsLeft`(= 3 − `quiz_regenerations`, 최소 0)도 준다. V14는 기존 행을 채운다: `quizzes` 행이 있으면 `READY`, 없고 `DONE`이면 `FAILED`, 나머지는 `NULL` |
| AC5 | 내 분석이 `DONE`, `quiz_status=FAILED`, 재생성 2회 이하 사용일 때 | `POST /api/analyses/{id}/quiz` | 202 `{analysisId, quizStatus: GENERATING, quizRegenerationsLeft}`. 조건부 UPDATE 한 문장으로 `quiz_status=GENERATING`, `quiz_regenerations+1`. 커밋 뒤 분석 실행기에서 F-14와 같은 입력(연결 개념·선별 파일·분석 level)으로 `QuizGenerator.generate`를 트랜잭션 밖에서 1회 부르고, 문항이 있으면 `quizzes`·`quiz_questions` 저장과 `READY`를 한 트랜잭션으로 기록한다. 이후 `GET /api/analyses/{id}/quiz`가 200이다. `llm_usage_logs`에 stage `quiz`, 그 user_id·analysis_id가 남는다 |
| AC6 | 재생성이 시작된 뒤 | 생성이 형식 오류 2회·LLM_503으로 빈 결과이거나, 저장 중 예기치 못한 예외가 나거나, 실행기가 가득 차 거절하면 | `quiz_status=FAILED`, `quizzes` 행 없음, 분석 `status`는 `DONE` 그대로다. 사용한 재생성 횟수는 돌려주지 않는다 |
| AC7 | 재생성할 수 없는 경우 | `POST /api/analyses/{id}/quiz` | 분석이 `DONE`이 아님, `quiz_status`가 `READY`·`GENERATING`·`NULL` → 409 `QUIZ_409`. 재생성 3회(`quiz.max-regenerations`)를 다 씀 → 429 `QUIZ_429`. 없는·삭제된·남의 분석(팀 공유 멤버 포함) → 404 `ANALYSIS_404`. 같은 분석에 요청 2개가 동시에 오면 하나만 202이고 다른 하나는 409이며 LLM 호출은 1회다 |
| AC8 | `GENERATING`인 분석 | 생성이 끝나기 전에 분석이 삭제되거나(F-19) 회원이 탈퇴하면 / 서버가 다시 시작되면 | 삭제·탈퇴: 저장 단계의 조건부 UPDATE(`quiz_status=GENERATING AND deleted_at IS NULL`)가 0행이라 퀴즈를 넣지 않고 예외도 없다. 재시작: `AnalysisRecovery`가 `GENERATING`을 모두 `FAILED`로 바꾼다 |
| AC9 | 분석 결과 화면(SC-07)에서 내 분석이 `FAILED`일 때 | 화면을 열면 | URL 분석: 실패 사유 아래 **"같은 설정으로 다시 실행"** 버튼. 누르면 `rerunAnalysis(id)` → 새 id로 `router.replace`, 진행 상태 폴링이 새 분석으로 다시 시작된다. 요청 중에는 버튼이 비활성이다. 429·404·409면 버튼 아래에 서버 메시지를 보이고 화면은 그대로다. ZIP 분석: "ZIP 분석은 원본을 보관하지 않습니다. ZIP 파일을 다시 올려 주세요." 문구와 요청 화면(`analysis-new`) 링크. 상세를 아직 못 불러왔으면 기존 "다시 요청하기" 링크를 보인다 |
| AC10 | 퀴즈 탭에서 내 분석의 퀴즈가 없을 때 (`QUIZ_404`) | 탭을 열면 | `quizStatus=FAILED`이고 남은 횟수 > 0: "퀴즈를 만들지 못했습니다." + **"퀴즈 다시 만들기 (남은 n회)"** 버튼. 남은 횟수 0: 버튼 없이 "다시 만들기 횟수를 모두 썼습니다." 버튼을 누르거나 처음부터 `GENERATING`이면 "퀴즈를 만드는 중입니다…"를 보이고 `GET /status`를 2초마다 조회해 `READY`면 퀴즈를 불러오고 `FAILED`면 실패 문구·버튼(남은 횟수 갱신)으로 돌아간다. 탭을 떠나면 폴링을 멈춘다. 429·409·404는 서버 메시지를 보인다 |

## 3. API 계약

> Swagger Tag: `분석` (재실행), `퀴즈` (재생성) (`docs/conventions.md` 8절). 구현 후 `docs/api/openapi.json`과 일치해야 한다.

### `POST /api/analyses/{id}/rerun`

- 권한: 로그인 필요 (분석 주인만)
- 요청: 본문 없음
- 응답 `202` (기존 `CreateAnalysisResponse`)

```json
{ "analysisId": 57, "status": "FETCHING" }
```

- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 401 | AUTH_401 | 토큰 없음·만료 |
| 404 | ANALYSIS_404 | 없는·삭제된·남의 분석, 동시 재실행에서 진 요청 |
| 409 | ANALYSIS_008 | ZIP 분석이거나 `FAILED`가 아님. 문구: "실패한 저장소 주소 분석만 다시 실행할 수 있습니다. ZIP은 파일을 다시 올려 주세요." |
| 429 | ANALYSIS_429 | 오늘 분석 요청 한도 초과 (기존) |

### `POST /api/analyses/{id}/quiz`

- 권한: 로그인 필요 (분석 주인만)
- 요청: 본문 없음
- 응답 `202` (새 `QuizRegenerationResponse`)

```json
{ "analysisId": 42, "quizStatus": "GENERATING", "quizRegenerationsLeft": 2 }
```

- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 401 | AUTH_401 | 토큰 없음·만료 |
| 404 | ANALYSIS_404 | 없는·삭제된·남의 분석 (팀 공유 멤버 포함) |
| 409 | QUIZ_409 | 분석이 `DONE`이 아니거나 퀴즈가 이미 있거나 만드는 중. 문구: "퀴즈를 다시 만들 수 없는 상태입니다. 화면을 새로 고쳐 주세요." |
| 429 | QUIZ_429 | 재생성 횟수 소진. 문구: "이 분석의 퀴즈 다시 만들기 횟수({limit}회)를 모두 썼습니다." |

### 기존 응답에 필드 추가

- `GET /api/analyses/{id}/status` (`AnalysisStatusResponse`): `quizStatus` (`GENERATING`·`READY`·`FAILED`·`null`)

```json
{ "analysisId": 42, "status": "DONE", "progress": 100, "errorCode": null, "errorMessage": null, "quizStatus": "GENERATING" }
```

- `GET /api/analyses/{id}` (`AnalysisDetailResponse`): `quizStatus`, `quizRegenerationsLeft` (int, 0~3)
- 새 enum `QuizStatus { GENERATING, READY, FAILED }` (패키지 `quiz`). 응답 스키마에 허용값과 nullable을 드러낸다.

## 4. DB 변경

- Flyway: `V14__analysis_quiz_status.sql` (번호는 구현 직전 `node scripts/next-migration.mjs`로 다시 확인)

```sql
ALTER TABLE analyses
    ADD COLUMN quiz_status VARCHAR(16) NULL COMMENT 'GENERATING·READY·FAILED, QUIZ 단계 전 NULL (L-1a)',
    ADD COLUMN quiz_regenerations INT NOT NULL DEFAULT 0 COMMENT '퀴즈 재생성 요청 수 (L-1a)';

UPDATE analyses a
SET quiz_status = CASE
    WHEN EXISTS (SELECT 1 FROM quizzes q WHERE q.analysis_id = a.id) THEN 'READY'
    WHEN a.status = 'DONE' THEN 'FAILED'
    ELSE NULL END;
```

- 사용 테이블: `analyses`(위 두 열, 재실행 INSERT·soft delete), `quizzes`·`quiz_questions`(재생성 INSERT, `uk_quizzes_analysis`로 분석당 1개 유지), `analysis_files`·`analysis_concepts`(재생성 입력), `llm_usage_logs`
- 조건부 UPDATE (Mapper XML, `${}` 없음)
  - 재실행 대상 삭제: `UPDATE analyses SET deleted_at=?, updated_at=? WHERE id=? AND user_id=? AND deleted_at IS NULL AND status='FAILED' AND source_type='URL'` → 0행이면 `findOwned`로 404·409를 고른다 (F-19 `delete`와 같은 방식)
  - 재생성 시작: `... SET quiz_status='GENERATING', quiz_regenerations=quiz_regenerations+1 WHERE id=? AND user_id=? AND deleted_at IS NULL AND status='DONE' AND quiz_status='FAILED' AND quiz_regenerations < #{max}` → 0행이면 다시 읽어 404·409·429를 고른다
  - 재생성 저장: `... SET quiz_status=? WHERE id=? AND quiz_status='GENERATING' AND deleted_at IS NULL` → 1행일 때만 퀴즈 INSERT
- ERD(`docs/산출물/03-ERD.md`) `analyses`에 두 열 추가

## 5. 프론트엔드

> F-29 공통 PR(12/02) 전이므로 기존 클래스(`retry-button`·`form-error`·`hint`·`EmptyState`)만 쓰고 새 스타일을 만들지 않는다 (엉킴 방지 규칙 2).

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `/analyses/:id` → `AnalysisDetailView.vue` (SC-07) |
| 컴포넌트 | `AnalysisDetailView.vue` FAILED 영역(재실행 버튼·ZIP 안내), `components/analysis/AnalysisQuizTab.vue`(재생성 버튼·만드는 중·폴링). 퀴즈 탭은 상세의 `quizStatus`·`quizRegenerationsLeft`를 props로 받는다 |
| store | 없음 |
| API 함수 | `src/api/analyses.js` `rerunAnalysis(id)`, `src/api/quizzes.js` `regenerateQuiz(analysisId)`. 상태 폴링은 기존 `fetchAnalysisStatus(id)` |
| 상태별 UI | 재실행 요청 중 버튼 비활성 / 실패 메시지 / 퀴즈 만드는 중 / 재생성 실패(남은 횟수) / 횟수 소진 |

## 6. LLM (AI 기능만)

| 항목 | 내용 |
|---|---|
| 파이프라인 단계 | 재실행: 기존 파이프라인 전체(FETCHING~QUIZ). 재생성: `LlmStage.QUIZ` 1회(형식 오류면 F-14 AC4대로 1회 재시도) |
| 프롬프트 파일 | 기존 `prompts/quiz/<level>.md` (바꾸지 않음. AI 출력을 바꾸지 않으므로 EV-1 기준선과 무관) |
| 입력 | F-14 6.2절과 같음 (`analysisConceptService.findForQuiz`, `analysisService.findFiles`) |
| 출력 JSON 스키마 | 기존 `llm-schemas/quiz.json` |
| Mock fixture | 기존 `llm-fixtures/quiz.json`. 실패 경로는 테스트에서 `FakeLlmClient`·`@MockitoBean QuizGenerator`로 만든다 (fixture 추가 없음) |
| 캐시·재사용 | 없음. 선택지 섞기 seed는 F-14 AC3 그대로(`analysisId * 31 + k`). 다시 풀 때 새로 섞기는 L-1b |

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1~AC3 | `analysis/AnalysisRerunApiTest#AC1_…`~`AC3_…` (새 행 열 복사, 원본 soft delete·자식 삭제, 이벤트로 파이프라인 시작은 `@MockitoBean AnalysisPipeline` 호출 확인, 한도 429 롤백, 404·409, `CountDownLatch` 동시 요청 2개) | MockMvc 통합 |
| AC4 | `analysis/AnalysisQuizPipelineTest`에 `quiz_status` READY·FAILED 기대값 추가, `AnalysisControllerTest`(상태)·`AnalysisResultApiTest`(상세)에 `quizStatus`·`quizRegenerationsLeft` | 통합 |
| AC4 (V14 채우기) | `db/V14QuizStatusBackfillTest`: 별도 스키마에 Flyway `target=13`으로 만들고 DONE+퀴즈·DONE·FAILED 행을 넣은 뒤 V14 적용 | 통합 (Testcontainers) |
| AC5, AC7 | `quiz/QuizRegenerateApiTest` (202·열 갱신·실행 후 GET 200·usage 로그, 409·429·404, 동시 요청 2개 → LLM 1회) | MockMvc 통합 |
| AC6, AC8 | `quiz/QuizRegenerationRunnerTest` (빈 결과·저장 예외·실행기 거절 → FAILED, 삭제·탈퇴 뒤 저장 0행), `analysis/AnalysisRecoveryTest`에 GENERATING → FAILED | 통합 |
| AC9 | `src/views/__tests__/AnalysisDetailView.spec.js` (URL·ZIP 분기, 재실행 → `router.replace`·폴링 재시작, 429 메시지, 요청 중 비활성) | Vitest |
| AC10 | `src/components/analysis/__tests__/AnalysisQuizTab.spec.js` (FAILED·남은 횟수·0회, 재생성 → GENERATING 폴링 → READY 로드·FAILED 복귀, unmount 시 폴링 정지, 에러 메시지) | Vitest |
| API 계약 | `OpenApiExportTest` → `docs/api/openapi.json`, contract-checker | 통합 |

## 8. 범위 밖

- ZIP 원본 보관과 ZIP 재실행 (결정로그 11-26: 하지 않음)
- 실패하지 않은 분석(`DONE`)의 재분석·최신 커밋 재분석 (P-1a·P-1b 프로젝트·버전)
- 퀴즈가 있는 분석의 퀴즈 교체·문항 추가, 다시 풀 때 선지 새로 섞기 (L-1b)
- 실행 흐름(F-07)·개요·파일 설명 단계별 재생성 (흐름은 이미 "다시 만들기"가 있다)
- 내 분석 목록(SC-06)에서 바로 재실행 (상세 화면에서만)
- 재생성 횟수를 일일 분석 한도에 합치기, 서버 재시작으로 실패한 재생성의 횟수 환불
- 퀴즈 재생성 완료 알림(피드·Mattermost)

## 9. 결정 필요

- 없음 (11-26 사용자 선택: 재실행은 새 분석 + 실패 행 삭제, 재생성은 비동기 + `quiz_status`, 분석당 3회)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [ ] 브랜치 `feature/L-1a-rerun-quiz-regen`
- [ ] Flyway `V14__analysis_quiz_status.sql`
- [ ] AC별 실패 테스트 작성 (red 확인)
- [ ] BE 구현 (green): `AnalysisService.rerun`, `QuizRegenerationService`·런처(`AnalysisExecutorConfig.EXECUTOR`), `QuizSaveService` 상태 기록, `AnalysisRecovery`, `ErrorCode` ANALYSIS_008·QUIZ_409·QUIZ_429, `quiz.max-regenerations: 3`
- [ ] FE 구현 (green)
- [ ] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [ ] contract-checker 통과
- [ ] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일). 로그인이 필요한 FAILED·퀴즈 없음 화면은 연결된 Chrome으로 확인
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영: 화면 설계서 SC-07(재실행·재생성), ERD `analyses` 두 열, 요구사항 정의서 F-14 행(생성 실패 시 재생성)
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
