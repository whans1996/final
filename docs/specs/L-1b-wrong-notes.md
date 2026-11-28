# L-1b 풀이 기록·오답 노트·선지 다시 섞기

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | L-1b (로드맵 트랙 2, 기획서 6.1절 "오답 노트(L-1b)") |
| 우선순위 | T0 (기획서 7.4b) |
| 영역 오너 | A 김동환 (AI·분석·학습 루프) |
| 관련 필수 요구사항 | ⑫ ⑬ (F-14 퀴즈·F-15 복습 보강) |
| 의존 spec | F-14(퀴즈), F-15(복습 계획), F-19(분석 삭제), F-20b(팀 분석 공유), L-1a(퀴즈 상태) |
| 관련 이슈 | #250 일부 (SC-07 `AnalysisQuizTab.vue`·SC-08 `QuizView.vue` 칸만 체크, `Closes` 없음. 로드맵 "A 화면 이전") |
| 승인 | 김동환 2026-11-28 |

## 1. 목적

퀴즈는 여러 번 풀 수 있고 풀이 기록도 모두 남지만(F-14), 화면에는 마지막 점수만 보여서 무엇을 틀렸는지 다시 볼 곳이 없다. 시연 줄거리(기획서 1절 "퀴즈·오답 노트 → 팀 약점")에서 학습 루프가 이어지도록 ① 분석 퀴즈 탭에서 지난 풀이의 문항별 내 답·정답·해설을 다시 보고, ② 모든 분석에서 **마지막 답이 틀린 문항**을 개념별로 모은 "오답 노트"를 열고, ③ 그 분석 퀴즈를 **선지 순서를 새로 섞어** 다시 풀게 한다. 오답만 모은 새 퀴즈 세트는 만들지 않는다(F-31 변형 문항과 겹침, 결정로그 11-26).

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다. "열 수 있는 분석" = F-14 퀴즈 조회와 같은 규칙: 삭제되지 않은 내 분석, 또는 지금 내가 속한 팀에 공유된 삭제되지 않은 분석.

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 내가 같은 퀴즈를 3번 풀었고, 같은 공유 분석을 팀원도 풀었을 때 | `GET /api/quizzes/{quizId}/attempts` | 200, **내 풀이만** 최신순(`createdAt` 내림차순, 같으면 id 내림차순)으로 받는다. 각 항목은 `attemptId·score·total·createdAt`. 21번 넘게 풀었으면 최근 20개만 받는다 |
| AC2 | 퀴즈를 열 수 없을 때 (남의 비공유 분석, 삭제한 분석, 공유가 끊긴 팀 분석, 없는 quizId) | 풀이 목록·풀이 상세를 조회하면 | 404 `QUIZ_404`. 공유받은 팀원은 자기 풀이를 조회할 수 있다 |
| AC3 | 내 풀이가 있을 때 | `GET /api/quizzes/{quizId}/attempts/{attemptId}` | 200, `attemptId·score·total·createdAt`과 `results`(no 순서. 문항마다 `questionId·no·question·choices·selectedIndex·answerIndex·correct·explanation·concept`)를 받는다. 선택지·위치는 **저장된 순서** 기준이다. `concept`은 공개(PUBLISHED) 카드만, 아니면 null (F-14 제출 응답과 같은 모양) |
| AC4 | 퀴즈는 열 수 있지만 attemptId가 남의 풀이거나 다른 퀴즈의 풀이거나 없을 때 | 풀이 상세를 조회하면 | 404 `QUIZ_001` |
| AC5 | 한 문항을 1회차 틀리고(답 2) 2회차 틀리고(답 3), 다른 문항을 1회차 틀리고 2회차 맞혔을 때 | `GET /api/users/me/wrong-notes` | 첫 문항만 들어 있고 `selectedIndex`는 **마지막 답**(3), `wrongCount`는 2, `lastAnsweredAt`은 2회차 제출 시각이다. 두 번째 문항(마지막 답이 정답)은 없다. 한 번도 틀리지 않은 문항도 없다 |
| AC6 | 오답 문항이 개념 A(2문항)·개념 B(1문항)·개념 없음·비공개 개념에 걸쳐 있을 때 | 오답 노트를 조회하면 | `groups`는 개념별 묶음이다. 공개 개념 묶음은 문항 수 내림차순 → 그 묶음의 가장 최근 `lastAnsweredAt` 내림차순 → 개념 id 오름차순, **개념이 없거나 공개되지 않은 문항은 `concept: null` 한 묶음으로 맨 뒤**. 묶음 안 항목은 `lastAnsweredAt` 내림차순 → `questionId` 오름차순. 항목마다 `analysisId·analysisTitle·quizId`가 있고 `analysisTitle`은 분석 제목 → 프로젝트 이름 → "제목 없는 분석" 순 |
| AC7 | 오답 문항이 내 분석·공유받은 팀 분석·삭제한 내 분석·공유가 끊긴 팀 분석에 있을 때 | 오답 노트를 조회하면 | **지금 열 수 있는 분석**(내 분석·공유받은 분석)의 문항만 보인다. 삭제·공유 해제로 빠진 문항의 풀이 기록은 DB에 그대로 남는다(F-19). 다른 사람의 풀이는 내 노트에 영향을 주지 않는다 |
| AC8 | 경계값 | 오답이 하나도 없으면 / 오답 문항이 201개 이상이면 | 200 `{ "totalCount": 0, "groups": [] }` / `lastAnsweredAt`이 가장 최근인 200개만 묶고 `totalCount`는 전체 수 |
| AC9 | 로그인하지 않았을 때 | 세 API를 부르면 | 401 `AUTH_401` |
| AC10 | (FE SC-07 퀴즈 탭) 퀴즈와 지난 풀이가 있을 때 | 탭을 열고 "풀이 기록"의 한 회차를 누르면 | 회차 목록(날짜·시각, 점수)이 최신순으로 보이고, 누른 회차 아래에 문항별 정오·내 답·정답·해설·관련 카드가 펼쳐진다(다시 누르면 접힘). 상세는 처음 펼칠 때 한 번만 부른다. 목록·상세 조회가 실패하면 그 자리에 오류 문구와 "다시 시도"가 보인다. 풀이가 없으면 "풀이 기록" 영역이 보이지 않는다 |
| AC11 | (FE SC-08) 한 번 제출해 결과를 봤을 때 | "다시 풀기"를 누르면 | 답이 비고 **문항마다 선지 순서가 직전 화면과 다르게** 섞인다(문항 순서는 그대로). 처음 연 화면은 저장된 순서다. 제출할 때는 고른 선지를 **저장된 위치**로 바꿔 보내고, 결과 화면은 섞인 순서대로 정답·내 답 강조가 맞는 선지에 붙는다 |
| AC12 | (FE SC-27 오답 노트 `/wrong-notes`) 오답이 있을 때 | 화면을 열면 | 맨 위에 "다시 볼 문항 n개"(n = `totalCount`, 200 넘으면 "최근 200개를 보여 줘요" 안내), 개념 묶음마다 카드 이름(공개 카드 링크)·문항 수, 항목마다 질문·내 답(오답 표시)·정답·해설·"n번 틀림"·분석 제목과 "다시 풀기"(`/analyses/{id}/quiz?from=wrong-notes`)가 보인다. 그 퀴즈 화면의 돌아가기 링크는 "← 오답 노트로 돌아가기"다. 로딩 / 빈 상태("틀린 문항이 없어요" + 내 분석 링크) / 오류 + "다시 시도"가 있다 |
| AC13 | (FE 진입) 로그인했을 때 | 메뉴·대시보드·복습 계획을 보면 | 메뉴 "학습" 그룹에 "오답 노트"(복습 다음)가 있고, 대시보드(SC-04)와 복습 계획(SC-11)에 오답 노트 링크가 있다. 비로그인으로 `/wrong-notes`에 가면 로그인으로 보낸다 |
| AC14 | (FE 늦은 응답) SC-07·SC-08·SC-27에서 | 조회 중에 분석이 바뀌거나 화면을 떠나면 | 늦게 온 응답이 화면 상태를 바꾸지 않는다 (`useLatestRequest`, 세 파일을 `latestRequestAdoption.spec.js` `ADOPTED`에 등록) |

