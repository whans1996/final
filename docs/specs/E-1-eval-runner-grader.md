# E-1 평가 러너 전 단계 지원 + DS-04 채점기 + 수준 구분 지표

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | E-1 (로드맵 트랙 1, 11/26~11/29, 11/29 B에 넘김 → EV-1 기준선) |
| 우선순위 | T0 (기획서 7.4b) |
| 영역 오너 | A 김동환 (AI·분석·학습 루프) |
| 관련 필수 요구사항 | NF-04 (AI 품질 평가), 산출물 08·09 |
| 의존 spec | F-06(개요·파일), F-07(흐름), F-08(개념), F-14(퀴즈), F-27(공급자 하나만 부르는 러너, AC13) |
| 관련 이슈 | #107 (평가 체계 아이디어 → 2차 점검에서 E-1·V-2로 배치) |
| 승인 | 김동환 2026-11-26 |

## 1. 목적

기준선(EV-1, 12/01)과 v2(EV-2, 12/12)를 숫자로 내려면 러너가 분석 전 단계(개요·파일·흐름·개념·퀴즈)를 앱과 같은 경로로 돌리고, 채점기가 DS-04 라벨과 대조해 지표 A~E·커버리지·근거 유효율을 계산해야 한다. 지금 러너는 개요·파일만 돌고 채점기는 없다 (`docs/점검/2026-11-26-2차점검.md` 5절).
**블라인드 분업**(점검 2절 규칙 6): A는 러너·채점기 코드만 만들고 dev 데이터(DS-02-01)로 만든 **합성 라벨**로 테스트한다. eval 실행·채점은 B가 하고 점수만 공유한다. 이 spec과 테스트에는 eval 저장소·라벨 내용이 들어가지 않는다.

지표 (11-25 결정로그 검증 행, 기획서 6.1절 v2 합격선):

| 지표 | 이번 E-1에서 계산하는 것 | v2 절대 하한 |
|---|---|---|
| A 스키마 적합률 | 단위(LLM 호출 묶음)별 첫 시도 통과율·최종 통과율 | 최종 100% |
| B 환각률 | 기계 판정: 없는 파일 경로, 그 파일에 없는 심볼, 라벨 금지 사실 패턴 | - |
| C 개념 재현율·정밀도 | must 재현율, (must+ok) 정밀도, wrong 비율, must 근거 파일 일치율 | - |
| D 수준 구분도 | 세 수준 출력을 섞어 판정 LLM(Gemini)이 수준을 맞힌 비율 (우연 33%) | 70% |
| E 퀴즈 품질 | 기계 검사(보기 4개 서로 다름, 정답 위치, 해설의 선지 번호·위치 언급) + 판정 LLM의 정답 유일률 | 정답 유일 100% |
| 커버리지 | 라벨 파일 선별률(전체·FE), API 탐지 재현율·정밀도 | - |
| 근거 유효율 | 개념 `lineHint`가 파일 줄 수 안, 흐름 단계 경로·심볼이 실제로 있음 | 90% |

## 2. 수용 기준 (Acceptance Criteria)

