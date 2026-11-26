# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-26 11:05 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/common-docs-fix-1126

## 현재 기능

**D-1 문서 정정 (점검 문서 3절, 트랙 1)** — 편집 끝, 하네스 검증 통과. docs PR(base develop) 생성 단계. 재인터뷰(기획서 7절)는 1~4단계 끝, PR #174 머지됨(김준섭 합의 답 없음)

## 진행

- ✅ CLAUDE.md(public·브랜치 보호, 규칙 6 GMS, macOS, 하네스 동결, `/project-interview`, `/progress` 기준) · prompt-evaluator(overview·file만, 블라인드) · `/progress` 스킬(2차 백로그) · backend·frontend CLAUDE.md · conventions(ANALYSIS_400·404·500, export-api-doc) · 환경설정(4·6·8절) · 워크플로 주석
- ✅ 02·09 6절·기획서 아키텍처 Gemini → GMS · 03 ERD V1~V13 · 진행현황 랜딩(SC-01 자리 표시자) · CODEOWNERS(`post`, 주인 없는 파일 0개, FE `**/` 패턴) · NOW 머리말(#166)은 이미 정정돼 할 일 없음

## 바로 다음 할 일

1. 커밋 → push → `gh pr create --base develop` (공통 파일 CODEOWNERS·conventions·CLAUDE.md 변경이라 @JoonseobKim 멘션) → CI 초록이면 GitHub에서 머지
2. 트랙 1 E-1: `backend/src/test/java/com/vibenote/prompteval/PromptEvalRunner.java:98` stage 제한 풀어 flow·concept·quiz 지원 + DS-04 라벨 채점기(dev 합성 라벨로 테스트) + 수준 구분 지표. 새 세션 Opus/high
3. 11/27 Figma 세션 첫머리에 김준섭 합의 확정(#174 본문 체크리스트) → `/project-interview` 5단계 UI → 6단계 운영 → 기획서 6절·WBS·간트·01·02 반영 docs PR
4. 트랙 3: K-3 검수 스크립트 ①형식 ②예제 실행
5. dh-home V-2 블라인드 점검: `git fsck --unreachable --no-reflogs` → JoonseobKim·11-25 커밋이 있으면 `git reflog expire --expire-unreachable=now --all && git gc --prune=now`

## 막힌 점 / 결정 필요
- 김준섭 합의(#174 본문) · 사람: 노션 팀원 초대, F-05b liveTest, 제출 zip 이름

## 주의
- 다음 마이그레이션은 V14 (`node scripts/next-migration.mjs`) · 브랜치 보호 관리자 포함
- node 스크립트로 Java 편집 시 백슬래시가 깨짐 → Edit 도구 사용 · 이 PC에 python 없음
