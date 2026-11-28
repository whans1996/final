# NT-1 헤더 알림 기반 + 소셜 이벤트

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | NT-1 (로드맵 항목. 후속 NT-2는 A의 분석·복습·팀 이벤트) |
| 우선순위 | T1 (12/14 기능 동결 전, 12/02 B→A 넘겨주기) |
| 영역 오너 | B 김준섭 (카드·소셜) |
| 관련 필수 요구사항 | 없음 (결정로그 11-27 "새 기능: 헤더 알림") |
| 의존 spec | F-02 (로그인), F-29b (앱 셸 `AppHeader` `#notifications` 슬롯, AC12), F-18 (댓글 `CommentService.create`), F-04 (팔로우 `FollowService.follow`), F-25 (챌린지 달성 `ChallengeProgressService.recount`, **A 영역**), F-03 (탈퇴 `UserWithdrawalService`, 공통) |
| 관련 이슈 | 없음 |
| 승인 | 김준섭 2026-11-29 |

## 1. 목적

로그인한 회원이 헤더의 종 아이콘에서 **나에게 생긴 일**(내 글에 달린 댓글, 새 팔로워, 챌린지 달성)을 보고 눌러서 그 화면으로 간다.
학습 루프(복습)·팀 시나리오로 다시 불러오는 장치이며 (결정로그 11-27), 이 항목은 **기반**(테이블·API·읽음 처리·드롭다운·알림 생성 공통 서비스)과 B 소셜 이벤트를 만든다.
A는 NT-2(12/12~12/14)에서 `NotificationService.notify(...)` 한 줄 호출과 `NotificationType` 상수 추가만으로 분석 완료·실패·오늘 복습·팀 알림을 붙인다 (12/02 넘겨주기).

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_내_알림만_최신순으로_돌려준다()`

### 알림 API (BE)

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 로그인한 회원 A에게 알림 3개, 회원 B에게 알림 1개가 있을 때 | A가 `GET /api/notifications` (`?page=&size=`, 기본 20) | 200 `PageResponse` `{items, page, size, total}`. **A의 알림만** `created_at` 최신순(같으면 id 내림차순). 항목은 `{id, type, message, link, actor, read, createdAt}` (3절). `actor`는 알림을 일으킨 회원의 **지금** `{id, nickname}`(탈퇴했으면 마스킹 닉네임 `#탈퇴{id}` 그대로), 없으면 `null`. page·size가 범위 밖이면 400 `COMMON_400` |
| AC2 | A에게 안 읽은 알림 2개·읽은 알림 1개가 있을 때 | `GET /api/notifications/unread-count` | 200 `{count: 2}`. 알림이 없으면 `{count: 0}` |
| AC3 | A의 안 읽은 알림 n이 있을 때 | `PATCH /api/notifications/{n}/read` | 204. `read_at`이 채워지고 unread-count가 1 줄어든다. **이미 읽은 알림이면** 204이고 `read_at`은 처음 값 그대로다 (멱등) |
| AC4 | A에게 안 읽은 알림 3개, B에게 안 읽은 알림 1개가 있을 때 | A가 `PATCH /api/notifications/read-all` | 204. A의 알림이 모두 읽음이 되고 B의 알림은 그대로다. 안 읽은 알림이 없어도 204 |
| AC5 | | 남의 알림·없는 id로 `PATCH /{id}/read` / 토큰 없이 네 API 중 아무거나 / 탈퇴한 회원의 남은 Access 토큰으로 `PATCH` | 각각 404 `NOTIFICATION_404`(남의 것과 없는 것을 구분하지 않는다) / 401 `AUTH_401` / 401 `AUTH_401`. 어느 경우든 알림 행은 바뀌지 않는다 |

### 알림 생성 공통 서비스 (BE, 12/02 A에 넘기는 계약)