> 러너·채점기는 `backend/src/test/java/com/vibenote/prompteval/`에 둔다. 실제 LLM을 부르는 실행(`promptEval`·`promptGrade`)은 `@Tag("prompt-eval")`로 `test`에서 빠진다. 아래 테스트는 전부 `test`에서 **mock LLM·코드로 만든 작은 ZIP·합성 라벨**로 돈다.

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 개요가 API를 탐지한 fixture로 | `-Pstage=flow`(기본 `-Pflows=3`)로 돌리면 | 수준마다 탐지 순서 앞 N개 API의 흐름을 앱과 같은 프롬프트·파서(`FlowGenerator`에서 DB 없이 부를 수 있게 분리한 메서드)로 만들어 `results.<level>.flows[]`(`endpoint`·`summary`·`steps`·`error`)에 남긴다. API가 0개면 `flows: []`와 `skipped: "NO_API"` |
| AC2 | 시드 카드를 적용한 DB(Testcontainers + Flyway, 앱과 같은 카탈로그)로 | `-Pstage=concept` 또는 `quiz`로 돌리면 | `ConceptExtractor`로 뽑은 `concepts[]`(`name`·`conceptId`(초안이면 null)·`draft`·`category`·`path`·`lineHint`·`contextNote`)를 남기고, `quiz`는 그 결과로 연결 개념을 만들어(초안은 카탈로그 최대 id 뒤 임시 id) `QuizGenerator`를 불러 `quiz[]`(`conceptName`·`question`·`choices`·`answerIndex`·`explanation`)를 남긴다 |
| AC3 | 아무 fixture로 | `-Pstage=all`로 돌리면 | 수준마다 개요 → 파일(`-Pbatches`, 기본 전부) → 흐름 → 개념 → 퀴즈를 한 보고서에 남긴다. 호출 전에 예상 호출 수를 출력한다. 한 단계가 실패하면 그 결과에 의존하는 단계만 `skipped: "<앞 단계> 실패"`로 남기고 나머지는 계속한다 (개요 실패 → 파일·흐름 건너뜀, 개념 실패 → 퀴즈 건너뜀) |
| AC4 | 러너가 만든 보고서에는 | 채점에 필요한 메타가 | `fixtureZip`(경로)·`provider`·`model`·`split`·`contamination`·`index[]`(`path`·`layer`·`selected`·`lines`)·`units[]`(`stage`·`level`·`key`·`calls`·`firstPass`·`ok`)가 있다. `key`는 같은 입력을 수준 사이에 짝짓는 값이다 (개요 `overview`, 파일 경로, 흐름 `METHOD path`) |
| AC5 | 보고서를 | `promptGrade -Preport=<파일>`로 채점하면 | A: 단계별 `firstPass`·`ok` 비율. B: 출력이 가리킨 경로(`readingOrder`·흐름 단계·개념 `path`)가 `index`에 없는 비율, 흐름 `symbol`(클래스.메서드)의 메서드 이름이 그 파일 본문에 없는 비율. 근거 유효율: 개념 `lineHint` 범위가 파일 줄 수 안이고 흐름 단계 경로·심볼이 있는 비율. 라벨 없이도 이 셋은 계산된다 |
| AC6 | 합성 라벨이 있는 보고서를 | `-Plabels=<폴더>`(파일 이름 `<fixture id>.json`)로 채점하면 | C: must 재현율, (must+ok) 정밀도, wrong 비율, must 근거 파일 일치율. 커버리지: 라벨 파일 중 선별된 비율(전체·FE layer), API 탐지 재현율·정밀도. B에 라벨 금지 사실 패턴이 출력 텍스트에 나온 항목 수를 더한다. 개념 이름은 대소문자·공백·`@`를 무시해 비교한다. 라벨이 없는 fixture는 C·커버리지를 `"라벨 없음"`으로 둔다 |
| AC7 | 세 수준이 모두 있는 보고서를 | 판정을 켜고(`-Pjudge=gemini`, 기본) 채점하면 | 같은 `key`의 세 수준 출력(개요, 파일 설명, 흐름)을 `-Pseed`(기본 고정값)로 섞어 Gemini에 수준을 묻고 D = 맞힌 출력 수 / 판정한 출력 수를 낸다. 판정 응답이 형식에 맞지 않으면 1회 다시 묻고, 또 틀리면 그 묶음을 빼고 뺀 수를 표시한다. 같은 seed면 섞는 순서가 같다 |
| AC8 | 퀴즈가 있는 보고서를 | 채점하면 | E 기계 검사: 보기 4개가 서로 다름, `answerIndex` 0~3, 해설에 선지 번호·위치 언급("2번", "첫 번째 보기", "A)", "위의 보기" 등 정규식) 없음의 문항 비율. 판정을 켜면 Gemini가 정답으로 볼 수 있는 보기를 모두 고르게 해 **정확히 하나이고 `answerIndex`와 같은** 문항 비율(정답 유일률)을 더한다 |
| AC9 | Gemini 키가 없거나 `-Pjudge=none`이면 | 채점하면 | D와 E 정답 유일률만 `"판정 건너뜀"`이고 나머지 지표는 그대로 계산된다 (LLM을 부르지 않는다) |
| AC10 | 채점 결과 여러 개를 | `-Preport=<폴더>`로 채점하면 | 공급자 × 오염 그룹(팀 생성 `none` / `low` / `high`)으로 묶은 요약 Markdown(`summary.md`)과 v2 하한 대비 표시(✅/❌)를 낸다. **요약과 콘솔에는 점수·개수만** 쓰고 라벨의 개념 이름·파일 경로·금지 사실은 쓰지 않는다. 항목별 세부는 `.harness/prompt-eval/`(gitignore)의 `*-grade.json`에만 남는다 |
| AC11 | 채점기 테스트의 합성 라벨·보고서 fixture는 | `split`을 확인하면 | 모두 `data/datasets.json`에서 `split: dev`인 저장소 id(DS-02-01)에서 만든 것이다. 테스트가 이를 검사한다 (eval id를 쓰면 실패) |

## 3. API 계약

없음 (테스트 소스의 수동 실행 도구. 앱 API·`openapi.json` 변경 없음).

명령 (B가 eval로, A는 dev로만 실행):

