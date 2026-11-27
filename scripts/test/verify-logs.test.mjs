// HP-5 AC15 (#180): .harness/logs의 verify 로그는 최근 50개만 남긴다
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import { VERIFY_LOG_KEEP, logsToPrune } from '../lib/verify.mjs';

const log = (i) => `verify-2026-11-${String(10 + Math.floor(i / 24)).padStart(2, '0')}T${String(i % 24).padStart(2, '0')}-00-00-000Z.log`;

test('HP-5 AC15 verify 로그가 keep개를 넘으면 가장 오래된 것부터 지울 목록을 고른다', () => {
  const names = Array.from({ length: 53 }, (_, i) => log(i)).reverse();
  const pruned = logsToPrune(names, 50);
  assert.deepEqual(pruned, [log(0), log(1), log(2)]);
  assert.deepEqual(logsToPrune(names.slice(0, 50), 50), []);
  assert.equal(VERIFY_LOG_KEEP, 50);
});

test('HP-5 AC15 verify- 아닌 파일(bootrun 등)은 지우지 않고, 방금 쓴 로그도 지우지 않는다', () => {
  const names = ['bootrun-pr91.log', 'notes.txt', log(0), log(1), log(2)];
  assert.deepEqual(logsToPrune(names, 1), [log(0), log(1)]);
  // keep이 0이어도 방금 쓴 로그(current)는 남긴다
  assert.deepEqual(logsToPrune(names, 0, log(2)), [log(0), log(1)]);
});

test('HP-5 AC15 verify.mjs는 로그를 쓴 뒤 정리한다', () => {
  const src = readFileSync(new URL('../verify.mjs', import.meta.url), 'utf8');
  const write = src.indexOf('writeFileSync(logFile');
  const prune = src.indexOf('logsToPrune(');
  assert.ok(write > 0 && prune > write);
});
