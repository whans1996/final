// HP-5 작업로그 주차별 보관 (HX-9): 지난 주 항목을 docs/기록/작업로그-Wn.md로 옮긴다
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';
import { ROOT } from '../lib/common.mjs';
import { archiveHeader, mergeArchive, parseWorklog, splitByWeek, weekOf } from '../lib/worklog-archive.mjs';

const PREAMBLE = `# 작업로그

> 교대할 때마다 **맨 아래에** 항목을 추가한다.

형식:

\`\`\`
## YYYY-MM-DD HH:mm · 작업자 · PC id · 브랜치
- 한 일:
\`\`\`

---`;

const W1a = '## 2026-11-18 09:00 · whans1996 · dh-ssafy · feature/x\n- 한 일: W1 첫째';
const W1b = '## 2026-11-23 23:50 · JoonseobKim · js-home · feature/y\n- 한 일: W1 마지막 날';
const W2a = '## 2026-11-24 09:00 · whans1996 · dh-ssafy · feature/z\n- 한 일: W2 첫째';
const W1late = '## 2026-11-20 10:00 · JoonseobKim · js-ssafy · feature/w\n- 한 일: union 머지로 W2 뒤에 붙은 W1 항목';
const LOG = [PREAMBLE, W1a, W2a, W1late, W1b].join('\n\n') + '\n';

test('HP-5 AC1 주는 기획서 마일스톤 표로 나눈다 (W1 11/17 … 버퍼 12/21)', () => {
  assert.equal(weekOf('2026-11-17').id, 'W1');
  assert.equal(weekOf('2026-11-23').id, 'W1');
  assert.equal(weekOf('2026-11-24').id, 'W2');
  assert.equal(weekOf('2026-12-07').id, 'W3');
  assert.equal(weekOf('2026-12-14').id, 'W4');
  assert.equal(weekOf('2026-12-20').id, 'W5');
  assert.equal(weekOf('2026-12-24').id, '버퍼');
  assert.equal(weekOf('2026-11-16'), null);
});

test('HP-5 AC1 머리말은 코드 블록 안의 형식 예시(## YYYY-…)를 항목으로 보지 않는다', () => {
  const { preamble, entries } = parseWorklog(LOG);
  assert.equal(preamble, PREAMBLE);
  assert.equal(entries.length, 4);
  assert.deepEqual(
    entries.map((x) => x.date),
    ['2026-11-18', '2026-11-24', '2026-11-20', '2026-11-23'],
  );
});

test('HP-5 AC1 지금 주보다 앞선 주의 항목만 옮기고, 남는 항목은 원래 순서대로 둔다 (문서 순서가 아니라 날짜로 판정)', () => {
  const r = splitByWeek(LOG, '2026-11-29');
  assert.deepEqual([...r.moved.keys()], ['W1']);
  assert.deepEqual(r.moved.get('W1').map((x) => x.date), ['2026-11-18', '2026-11-20', '2026-11-23']);
  assert.equal(r.log, `${PREAMBLE}\n\n${W2a}\n`);
  assert.deepEqual(r.warnings, []);
});

test('HP-5 AC1 새 보관 파일 머리에 제목과 원래 파일 링크를 두고 항목은 날짜 순으로 쓴다', () => {
  const { moved } = splitByWeek(LOG, '2026-11-29');
  const r = mergeArchive(null, 'W1', moved.get('W1'));
  assert.ok(r.text.startsWith(`${archiveHeader('W1')}\n\n`));
  assert.match(archiveHeader('W1'), /^# 작업로그 W1 \(11\/17~11\/23\)\n/);
  assert.match(archiveHeader('W1'), /\]\(\.\.\/작업로그\.md\)/);
  assert.ok(r.text.endsWith(`${W1a}\n\n${W1late}\n\n${W1b}\n`));
  assert.equal(r.added, 3);
  assert.equal(r.duplicates, 0);
});

test('HP-5 AC2 옮길 항목이 없으면 moved가 비고 로그는 그대로다', () => {
  const only = `${PREAMBLE}\n\n${W2a}\n`;
  const r = splitByWeek(only, '2026-11-29');
  assert.equal(r.moved.size, 0);
  assert.equal(r.log, only);
  // 오늘이 W1이면 아무것도 옮기지 않는다
  assert.equal(splitByWeek(LOG, '2026-11-20').moved.size, 0);
});