| ID | Given | When | Then |
|---|---|---|---|
| AC6 | 활성 회원 R이 있을 때 | `notify(R, type, actorId, message, link)` | `notifications`에 행 1개(`read_at` NULL). **R == actorId면** 넣지 않는다(내가 한 일은 나에게 알리지 않는다). **R이 탈퇴했거나 없으면** 넣지 않는다(예외 없음). `message`가 120자를 넘으면 117자 + `…`로 자른다. `link`가 `/`로 시작하지 않거나 `//`로 시작하면 `IllegalArgumentException`(서버 코드 실수, 사용자 입력 아님) |
| AC7 | 호출한 쪽 트랜잭션이 롤백되면 | (예: 댓글 저장 뒤 예외) | 알림도 남지 않는다 (호출한 쪽 트랜잭션 안에서 넣는다, `ActivityService`와 같은 방식) |
| AC8 | 알림 중 `created_at`이 90일보다 오래된 것과 90일 이내인 것이 있을 때 | 정리 작업 `NotificationPurgeJob.purge()` (매일 03:30 Asia/Seoul, `notification.purge-cron`이 `-`이면 돌지 않음) | 90일보다 오래된 행만 지워진다 (읽음 여부와 무관). 경계: 정확히 90일 전 시각의 행은 남는다 |
| AC9 | 회원 R에게 알림이 있을 때 | R이 탈퇴하면 (`UserWithdrawalService`) | R이 **받은** 알림이 모두 지워진다. R이 **일으킨** 남의 알림은 남고 `actor.nickname`이 마스킹 값으로 보인다 (AC1) |

### 소셜 이벤트 (BE)

| ID | Given | When | Then |
|---|---|---|---|
| AC10 | 회원 W가 회원 A의 글 P(제목 "Spring 질문")에 | 댓글을 쓰면 (`POST /api/posts/{P}/comments`) | A에게 `COMMENT` 알림: actor W, message `회원님의 글에 댓글을 남겼어요 · Spring 질문`(제목은 40자 넘으면 39자 + `…`), link `/community/{P}`. **A가 자기 글에 쓰면** 알림 없음(AC6). 댓글 수정·삭제는 알림을 만들지 않는다 |
| AC11 | 회원 F가 회원 A를 팔로우하지 않은 상태에서 | `POST /api/users/{A}/follow` | A에게 `FOLLOW` 알림: actor F, message `회원님을 팔로우했어요`, link `/users/{F}`. **이미 팔로우 중이라 행이 새로 생기지 않으면** 알림 없음. 언팔로우 뒤 다시 팔로우해도 **F→A의 안 읽은 `FOLLOW` 알림이 이미 있으면** 새로 만들지 않는다 (반복 팔로우로 알림이 쌓이지 않게) |
| AC12 | 회원 A가 챌린지 C(제목 "11월 복습 10회")에 참여 중이고 | 활동으로 목표를 처음 달성하면 (`ChallengeProgressService.recount`에서 `CHALLENGE_COMPLETE` 활동을 남기는 순간) | A에게 `CHALLENGE_COMPLETE` 알림: actor `null`, message `챌린지를 달성했어요 · 11월 복습 10회`, link `/challenges/{C}`. 달성 뒤 다시 세어도(이미 달성) 알림은 1개뿐이다 |

### 프론트엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC13 | 로그인한 회원이 앱 셸(`AppShell`)의 아무 화면에 있을 때 | | 헤더 `#notifications` 슬롯에 종 버튼(`aria-label="알림"`, 안 읽은 게 있으면 `"알림, 안 읽음 N개"`)이 보인다. 안 읽은 수가 1~9면 숫자 배지, 10 이상이면 `9+`, 0이면 배지 없음. 비회원 셸(`GuestShell`)에는 없다 |
| AC14 | 종 버튼이 보이는 동안 | | 안 읽은 수를 화면 진입 때 1회, 이후 **60초마다** 다시 조회한다 (`usePolling`, 문서가 숨겨져 있으면 건너뛰고 다시 보이면 바로 1회). 로그아웃하거나 셸을 떠나면 멈춘다. 조회가 실패하면 배지를 그대로 두고 다음 주기에 다시 한다(에러 문구 없음) |
| AC15 | 종 버튼을 누르면 | | 드롭다운(Reka UI, Esc·바깥 클릭으로 닫힘)이 열리며 목록 첫 20개를 불러온다(열 때마다 새로). 항목은 actor가 있으면 **{닉네임}**님이 + message, 없으면 message만, type별 아이콘(COMMENT `MessageSquare`, FOLLOW `UserPlus`, CHALLENGE_COMPLETE `Trophy`, 모르는 type `Bell`), 상대 시각(F-04 피드와 같은 규칙), 안 읽은 항목은 점·배경 강조. 로딩 중 스켈레톤, 0개면 "새 알림이 없습니다", 실패면 서버 `message` + "다시 시도". 20개가 넘으면 "더 보기"(`useMoreList`, 이미 보인 id는 거름) |
| AC16 | 드롭다운에서 | 안 읽은 항목을 누르면 / 읽은 항목을 누르면 / "모두 읽음"을 누르면 | 읽음 요청(AC3)을 보내고 **응답을 기다리지 않고** `link`로 이동하며 드롭다운을 닫는다. 화면의 그 항목·배지를 바로 읽음으로 바꾼다(요청이 실패해도 이동은 한다) / 바로 `link`로 이동 / AC4 요청 후 목록 항목과 배지를 모두 읽음으로 바꾼다(요청 중 버튼 비활성, 실패하면 버튼 옆에 서버 `message`). 안 읽은 게 없으면 "모두 읽음"은 비활성 |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다. Swagger Tag: **`알림`** (새 태그, `docs/conventions.md` 8절에 추가). 네 API 모두 로그인 필요 (`SecurityConfig` 공개 경로에 넣지 않는다).