## 3. API 계약

> Swagger Tag: `퀴즈` (`docs/conventions.md` 8절, 기존 `QuizController`). 구현 후 `docs/api/openapi.json`과 일치해야 한다.

### `GET /api/quizzes/{quizId}/attempts`

- 권한: 로그인 필요 · 퀴즈를 열 수 있는 사람 (내 분석 또는 공유받은 팀 분석)
- 응답 `200` — 내 풀이 최신순 최대 20개. 없으면 `[]`

```json
[
  { "attemptId": 31, "score": 5, "total": 6, "createdAt": "2026-11-30T21:14:03+09:00" },
  { "attemptId": 16, "score": 3, "total": 6, "createdAt": "2026-11-29T17:42:05+09:00" }
]
```

- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 401 | AUTH_401 | 비로그인 |
| 404 | QUIZ_404 | 퀴즈가 없거나 열 수 없음 (AC2) |

### `GET /api/quizzes/{quizId}/attempts/{attemptId}`

- 권한: 위와 같음 + 풀이가 내 것이고 이 퀴즈의 풀이
- 응답 `200` — `results` 항목은 F-14 `QuizResultItem`을 그대로 쓴다 (`reviewAdded`는 없다)

```json
{
  "attemptId": 16,
  "score": 3,
  "total": 6,
  "createdAt": "2026-11-29T17:42:05+09:00",
  "results": [
    {
      "questionId": 22,
      "no": 2,
      "question": "JwtTokenFilter가 Authorization 헤더에서 토큰을 꺼낸 뒤 하는 일로 알맞은 것은?",
      "choices": ["토큰을 DB에 저장한다", "사용자 id를 꺼내 SecurityContext에 넣는다", "새 토큰을 발급한다", "요청을 거절한다"],
      "selectedIndex": 3,
      "answerIndex": 1,
      "correct": false,
      "explanation": "jwtService.getSubFromToken()으로 사용자 id를 꺼내 SecurityContextHolder에 넣습니다.",
      "concept": { "id": 57, "name": "JWT", "category": "security", "difficulty": "JUNIOR", "summary": "서명된 토큰으로 사용자를 확인하는 방식", "wishCount": 4 }
    }
  ]
}
```

- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 401 | AUTH_401 | 비로그인 |
| 404 | QUIZ_404 | 퀴즈가 없거나 열 수 없음 |
| 404 | QUIZ_001 | 새 코드. "풀이 기록을 찾을 수 없습니다." 남의 풀이·다른 퀴즈의 풀이·없는 id (AC4) |

### `GET /api/users/me/wrong-notes`

- 권한: 로그인 필요
- 응답 `200`

```json
{
  "totalCount": 3,
  "groups": [
    {
      "concept": { "id": 57, "name": "JWT", "category": "security", "difficulty": "JUNIOR", "summary": "서명된 토큰으로 사용자를 확인하는 방식", "wishCount": 4 },
      "items": [
        {
          "questionId": 22,
          "quizId": 3,
          "analysisId": 7,
          "analysisTitle": "JWT 기반 인증",
          "question": "JwtTokenFilter가 Authorization 헤더에서 토큰을 꺼낸 뒤 하는 일로 알맞은 것은?",
          "choices": ["토큰을 DB에 저장한다", "사용자 id를 꺼내 SecurityContext에 넣는다", "새 토큰을 발급한다", "요청을 거절한다"],
          "selectedIndex": 3,
          "answerIndex": 1,
          "explanation": "jwtService.getSubFromToken()으로 사용자 id를 꺼내 SecurityContextHolder에 넣습니다.",
          "wrongCount": 2,
          "lastAnsweredAt": "2026-11-30T21:14:03+09:00"
        }
      ]
    },
    {
      "concept": null,
      "items": [
        {
          "questionId": 40,
          "quizId": 5,
          "analysisId": 12,
          "analysisTitle": "제목 없는 분석",
          "question": "ArticleApi의 updateArticle()이 수정 전에 하는 일은 무엇인가요?",
          "choices": ["작성자인지 확인한다", "slug를 바꾼다", "토큰을 새로 발급한다", "글을 지우고 다시 만든다"],
          "selectedIndex": 2,
          "answerIndex": 0,
          "explanation": "작성자가 아니면 403을 던집니다.",
          "wrongCount": 1,
          "lastAnsweredAt": "2026-11-29T10:02:41+09:00"
        }
      ]
    }
  ]
}
```