test('HP-5 AC3 보관 파일에 이미 있는 항목은 다시 붙이지 않고, 없는 새 항목만 날짜 순으로 끝에 붙인다', () => {
  const archived = mergeArchive(null, 'W1', parseWorklog([PREAMBLE, W1a, W1b].join('\n\n')).entries).text;
  // union 머지로 W1a가 앞뒤 공백만 다르게 작업로그에 다시 나타나고, 같은 주의 새 항목 W1late가 생겼다
  const again = [PREAMBLE, W2a, `${W1a}\n\n\n`, W1late].join('\n\n') + '\n';
  const { moved, log } = splitByWeek(again, '2026-11-29');
  assert.equal(log, `${PREAMBLE}\n\n${W2a}\n`);
  const r = mergeArchive(archived, 'W1', moved.get('W1'));
  assert.equal(r.duplicates, 1);
  assert.equal(r.added, 1);
  assert.ok(r.text.endsWith(`${W1b}\n\n${W1late}\n`));
  assert.equal(r.text.split(W1a).length, 2, 'W1a는 한 번만');
});

test('HP-5 AC4 날짜를 읽을 수 없는 헤더와 W1 이전 항목은 작업로그에 남기고 경고한다', () => {
  const odd = '## 메모 · 날짜 없음\n- 자료를 잃지 않는다';
  const early = '## 2026-11-10 · 미리 쓴 항목 · main\n- W1 이전';
  const log = [PREAMBLE, odd, W1a, early, W2a].join('\n\n') + '\n';
  const r = splitByWeek(log, '2026-11-29');
  assert.equal(r.log, [PREAMBLE, odd, early, W2a].join('\n\n') + '\n');
  assert.deepEqual(r.warnings.length, 2);
  assert.match(r.warnings[0], /## 메모 · 날짜 없음/);
  assert.match(r.warnings[1], /## 2026-11-10 · 미리 쓴 항목 · main/);
});

const cli = (dir, ...args) => spawnSync(process.execPath, [join(ROOT, 'scripts/archive-worklog.mjs'), '--docs', dir, ...args], { encoding: 'utf8' });

test('HP-5 AC1·AC2 CLI: 보관 → 다시 실행하면 "보관할 항목 없음", --dry-run은 파일을 바꾸지 않는다', () => {
  const dir = mkdtempSync(join(tmpdir(), 'archive-worklog-'));
  try {
    const logFile = join(dir, '작업로그.md');
    const archive = join(dir, '기록', '작업로그-W1.md');
    writeFileSync(logFile, LOG);

    const dry = cli(dir, '--today', '2026-11-29', '--dry-run');
    assert.equal(dry.status, 0, dry.stderr);
    assert.match(dry.stdout, /W1.*3/);
    assert.equal(readFileSync(logFile, 'utf8'), LOG);
    assert.equal(existsSync(archive), false);

    const run = cli(dir, '--today', '2026-11-29');
    assert.equal(run.status, 0, run.stderr);
    assert.equal(readFileSync(logFile, 'utf8'), `${PREAMBLE}\n\n${W2a}\n`);
    assert.match(readFileSync(archive, 'utf8'), /^# 작업로그 W1/);

    const before = readFileSync(archive, 'utf8');
    const again = cli(dir, '--today', '2026-11-29');
    assert.equal(again.status, 0, again.stderr);
    assert.match(again.stdout, /보관할 항목 없음/);
    assert.equal(readFileSync(archive, 'utf8'), before);
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});

test('HP-5 AC2 CLI: --today 형식이 틀리면 exit 1', () => {
  const dir = mkdtempSync(join(tmpdir(), 'archive-worklog-'));
  try {
    mkdirSync(join(dir, '기록'));
    writeFileSync(join(dir, '작업로그.md'), LOG);
    const r = cli(dir, '--today', '11/29');
    assert.equal(r.status, 1);
    assert.equal(readFileSync(join(dir, '작업로그.md'), 'utf8'), LOG);
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});
