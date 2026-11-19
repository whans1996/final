# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 16:22 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-08-concept-extraction

## 현재 기능

F-08 개념 카드 추출·연결 — `docs/specs/F-08-concept-extraction.md` APPROVED, `/implement` 진행 중. 이번 세션 목표: BE AC1~AC7·AC10 → FE AC8·AC9

## 진행

- ✅ AC1~AC6·AC10 (BE): `analysis/concept/` ConceptPrompts·ConceptResponseParser·ConceptExtractor·ConceptLinkService·AnalysisConceptMapper, 파이프라인 EXTRACTING(70), 프롬프트 3종, fixture `concept.json`(main: 시드 id 39·18·42·62·57 + GraphQL 초안, test: 빈 배열), #23 `ExplainPrompts.escapeDelimiters`
- ✅ 공통 파일: `concept/ConceptDraftService`(새 파일, 분석 도메인이 ConceptMapper를 직접 부르지 않게), `ConceptMapper` F-08 블록(`findAllForMatching`·`insertDraft`) → PR에 @JoonseobKim 멘션
- ✅ AC7 `GET /api/analyses/{id}/concepts` (`AnalysisConceptService`·DTO 2개·`AnalysisController.listConcepts`, openapi.json 갱신)
- ✅ FE AC8·AC9: `api/analyses.js` `fetchAnalysisConcepts`, `components/analysis/AnalysisConceptsTab.vue`, `AnalysisDetailView` TABS에 개념(실행 흐름 다음)
- ✅ contract-checker(AnalysisConceptItem nullable 4개 반영), 산출물(02 UC24·03 ERD·06 SC-07 개념 탭·08 F-08 행·09·datasets.json DS-06)

## 바로 다음 할 일

1. ui-verifier: 사람이 ui-admin으로 다시 로그인해 `.harness/ui-admin.json` 갱신(refresh 401로 만료) + mock 분석 1건 요청 → `/analyses/{id}?tab=concepts` 데스크톱·375px 점검, 스크린샷 `SC-07-concepts-*` 06에 추가 (AC10 확인)
2. `/pr-review` (본문 `Closes #23`, 공통 파일 ConceptDraftService·ConceptMapper → @JoonseobKim)

## 막힌 점 / 결정 필요

- red를 따로 확인하지 않고 테스트와 구현을 함께 썼다 (AC1~AC6). AC7은 red(404) 확인 후 구현
- 이 세션에서 띄운 bootRun을 멈췄지만 8080이 계속 응답했다 (자바 프로세스가 남았을 수 있음, 프로세스 확인은 권한 거부). 사람이 확인 후 종료
- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부

## 주의

- B 파일(`ConceptService`·`ConceptController`·`api/concepts.js`·`routes/concept.js`)과 공용 `ConceptCard.vue`는 고치지 않는다. Gradle 테스트를 bootRun과 동시에 돌리지 않는다. V1~V5 수정 금지
- 테스트 카드는 `f08-test-` 이름, `@AfterEach`에서 지운다. test classpath의 `llm-fixtures/concept.json`은 빈 배열이라 다른 파이프라인 테스트가 카드를 만들지 않는다
- Git Bash heredoc·sed에서 한글·`\n`이 깨질 수 있다 → 한글 수정은 Edit 도구로
