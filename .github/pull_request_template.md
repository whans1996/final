<!-- 제목: 타입(F-xx): 요약  예) feat(F-05): 분석 요청 API -->

## 기능

- 기능 ID: F-xx
- spec: `docs/specs/F-xx-slug.md`
- 영역 오너(머지): <!-- @whans1996 (A) 또는 @JoonseobKim (B). 본인이 머지한다 -->
- [ ] 머지 직전 `git merge origin/develop` 후 push해 CI 초록을 다시 확인했다 (상대 PR이 먼저 머지된 경우. 마이그레이션 번호·openapi.json 충돌은 머지 뒤에야 드러난다)
- [ ] 상대 영역·공통 파일(`node scripts/owners.mjs --diff origin/develop`, `.github/CODEOWNERS`)을 바꿨으면 이유를 적고 상대를 @멘션했다

## 해결 이슈

<!-- 이 PR이 해결하는 follow-up 이슈. develop에 머지되면 close-issues 워크플로가 닫는다. 이슈마다 키워드를 붙인다. 없으면 "없음" -->
- Closes #

## 변경 요약

- 

## 수용 기준

| AC | 내용 | 테스트 | 결과 |
|---|---|---|---|
| AC1 | | `XxxTest#AC1_...` | ✅ |

## 확인 체크리스트

- [ ] **CI 초록** (GitHub Actions) — 초록일 때만 머지
- [ ] `node scripts/verify.mjs all` 로컬 통과
- [ ] API 변경 시 `docs/api/openapi.json` 갱신 커밋 (CI가 불일치를 잡음)
- [ ] contract-checker: spec ↔ openapi.json(BE) ↔ FE `src/api` 일치
- [ ] reviewer 지적 사항 중 "반드시 수정" 반영
- [ ] 새 Flyway 마이그레이션: 없음 / `V?__...sql` (번호 충돌 없음 확인)
- [ ] 새 환경변수: 없음 / `.env.example`에 추가함
- [ ] FE 변경 시 스크린샷 첨부 (ui-verifier, 데스크톱·모바일 → `docs/산출물/screens/` 커밋 + `node scripts/pr-screens.mjs`)
- [ ] 산출물 영향(화면 설계서, ERD, 요구사항 정의서) 반영 또는 후속 작업으로 기록
- [ ] wip 커밋이 있으면 **Squash and merge**

## 스크린샷

## 리뷰어가 특히 볼 곳

## 결정 필요 / 후속 작업

<!-- 후속 작업은 follow-up 이슈로 만들고(area 라벨·담당자 포함) 여기에는 이슈 번호만 적는다 -->
