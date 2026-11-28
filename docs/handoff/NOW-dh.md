# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-28 18:32 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/I-1a-interview-prep

## 현재 기능

**I-1a 면접 준비 예상 질문·근거·핵심 포인트** `docs/specs/I-1a-interview-prep.md` (**APPROVED** 11-28) · 브랜치 `feature/I-1a-interview-prep` (spec 승인 커밋 push됨)

## 진행

- ✅ K-1 PR #261 머지 11/28 09:08 KST (로드맵 날짜와 같아 재조정 없음, #163·#204 닫힘)
- ✅ I-1a spec 초안 AC1~AC15 (사용자 결정 4개: 요청 시 비동기 생성, NOT_MINE은 T-1로, BE + SC-07 읽기 전용 탭, DONE 다시 만들기 분석당 2회)
- ✅ spec 승인 커밋에 로드맵 I-1a·T-1 행, WBS·간트, 결정로그 11-28 반영

## 바로 다음 할 일

1. (완료) spec 승인 커밋·push
2. `/clear` → `/implement I-1a`: `node scripts/next-migration.mjs`로 V17 확인 → `V17__interview_questions.sql` → `InterviewPromptsTest`·`InterviewResponseParserTest`(AC2·AC3) red부터
3. 그다음 순서: L-1b → K-3b → SD-1(12/03) → P-1b (L-1b `/spec`은 develop에서 새 브랜치)

## 막힌 점 / 결정 필요
- 8080에 이전 빌드 BE가 떠 있다(내가 끌 수 없음). V16·V17을 쓰려면 사람이 재시작
- 사람: 노션 초대, F-05b liveTest + #226 실제 LLM 점검(12/08 전), #251 js PC `--move-env`

## 주의
- 테스트 리소스 `llm-fixtures/*.json`이 main fixture를 가린다 → main fixture 검증 테스트는 파일 경로로 읽는다
- I-1a mock fixture 근거는 fileNo 1~2, L1~L3 안에서만 (AC3이 근거 없는 질문을 버림)
