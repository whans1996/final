# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-28 00:57 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/K-1-card-levels

## 현재 기능

**K-1 카테고리 12종·카드 수준별 요약·본문·매칭 버그** `docs/specs/K-1-card-levels.md` (APPROVED, AC1~13) · 브랜치 `feature/K-1-card-levels` · PR #261 열림, 머지 대기 (EV-1 뒤)

## 진행

- ✅ AC1~AC13 구현, verify BE·FE·하네스 green, contract-checker·ui-verifier·reviewer 반영 (괄호 안 키끼리 비교 안 함·숨기지 않은 카드 우선, 결정로그 11-28)
- ✅ PR #261 (Roadmap: K-1, Closes #163·#204, @JoonseobKim) — CI 8개 초록, Codex ✅ aecdcfe, spec DONE 표시
- ⏸️ 머지 대기: EV-1 기준선(12/01) 측정 뒤 사람이 GitHub에서 머지

## 바로 다음 할 일

1. (사람) 12/01 EV-1 기준선 뒤 PR #261 머지. 상대 PR이 먼저 머지됐으면 `git merge origin/develop` → push → CI 재확인 (openapi.json·V16 번호)
2. 머지 전 다른 일: `/pickup` 후 로드맵 다음 항목(L-1b·I-1a·P-1b 11/28~) `/spec`. 새 브랜치는 develop에서 (K-1 미머지라 K-1 코드에 기대지 않는 항목)
3. 머지일이 12/02와 다르면 다음 `/pickup`이 로드맵·WBS·간트 K-1 끝 날짜를 고친다

## 막힌 점 / 결정 필요
- 8080에 이전 빌드 BE가 떠 있다(내가 끌 수 없음). 새 V16을 쓰려면 사람이 재시작 (상세 levels 화면은 CD-1)
- 사람: 노션 초대, F-05b liveTest + #226 실제 LLM 점검(12/08 전), #251 js PC `--move-env`

## 주의
- 테스트 리소스 `llm-fixtures/concept.json`(빈 목록)이 main fixture를 가린다 → 파일 경로로 읽는다. B 화면은 라벨만 바꿨다(Tailwind는 CD-1)
