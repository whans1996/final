# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 17:04 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-26a-index-selection

## 현재 기능

**F-26a 수집·선별 개편** `/implement` 중. spec `docs/specs/F-26a-index-selection.md` APPROVED. AC1~AC7·AC11 끝, 남은 것 AC8·AC9·AC10

## 진행

- ✅ AC1~AC3 `a00e0ee` · AC4 `ffb6150` · AC5~7·11 `e85491b` (ImportanceRanker), 모두 push, BE 1224개 통과
- 지금 파이프라인은 ranker 결과 중 selected만 저장한다 (`AnalysisPipeline.select`). `IndexResult.dropped`는 아직 Context에 안 넘김

## 바로 다음 할 일

1. AC8·AC10: `V12__analysis_index.sql`(spec 4절 DDL 그대로) → `AnalysisFile`에 selected·importance, `AnalysisPipeline.filter`에서 dropped를 Context에, `select`가 ranker 전체를 redact해 저장 · `AnalysisMapper.xml` insertFiles 100행 청크·`findFiles`/`findFileSummaries`/`findFile`에 `AND selected = TRUE`·file_count 서브쿼리도 · `findIndexFiles` 추가 · indexed/dropped는 같은 트랜잭션. 테스트 `AnalysisPipelineTest#AC8_*`, `AnalysisControllerTest#AC8_선별되지_않은_파일_상세는_404`, `AC10_*`
2. AC9: `ExplainPrompts.overviewInput`에 README(루트 README.md, 4000자)·인덱스 파일 목록, `ExplainProperties.readmeMaxChars`, 프롬프트 3종 `{{readme}}`, `OverviewExplainer` LAYERS에서 test·migration·doc 제외
3. 마무리: ERD·요구사항 정의서, backend/CLAUDE.md 104줄(200개·10MB 문구), contract-checker → `/pr-review`

## 막힌 점 / 결정 필요

- 사람: GMS 키 수령(12/01 전), 노션 팀원 초대, F-05b liveTest, 제출 zip 이름

## 주의

- 커밋 범위는 F-xx|common|harness만 · Git Bash에서 `/pr-review` 같은 인자는 MSYS 경로 변환됨 (`MSYS_NO_PATHCONV=1`)
- ErrorCode(공통) 문구를 바꿨으니 PR 본문에서 김준섭 @멘션 · 진행현황·WBS·간트는 `/progress` docs PR에서만
- node 스크립트로 Java 편집 시 백슬래시가 깨짐 → Edit 도구 사용 · 이 PC에 python 없음
