# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-29 06:02 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/NT-1-header-notifications

## 현재 기능

**NT-1 헤더 알림 기반 + 소셜 이벤트** `docs/specs/NT-1-header-notifications.md` (APPROVED 11-29, /implement 중) · 브랜치 `feature/NT-1-header-notifications` · 로드맵 NT-1 (11/28~12/02, 12/02 B→A 넘겨주기)

## 진행

- ✅ 작성(미커밋): `V18__notifications.sql`, `notification` 패키지 전체, `ErrorCode.NOTIFICATION_404`, yml·IntegrationTestBase `notification.purge-cron`, `UserWithdrawalService`가 `notificationService.removeAllOf` 호출(AC9)
- ✅ 테스트(미커밋): `notification/` AC1~AC9 5개 클래스, ErrorCodeTest·FlywayMigrationTest 목록에 NOTIFICATION·notifications 추가, UserWithdrawalServiceTest 순서 검증에 알림 추가. AC2 0 확인은 별도 회원(MyBatis 1차 캐시)
- ✅ 테스트(미커밋, red 확인 중): `post/CommentNotificationTest`(AC10)·`follow/FollowNotificationTest`(AC11)·`challenge/ChallengeNotificationTest`(AC12)
- ✅ 문서(미커밋): conventions 3·8절, 기획서 ERD·API 표, 산출물 03 ERD(관계·엔티티·설명·V18)

## 바로 다음 할 일

1. AC10: `CommentMapper`에 글쓴이·제목 select 추가 → `CommentService.create`에서 `notify(author, COMMENT, userId, "회원님의 글에 댓글을 남겼어요 · "+clip(title,40), "/community/"+postId)`
2. AC11: `FollowMapper.insertIgnore` int 반환 → `FollowService.follow`에서 1이면 `notifyUnlessUnread(targetId, FOLLOW, userId, "회원님을 팔로우했어요", "/users/"+userId)`
3. AC12: `ChallengeParticipantMapper.xml findOpenOneForUpdate`에 `c.title` 추가, `ChallengeProgressService.recount` 달성 분기(updated==1)에 notify(actor null, "/challenges/"+id)
4. verifier green → `feat(NT-1): BE 알림 API·공통 서비스·소셜 이벤트 (AC1~AC12)` 커밋(openapi.json 포함)·push → FE AC13~16

## 막힌 점 / 결정 필요
- 없음. BE/FE PR 분리 여부 미결. GMS 키 만료 12/09 18:00

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용을 적지 않는다.**
- A 파일 `ChallengeProgressService`·`ChallengeParticipantMapper.xml`, 공통 `UserWithdrawalService`·`IntegrationTestBase`·`ErrorCode` → PR에서 @whans1996 멘션
