# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-28 19:44 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/I-1a-interview-prep

## 현재 기능

**I-1a 면접 준비 예상 질문·근거·핵심 포인트** `docs/specs/I-1a-interview-prep.md` (APPROVED, 구현 중) · 브랜치 `feature/I-1a-interview-prep`

## 진행

- ✅ BE AC1~AC11 green (commit 6ff7cd6): V17, `com.vibenote.interview`, ErrorCode INTERVIEW_409·429, `LlmStage.INTERVIEW`, fixture, openapi.json, conventions 3절, CODEOWNERS
- ✅ 산출물 03·06·01·09·datasets 반영 (3dd32d1), contract-checker 0건
- ✅ FE AC12~AC15 green: `api/interviews.js`, `utils/interviewFormat.js`, `components/interview/InterviewTab·InterviewQuestionCard`, SC-07 탭 `interview`, CodeViewer `highlight`·AnalysisFilesTab `highlight` (둘 다 Tailwind·useLatestRequest, ADOPTED 추가)

## 바로 다음 할 일

1. 사람이 8080 BE를 새 빌드로 재시작(`cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'`, V17 적용) + FE `npm run dev` → ui-verifier: SC-07 `?tab=interview` NONE→만들기→DONE(mock 8문항), 근거 칩 → 파일 탭 줄 강조, 1280·375px, 라이트·다크. 스크린샷 `docs/산출물/screens/SC-07-interview-*`·`SC-07-files-highlight-*`를 06 화면설계서 면접 준비 절 표에 넣는다
2. `/pr-review` (contract-checker 불일치 0건 확인됨. 공통 파일 ErrorCode·ErrorCodeTest·FlywayMigrationTest·AnalysisRecovery·CODEOWNERS·latestRequestAdoption.spec → @JoonseobKim 멘션, #250 AnalysisFilesTab 칸 체크, PR 본문 `Roadmap: I-1a`)

## 막힌 점 / 결정 필요
- 8080에 이전 빌드 BE가 떠 있다(내가 끌 수 없음). V16·V17을 쓰려면 사람이 재시작
- 사람: 노션 초대, F-05b liveTest + #226 실제 LLM 점검(12/08 전), #251 js PC `--move-env`

## 주의
- main fixture 검증 테스트는 파일 경로로 읽는다 (테스트 리소스 fixture가 가림)
- `AnalysisService.findFiles`는 importance를 안 읽음 → `InterviewMapper.findFiles` 사용
