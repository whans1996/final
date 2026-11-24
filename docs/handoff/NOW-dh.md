# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신: 2026-11-24 13:24 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-21a-structure-graph

## 현재 기능

**F-21a 구조 시각화: 파일 의존 그래프** (P1) spec `docs/specs/F-21a-structure-graph.md` — APPROVED, `/implement` 진행 중. BE AC1~AC13 커밋 완료, FE AC14~AC17 구현(미커밋, green 확인 중)

## 진행

- ✅ BE AC1~AC13 (JavaStructureReader·MyBatisXmlReader·JsTokenizer·JsModuleReader·HttpLinker·StructureAnalyzer·StructureController GET /api/analyses/{id}/structure, openapi.json, contract-checker BE 불일치 0)
- FE (미커밋): mermaid 12.3.0 고정(11-03 출시, MIT), `utils/mermaid.js`(escapeMermaidLabel·buildStructureMermaid), `utils/structure.js`(사유·간선 종류 라벨, MYBATIS='MyBatis 매핑'), `components/analysis/MermaidDiagram.vue`(동적 import·strict·useMaxWidth false·실패 문구·원본·복사), `AnalysisStructureTab.vue`, `api/analyses.js` fetchAnalysisStructure, `AnalysisDetailView.vue` TABS 개요 다음 structure
- 테스트: `utils/__tests__/mermaid.spec.js`, `components/analysis/__tests__/MermaidDiagram.spec.js`·`AnalysisStructureTab.spec.js` red 15건 assertion 실패 확인(11-24) → 구현. `views/__tests__/AnalysisDetailView.spec.js` 탭 목록·인덱스 +1, F-21a AC14 테스트 2개 추가
- 산출물 반영(미커밋): 06 SC-07 구조 탭 절·탭 목록, 01 F-21 비고, README 기술 스택. 07은 openapi 자동 생성이라 손대지 않음

## 바로 다음 할 일

1. `node scripts/verify.mjs fe` green 확인(빌드에서 mermaid가 별도 chunk인지) → 실패 시 수정 → 커밋 `feat(F-21a): AC14~AC17 구조 탭과 mermaid 다이어그램` + push, spec 10절 체크리스트 갱신
2. contract-checker(FE 포함) → BE local + FE dev 띄워 ui-verifier·Chrome 375px(구조 탭, 그림 상자만 가로 스크롤) → 06 스크린샷(SC-07-structure-desktop/mobile.png) → /pr-review

## 막힌 점 / 결정 필요

- 사람: 노션 팀원 초대, #77 담당(F-08 A·F-13 B 걸침)

## 주의

- StructureAnalyzerTest(AC7~AC12)는 red를 건너뛰어 뮤테이션 4종으로 사후 확인(11-24)
- PR: 공통 파일 application.yml(analysis.structure) 변경 → "공통 코드 변경"에 적고 @JoonseobKim 멘션. 적을 것: AC6 코드 아닌 확장자(.css 등) 미보고, AC4는 "DOM 파서" 대신 SAX(줄 번호), MYBATIS 목록 라벨 'MyBatis 매핑'(spec 미정), unresolvedTruncated 안내 문구 없음(spec에 없음)
- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시) · 하네스 동결(11-23): 버그·작업 막는 문제만 고친다
- 내 영역 이슈 #104·#105(F-25 집계)는 F-21 뒤 기능 사이에 처리
