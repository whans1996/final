# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-24 12:12 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-21a-structure-graph

## 현재 기능

**F-21a 구조 시각화: 파일 의존 그래프** (P1) spec `docs/specs/F-21a-structure-graph.md` — APPROVED, `/implement` 진행 중. 이번 세션: AC1~AC6 완료 → AC7~AC11(HTTP)

## 진행

- ✅ AC1~AC3 `analysis/structure/java/JavaStructureReader`(JavaParser 3.28.2, 2026-05-31 출시): 타입 위치·어노테이션·단일/static import·정적 호출 대상을 파일 안→단일 import→같은 패키지→와일드카드 순으로 해석. import 줄도 근거(줄 번호)로 센다. import 없는 단순 이름은 FQN을 몰라 NOT_SELECTED를 만들지 않음
- ✅ AC4 `xml/MyBatisXmlReader`: 줄 번호가 필요해 DOM 대신 SAX(외부 DTD·엔티티 off, 시작 태그 첫 줄 역추적). XXE 테스트 포함
- ✅ AC5 `js/JsTokenizer`(템플릿 중첩·정규식/나눗셈·.vue script만 원본 위치 유지), AC6 `js/JsModuleReader`·`JsModule`·`JsImport`(import·export from·import()·require, @/ = 가장 가까운 상위 src/). .css 등 코드 아닌 확장자는 NOT_SELECTED 안 남김
- 공용: `structure/` EdgeType·UnresolvedKind·UnresolvedReason·StructureEdge·UnresolvedLink·StructureLinks, 테스트 `StructureFiles`

## 바로 다음 할 일

1. AC7~AC10: `JsModuleReader`에 export·const·함수(매개변수·본문 범위)·HTTP 호출 후보 추출을 더하고 `http/UrlEvaluator`로 URL·baseURL·래퍼 한 단계 인라인 계산. 테스트 `http/UrlEvaluatorTest`, `StructureAnalyzerTest#AC7_…`~`#AC10_…` (오탐 방지: router.push·Map.get·route path)
2. AC11 `http/ApiPathMatcher` (+ApiPathMatcherTest)
3. 그 뒤 AC12 `StructureAnalyzer`, AC13 컨트롤러, FE AC14~AC17

## 막힌 점 / 결정 필요

- 사람: 노션 팀원 초대, #77 담당(F-08 A·F-13 B 걸침)

## 주의

- PR 본문에 적을 것: AC6 코드 아닌 확장자(.css 등) 미보고, AC4는 spec 문구 "DOM 파서" 대신 SAX(같은 보안 설정, 줄 번호 때문)
- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시) · 하네스 동결(11-23): 버그·작업 막는 문제만 고친다
- mermaid는 12.x 중 출시 2주 이상 지난 정확한 버전으로 고정 (12.3.0은 11-11 출시)
- 내 영역 이슈 #104·#105(F-25 집계)는 F-21 뒤 기능 사이에 처리
