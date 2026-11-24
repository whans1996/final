# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-24 16:18 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-21a-structure-graph

## 현재 기능

**F-21a 구조 시각화: 파일 의존 그래프** (P1) spec `docs/specs/F-21a-structure-graph.md` — APPROVED. PR #118 (**draft로 전환**, 구조 정리 중). 11-24 사용자 결정: Codex 지적을 하나씩 막지 않고 전수 점검 결과로 **구조를 바꿔 한 번에** + 캐시(#117) 흡수 → spec **5.3절**이 작업 목록, 결정로그 11-24 행

## 진행

- ✅ Codex 1~9차 지적 반영 (9차 3건 `a94d89e`: 경로 비교 메모리 P1, class 이름 가림, 객체 리터럴 뒤 나눗셈). 재리뷰 9회
- ✅ 전수 점검 4영역(이름 가림·토크나이저·계산량·HTTP 의미) 약 40건 → spec 5.3절 1~7로 정리
- ✅ 5.3-3 앞부분 `d22972e`: `WorkBudget`(2천만 단계·3초, 설정 max-work-steps·time), 소진 시 남은 파일 PARSE_ERROR·남은 HTTP 호출 URL_UNKNOWN·edgesTruncated, 파일별 RuntimeException·StackOverflowError → PARSE_ERROR. 테스트 4개(StructureAnalyzerTest 끝 "5.3절 3" 묶음)
- 🟡 5.3-3 뒷부분: 제곱 경로 색인화 → 메모리 항목

## 바로 다음 할 일

1. spec 5.3절 순서대로: 3 남은 것(색인: `JsModule.lookup` 이름별 범위 사슬·`importOf` 맵·`functionScopeOf` 스택·`expressionEnd` 메모·`HttpLinker.callSites` receiver 색인·`ApiPathMatcher` 분석당 1회 컴파일·Java `declaredAround`/`variableAt` 선계산·`scriptBlocks` 한 번 훑기·`excerpt`·`declarators`/`dottedName` 반복문, 그다음 메모리: JS 토큰 수 상한·구두점 공유·JavaParser storeTokens(false)·unresolved 상한 보관) → 1(UNKNOWN 선언, `JsDeclarations`·`JsModule.lookup`·`HttpLinker`) → 5(HTTP 의미 6종) → 2·6(토크나이저) → 4(캐시, `StructureService`, 선별 파일 불변·soft delete 확인 먼저). 5.3-7은 follow-up 이슈(area:A)
2. `node scripts/verify.mjs all` → push → `gh pr ready 118` → `node scripts/codex-review.mjs 118 --request --wait --status` (background)
3. Codex COMPLETE + CI 초록 → `docs(F-21a): 완료 표시 (spec DONE)` → 사람이 GitHub에서 머지

## 막힌 점 / 결정 필요

- 사람: 노션 팀원 초대

## 주의

- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시) · 하네스 동결(11-23): 버그·작업 막는 문제만 고친다
- 이 PC의 Git Bash heredoc에서 python `'''` 문자열의 백슬래시가 한 번 풀린다 → Java 편집은 Write로 만든 스크립트 파일(raw 문자열)로
- 내 영역 이슈 #104·#105(F-25 집계)는 F-21 뒤 기능 사이에 처리
