# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 16:59 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-26a-index-selection

## 현재 기능

**F-26a 수집·선별 개편** `/implement` 중. spec `docs/specs/F-26a-index-selection.md` APPROVED. 이번 세션 목표: AC1~AC7·AC11, 가능하면 AC8

## 진행

- ✅ AC1~AC3 `a00e0ee` (ZipArchiveReader 넓은 인덱스·상한 버림, layer 판정, max-index-*), push·pre-push 전체 통과
- ✅ AC4 `ffb6150` (FileSelectorTest#AC4_* 판정 표)
- 🔄 AC5~7·11 green 구현 **미커밋**, BE 전체 검증 중: `pipeline/ImportanceRanker`(그래프 연결 수·가중치·진입점·config 전역 상한·FE/BE 몫·저장 순서, 예산 소진 warn), `SelectionProperties`·`RankedFile`, `analysis.selection.*` yml, `AnalysisPipeline.select`·`PromptEvalRunner.select`가 ranker 사용(selected만 저장), `FileSelector.select` 삭제 → 그 테스트 3개는 `ImportanceRankerTest#F05a_*·F07_*`로 이동
- AC4 `ffb6150`은 아직 push 안 함 (다음 커밋과 함께 push)

## 바로 다음 할 일

1. 검증 초록이면 `feat(F-26a): AC5~AC7·AC11 중요도 선별` 커밋 → `git push`. 실패면 `ImportanceRankerTest`부터 고친다
3. AC8: `V12__analysis_index.sql`, 인덱스 전체 저장(100행 청크)·`selected` 필터·`indexed_files`/`dropped_files`

## 막힌 점 / 결정 필요

- 사람: GMS 키 수령(12/01 전), 노션 팀원 초대, F-05b liveTest, 제출 zip 이름

## 주의

- 커밋 범위는 F-xx|common|harness만 · Git Bash에서 `/pr-review` 같은 인자는 MSYS 경로 변환됨 (`MSYS_NO_PATHCONV=1`)
- ErrorCode(공통) 문구를 바꿨으니 PR 본문에서 김준섭 @멘션 · 진행현황·WBS·간트는 `/progress` docs PR에서만
- node 스크립트로 Java 편집 시 백슬래시가 깨짐 → Edit 도구 사용 · 이 PC에 python 없음