### `GET /api/notifications`

- 권한: 로그인 필요. `?page=1&size=20` (`PageQuery`)
- 응답 `200`. `link`는 FE 라우터 경로(서버가 만든 값). `read` = `read_at IS NOT NULL`

```json
{
  "items": [
    {
      "id": 41, "type": "COMMENT",
      "message": "회원님의 글에 댓글을 남겼어요 · Spring 질문",
      "link": "/community/12",
      "actor": { "id": 7, "nickname": "java러버" },
      "read": false, "createdAt": "2026-11-29T14:02:11+09:00"
    },
    {
      "id": 38, "type": "CHALLENGE_COMPLETE",
      "message": "챌린지를 달성했어요 · 11월 복습 10회",
      "link": "/challenges/3",
      "actor": null,
      "read": true, "createdAt": "2026-11-28T21:40:00+09:00"
    }
  ],
  "page": 1, "size": 20, "total": 2
}
```

- 에러: 400 `COMMON_400`(page·size), 401 `AUTH_401`

### `GET /api/notifications/unread-count`

- 응답 `200`: `{ "count": 3 }`
- 에러: 401 `AUTH_401`

### `PATCH /api/notifications/{id}/read`

- 요청 본문 없음. 응답 `204`
- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 401 | AUTH_401 | 토큰 없음·무효, 탈퇴한 회원의 토큰 |
| 404 | NOTIFICATION_404 | 없거나 남의 알림 ("알림을 찾을 수 없습니다.") |

### `PATCH /api/notifications/read-all`

- 요청 본문 없음. 응답 `204`. 에러: 401 `AUTH_401`

### 알림 생성 공통 서비스 (내부 계약, A NT-2용)

```java
// com.vibenote.notification.NotificationService
@Transactional  // 호출한 쪽 트랜잭션에 합류 (AC7)
public void notify(long recipientId, NotificationType type, Long actorId, String message, String link);

// com.vibenote.notification.NotificationType — 자기 도메인 블록에만 상수를 추가한다 (ActivityType과 같은 규칙)
// ── SOCIAL (B NT-1) ──  COMMENT, FOLLOW, CHALLENGE_COMPLETE,
// ── ANALYSIS·REVIEW·TEAM (A NT-2) ──  (A가 추가: 예 ANALYSIS_DONE, ANALYSIS_FAILED, REVIEW_DUE, TEAM_...)
```

- `type` 컬럼은 `VARCHAR(40)`이라 **A가 상수를 더할 때 마이그레이션이 필요 없다**. FE는 모르는 type도 `Bell` 아이콘 + message로 그린다 (AC15) → A는 FE를 고치지 않아도 된다(아이콘을 바꾸고 싶으면 `notificationIcons.js` 한 줄).
- 문장은 호출하는 쪽이 만든다. actor가 있으면 FE가 앞에 "{닉네임}님이 "를 붙이므로 message는 주어 없이 쓴다 (`회원님의 글에 댓글을 남겼어요 · …`). 제목을 넣을 때는 `NotificationService.clip(title, 40)`을 쓴다.

## 4. DB 변경

- Flyway: `V18__notifications.sql` (번호는 구현 시작 때 `node scripts/next-migration.mjs`로 다시 확인)

```sql
CREATE TABLE notifications (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT       NOT NULL,
    type         VARCHAR(40)  NOT NULL,
    actor_id     BIGINT       NULL,
    message      VARCHAR(120) NOT NULL,
    link         VARCHAR(255) NOT NULL,
    read_at      DATETIME     NULL,
    created_at   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notifications_recipient_created (recipient_id, created_at, id),
    KEY idx_notifications_recipient_unread (recipient_id, read_at),
    KEY idx_notifications_created (created_at),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL
);
```

