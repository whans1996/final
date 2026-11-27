// 시드 카드 원본 → R__seed_concepts.sql 생성기 (K-3a AC1·AC3)
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';
import { ROOT } from '../lib/common.mjs';
import { LEVEL_SECTIONS, buildSql, parseCard, readCards, renderCard, sqlString, toRow } from '../lib/seed/card.mjs';

/** 수준별 선택 절이 없을 때의 6칸 (K-1 AC13) */
const NO_LEVELS = Object.fromEntries(Object.values(LEVEL_SECTIONS).map((c) => [c, null]));

const row = (over = {}) => ({
  name: '예외 처리',
  category: 'java',
  difficulty: 'BEGINNER',
  summary: '예외는 정상 흐름을 벗어난 상황을 알린다.',
  body: '## 핵심\n- checked 예외\n\n## 주의\n삼키지 않는다.',
  example_code: '',
  reference_url: 'https://docs.oracle.com/javase/tutorial/essential/exceptions/',
  ...over,
});
const card = (file, over = {}, blocks = [{ lang: 'java', code: 'int a = 1;' }]) => parseCard(renderCard(row(over), blocks), file);

/** MySQL 기본 모드 문자열 리터럴을 값으로 되돌린다 (테스트 검증용) */
function mysqlLiterals(sql) {
  const values = [];
  for (let i = 0; i < sql.length; i++) {
    if (sql[i] === '-' && sql[i + 1] === '-') {
      i = sql.indexOf('\n', i);
      if (i < 0) break;
      continue;
    }
    if (sql[i] !== "'") continue;
    let v = '';
    for (i++; i < sql.length; i++) {
      const c = sql[i];
      if (c === '\\') v += { n: '\n', t: '\t', 0: '\0' }[sql[++i]] ?? sql[i];
      else if (c === "'" && sql[i + 1] === "'") (v += "'"), i++;
      else if (c === "'") break;
      else v += c;
    }
    values.push(v);
  }
  return values;
}

test('AC1_카드를_읽어_행_값을_만든다 (예제는 블록을 빈 줄 하나로 잇고 언어 태그는 버린다)', () => {
  const c = card('data/seed/concepts/mybatis/dynamic-sql.md', { category: 'mybatis' }, [
    { lang: 'java', code: 'List<Post> search(@Param("keyword") String keyword);' },
    { lang: 'xml', code: '<select id="search">\n  ## 코드 안의 제목 줄은 섹션이 아니다\n</select>' },
  ]);
  assert.deepEqual(c.problems, []);
  assert.deepEqual(
    c.blocks.map((b) => b.lang),
    ['java', 'xml'],
  );
  const r = toRow(c);
  assert.equal(r.example_code, 'List<Post> search(@Param("keyword") String keyword);\n\n<select id="search">\n  ## 코드 안의 제목 줄은 섹션이 아니다\n</select>');
  assert.equal(r.body, row().body); // 본문의 "## 핵심" 같은 한글 제목은 섹션으로 보지 않는다
  assert.equal(r.reference_url, row().reference_url);
});

test('AC1_같은_입력이면_바이트까지_같은_출력이고_읽는_순서와_관계없다', () => {
  const cards = [
    card('data/seed/concepts/vue/ref.md', { name: 'ref', category: 'vue' }),
    card('data/seed/concepts/java/b.md', { name: 'B' }),
    card('data/seed/concepts/java/a.md', { name: 'A' }),
    card('data/seed/concepts/spring/bean.md', { name: '빈', category: 'spring' }),
  ];
  const sql = buildSql(cards);
  assert.equal(buildSql([...cards].reverse()), sql);
  // 카테고리 순(java·spring·…·vue) → 파일 이름순
  const names = [...sql.matchAll(/^-- (data\/seed\/\S+)$/gm)].map((m) => m[1]);
  assert.deepEqual(names, ['data/seed/concepts/java/a.md', 'data/seed/concepts/java/b.md', 'data/seed/concepts/spring/bean.md', 'data/seed/concepts/vue/ref.md']);
  assert.match(sql, /^-- 생성 파일, 직접 고치지 말 것/);
  assert.match(sql, /ON DUPLICATE KEY UPDATE\n {4}status {8}= IF\(concepts\.status = 'DRAFT', seed\.status, concepts\.status\),/);
  // SeedConceptsTest가 파일의 행 수를 이 꼬리로 센다
  assert.equal(sql.split("'PUBLISHED', 'SEED', 'https").length - 1, 4);
  assert.match(sql, /'PUBLISHED', 'SEED', 'https:\/\/[^']+'\)\n\nAS seed/);
});

test('AC1_작은따옴표와_역슬래시를_이스케이프해서_값이_원본과_같다', () => {
  assert.equal(sqlString("It's \\d+"), "'It''s \\\\d+'");
  const c = card('data/seed/concepts/java/regex.md', { name: "It's \\d+", summary: "정규식 '\\d+'는 숫자다" }, [
    { lang: 'java', code: 'Pattern p = Pattern.compile("\\\\d+");\nString s = "It\'s";' },
  ]);
  const sql = buildSql([c]);
  const values = mysqlLiterals(sql.slice(sql.indexOf('VALUES')));
  const r = toRow(c);
  assert.deepEqual(values.slice(0, 6), [r.name, 'java', 'BEGINNER', r.summary, r.body, r.example_code]);
  assert.equal(values[6], 'PUBLISHED');
  assert.equal(values[8], r.reference_url);
});

