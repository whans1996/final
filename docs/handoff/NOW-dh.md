# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 17:23 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-26a-index-selection

## 현재 기능

**F-26a 수집·선별 개편** `/implement` 중. spec `docs/specs/F-26a-index-selection.md` APPROVED. AC1~AC11 구현·커밋 끝, BE 1233개 통과. 다음은 contract-checker → /pr-review

## 진행

- ✅ AC1~AC3 `a00e0ee` · AC4 `ffb6150` · AC5~7·11 `e85491b` (ImportanceRanker), push됨
- AC8·AC10: V12 마이그레이션, `saveFiles(id, files, dropped)` 100행 청크 + `updateIndexCounts`, 조회 3종·file_count에 `selected = TRUE`, 파이프라인이 인덱스 전체 저장. AC8·AC10 상태로 BE 1228개 통과
- AC9: `findIndexFiles`, `ExplainPrompts.overviewInput(files, index, apis, max, readmeMax)`, `OverviewExplainer.explain(analysis, files, index, apis)`·LAYERS에서 test·migration·doc 제외, 프롬프트 3종 `{{readme}}`, `readme-max-chars: 4000`. 테스트 `ExplainPromptsTest#F26a_AC9_*` 4개, `OverviewExplainerTest#F26a_AC9_*`. 시그니처 변경으로 `MainFixturesTest:74`·`FileExplainerTest.PROPS`·`PromptEvalRunner.select`도 고침 (testCompile 통과)
- 문서: ERD(V12 열·이력), 요구사항 정의서·유스케이스·데이터셋 문구, backend/CLAUDE.md 104줄, spec 체크리스트

## 바로 다음 할 일

1. contract-checker (API 형식 변화 없음 확인, openapi.json 그대로인지) → `/pr-review` (새 세션 권장)

## 막힌 점 / 결정 필요

- 사람: GMS 키 수령(12/01 전), 노션 팀원 초대, F-05b liveTest, 제출 zip 이름
- PR 후속에 적기: 화면 설계서·FE 분석 요청 화면의 "코드 파일 200개 · 10MB 이하" 안내 문구(FE, spec 범위 밖) · 개요 파일 목록에 미선별 파일이 생겨 readingOrder가 미선별 경로를 고르면 버려짐(파싱이 선별 id만 받음)

## 주의

- 커밋 범위는 F-xx|common|harness만 · Git Bash에서 `/pr-review` 같은 인자는 MSYS 경로 변환됨 (`MSYS_NO_PATHCONV=1`)
- 공통 파일 변경(ErrorCode 문구, V12, WithdrawalApiTest 시그니처) → PR 본문에서 김준섭 @멘션 · 진행현황·WBS·간트는 `/progress` docs PR에서만
- node 스크립트로 Java 편집 시 백슬래시가 깨짐 → Edit 도구 사용 · 이 PC에 python 없음
