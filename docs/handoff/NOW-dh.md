# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 17:09 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/issue-249-env-outside

## 현재 기능

**#249 .env 저장소 밖으로·가드 범위 고정 PR #253** — 브랜치 `feature/issue-249-env-outside` (head 8867b12). `Closes #249`, 후속 #251. 라벨 `codex-stopped` 생성함

## 진행

- ✅ 제안 1~4 구현(f27f158) + reviewer 반영(8867b12): guard가 AI의 codex-stopped 라벨 추가 차단, 키 단위 병합 안내, moveEnv 실패 처리, yml 순서 단언, AGENTS.md 면제 축소
- ✅ verify all·pre-push 통과. PR #253 열림, @JoonseobKim 멘션(머지 뒤 js PC 2대 `--move-env`). 이 PC는 이미 옮김(머지 전엔 이 브랜치에서만 BE)
- 🟡 CI 진행 중, Codex `--request --wait --status` 백그라운드 대기 중

## 바로 다음 할 일

1. `gh pr checks 253`에서 Backend 검증 초록 확인 (문서 커밋 뒤 CI 재실행)
2. CI 초록 + Codex COMPLETE → 사람이 GitHub에서 #253 머지 → #249 닫힘 확인
3. 머지 뒤 dh-home에서 `node scripts/setup.mjs --move-env` (#251 체크리스트)
4. 11/29부터 P-1a `/spec` (Opus/high)

## 막힌 점 / 결정 필요
- 사람: 노션 팀원 초대, F-05b liveTest · 로드맵 E-2 행

## 주의
- 인라인 node -e에 .env 문자열을 넣으면 가드가 막음(스크립트 파일로) · scripts/에 prettier 금지 · 다음 마이그레이션 V15
- 하네스 빈틈: switch로 package.json이 바뀌어도 npm ci 안내 없음(이슈 후보) · 상대 PR #252(V-4) 열림