test('AC1_renderCard와_parseCard는_왕복된다 (코드 안의 백틱·frontmatter 앞뒤 공백)', () => {
  const blocks = [{ lang: 'js', code: 'const s = `a ${b}`;\n// ```\n' + 'x();' }];
  const r = row({ name: ' 앞뒤 공백 ', summary: '한 줄\n두 줄' });
  const c = parseCard(renderCard(r, blocks), 'x.md');
  assert.deepEqual(c.problems, []);
  assert.deepEqual(toRow(c), { ...NO_LEVELS, ...r, example_code: blocks[0].code });
});

test('AC1_BE_이스케이프_픽스처가_생성기_출력과_같다 (SeedConceptsTest가 DB 값을 확인)', () => {
  const dir = join(ROOT, 'backend/src/test/resources/seed');
  const sql = readFileSync(join(dir, 'escape-seed.sql'), 'utf8').replace(/\r\n/g, '\n');
  assert.equal(buildSql(readCards(join(dir, 'cards'), ROOT)), sql, '다시 생성: node scripts/seed-build.mjs --src backend/src/test/resources/seed/cards --out backend/src/test/resources/seed/escape-seed.sql');
});

function runBuild(args) {
  return spawnSync(process.execPath, [join(ROOT, 'scripts/seed-build.mjs'), ...args], { encoding: 'utf8' });
}

test('AC3_check는_생성본이_원본과_다르면_종료_코드_1', () => {
  const dir = mkdtempSync(join(tmpdir(), 'seed-build-'));
  try {
    const src = join(dir, 'concepts');
    mkdirSync(join(src, 'java'), { recursive: true });
    const cardFile = join(src, 'java', 'a.md');
    writeFileSync(cardFile, renderCard(row(), [{ lang: 'java', code: 'int a = 1;' }]));
    const out = join(dir, 'R__seed.sql');
    const io = ['--src', src, '--out', out];

    let r = runBuild([...io, '--check']); // 생성본 없음
    assert.equal(r.status, 1);
    assert.match(r.stderr, /생성본이 원본과 다름/);

    assert.equal(runBuild(io).status, 0);
    r = runBuild([...io, '--check']);
    assert.equal(r.status, 0, r.stderr);

    // 카드를 고치고 다시 생성하지 않음
    writeFileSync(cardFile, renderCard(row({ summary: '바뀐 요약' }), [{ lang: 'java', code: 'int a = 1;' }]));
    r = runBuild([...io, '--check']);
    assert.equal(r.status, 1);
    assert.match(r.stderr, /생성본이 원본과 다름/);

    // SQL을 손으로 고침
    assert.equal(runBuild(io).status, 0);
    writeFileSync(out, readFileSync(out, 'utf8').replace('바뀐 요약', '손으로 고친 요약'));
    assert.equal(runBuild([...io, '--check']).status, 1);
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});

test('AC3_읽을_수_없는_카드가_있으면_생성하지_않는다', () => {
  const dir = mkdtempSync(join(tmpdir(), 'seed-build-'));
  try {
    mkdirSync(join(dir, 'java'), { recursive: true });
    writeFileSync(join(dir, 'java', 'a.md'), '## summary\n\n요약\n');
    const r = runBuild(['--src', dir, '--out', join(dir, 'out.sql')]);
    assert.equal(r.status, 1);
    assert.match(r.stderr, /frontmatter/);
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});

// ── K-1 AC13 수준별 선택 절 ──

test('K1_AC13_수준별_절은_선택이고_파싱_렌더가_왕복된다', () => {
  const levels = { summary_junior: '주니어 요약', body_senior: "## 시니어\n- It's \\d+" };
  const r = row(levels);
  const text = renderCard(r, [{ lang: 'java', code: 'int a = 1;' }]);
  assert.match(text, /## summary-junior\n\n주니어 요약\n/);
  assert.match(text, /## body-senior\n/);
  assert.doesNotMatch(text, /## summary-beginner/);
  const c = parseCard(text, 'x.md');
  assert.deepEqual(c.problems, []);
  assert.deepEqual(toRow(c), { ...NO_LEVELS, ...r, example_code: 'int a = 1;' });
});

test('K1_AC13_INSERT와_ON_DUPLICATE_KEY_UPDATE에_6칸이_들어가고_절이_없으면_NULL', () => {
  const withLevels = card('data/seed/concepts/java/a.md', { summary_beginner: '입문', body_junior: '## 주니어' });
  const plain = card('data/seed/concepts/java/b.md', { name: '다른 카드' });
  const sql = buildSql([withLevels, plain]);
  for (const column of Object.values(LEVEL_SECTIONS)) {
    assert.match(sql.slice(0, sql.indexOf('VALUES')), new RegExp(`\\b${column}\\b`), `INSERT 열 ${column}`);
    assert.match(sql, new RegExp(`\\n\\s+${column}\\s+= seed\\.${column},`), `UPDATE ${column}`);
  }
  const rowA = sql.slice(sql.indexOf('-- data/seed/concepts/java/a.md'), sql.indexOf('-- data/seed/concepts/java/b.md'));
  assert.match(rowA, /\n '입문', NULL, NULL,\n/);
  assert.match(rowA, /\n NULL, '## 주니어', NULL,\n/);
  const rowB = sql.slice(sql.indexOf('-- data/seed/concepts/java/b.md'));
  assert.equal((rowB.match(/NULL, NULL, NULL,/g) ?? []).length, 2);
});
