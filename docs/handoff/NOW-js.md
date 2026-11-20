# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 15:16 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-12-concept-wish

---

## 현재 기능

F-12 카드 찜 — spec APPROVED, **PR #65 열림** (`Closes #57`), Codex 리뷰 루프 진행 중

## 진행

- ✅ develop(F-16) 합침(`b56883b`, 충돌 없음) + openapi.json 태그 순서 재생성(`b93d56d`)
- ✅ verify all 통과(BE·FE 전체), contract-checker 0건, reviewer 반드시 수정 0건
- ✅ reviewer 권장 4건 반영 `d0c7597`: 탈퇴 회원 찜 401 테스트(`ConceptWishApiTest#AC11_탈퇴한_회원의_남은_토큰이면_...`, red 확인), `UserWishes` 더 보기 중복 id 거르기·한 쪽 미만이면 끝, `ActivityService.recordOnce` javadoc, spec 4절 쓰기 순서·401 행, 결정로그
- ✅ follow-up #64 (F-04 피드 WISH 활동 거르기) 생성, PR 후속에 기록
- ⏳ CI 진행 중, Codex `node scripts/codex-review.mjs 65 --request --wait --status` 대기 (head `d0c7597`)

## 바로 다음 할 일

1. Codex 결과 확인: FINDINGS면 spec AC·보안을 깨는 지적만 실패 테스트부터 고쳐 `fix(F-12): ... (Codex 리뷰)` → push → 위 명령 재실행. 그 밖의 P2는 follow-up 이슈로 넘기고 `--resolve`
2. COMPLETE + CI 초록이면 `docs(F-12): 완료 표시 (spec DONE)`: spec 헤더 `DONE (PR #65)`, 체크리스트 "PR CI 초록"·"상태 DONE" 체크
3. 머지(사람, GitHub). 머지 직전 `git rev-list --count HEAD..origin/develop`가 0인지 확인

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경(`concept`·`SecurityConfig`·`feed`·`src/api/concepts.js`)은 PR 본문에서 @whans1996 멘션함
- `codex-review.mjs`가 Codex 실행 실패를 RUNNING으로 기다린다 (#48)
