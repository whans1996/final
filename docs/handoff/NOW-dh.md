# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-28 00:08 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/K-1-card-levels

## 현재 기능

**K-1 카테고리 12종·카드 수준별 요약·본문·매칭 버그** `docs/specs/K-1-card-levels.md` (APPROVED, AC1~13) · 브랜치 `feature/K-1-card-levels` · `/implement K-1` 진행 중

## 진행

- ✅ 구현 (미커밋→커밋 중): V16, `ConceptCategories` 12종·별칭, 파서(levelSummaries·별칭·괄호 안 키·HIDDEN 버림), `ConceptLinkService`(UNIQUE로 HIDDEN 닿으면 연결 안 함), 상세 `viewerLevel`·`levels`, 관리자 `levels`(생략 시 유지·중복 400), 스키마 enum·프롬프트 3개·fixture, 시드 도구 선택 절 6개 + R__ 재생성, FE `categoryLabel` 4곳·12종
- ✅ 테스트: BE 개념 관련 218개 green(AC11 상세 테스트는 main fixture 직접 읽게 수정), FE 193개 green, 시드 node:test 37개 green
- ✅ 문서: F-08 AC5·5.4절, F-09·F-13·K-3a, 03 ERD V16, 01 K-1 행, 02·06·09, 기획서 ERD·F-13, datasets.json DS-05

## 바로 다음 할 일

1. `node scripts/verify.mjs be` 전체 결과 확인 → `docs/api/openapi.json` 갱신분 포함해 커밋·push
2. contract-checker(상세·관리자 levels) → ui-verifier(카드 목록·상세·커뮤니티 첨부, 라벨) → `/pr-review`
3. PR 본문 `Roadmap: K-1`, `Closes #163`, `Closes #204`, @JoonseobKim (공통: concept 패키지·UserService.findActiveLevel·FE 카드·커뮤니티 컴포넌트·conceptCategories.js). 머지는 EV-1 기준선(12/01) 뒤

## 막힌 점 / 결정 필요
- 없음
- 사람: 노션 초대, F-05b liveTest + #226 실제 LLM 점검(12/08 전), #251 js PC `--move-env`

## 주의
- 테스트 리소스 `llm-fixtures/concept.json`(빈 목록)이 클래스패스에서 main fixture를 가린다 → main fixture는 파일 경로로 읽는다
- B 화면은 라벨만 바꿨다 (Tailwind 이전은 CD-1)
