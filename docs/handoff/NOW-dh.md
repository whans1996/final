# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-28 00:35 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/K-1-card-levels

## 현재 기능

**K-1 카테고리 12종·카드 수준별 요약·본문·매칭 버그** `docs/specs/K-1-card-levels.md` (APPROVED, AC1~13) · 브랜치 `feature/K-1-card-levels` · 구현 끝, `/pr-review` 단계

## 진행

- ✅ AC1~AC13 구현·커밋·push (499be2d BE, 6f26232 시드 도구, 9f03d93 FE, d483418 문서, bec0e6b 계약 점검 반영)
- ✅ verify: BE 전체(백필 테스트 2개는 `repeatableSqlMigrationPrefix("SKIP_REPEATABLE")`로 R__ 제외), FE lint·test·build, 하네스 347개 green
- ✅ contract-checker: 중간 1(관리자 FE JSDoc levels)·낮음 1(응답 levels minItems·maxItems 3) 반영, 응답 category enum은 spec 요구 아님 → 유지
- 🔨 ui-verifier(카드 목록·상세 라벨, 8080은 이전 BE가 떠 있어 FE만 점검)·reviewer 결과 대기

## 바로 다음 할 일

1. reviewer 지적 반영(파서 `NameIndex`·`ConceptLinkService.findHiddenIds`·`AdminConceptService.ensureDistinctLevels` 중심) → verify → 커밋·push
2. PR 생성: base develop, 본문 `Roadmap: K-1`, `Closes #163`, `Closes #204`, 공통 코드 @JoonseobKim (concept 패키지·`UserService.findActiveLevel`·FE `ConceptCard`·`ConceptDetailView`·`PostAttachment`·`AttachmentPicker`·`conceptCategories.js`). 스크린샷은 ui-verifier 결과로
3. CI·Codex 루프(`node scripts/codex-review.mjs <PR> --request --wait`) → COMPLETE. 머지는 사람이 EV-1 기준선(12/01) 뒤

## 막힌 점 / 결정 필요
- 8080 BE(이전 빌드, PID 31640)를 내가 끌 권한이 없어 상세 `levels` 화면 확인은 BE 재시작 후 가능 (화면은 CD-1이라 필수 아님)
- 사람: 노션 초대, F-05b liveTest + #226 실제 LLM 점검(12/08 전), #251 js PC `--move-env`

## 주의
- 테스트 리소스 `llm-fixtures/concept.json`(빈 목록)이 main fixture를 가린다 → 파일 경로로 읽는다. B 화면은 라벨만 바꿨다(Tailwind는 CD-1)
