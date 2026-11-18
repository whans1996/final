# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 11:55 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-03-profile

## 현재 기능

F-03 프로필·탈퇴 — **/implement 진행 중** (`docs/specs/F-03-profile.md` APPROVED, AC10개, Flyway 없음). 세션 목표: BE AC1~AC8 → FE AC9·AC10

## 진행

- ✅ spec 승인 커밋 b619923
- 🟡 AC1~AC5 구현 완료, **미커밋** (AC1·2·4·5 green 확인, AC3 green 확인 중): `ErrorCode` +USER_404·USER_001, `User.createdAt`, `UserMapper`(+existsByNicknameExceptId·updateProfile), dto `UpdateProfileRequest`·`MyProfileResponse`·`UserProfileResponse`·`UserStatsResponse`, `UserService.getActiveMember/getPublicMember/getMe/getPublicProfile/updateMe`, `UserStatsService`+`UserStatsMapper.xml`, `UserController`(GET·PATCH /me, GET /{id:\d+}, /{id:\d+}/stats), `SecurityConfig.PUBLIC_GET_PATHS`(GET 전용)
- 테스트: `user/UserTestSupport`, `UserProfileApiTest`, `UserStatsApiTest`, `UserServiceTest#AC5_수정_저장_시_...`, `SecurityConfigTest#F03_공개_프로필_GET만_...`
- conventions 3절 USER_404·USER_001, backend/CLAUDE.md 통계 문구(분석 수는 삭제 제외) 반영

## 바로 다음 할 일

1. ✅ AC1~AC5 커밋
2. AC6~8 `user/WithdrawalApiTest` 먼저(red) → `UserWithdrawalService`(UserMapper·PasswordEncoder·AuthService·FollowService), `AuthService.revokeAll`, `RefreshTokenMapper.deleteByUserId`, `follow/FollowService.removeAllOf`+`FollowMapper.xml`, `dto/WithdrawRequest`, `UserMapper.withdraw`(email `withdrawn:{id}`, nickname `#탈퇴{id}`), DELETE /me(204 + `RefreshTokenCookie.clear()`)

## 막힌 점 / 결정 필요

- WBS 3.1 일정, 팀원 GitHub 아이디, 제출 zip 이름 (사람)

## 주의

- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금)
- V1·V2 수정 금지. 이번 기능은 마이그레이션 없음
