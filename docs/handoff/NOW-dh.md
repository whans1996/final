# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신: 2026-11-24 13:25 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-21a-structure-graph

## 현재 기능

**F-21a 구조 시각화: 파일 의존 그래프** (P1) spec `docs/specs/F-21a-structure-graph.md` — APPROVED, `/implement` 진행 중. AC1~AC17 모두 green·커밋(`48907d9`), 화면 점검 남음

## 진행

- ✅ BE AC1~AC13 (JavaStructureReader·MyBatisXmlReader·JsTokenizer·JsModuleReader·HttpLinker·StructureAnalyzer·StructureController GET /api/analyses/{id}/structure, openapi.json)
- ✅ FE AC14~AC17 (`48907d9`): mermaid 12.3.0 고정(11-03 출시, MIT, 별도 chunk로 지연 로딩 확인), `utils/mermaid.js`·`utils/structure.js`, `MermaidDiagram.vue`(동적 import·strict·useMaxWidth false), `AnalysisStructureTab.vue`, `fetchAnalysisStructure`, `AnalysisDetailView` TABS 개요 다음 structure. red 15건 assertion 실패 확인 후 green, verify fe 716/716
- ✅ contract-checker spec↔openapi↔FE 불일치 0 (11-24)
- ✅ 산출물: 06 SC-07 구조 탭 절(스크린샷 없음), 01 F-21 비고, README 기술 스택. 07은 openapi 자동 생성이라 손대지 않음

## 바로 다음 할 일

1. BE를 이 브랜치로 재시작(`cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'`, 13:25 기준 떠 있던 BE에는 /structure 없음) → 분석 DONE 하나에서 `/analyses/{id}?tab=structure`를 ui-verifier·Chrome(로그인, 데스크톱·375px: 그림 상자만 가로 스크롤, 콘솔 에러, 연결 목록 링크 → 파일 탭)로 점검
2. 스크린샷 `docs/산출물/screens/SC-07-structure-desktop.png`·`-mobile.png` → 06 구조 탭 절에 표 추가 → 커밋 → /pr-review

## 막힌 점 / 결정 필요

- 사람: 노션 팀원 초대, #77 담당(F-08 A·F-13 B 걸침)

## 주의

- StructureAnalyzerTest(AC7~AC12)는 red를 건너뛰어 뮤테이션 4종으로 사후 확인(11-24)
- PR: 공통 파일 application.yml(analysis.structure) 변경 → "공통 코드 변경"에 적고 @JoonseobKim 멘션. 적을 것: AC6 코드 아닌 확장자(.css 등) 미보고, AC4는 "DOM 파서" 대신 SAX(줄 번호), MYBATIS 목록 라벨 'MyBatis 매핑'(spec 미정), unresolvedTruncated 안내 문구 없음(spec에 없음)
- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시) · 하네스 동결(11-23): 버그·작업 막는 문제만 고친다
- 내 영역 이슈 #104·#105(F-25 집계)는 F-21 뒤 기능 사이에 처리
