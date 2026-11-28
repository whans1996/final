# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-28 19:17 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/I-1a-interview-prep

## 현재 기능

**I-1a 면접 준비 예상 질문·근거·핵심 포인트** `docs/specs/I-1a-interview-prep.md` (APPROVED, 구현 중) · 브랜치 `feature/I-1a-interview-prep`

## 진행

- ✅ BE AC1~AC11 green: V17, `com.vibenote.interview` (Prompts·Parser·Generator·SaveService·Service·Controller·Launcher), ErrorCode INTERVIEW_409·429, `LlmStage.INTERVIEW`, fixture `llm-fixtures/interview.json`, 스키마, AnalysisRecovery·AnalysisDeletedEvent 연결
- ✅ openapi.json (operationId requestInterview·getInterview), conventions 3절 INTERVIEW, CODEOWNERS interview 경로

## 바로 다음 할 일

1. FE: `src/api/interviews.js`(`fetchInterview(id,{signal})`·`requestInterview(id)`) + `api/__tests__/interviews.spec.js` → `utils/interviewFormat.js`(범주 라벨·`parseLines('12-30')`) → `components/interview/InterviewTab.vue`·`InterviewQuestionCard.vue`(Tailwind, useLatestRequest·usePolling 3초·120회) → `AnalysisDetailView.vue` 탭 `interview`(주인+DONE, 퀴즈 뒤)
2. 파일 탭 줄 강조(AC15): `CodeViewer.vue`에 `highlight` prop(첫 줄 scrollIntoView center)·`AnalysisFilesTab.vue`에 `lines` query 전달, 둘 다 Tailwind로 이전, 다른 파일 고르면 `lines` 지움 (`AnalysisDetailView.selectFile`)
3. 마무리: contract-checker·ui-verifier, 산출물(03 ERD·06 SC-07·01 I-1)·datasets DS-06·로드맵 T-1 행, `/pr-review` (공통 파일 ErrorCode·FlywayMigrationTest·AnalysisRecovery 변경 → @JoonseobKim 멘션)

## 막힌 점 / 결정 필요
- 8080에 이전 빌드 BE가 떠 있다(내가 끌 수 없음). V16·V17을 쓰려면 사람이 재시작
- 사람: 노션 초대, F-05b liveTest + #226 실제 LLM 점검(12/08 전), #251 js PC `--move-env`

## 주의
- main fixture 검증 테스트는 파일 경로로 읽는다 (테스트 리소스 fixture가 가림)
- `AnalysisService.findFiles`는 importance를 안 읽음 → `InterviewMapper.findFiles` 사용
