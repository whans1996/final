# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-20 14:59 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-16-recommendation

## 현재 기능

F-16 맞춤 개념 카드 추천 — spec **DONE (PR #62)**, CI 초록, **Codex ✅ 761f962** (이후 docs만). 사람이 GitHub에서 **Squash and merge**만 남음 (wip 커밋 있음)

## 진행

- ✅ reviewer 권장 2건 `928eb83`, develop(F-17) 합침 `f8d6dd9`, Codex P2(추천 요청 타임아웃 370초) `761f962`
- ✅ 이슈 #63(area:B): B의 PostDetailView·PostWriteView 테스트가 dh-home에서 지연 로딩 타이밍으로 실패 → 이 PC FE 정식 커밋이 hook에 막힘

## 바로 다음 할 일

1. 사람: PR #62 머지 직전 `git fetch && git rev-list --count HEAD..origin/develop`가 0인지 확인 (아니면 `git merge origin/develop` → push → CI 초록) 후 GitHub에서 **Squash and merge**
2. 머지 후 `/clear` → `/pickup`: develop 받고 다음 A 기능 고르기 (`docs/기획서.md` 4절 일정, `gh issue list --label follow-up --label area:A`)

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부
- 실제 LLM 추천 이유 품질은 사용자 요청 시 prompt-evaluator(RECOMMEND)

## 주의

- #63 해결 전에는 이 PC에서 FE 파일 정식 커밋이 막힌다 (사용자 확인 후 wip + Squash)
- B F-12(찜) 진행 중: concept 패키지 공통 변경 예정 → 먼저 머지되면 F-16 머지 전 develop 합치기
- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시)
