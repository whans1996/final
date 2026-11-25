# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 16:40 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-26a-index-selection

## 현재 기능

**F-26a 수집·선별 개편** `/implement` 중. spec `docs/specs/F-26a-index-selection.md` APPROVED. 이번 세션 목표: AC1~AC3(수집) → AC4(layer) → AC5~7·11(ImportanceRanker)

## 진행

- 🔄 AC1~AC3 green 구현 끝, **미커밋** · BE 전체 검증 중: `ZipArchiveReader`(ts·tsx·jsx·md, 생성물 제외, (묶음, 경로) 순서, `max-index-files`·`max-index-bytes` 상한에서 버림 → `IndexResult.dropped`), `FileSelector.layer`(F-26a 5.3절 순서 전체, content null이면 경로만), `Layer.TEST·MIGRATION·DOC`+`indexOnly()`, `AnalysisProperties.maxIndexFiles·maxIndexBytes`, `ErrorCode.ANALYSIS_413` 문구, `AnalysisPipelineTest` 413 테스트 3개 → `F26a_AC2_*` DONE 기대
- V12 아직 없음 (`next-migration` → V12 확인됨)

## 바로 다음 할 일

1. `node scripts/verify.mjs be` 초록 확인 → `FileSelectorTest#AC5_layer_판정`의 `vite.config.js`→JS 기대가 깨지면 CONFIG로 고치고 이유(F-26a AC4)를 커밋 메시지에 → `feat(F-26a): AC1~AC3 넓은 인덱스 수집` 커밋·push
2. AC4: `FileSelectorTest#AC4_*`(@Service·@Repository·@Mapper·@Configuration·@Entity, 주석 무시, vite/eslint config, test)
3. AC5~7·11: 새 `pipeline/ImportanceRanker` + `ImportanceRankerTest` (spec 5.4절), `analysis.selection.*` 설정
4. AC8: `V12__analysis_index.sql`, 인덱스 전체 저장(100행 청크)·`selected` 필터·`indexed_files`/`dropped_files`(AnalysisPipeline Context에 dropped 전달), `PromptEvalRunner.select` 맞추기

## 막힌 점 / 결정 필요

- 사람: GMS 키 수령(12/01 전), 노션 팀원 초대, F-05b liveTest, 제출 zip 이름

## 주의

- 커밋 범위는 F-xx|common|harness만 · Git Bash에서 `/pr-review` 같은 인자는 MSYS 경로 변환됨 (`MSYS_NO_PATHCONV=1`)
- ErrorCode(공통) 문구를 바꿨으니 PR 본문에서 김준섭 @멘션 · 진행현황·WBS·간트는 `/progress` docs PR에서만
- node 스크립트로 Java 편집 시 백슬래시가 깨짐 → Edit 도구 사용 · 이 PC에 python 없음
