# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 21:22 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/issue-254-roles-roadmap

## 현재 기능

**#254 역할·넘겨주기·일정 정리 docs PR** (`feature/issue-254-roles-roadmap`, develop 기반) — 커밋 4a5a396·2487cfa, PR 아직 안 염. 그 뒤 **P-1a** 계속 (`feature/P-1a-project-versions`, BE AC1~13 ✅ push됨, FE 남음)

## 진행

- ✅ 문서 점검 3갈래(로드맵·이슈·문서) 결과 반영: 로드맵(E-2·NT-1·NT-2·AM-1·HP-6, F-27b·EV-2 당김, 실제 머지일, 넘겨주기 표), 기획서 4·6·7절, CLAUDE.md·conventions·/pickup·/pr-review·/progress(문서 즉시 갱신·일정 바로 반영 규칙), CODEOWNERS landing, 09·03·08·01 산출물, 결정로그 3행
- ✅ 이슈: #251 dh-home 체크, #254·#241 담당 추가, #163 제목 K-1, #131·#140·#181·#228·#229 HP-6 댓글, #243·#250·#121·#156·#201·#203 흡수처 댓글, area 라벨 설명
- 🟡 04-WBS·05-간트·01 요구사항(F-26a·F-27·F-29·L-1a 행)은 서브에이전트가 고치는 중 (작업 트리 미커밋)

## 바로 다음 할 일

1. `git diff docs/산출물/04-WBS.md docs/산출물/05-간트차트.md docs/산출물/01-요구사항정의서.md` 확인 → `node scripts/roadmap.mjs`와 대조 → 커밋 → `/pr-review` (Closes #254, 공통 파일 CLAUDE.md·CODEOWNERS·conventions·로드맵 → @JoonseobKim 멘션, 결정로그 "김준섭 확인 필요" 항목 알림: NT-1 11/28 당김, Figma 생략, landing 주인, F-33 FE A)
2. P-1a 브랜치로 돌아가 `docs/specs/P-1a-project-versions.md` 6행 "넘겨주기 12/02 A→A·B" → "A→A (T-1·I-1b·I-2)" 고치고 develop 합친 뒤 FE AC14~16 (`src/api/projects.js`, `views/project/*`, `LatestCommitBadge.vue`, `AnalysisRequestView` `?projectId=`, `AnalysisDetailView` 링크). 화면 이전 규칙: 고치는 화면은 Tailwind·`useLatestRequest`로, #250 칸 체크

## 막힌 점 / 결정 필요
- 남은 질문: idea 이슈 #107~#115 댓글의 옛 ID 정리(닫기/댓글만/그대로), #205·#226 처리 시점
- 사람: 노션 초대, F-05b liveTest, #251 js-ssafy·js-home `--move-env`

## 주의
- 일정이 일찍·늦게 끝나거나 새 일정이 생기면 즉시 로드맵·WBS·간트·결정로그·이슈 반영 (CLAUDE.md 작업 흐름, 11-27)
- P-1a: V15 develop 미머지, 공통 변경(ErrorCode·application.yml·IntegrationTestBase max-connections 등) PR에서 @JoonseobKim
