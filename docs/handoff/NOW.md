# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 12:45 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-00-codex-docs-skip

## 현재 기능

하네스 PR #7 (`codex-review.mjs` 보완, 앱 코드 변경 없음). PR #6(세션 운영·Codex 재리뷰 루프)은 42e2626(Codex 5회차 ✅)에서 머지됨. F-01~F-03 모두 DONE

## 진행

- ✅ F-03 spec DONE·작업로그, 화면설계서 스크린샷(`docs/산출물/screens/`), WBS·간트 실적(기준선 유지), 첫 `docs/진행현황.md`(W1 앞섬)
- ✅ 하네스: 머지 후 정리(`/pickup`), WBS 동기화(`/progress`), 스크린샷 직접 첨부(`scripts/pr-screens.mjs`), CLAUDE.md "세션 운영"·에이전트 effort·Stop hook 200k 안내·`scripts/token-report.mjs`
- ✅ Codex 재리뷰 루프: `scripts/codex-review.mjs <PR> --request --wait`(COMPLETE까지, `/pr-review` 8단계·`/pickup`). 첫 실전 루프에서 Codex P2(재요청 판정 경쟁 조건) 반영: head sha 마커, "지적 없음" 영수증 인식. 2회차 P2(댓글 100개 초과 시 첫 페이지만 읽음) → `--paginate --slurp`. 3회차 ✅ 868f20f, 4회차(NOW.md 커밋) P2 출력 버퍼 1MiB → `maxBuffer` 64MiB. 5회차 ✅ 42e2626 → 머지
- 🔄 PR #7: 1d5e4ee 문서만 바뀐 커밋은 재리뷰 생략(`docsOnlySince`, `docs/`·openapi.json 제외, 마지막 리뷰가 지적 없을 때만). b5081ca Codex "create an environment" 안내를 `ERROR`로 즉시 보고(`envNotice`, 11-18 Codex 환경이 사라져 사람이 재생성). 1회차 P2(옛 영수증 뒤 지적을 덮음) → e7c1646 마지막 인라인 지적보다 늦은 영수증만 기준. 2회차 P2(환경 안내 뒤 --request 재시도가 계속 ERROR) → 환경 안내가 마지막 요청보다 늦으면 --request가 요청을 새로 닮(`envMissing`, `postRequest`). 3회차 리뷰는 아직 요청 안 함 (사용 한도)

## 바로 다음 할 일

1. PR #7: `node scripts/codex-review.mjs 7 --request --wait`로 e7c1646 COMPLETE까지 → NOW.md 커밋·push 후 `node scripts/codex-review.mjs 7`이 재리뷰 요청 없이 `COMPLETE … 문서만 변경 (Codex ✅ <sha> 이후)`를 내는지 실전 확인 → CI 초록이면 영역 오너가 머지
2. (선택, 사람) `/context` 결과(11-18): 고정 비용 약 39k(도구 17.9k, 스킬 9.9k 중 플러그인 약 7k, CLAUDE.md 3.5k). 플러그인은 claude.ai 동기화라 저장소 설정으로 못 끔 → 줄이려면 `/plugin`에서 searchfit-seo·product-management·data·design·brightdata·figma 비활성화 (턴당 약 7k, 효과는 `/clear`보다 작음)
3. 다음 기능 (W1 남은 일, 둘 다 W2 기능의 전제): **2.5 시드 개념 카드 30~50개** (`backend/src/main/resources/db/migration/R__seed_concepts.sql`, 카드별 `reference_url`, `data/datasets.json` split이 eval인 데이터 금지) 또는 **2.4 LlmClient Gemini·Ollama** (`backend/.../llm/`, 실제 호출 테스트는 사람 요청 시만). `/spec`으로 시작
4. 그다음 W2: F-05 분석 요청(A), F-09·F-10 카드 목록·상세(B)

## 막힌 점 / 결정 필요

- 팀원 GitHub 아이디(리뷰어 지정), 제출 zip 이름용 지역·반·이름 (사람)

## 주의

- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금). bootRun은 TaskStop 후에도 java.exe가 남을 수 있어 8080 PID를 확인해 종료
- 셸(node -e, 따옴표 없는 heredoc)로 Markdown을 쓸 때 백틱이 명령 치환된다 → Edit 도구 또는 `<<'EOF'`
- V1·V2 수정 금지
