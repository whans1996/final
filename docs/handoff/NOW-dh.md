# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 16:29 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-26a-index-selection

## 현재 기능

**F-26a 수집·선별 개편** (T0, H-1이 끝나 11/26~28이 비어서 W3 일을 당김). spec `docs/specs/F-26a-index-selection.md` **APPROVED** (whans1996 11-25)

## 진행

- ✅ PR #153(#146)·#154(#143) 머지, worktree 정리 끝
- ✅ F-26a spec 승인: AC1~AC11, V12(`analysis_files.selected·importance`, `analyses.indexed_files·dropped_files`), API 형식 불변. 인터뷰 8건(F-33 RAG 전제) 결정로그 11-25 F-26a 행에 기록

## 바로 다음 할 일

1. `/clear` → `/implement F-26a`: `node scripts/next-migration.mjs`로 V12 확인 → `ZipArchiveReaderTest` AC1~AC3 red부터
2. 11/27 Figma 세션 → 11/29~ V-1 MVP 감사

## 막힌 점 / 결정 필요

- 사람: GMS 키 수령(12/01 전), 노션 팀원 초대, F-05b liveTest, 제출 zip 이름

## 주의

- 커밋 범위는 F-xx|common|harness만 · Git Bash에서 `/pr-review` 같은 인자는 MSYS 경로 변환됨 (`MSYS_NO_PATHCONV=1`)
- Codex: 3회차부터 P2는 follow-up 이관 · 진행현황·WBS·간트는 `/progress` docs PR에서만
- 이 PC에 python 없음 (파일 편집은 Edit·node로)
