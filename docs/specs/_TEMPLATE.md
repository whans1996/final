# F-xx 기능 이름

| 항목 | 값 |
|---|---|
| 상태 | DRAFT <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-xx |
| 우선순위 | P0 |
| 영역 오너 | A 김동환 (AI·분석·학습 루프) / B 김준섭 (카드·소셜) |
| 관련 필수 요구사항 | ⑬ |
| 의존 spec | F-01 (로그인 필요) |
| 관련 이슈 | 없음 <!-- 이 기능에 흡수하는 follow-up 이슈 #n. 구현 PR에 Closes #n --> |
| 승인 | (승인한 사람, 날짜) |

## 1. 목적

이 기능이 해결하는 사용자 문제와 시나리오를 2~4줄로.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_정상_요청이면_201을_반환한다()`

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 로그인한 사용자가 | 유효한 값으로 요청하면 | 201과 생성된 id를 받는다 |
| AC2 | | | |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: `개념 카드` 등 (`docs/conventions.md` 8절).

### `METHOD /api/...`

- 권한: 로그인 필요 / 공개 / ADMIN
- 요청

```json
{ }
```

- 응답 `200`

```json
{ }
```

- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | 검증 실패 |

## 4. DB 변경

- Flyway: `V{n}__설명.sql` (새로 추가할 파일. 없으면 "없음")
- 사용 테이블: 

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `/path` → `XxxView.vue` |
| 컴포넌트 | |
| store | |
| API 함수 | `src/api/xxx.js`의 `fetchXxx()` |
| 상태별 UI | 로딩 / 빈 목록 / 에러 메시지 |

## 6. LLM (AI 기능만)

| 항목 | 내용 |
|---|---|
| 파이프라인 단계 | |
| 프롬프트 파일 | `backend/src/main/resources/prompts/<stage>/<level>.md` |
| 입력 | |
| 출력 JSON 스키마 | (아래) |
| Mock fixture | `backend/src/main/resources/llm-fixtures/<stage>.json` |
| 캐시·재사용 | |

```json
{ }
```

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1 | `XxxControllerTest#AC1_...` | MockMvc 통합 |
| AC2 | `xxx.spec.js` | Vitest |

## 8. 범위 밖

- 이번 spec에서 하지 않는 것

## 9. 결정 필요

- (구현 중 생긴 질문. 사람이 답하면 본문에 반영하고 이 항목을 지운다)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [ ] 브랜치 `feature/F-xx-slug`
- [ ] Flyway 마이그레이션
- [ ] AC별 실패 테스트 작성 (red 확인)
- [ ] BE 구현 (green)
- [ ] FE 구현 (green)
- [ ] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [ ] contract-checker 통과
- [ ] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영 (화면 설계서·ERD·요구사항 정의서에 영향이 있으면)
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
