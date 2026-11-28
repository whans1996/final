# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-28 22:18 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/I-1a-interview-prep

## 현재 기능

**I-1a 면접 준비 예상 질문·근거·핵심 포인트** `docs/specs/I-1a-interview-prep.md` (**DONE (PR #262)**, 머지 대기) · 브랜치 `feature/I-1a-interview-prep`

## 진행

- ✅ AC1~AC15 green, contract-checker 0건, ui-verifier 통과, reviewer 권장 5 + 무시 가능 1 반영 (96984b5·224f46c·4c2c2f6)
- ✅ Codex P1 1건(폴링 중 GET 실패 시 다시 시도, AC14) 반영 7937968 → **Codex ✅ 7937968** (재리뷰 2회), CI 8개 초록
- ✅ 완료 표시: spec DONE, 로드맵·WBS·간트 I-1a 11/28~11/28. 빈 11/29~12/02는 L-1b·K-3b·P-1b가 이미 겹쳐 있어 뒤 항목은 그대로

## 바로 다음 할 일

1. 사람이 GitHub에서 PR #262 머지 (머지 전 develop에 새 커밋이 있으면 `git merge origin/develop` → push → CI 초록 재확인). 머지일이 11/28이 아니면 다음 `/pickup`이 로드맵·WBS·간트·spec 날짜를 고친다
2. `/clear` → `/pickup` → 다음 A 항목 `/spec L-1b` (풀이 기록·오답 노트·선지 다시 섞기, 의존 L-1a ✅, 11/28~12/02). 그다음 K-3b → SD-1(12/03 A→B 넘겨주기) → P-1b

## 막힌 점 / 결정 필요
- 사람: 노션 초대, F-05b liveTest + #226 실제 LLM 점검(12/08 전), #251 js PC `--move-env`

## 주의
- 점검용 회원 `i1a-check@vibenote.local`(로컬 DB만), 분석 id 18
- `AnalysisService.lockOwnedDetail`은 `Propagation.MANDATORY` (호출자 트랜잭션 안에서만)
- 로컬 FE 전체 검증에서 `UserMenu.spec.js`(B, F-29b)가 부하 때 5초 타임아웃 1회. 단독·재실행·CI는 통과 (되풀이되면 이슈로)
