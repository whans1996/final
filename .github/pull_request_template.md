<!-- 제목: 타입(F-xx): 요약  예) feat(F-05): 분석 요청 API -->

## 기능

- 기능 ID: F-xx
- spec: `docs/specs/F-xx-slug.md`
- 영역 오너(머지): <!-- @whans1996 (A) 또는 @JoonseobKim (B). 본인이 머지한다 -->
- [ ] 공통 코드(SecurityConfig, GlobalExceptionHandler, 공통 응답·ErrorCode, FE 라우터·레이아웃·공용 컴포넌트·api 클라이언트, 빌드 설정)를 바꿨으면 상대를 @멘션했다

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
