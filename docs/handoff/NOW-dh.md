# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 19:55 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/issue-49-mock-concept-names

## 현재 기능

follow-up #49 mock concept fixture가 시드 카드 id에 기대는 문제 — **구현·커밋 완료 `c69697e`, PR 전** (F-08 PR #50·F-10 PR #52는 머지됨, #23 닫힘)

## 진행

- ✅ `MockLlmClient.linkConceptNames`: CONCEPT 단계 fixture의 `conceptName`을 프롬프트 카드 목록(`id | 이름 | 카테고리`)에서 id로 바꿈, 목록에 없는 이름은 뺌
- ✅ `llm-fixtures/concept.json` 기존 카드 5개를 시드 이름으로 (DI, @RestController와 요청 매핑, Bean Validation, Mapper, JWT)
- ✅ 테스트: `MockLlmClientTest#issue49_*`, `MainFixturesTest#issue49_*`(id를 1000부터 역순으로 붙여도 의도한 카드), AC10은 실제 시드 이름 + `ConceptPrompts.input`으로 검사. red→green 확인, verify be 통과
- ✅ F-08 spec 186행에 #49 이후 방식 한 줄 추가

## 바로 다음 할 일

1. `/pr-review` → PR(`Closes #49`, develop 대상). FE 변경 없음, API 변경 없음
2. (선택) 로컬 DB로 분석을 돌려 개념 탭 카드·맥락 문장이 맞는지 확인하고 `docs/산출물/screens/SC-07-concepts-*.png` 다시 찍기 (기존 스크린샷은 어긋난 카드)
3. 머지 뒤 다음 기능: F-14 코드 이해 퀴즈 `/spec` (Opus/high)

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부

## 주의

- B 파일(`ConceptService`·`ConceptController`·`api/concepts.js`·`routes/concept.js`)과 공용 `ConceptCard.vue`, 시드 `R__seed_concepts.sql`은 고치지 않는다. Gradle 테스트를 bootRun과 동시에 돌리지 않는다. V1~V5 수정 금지
- Git Bash heredoc·sed에서 한글·`\n`이 깨질 수 있다 → 한글 수정은 Edit 도구로