```
# 러너 (공급자 하나만, F-27 AC13)
LLM_PROVIDER=gms ./gradlew promptEval -Pstage=all -Plevel=all -Pfixture=<id|zip> [-Pbatches=N] [-Pflows=3] [-PpauseMs=4000]
# 채점 (보고서 하나 또는 폴더)
./gradlew promptGrade -Preport=../.harness/prompt-eval/<파일|폴더> [-Plabels=<폴더>] [-Pjudge=gemini|none] [-Pseed=42]
```

채점 결과 예시 (`*-grade.json`의 `scores`, 합성 값):

```json
{
  "fixture": "DS-02-01", "provider": "gms", "model": "gpt-4.1-mini", "split": "dev", "contamination": "high",
  "A": { "overview": { "firstPass": 1.0, "ok": 1.0 }, "file": { "firstPass": 0.83, "ok": 1.0 }, "flow": { "firstPass": 1.0, "ok": 1.0 }, "concept": { "firstPass": 1.0, "ok": 1.0 }, "quiz": { "firstPass": 0.67, "ok": 1.0 } },
  "B": { "unknownPath": 0.0, "unknownSymbol": 0.08, "forbiddenHits": 1 },
  "C": { "mustRecall": 0.71, "precision": 0.8, "wrongRate": 0.05, "mustEvidenceMatch": 0.6 },
  "D": { "accuracy": 0.58, "judged": 27, "excluded": 0, "judge": "gemini-2.5-flash" },
  "E": { "mechanical": 0.92, "uniqueAnswer": 0.88, "questions": 15 },
  "coverage": { "labelFiles": 0.75, "labelFeFiles": 0.5, "apiRecall": 1.0, "apiPrecision": 0.95 },
  "evidence": { "valid": 0.86, "checked": 42 }
}
```

라벨 형식 (김준섭 `LABEL_SCHEMA.md`의 필드 구조, 11-26 받음. 값은 합성 예시. `source`·`labeledBy`·`labeledAt`·`why`·`new`·`notes`는 채점에 쓰지 않는다):

```json
{
  "id": "DS-02-01",
  "source": { "commit": "<고정 SHA>", "paths": ["/"] },
  "labeledBy": "…", "labeledAt": "YYYY-MM-DD",
  "files": [{ "path": "src/main/java/io/spring/api/ArticlesApi.java", "layer": "controller", "role": "게시글 목록·작성 API" }],
  "apis": [{ "method": "POST", "path": "/articles" }],
  "concepts": [{ "name": "@Transactional", "tier": "must", "evidence": ["src/main/java/io/spring/application/article/ArticleCommandService.java"], "why": "…", "new": false }],
  "forbidden": ["JPA"],
  "notes": "…"
}
```

- `layer`: controller | service | mapper | domain | config | view | component | store | api | router | util | other. FE 커버리지는 view·component·store·api·router (앱 index의 vue·js도 FE로 본다)
- `forbidden[]`: 문자열마다 출력 텍스트(경로·layer·메타 제외)에 대소문자 무시 부분 일치로 나오면 1건 (수준마다 센다)
- `evidence`: `tier=must`이면 1개 이상. must 근거 파일 일치율은 evidence가 있는 must만 센다

## 4. DB 변경

- Flyway: 없음
- 사용 테이블: `concepts` (읽기만, Testcontainers DB의 시드 카탈로그. AC2)

## 5. 프론트엔드

없음.

## 6. LLM (AI 기능만)

