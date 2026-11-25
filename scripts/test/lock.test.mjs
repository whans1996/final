// verify.mjs의 Gradle 직렬화 잠금
import assert from 'node:assert/strict';
import { existsSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';
import { STALE_MS, acquireLock, isStale } from '../lib/lock.mjs';

test('주인이 죽었거나 오래되었거나 깨진 잠금은 버려진 것이다', () => {
  const now = 1_000_000_000;
  assert.ok(!isStale({ pid: 1, at: now - 1000 }, now, () => true));
  assert.ok(isStale({ pid: 1, at: now - 1000 }, now, () => false));
  assert.ok(isStale({ pid: 1, at: now - STALE_MS - 1 }, now, () => true));
  assert.ok(isStale(null, now, () => true));
  assert.ok(isStale({ pid: 'x', at: now }, now, () => true));
});

test('잠금을 얻고 풀며, 살아 있는 주인이 있으면 기다리다 시간 초과로 false', () => {
  const dir = mkdtempSync(join(tmpdir(), 'lock-'));
  const path = join(dir, 'gradle.lock');
  try {
    const release = acquireLock(path, { label: 'test' });
    assert.equal(typeof release, 'function');
    assert.ok(existsSync(path));
    let waitedOn = null;
    // 같은 프로세스(살아 있음)가 쥐고 있으므로 기다린다
    assert.equal(acquireLock(path, { pollMs: 10, timeoutMs: 50, onWait: (info) => (waitedOn = info) }), false);
    assert.equal(waitedOn.label, 'test');
    release();
    assert.ok(!existsSync(path));
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});

test('버려진 잠금은 지우고 가져간다', () => {
  const dir = mkdtempSync(join(tmpdir(), 'lock-'));
  const path = join(dir, 'gradle.lock');
  try {
    writeFileSync(path, JSON.stringify({ pid: process.pid, at: Date.now() - STALE_MS - 1 }));
    const release = acquireLock(path, { pollMs: 10, timeoutMs: 50 });
    assert.equal(typeof release, 'function');
    release();
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});