- 에러: 401 `AUTH_401`

### 기존 API

- `GET /api/analyses/{id}/quiz`·`POST /api/quizzes/{quizId}/attempts`는 바꾸지 않는다. 선지 섞기는 FE가 표시 순서만 바꾸고 `selectedIndex`는 저장된 위치로 보낸다 (AC11).

## 4. DB 변경

- Flyway: **없음**
- 사용 테이블: `quizzes`·`quiz_questions`·`quiz_attempts`·`quiz_answers`(읽기), `analyses`(삭제 여부·제목·`owner`), `projects`(이름), `team_analyses`·`team_members`(공유), `concepts`(이름·공개 상태)
- 오답 노트 쿼리: 내 풀이(`quiz_attempts.user_id = 나`)의 답을 문항별로 `ROW_NUMBER() OVER (PARTITION BY question_id ORDER BY a.created_at DESC, a.id DESC)`로 마지막 답을 고르고, 마지막 답이 오답인 문항만 남긴다. `wrongCount`는 같은 범위의 `SUM(NOT is_correct)`. 열 수 있는 분석 조건은 `TeamMapper.isSharedWith`와 같은 JOIN을 쓴다. 기존 인덱스 `idx_quiz_attempts_user_created`·`uk_quiz_answers_attempt_question`로 충분하다 (MySQL 8 윈도 함수)

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | SC-07 퀴즈 탭(`AnalysisQuizTab.vue`) 풀이 기록, SC-08 `/analyses/:id/quiz`(`QuizView.vue`) 선지 다시 섞기·`?from=wrong-notes`, **SC-27 새 화면** `/wrong-notes` → `views/WrongNoteView.vue` (`meta.requiresAuth`, `nav: { label: '오답 노트', order: 35, show: 'user', group: 'learn', icon: 'NotebookPen' }`, 라우트는 `routes/analysis.js`. 아이콘은 공통 파일 `utils/navIcons.js`에 `NotebookPen` 한 줄 추가 → PR에서 김준섭 @멘션) |
| 컴포넌트 | `components/quiz/QuizAttemptHistory.vue`(회차 목록·펼치기, 상세는 `QuizQuestion` 결과 모드 재사용), `components/quiz/WrongNoteItem.vue`, `QuizQuestion.vue`는 표시 순서(`order`)를 받아 섞인 순서로 그린다 |
| 유틸 | `utils/quizShuffle.js` `reshuffle(prevOrder, random)` — 직전 순서와 다른 순열을 만든다(같으면 한 칸 회전). 난수를 주입해 테스트한다 |
| store | 없음 (화면 안 상태) |
| API 함수 | `src/api/quizzes.js`의 `fetchQuizAttempts(quizId)`, `fetchQuizAttempt(quizId, attemptId)`, `fetchWrongNotes()` |
| 상태별 UI | SC-07 기록: 펼친 회차 로딩·오류+다시 시도 / SC-27: 로딩·빈 상태·오류+다시 시도 |
| 진입 링크 | `DashboardView.vue`·`ReviewPlanView.vue`에 "오답 노트" 링크 한 줄 (두 화면의 Tailwind 이전은 AM-1) |
| 화면 이전 | 내용을 고치는 `QuizView.vue`·`AnalysisQuizTab.vue`·`QuizQuestion.vue`와 새 파일은 Tailwind·토큰(F-29)과 `useLatestRequest`로 쓴다. #250의 `AnalysisQuizTab.vue`·`QuizView.vue` 칸을 체크한다. 재생성 폴링(`usePolling`)은 그대로 둔다 |