- 넣기는 `INSERT … SELECT … FROM users WHERE id = #{recipientId} AND status = 'ACTIVE'`로 한 문장에서 수신자 활성 여부를 본다 (AC6, 조회 추가 없음).
- `created_at`은 서비스 `Clock` 시각(초 단위)으로 채운다 (정리 작업·테스트 시각 고정).
- 사용 테이블: `notifications`(새), `users`(actor 닉네임 조인), `follows`(`insertIgnore`가 넣은 행 수를 돌려주게 바꿈).
- 산출물: ERD(`docs/산출물/03`), `docs/기획서.md` ERD·주요 API 표에 `notifications`·알림 API 4개 추가 (구현 PR).

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | 새 화면 없음. `AppShell.vue`가 `AppHeader`의 `#notifications` 슬롯에 `NotificationBell`을 넣는다 |
| 컴포넌트 | `src/components/notification/NotificationBell.vue` (종 버튼·배지·드롭다운), `NotificationItem.vue`, `notificationIcons.js` (type → Lucide 아이콘) |
| store | `src/stores/notifications.js` — `unreadCount`, `refreshCount()`, `markRead(id)`, `markAllRead()`. 로그아웃(`auth` 초기화) 때 0으로 |
| API 함수 | `src/api/notifications.js`의 `fetchNotifications({page,size})`, `fetchUnreadCount()`, `markNotificationRead(id)`, `markAllNotificationsRead()` |
| 상태별 UI | 로딩 스켈레톤 / "새 알림이 없습니다" / 서버 `message` + "다시 시도" (AC15) |
| 재사용 | `usePolling`(60초), `useMoreList`(더 보기), 상대 시각은 `@/utils/relativeTime`의 `formatRelativeTime` (`frontend/CLAUDE.md` V-4 규칙: 새 조회는 `useLatestRequest`) |
| 산출물 | 화면 설계서 2-1절 공통 셸에 알림 드롭다운 스크린샷(데스크톱·모바일) |

## 6. LLM (AI 기능만)

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1~AC5 | `NotificationControllerTest#AC1_…`~`AC5_…` | MockMvc 통합 (Testcontainers) |
| AC6·AC7 | `NotificationServiceTest#AC6_…`·`AC7_…` | 통합 (트랜잭션 롤백 확인) |
| AC8 | `NotificationPurgeJobTest#AC8_…` | 통합 (고정 `Clock`, 경계 90일) |
| AC9 | `UserWithdrawalServiceTest` 또는 `NotificationWithdrawalTest#AC9_…` | 통합 |
| AC10 | `CommentNotificationTest#AC10_…` | MockMvc 통합 |
| AC11 | `FollowNotificationTest#AC11_…` | MockMvc 통합 |
| AC12 | `ChallengeNotificationTest#AC12_…` | 통합 (`ChallengeProgressService.recount`) |
| AC13·AC14 | `components/notification/__tests__/NotificationBell.spec.js` | Vitest (가짜 타이머, `document.visibilityState`) |
| AC15·AC16 | `NotificationBell.spec.js`, `NotificationItem.spec.js` | Vitest |
| AC13~AC16 | ui-verifier (데스크톱·모바일, 라이트·다크) | 화면 |

## 8. 범위 밖

- A 이벤트(분석 완료·실패, 오늘 복습할 카드, 팀 활동) → **NT-2** (A, 12/12~12/14)
- 실시간 푸시(SSE·WebSocket), 브라우저·이메일 알림
- 전용 `/notifications` 화면, 알림 개별 삭제, 알림 종류별 끄기 설정
- 팔로우한 사람의 활동 알림(피드 F-04가 맡는다), 새 챌린지 공개 전체 알림
- 대상이 지워진 알림 정리 (글·챌린지가 지워져도 알림은 남고, 누르면 그 화면의 404 처리를 따른다)
- 같은 글에 댓글이 여러 개 달릴 때 알림 묶기 ("외 n개")

## 9. 결정 필요

- (없음. 11-29 spec 질문 답(문장·링크는 서버 저장, 챌린지 = 내가 달성, 60초 폴링 + 열 때 조회, 드롭다운 20개 + 더 보기·90일 정리) 반영)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/NT-1-header-notifications`
- [x] Flyway 마이그레이션
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] BE 구현 (green)
- [x] FE 구현 (green)
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [x] `docs/conventions.md` 5절 `NOTIFICATION` 에러 접두사·8절 `알림` 태그
- [ ] contract-checker 통과
- [ ] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영 (ERD·기획서 API 표·화면 설계서 2-1절·요구사항 정의서)
- [ ] PR 본문 "공통 코드 변경"에 @whans1996 (`ChallengeProgressService` A 영역 한 줄, `UserWithdrawalService` 공통, `AppShell`)
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