| 항목 | 내용 |
|---|---|
| 파이프라인 단계 | 러너: overview·file·flow·concept·quiz (앱 프롬프트·파서 그대로). 채점기: 판정 2종(수준 맞히기, 정답 고르기) |
| 프롬프트 파일 | 앱: 변경 없음. 판정: `backend/src/test/resources/prompt-eval/judge/level.md`·`answer.md` (수준 기준표는 기획서 6.1절 문장 그대로, 라벨·eval 내용 없음) |
| 입력 | 수준 판정: 같은 key의 출력 3개(섞음) + 수준 기준표. 정답 판정: 문항·보기·해설·개념 이름 (정답 위치는 주지 않음) |
| 출력 JSON | 수준: `{"items":[{"id":"x","level":"beginner"}]}` · 정답: `{"correct":[0]}` |
| 판정 모델 | Gemini (`LiveLlmClients.gemini`, 생성 모델 GMS·Ollama와 다른 계열이라 자기 선호 편향을 줄인다). 앱 `LlmStage`에 판정 단계를 넣지 않는다 |
| Mock fixture | 테스트는 판정 응답을 가짜 `LlmClient`로 준다 (앱 `llm-fixtures/` 변경 없음) |
| 비용 | 저장소 하나 `all` × 3수준 ≈ 생성 3×(1+파일 묶음+3+1+1) + 판정 (key 수 + 1) 회. 실행 전 예상 호출 수를 출력한다 |

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1 | `EvalPipelineTest#AC1_흐름_단계는_탐지한_API_앞_N개를_수준마다_만든다`, `#AC1_API가_없으면_흐름을_건너뛴다`, `FlowGeneratorTest`(기존, 분리 후에도 green) | 단위 (mock LLM, 코드로 만든 ZIP) |
| AC2 | `EvalPipelineCatalogTest#AC2_개념과_퀴즈를_시드_카탈로그로_만든다` | `IntegrationTestBase` (Testcontainers) |
| AC3 | `EvalPipelineTest#AC3_all은_다섯_단계를_한_보고서에_남긴다`, `#AC3_개요가_실패하면_의존_단계만_건너뛴다` | 단위 |
| AC4 | `EvalPipelineTest#AC4_보고서에_index와_units가_있다` | 단위 |
| AC5 | `EvalGraderTest#AC5_없는_경로와_심볼을_환각으로_센다`, `#AC5_lineHint가_줄_수를_넘으면_근거_무효` | 단위 |
| AC6 | `EvalGraderTest#AC6_must_재현율과_정밀도를_계산한다`, `#AC6_라벨이_없으면_C와_커버리지는_라벨_없음` | 단위 (합성 라벨) |
| AC7 | `LevelJudgeTest#AC7_같은_seed면_섞는_순서가_같다`, `#AC7_판정_형식_오류는_한_번_더_묻고_빼고_센다` | 단위 (가짜 판정 클라이언트) |
| AC8 | `QuizCheckTest#AC8_해설의_선지_위치_언급을_잡는다`, `#AC8_정답_유일률은_하나만_고르고_위치가_같을_때` | 단위 |
| AC9 | `EvalGraderTest#AC9_판정을_끄면_D와_정답_유일률만_건너뛴다` | 단위 |
| AC10 | `EvalSummaryTest#AC10_공급자와_오염_그룹으로_묶는다`, `#AC10_요약에_라벨_내용이_나오지_않는다` | 단위 |
| AC11 | `EvalFixturesTest#AC11_테스트_fixture는_dev_분할에서만_만든다` | 단위 (`data/datasets.json` 읽기) |

## 8. 범위 밖

- D·E 기준표 1~5점(LLM 판정)과 v1/v2 블라인드 쌍 비교 → E-2 (12/09 전, EV-2용. 로드맵 추가 필요)
- 판정 LLM으로 사실성(B)을 문장 단위로 판정하기 (이번은 기계 판정 + 라벨 금지 사실 패턴만)
- 근거 `파일:줄` 필드 자체의 생성·검증 (F-21b). 이번 근거 유효율은 지금 출력에 있는 `lineHint`·흐름 경로·심볼만 본다
- 프롬프트 수정 (F-27b, EV-1 뒤), eval 실행·점수 공유 (B, EV-1)
- 추천(recommend) 단계 평가
- `prompt-evaluator` 에이전트의 판단 로직 변경 (지원 단계·명령 안내만 고친다)

## 9. 결정 필요

1. ~~라벨 형식~~ → 11-26 김준섭에게 `LABEL_SCHEMA.md` 필드 구조를 받아 3절에 맞췄다 (`id`·`tier`·`forbidden: string[]`). 남은 확인: `forbidden` 문자열이 출력에 그대로 나올 표현(패턴)인지 사실 문장인지. 채점기는 부분 일치 패턴으로 센다

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/E-1-eval-runner`
- [ ] Flyway 마이그레이션 (없음)
- [x] `FlowGenerator`에서 DB 없이 부르는 메서드 분리 (`create(FlowSource, ApiEndpoint, files)`, F-07 테스트 green 유지)
- [x] `PromptEvalRunner` 핵심을 `EvalPipeline`으로 옮기고 flow·concept·quiz·all 추가 (AC1~AC4). 파일 단위 `key`는 묶음의 경로를 `,`로 이은 값, 러너는 시드 카탈로그 때문에 `IntegrationTestBase` 상속(Docker 필요)
- [x] 채점기 `EvalGrader`·`LevelJudge`·`QuizCheck`·`EvalSummary` + `promptGrade` Gradle 작업 (AC5~AC10). 실행은 `PromptGradeRunner`, 판정 프롬프트 `src/test/resources/prompt-eval/judge/`
- [x] 합성 라벨 fixture (DS-02-01, AC11). `src/test/resources/prompt-eval/synthetic/`에 둔다 (평가 종료 뒤 실제 라벨이 `prompt-eval/`로 옮겨 와도 AC11 검사 범위와 섞이지 않게)
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] BE 구현 (green)
- [x] `prompt-evaluator` 에이전트(지원 단계·`promptGrade`), `docs/산출물/09-데이터셋.md` DS-04 "사용 기능" 갱신
- [ ] dev 실호출 1회 확인 (사람 요청 시, DS-02-01 `all` × GMS, 판정 Gemini)
- [ ] reviewer 지적 사항 반영
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
