# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-28 09:03 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/K-1-card-levels

## 현재 기능

**K-1 카테고리 12종·카드 수준별 요약·본문·매칭 버그** `docs/specs/K-1-card-levels.md` (DONE) · 브랜치 `feature/K-1-card-levels` · PR #261 머지 가능 (사람이 오늘 11/28 머지)

## 진행

- ✅ AC1~AC13 구현, verify BE·FE·하네스 green, contract-checker·ui-verifier·reviewer 반영 (괄호 안 키끼리 비교 안 함·숨기지 않은 카드 우선, 결정로그 11-28, 사용자 승인)
- ✅ PR #261 (Roadmap: K-1, Closes #163·#204, @JoonseobKim) — CI 초록, Codex ✅ aecdcfe, MERGEABLE·CLEAN, develop 뒤처짐 0
- ✅ EV-1 기준선은 11/27 끝남(PR #240, 산출물 08 1.3절). spec의 "12/01"은 점수 공유일을 잘못 옮긴 것 → spec·로드맵·WBS·간트·08을 머지일 11/28로 고침

## 바로 다음 할 일

1. (사람) GitHub에서 PR #261 Merge. 머지일이 11/28이 아니면 다음 `/pickup`이 로드맵·WBS·간트 K-1 끝 날짜와 넘겨주기(11/28 A→B) 날짜를 고친다
2. 머지 뒤 `/clear` → `/pickup` → 로드맵 다음 항목(L-1b·I-1a·K-3b·P-1b 11/28~) `/spec`. 새 브랜치는 develop에서
3. B에게: K-1 API·openapi 넘겨주기가 11/28로 당겨짐 → CD-1(12/03~)은 B 판단으로 일찍 시작 가능 (B 행은 고치지 않음)

## 막힌 점 / 결정 필요
- 8080에 이전 빌드 BE가 떠 있다(내가 끌 수 없음). 새 V16을 쓰려면 사람이 재시작 (상세 levels 화면은 CD-1)
- 사람: 노션 초대, F-05b liveTest + #226 실제 LLM 점검(12/08 전), #251 js PC `--move-env`

## 주의
- 테스트 리소스 `llm-fixtures/concept.json`(빈 목록)이 main fixture를 가린다 → 파일 경로로 읽는다. B 화면은 라벨만 바꿨다(Tailwind는 CD-1)
