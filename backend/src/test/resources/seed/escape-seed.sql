-- 생성 파일, 직접 고치지 말 것.
-- 원본은 data/seed/concepts/<category>/<slug>.md 카드 파일이고 `node scripts/seed-build.mjs`로 다시 만든다.
-- CI가 `seed-build.mjs --check`로 원본과 같은지 확인하고, `seed-check.mjs`가 형식·예제를 검사한다 (K-3a).
--
-- 시드 개념 카드 (DS-05, WBS 2.5). 반복 마이그레이션이라 내용이 바뀌면 다시 적용된다.
--
-- 출처 규칙 (docs/산출물/09-데이터셋.md DS-01 카드 작성 규칙)
--   - 근거는 DS-01(split content) 공식 문서만 쓴다. 평가(eval) 저장소 코드는 쓰지 않는다.
--   - 본문은 팀이 직접 쓴다. 원문 번역·각색 금지. Oracle·Swagger·MDN은 링크만 건다.
--   - 예제 코드는 개념에 맞게 새로 쓴다. reference_url은 대표 출처 1개.
-- 주의
--   - name(UNIQUE) 기준 upsert라 id는 유지된다(찜·리뷰 FK 보존). 대신 카드 파일을 고치면
--     관리자(F-13)가 화면에서 수정한 시드 카드 내용이 카드 파일 내용으로 덮어써진다.
--   - status는 검수 전 초안(DRAFT)일 때만 파일 값으로 바꾼다(F-13 AC12). 새 카드와 AI가 먼저 초안으로
--     만든 이름은 파일의 PUBLISHED가 되고, 관리자가 정한 PUBLISHED·HIDDEN은 유지한다.
--     시드 카드를 내리려면 관리자 화면에서 숨긴다. (status 줄은 source보다 먼저 둔다: 기존 값을 본다)
--   - 카드 이름을 바꾸면 새 카드가 생긴다. 이름은 바꾸지 말고 필요하면 새 카드를 추가한다.
--   - 작은따옴표와 역슬래시는 생성기가 이스케이프한다 (MySQL 기본 모드에서 역슬래시는 이스케이프 문자).

INSERT INTO concepts (name, category, difficulty, summary, body, example_code, status, source, reference_url) VALUES

-- ───────────── java ─────────────

-- backend/src/test/resources/seed/cards/java/escape-fixture.md
('이스케이프 검사 카드 (테스트 전용)', 'java', 'BEGINNER',
 'It''s 테스트 전용 카드다. 정규식 ''\\d+''와 경로 C:\\temp가 그대로 들어가야 한다.',
 '- 작은따옴표: It''s, ''quoted''
- 역슬래시: \\d+ \\\\ \\n \\'' 끝\\',
 'Pattern digits = Pattern.compile("\\\\d+");
String path = "C:\\\\temp\\\\It''s";',
 'PUBLISHED', 'SEED', 'https://docs.oracle.com/javase/tutorial/essential/regex/')

AS seed
ON DUPLICATE KEY UPDATE
    status        = IF(concepts.status = 'DRAFT', seed.status, concepts.status),
    category      = seed.category,
    difficulty    = seed.difficulty,
    summary       = seed.summary,
    body          = seed.body,
    example_code  = seed.example_code,
    source        = seed.source,
    reference_url = seed.reference_url;
