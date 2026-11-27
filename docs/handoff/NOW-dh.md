# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 23:36 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/K-1-card-levels

## 현재 기능

**K-1 카테고리 12종·카드 수준별 요약·본문·매칭 버그** `docs/specs/K-1-card-levels.md` (📝 DRAFT, AC1~13) · 브랜치 `feature/K-1-card-levels` · `/implement K-1` 대기

## 진행

- ✅ P-1a PR #258 머지 (11/27 23:19 KST = 로드맵 날짜와 같아 재조정 없음), #257 닫힘
- ✅ K-1 spec 초안. 질문 4개 결정(모두 추천안): 열 6개 + 상세 `levels` 3개·`viewerLevel`, 관리자 `levels` 생략 시 유지, 12종 밖 카테고리는 별칭 정규화 → WARN 후 버림, 내 수준 목록 정렬은 K-2로
- ✅ 같은 변경: 로드맵·WBS K-2 행, 기획서 6.1절 카드 수준 맞춤, 결정로그 11-27 행 (미커밋)

## 바로 다음 할 일

1. ✅ spec 승인 커밋·push 완료 (spec·로드맵·WBS·기획서·결정로그)
2. `/clear` 후 `/implement K-1` (Opus/high). 순서: `V16__concept_levels.sql` → `ConceptCategoriesTest`(12종·별칭) → `ConceptResponseParserTest#AC7~AC10` → `ConceptLinkServiceTest` → 상세·관리자 API → 스키마·프롬프트·fixture → 시드 도구(`scripts/lib/seed/card.mjs` 선택 절) → FE `categoryLabel` 4곳
3. 머지는 EV-1 기준선(12/01) 뒤, 12/02 B 넘겨주기 전. PR에 `Closes #163`·`Closes #204`, @JoonseobKim

## 막힌 점 / 결정 필요
- 없음
- 사람: 노션 초대, F-05b liveTest + #226 실제 LLM 점검(12/08 전), #251 js PC `--move-env`

## 주의
- 다음 마이그레이션 V16 (develop V15까지). 시드 85장은 수준 칸 NULL로 두고 내용은 K-3c
- K-1은 공통·B 파일(`concept` 패키지, `R__seed_concepts.sql`, FE 카드·커뮤니티 컴포넌트)을 고친다 → B 화면은 라벨만 바꾸고 Tailwind 이전은 하지 않는다
