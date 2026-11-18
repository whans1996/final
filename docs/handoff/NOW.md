# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 11:00 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-03-profile

## 현재 기능

F-03 프로필·탈퇴 — **spec APPROVED (whans1996 11-18), /implement 시작 전** (`docs/specs/F-03-profile.md`, AC10개, Flyway 없음)

## 진행

- ✅ F-01·F-02 develop 머지(b6dd1d1). /pickup 시 verify all 통과 (BE 84, FE 51)
- ✅ spec 초안 작성. 확정(11-18): 즉시 마스킹·재가입 허용, 탈퇴 시 비밀번호 재확인(변경은 범위 밖), 학습한 카드 = 퀴즈 정답 ∪ 복습 DONE, 탈퇴 시 refresh 전부·follows 양방향 삭제 → 기획서 결정 로그 반영
- 새 에러 코드 예정: USER_404, USER_001. 공개 GET `/api/users/{id}`·`/{id}/stats`

## 바로 다음 할 일

1. ✅ spec 승인 커밋 완료
2. `/implement F-03`: 먼저 `user/UserProfileApiTest#AC1_...`(red) → `UserController` GET /me. 탈퇴는 순환 의존 때문에 `user/UserWithdrawalService`로 분리(`AuthService.revokeAll`, 새 `follow/FollowService.removeAllOf`)

## 막힌 점 / 결정 필요

- WBS 3.1(11/20~11/26)이 W1 마감(11/23)을 넘음 → F-03을 11/21까지 끝내면 W1 안 (사람 결정)
- 팀원 GitHub 아이디(리뷰어 지정), 제출 zip 이름용 지역·반·이름 (사람)
- 작업로그에는 FE 53개로 적혀 있지만 이번 verify는 51개 → PR 전에 개수 확인

## 주의

- 로컬 실행: refresh 쿠키는 `local` 프로필에서만 Secure=false. 비밀번호는 ASCII 출력 문자만 허용(BCrypt 72바이트)
- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금)
- V1·V2 수정 금지. 이번 기능은 마이그레이션 없음
