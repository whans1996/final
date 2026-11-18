# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 10:15 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-03-profile

## 현재 기능

F-03 프로필·탈퇴 — **spec 작성 전**. (F-01·F-02는 PR #3 머지, spec DONE)

## 진행

- ✅ F-01 회원가입·F-02 로그인·JWT 머지 (develop `b6dd1d1`). 인증 API 4개, FE `/login`·`/signup`, 세션 복원·401 자동 재발급
- ✅ CI가 PR 이벤트로 자동 실행됨 (11-18 해결)
- 이 브랜치는 develop에서 새로 만들었고, 첫 커밋은 spec DONE·작업로그·NOW.md 인계

## 바로 다음 할 일

1. `/spec F-03` (`docs/기획서.md`의 F-03 행과 명세서 구조 매핑 확인). 정할 것:
   - 탈퇴: `users.status=WITHDRAWN` soft delete(DELETE 금지, `backend/CLAUDE.md`), 이메일·닉네임 마스킹 규칙(재가입 허용 여부와 UNIQUE 제약)
   - 탈퇴 시 그 회원의 `refresh_tokens` 일괄 삭제 (user_id). `/refresh`는 이미 WITHDRAWN을 거부함(`AuthService.refresh`)
   - 프로필 조회·수정 범위(닉네임·수준 변경, 비밀번호 변경 여부)와 `UserService`·`UserMapper` 재사용
2. spec 승인 → `/implement F-03` → `/pr-review` (PR을 연 뒤 Codex 자동 리뷰 확인 필수, 8단계)

## 막힌 점 / 결정 필요

- WBS 3.1(11/20~11/26)이 W1 마감(11/23)을 넘음 → F-03을 W2 초로 조정 제안 (사람)
- 팀원 GitHub 아이디(리뷰어 지정), 제출 zip 이름용 지역·반·이름 (사람)

## 주의

- 로컬 실행: refresh 쿠키는 `local` 프로필에서만 Secure=false (http). 비밀번호는 ASCII 출력 문자만 허용(BCrypt 72바이트)
- 브라우저 콘솔의 첫 `POST /api/auth/refresh` 401은 비로그인 세션 복원이라 정상
- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금). 다른 PC는 pull 후 `git ls-files --eol | grep w/crlf` 확인
- V1·V2 수정 금지. 스키마 변경은 `V3__...sql` 새 파일로