## 6. LLM (AI 기능만)

해당 없음 (LLM 호출 없음).

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1~AC4, AC9 | `quiz/QuizAttemptHistoryTest` | MockMvc 통합 (Testcontainers) |
| AC5~AC9 | `quiz/WrongNoteTest` | MockMvc 통합 (Testcontainers) |
| AC10 | `components/quiz/__tests__/QuizAttemptHistory.spec.js`, `AnalysisQuizTab.spec.js` | Vitest |
| AC11 | `utils/__tests__/quizShuffle.spec.js`, `views/__tests__/QuizView.spec.js` | Vitest |
| AC12 | `views/__tests__/WrongNoteView.spec.js`, `QuizView.spec.js`(`?from=`) | Vitest |
| AC13 | `router/__tests__` 기존 nav 테스트, `DashboardView.spec.js`·`ReviewPlanView.spec.js` 링크 | Vitest |
| AC14 | 세 화면 spec의 늦은 응답 케이스 + `latestRequestAdoption.spec.js` | Vitest |
| AC10~AC13 | ui-verifier: SC-07 기록 펼침, SC-08 다시 풀기, SC-27 (데스크톱·모바일·다크) | 화면 점검 |

## 8. 범위 밖

- 오답만 모은 새 퀴즈 세트, 변형 문항, 오답 개인화 해설 → F-31
- 오답 노트에서 바로 풀기(문항 단위 채점) — "다시 풀기"는 그 분석의 퀴즈 전체로 보낸다
- 문항 순서 섞기, 풀이마다 섞은 순서 저장 (기록은 저장된 순서로 보인다)
- "학습함"(정답 우선) 정의·통계 API·이어서 학습 → L-1c
- 삭제·공유 해제된 분석의 풀이 기록 보기 (데이터는 남지만 화면에 보이지 않는다)
- 풀이 기록 페이지 넘기기 (최근 20개), 오답 노트 필터·검색
- 대시보드·복습 계획 화면의 Tailwind 이전 → AM-1

## 9. 결정 필요

- 없음 (11-28 사용자 선택 4건 반영: 마지막 답이 틀린 문항, 열 수 있는 분석만, FE에서 섞고 저장 위치로 제출, 새 화면 + 링크만)
- 구현 중 정정 (11-28): 새 에러 코드 이름 `QUIZ_ATTEMPT_404` → **`QUIZ_001`** (에러 코드 형식 규칙 `<도메인>_<상태 or 번호>`, `ErrorCodeTest`. 결정로그 11-28)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/L-1b-wrong-notes`
- [x] Flyway 마이그레이션 — 없음
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] BE 구현 (green): `QuizController` GET 2개, `WrongNoteController`(또는 `QuizController`) `/api/users/me/wrong-notes`, `ErrorCode.QUIZ_001`
- [x] FE 구현 (green)
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [x] contract-checker 통과
- [ ] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영: 화면 설계서 06(SC-07·SC-08 바뀐 부분, SC-27 새 행·흐름도·스크린샷), 요구사항 정의서 01(오답 노트 행), 유스케이스 02(퀴즈 다시 풀기·오답 노트), 07 API 명세 재생성
- [ ] #250 체크 (`AnalysisQuizTab.vue`·`QuizView.vue`)
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
