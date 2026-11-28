# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-28 20:03 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/I-1a-interview-prep

## 현재 기능

**I-1a 면접 준비 예상 질문·근거·핵심 포인트** `docs/specs/I-1a-interview-prep.md` (APPROVED, 구현·화면 점검 끝, PR 전) · 브랜치 `feature/I-1a-interview-prep`

## 진행

- ✅ BE AC1~AC11·FE AC12~AC15 green, 산출물 03·06·01·09·datasets, contract-checker 0건
- ✅ ui-verifier 통과(2차): 모바일 근거 칩 → 강조 줄 스크롤 버그 수정(`CodeViewer` `scrollReady`), 스크린샷 5장·06 화면설계서 표 (230b304)
- spec 체크리스트에서 남은 것: reviewer, PR CI, 상태 DONE

## 바로 다음 할 일

1. `/clear` 뒤 `/pr-review` (reviewer·verifier·contract-checker → PR 본문 `Roadmap: I-1a`, 공통 파일 ErrorCode·ErrorCodeTest·FlywayMigrationTest·AnalysisRecovery·CODEOWNERS·latestRequestAdoption.spec·AnalysisDetailView 탭 → @JoonseobKim 멘션, #250 AnalysisFilesTab 칸 체크(`Closes` 안 씀))
2. 다음 항목: L-1b → K-3b → SD-1(12/03) → P-1b

## 막힌 점 / 결정 필요
- 사람: 노션 초대, F-05b liveTest + #226 실제 LLM 점검(12/08 전), #251 js PC `--move-env`

## 주의
- 점검용 회원 `i1a-check@vibenote.local`(로컬 DB만), 분석 id 18
- main fixture 검증 테스트는 파일 경로로 읽는다 (테스트 리소스 fixture가 가림)
