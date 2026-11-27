// 시드 카드 검사 보고서·종료 코드 (K-3a AC10). --be 없이 돈다
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';
import { ROOT } from '../lib/common.mjs';
import { buildSql, readCards, renderCard } from '../lib/seed/card.mjs';

const row = (over = {}) => ({
  name: '예외 처리',
  category: 'java',
  difficulty: 'BEGINNER',
  summary: '예외는 정상 흐름을 벗어난 상황을 호출한 쪽에 알리는 방법이다. 커스텀 예외로 원인을 드러낸다.',
  body: '## 핵심\n' + '본문 문장이다. '.repeat(20),
  reference_url: 'https://docs.oracle.com/javase/tutorial/essential/exceptions/',
  ...over,
});

function fixture(cards) {
  const dir = mkdtempSync(join(ROOT, '.harness', 'seed-check-test-'));
  const src = join(dir, 'concepts');
  for (const [path, r, blocks] of cards) {
    mkdirSync(join(src, path.split('/')[0]), { recursive: true });
    writeFileSync(join(src, path), renderCard(r, blocks ?? [{ lang: 'java', code: 'int a = 1;' }]));
  }
  const sql = join(dir, 'R__seed.sql');
  writeFileSync(sql, buildSql(readCards(src, ROOT)));
  const run = (...extra) => {
    const r = spawnSync(process.execPath, [join(ROOT, 'scripts/seed-check.mjs'), '--src', src, '--sql', sql, '--report-dir', join(dir, 'report'), ...extra], { encoding: 'utf8' });
    const read = (f) => readFileSync(join(dir, 'report', f), 'utf8');
    return { ...r, json: () => JSON.parse(read('report.json')), md: () => read('report.md') };
  };
  return { dir, sql, run, cleanup: () => rmSync(dir, { recursive: true, force: true }) };
}

test('AC10_오류가_있으면_종료_코드_1이고_보고서에_파일·단계·심각도·메시지와_합계가_남는다', () => {
  const f = fixture([
    ['java/good.md', row()],
    ['java/bad.md', row({ name: '나쁜 카드', difficulty: 'EXPERT', summary: '짧음' })],
  ]);
  try {
    const r = f.run();
    assert.equal(r.status, 1, r.stdout + r.stderr);
    const json = r.json();
    assert.equal(json.totals.cards, 2);
    assert.equal(json.totals.errors, 1);
    assert.equal(json.totals.warnings, 1);
    assert.equal(typeof json.totals.unchecked, 'number');
    const err = json.issues.find((i) => i.severity === 'error');
    assert.match(err.file, /concepts\/java\/bad\.md$/);
    assert.equal(err.stage, '①');
    assert.equal(err.item, 'difficulty');
    assert.ok(err.message);
    const md = r.md();
    assert.match(md, /카드 2장 · 오류 1 · 경고 1 · 검사 안 한 블록 \d+/);
    assert.match(md, /\| \S*bad\.md \| ① \| difficulty \|/);
    assert.match(md, /BE 검사\(java·sql·xml·yaml\): --be로 실행/);
    assert.match(r.stdout, /bad\.md/);
  } finally {
    f.cleanup();
  }
});

test('AC10_경고만_있으면_종료_코드_0', () => {
  const f = fixture([['java/a.md', row({ summary: '짧은 요약' })]]);
  try {
    const r = f.run();
    assert.equal(r.status, 0, r.stdout + r.stderr);
    assert.deepEqual([r.json().totals.errors, r.json().totals.warnings], [0, 1]);
  } finally {
    f.cleanup();
  }
});

test('AC10_생성본이_원본과_다르면_오류, --card는_그_카드만_보고한다', () => {
  const f = fixture([
    ['java/a.md', row()],
    ['java/b.md', row({ name: '두 번째', difficulty: 'EXPERT', summary: '빈은 스프링 컨테이너가 만들고 관리하는 객체다. 생명주기와 스코프를 컨테이너가 정한다.' })],
  ]);
  try {
    let r = f.run('--card', 'a');
    assert.equal(r.status, 0, r.stdout + r.stderr);
    assert.equal(r.json().totals.cards, 1);
    assert.equal(r.json().totals.errors, 0);

    writeFileSync(f.sql, readFileSync(f.sql, 'utf8').replace('두 번째', '손으로 고침'));
    r = f.run();
    assert.equal(r.status, 1);
    assert.ok(r.json().issues.some((i) => i.item === 'generated' && /생성본이 원본과 다름/.test(i.message)));
  } finally {
    f.cleanup();
  }
});
