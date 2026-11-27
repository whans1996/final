# AGENTS.md

바이브노트(VibeNote) 저장소다. 프로젝트 규칙의 정본은 `CLAUDE.md`, `backend/CLAUDE.md`, `frontend/CLAUDE.md`, `docs/conventions.md`이다. 이 파일은 Codex PR 리뷰가 그 규칙과 같은 기준으로 심각도를 매기도록 요점만 적는다.

## Review guidelines

- 리뷰 댓글은 한국어로 쓴다.
- P1은 spec(`docs/specs/`) 수용 기준이나 보안 기준(인증·인가, 비밀 누출, 경로 조작, 주입, 서비스 중단)을 깨는 결함에만 쓴다. 그 밖의 개선은 P2로 쓴다 (`docs/conventions.md` 7절).
- **하네스 가드(`scripts/lib/guard.mjs`, `scripts/lib/file-guard.mjs`, `.claude/hooks/guard-*.mjs`)는 AI가 평소 쓰는 명령의 실수를 막는 장치이고 보안 경계가 아니다.** 비밀은 저장소 밖 `~/.vibenote/.env`에 두고, `.claude/settings.json` `permissions.deny`, git hook, 브랜치 보호, push protection이 따로 막는다. 그래서 인용·이스케이프·변수 치환·글롭·별칭·인라인 코드를 조합해 일부러 판정을 피하는 표기 변형 우회는 **P1로 보고하지 않는다**. 평범한 명령(예: 저장소 루트에서 `grep -r KEY .`, `cat backend/.env`, `git add -A`로 비밀 파일이 섞임)이 통과하는 경우만 P1이다.
- 하네스 스크립트(`scripts/`, `.claude/hooks/`)는 Windows 11(Git Bash·PowerShell)과 macOS에서 돈다. 경로 구분자·줄바꿈 차이로 깨지는 경우는 지적한다.
- `docs/api/openapi.json`은 테스트가 생성한다. 손으로 고친 흔적이나 코드와 어긋난 부분은 지적한다.
- 이미 커밋된 Flyway 파일(`backend/src/main/resources/db/migration/V*__*.sql`)을 고친 변경은 P1이다.
