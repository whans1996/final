<!-- 제목: 타입(F-xx): 요약  예) feat(F-05): 분석 요청 API -->

## 기능

- 기능 ID: F-xx
- spec: `docs/specs/F-xx-slug.md`
- 영역 오너(리뷰어): 

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
- [ ] FE 변경 시 스크린샷 첨부 (ui-verifier, 데스크톱·모바일)
- [ ] 산출물 영향(화면 설계서, ERD, 요구사항 정의서) 반영 또는 후속 작업으로 기록
- [ ] wip 커밋이 있으면 **Squash and merge**

## 스크린샷

## 리뷰어가 특히 볼 곳

## 결정 필요 / 후속 작업
