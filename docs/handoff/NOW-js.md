# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-19 17:40 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-10-concept-detail

## 현재 기능

F-10 개념 카드 상세 (`docs/specs/F-10-concept-detail.md` DONE). **PR #52 머지 대기** — CI 초록, Codex ✅ `de214b3`(지적 없음), 완료 표시 커밋 push함. 사람이 GitHub에서 머지

## 진행

- ✅ BE AC1~AC8, FE AC9~AC14 green. contract-checker 0건, ui-verifier 0건, SC-10 화면 설계서·스크린샷
- ✅ /pr-review: verifier all 통과, reviewer 반드시 수정 0건. 권장 3건 반영(`de214b3`): 출처 URL http(s)만 링크(`ConceptDetailView.referenceIsLink`), 빈 title → project_name(`ConceptMapper.xml` `NULLIF(TRIM)`), `MarkdownBody.spec.js` DOM XSS 테스트
- ✅ follow-up #51(F-13 BE reference_url http(s) 검증) 생성, PR 본문 `.harness/pr-F-10.md`(공통 파일 @whans1996 멘션)

## 바로 다음 할 일

1. PR #52 머지 확인(`gh pr view 52 --json state`) → `git switch develop && git pull`
2. 다음 B 기능 `/spec` (F-11 리뷰 or F-12 찜, `docs/기획서.md` 4절 순서 확인). F-13 spec 때 #51 흡수
3. 남는 시간엔 #46 (F-09 범위 밖 page URL 안내)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경(SecurityConfig·ConceptMapper·concept 패키지 등)은 PR 본문에 적고 @whans1996 멘션함
- 통합 테스트는 Docker Desktop 필요. 마이그레이션 없음
