# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 15:59 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-08-concept-extraction

## 현재 기능

F-08 개념 카드 추출·연결 — `docs/specs/F-08-concept-extraction.md` **APPROVED (11-19)**, 구현 시작 전. PR #43(유스케이스)·#42(F-00b)는 11-19 머지됨

## 진행

- ✅ spec 초안 AC1~AC10: EXTRACTING(70) 1회 호출, 기존 카드 `conceptId` 연결(15개), 없는 개념 AI 초안(요약+본문, 5개), 실패면 연결 없이 DONE, `GET /api/analyses/{id}/concepts`(PUBLISHED만 + pendingCount), SC-07 개념 탭, #23 구분자 `<<<`→`‹‹‹` 흡수. Flyway 없음
- ✅ 결정로그 1줄 추가 (F-08 결정)

## 바로 다음 할 일

1. `/clear` 후 새 세션(Opus/high)에서 `/implement F-08` (브랜치 `feature/F-08-concept-extraction` 이미 push됨, Flyway 없음): 첫 red는 `AnalysisExtractingPipelineTest#AC1_EXPLAINING_뒤에_EXTRACTING을_거쳐_DONE이_된다`, `ConceptResponseParserTest`

## 막힌 점 / 결정 필요

- ui 점검 계정 `ui-admin@vibe.dev`/`vibe1234`(로컬 DB에만, ADMIN). storageState `.harness/ui-admin.json`
- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부, 김준섭에게 F-00b 머지 알림(Mattermost)

## 주의

- B 파일(`ConceptService`·`ConceptController`·`api/concepts.js`·`routes/concept.js`)과 공용 `ConceptCard.vue`는 고치지 않는다, `ConceptMapper`는 F-08 블록에만. Gradle 테스트를 bootRun과 동시에 돌리지 않는다. V1~V5 수정 금지
- Git Bash heredoc·sed·python stdin에서 한글·`\n`·`\|`가 깨진다 → 한글·표(`|`)가 든 수정은 Edit 도구로 쓴다
