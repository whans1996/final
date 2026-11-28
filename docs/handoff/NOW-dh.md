# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-28 18:47 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/I-1a-interview-prep

## 현재 기능

**I-1a 면접 준비 예상 질문·근거·핵심 포인트** `docs/specs/I-1a-interview-prep.md` (APPROVED, 구현 중) · 브랜치 `feature/I-1a-interview-prep`

## 진행

- ✅ V17__interview_questions.sql (analysis_interviews·interview_questions)
- ✅ AC2 `InterviewPrompts`(순수 함수) · AC3·AC4(파서 부분) `InterviewResponseParser` — 테스트 green
- ✅ 프롬프트 `prompts/interview/{beginner,junior,senior}.md`, `llm-schemas/interview.json` (스테이지는 아직 없음)

## 바로 다음 할 일

1. AC1·AC4 생성: `LlmStage.INTERVIEW` 추가 → `llm-fixtures/interview.json`(질문 8개, 범주마다 2개, 근거 fileNo 1~2·L1~L3만, dev 데이터만) → `InterviewMapper`(+`mapper/interview/InterviewMapper.xml`: lockOwnedAnalysis·findFiles(importance 포함)·insert·complete·markFailed·failAllPending·startRegeneration·questions) → `InterviewGenerator`·`InterviewSaveService` → `InterviewGeneratorTest`(통합, `@MockitoBean LlmClient`) red부터
2. 그다음 `InterviewService`(AC5~AC7·AC10·AC11, `AnalysisService.deleteChildren`에 interviews 삭제, `AnalysisRecovery`에 failInterrupted) → `InterviewController`(AC8·AC9, ErrorCode INTERVIEW_409·429) → FE(AC12~AC15)
3. `application.yml`에 `interview.*` 설정, CODEOWNERS·conventions 3절·산출물은 마무리 때

## 막힌 점 / 결정 필요
- 8080에 이전 빌드 BE가 떠 있다(내가 끌 수 없음). V16·V17을 쓰려면 사람이 재시작
- 사람: 노션 초대, F-05b liveTest + #226 실제 LLM 점검(12/08 전), #251 js PC `--move-env`

## 주의
- 테스트 리소스 `llm-fixtures/*.json`이 main fixture를 가린다 → main fixture 검증 테스트는 파일 경로로 읽는다
- `AnalysisService.findFiles`는 importance를 읽지 않는다 → interview는 자기 Mapper로 선별 파일+importance를 읽는다
