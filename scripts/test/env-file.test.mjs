// .env 위치 (#249): 저장소 밖 ~/.vibenote/.env를 쓰고, 루트 .env는 옮기라고 알린다
import assert from 'node:assert/strict';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';
import { envFileRow, moveEnv } from '../lib/env-file.mjs';

test('~/.vibenote/.env만 있으면 ✅, 루트에 남아 있거나 없으면 ⚠️로 옮길 방법을 알린다', () => {
  assert.equal(envFileRow({ home: true, root: false }).status, 'ok');

  const left = envFileRow({ home: true, root: true });
  assert.equal(left.status, 'warn');
  assert.match(left.detail, /루트 \.env/);

  const notMoved = envFileRow({ home: false, root: true });
  assert.equal(notMoved.status, 'warn');
  assert.match(notMoved.detail, /setup\.mjs --move-env/);

  const none = envFileRow({ home: false, root: false });
  assert.equal(none.status, 'warn');
  assert.match(none.detail, /\.env\.example/);
});

function tempPaths() {
  const dir = mkdtempSync(join(tmpdir(), 'vibenote-env-'));
  return { dir, root: join(dir, 'repo', '.env'), home: join(dir, 'home', '.vibenote', '.env') };
}

test('--move-env는 루트 .env를 ~/.vibenote/.env로 옮기고 폴더를 만든다', () => {
  const p = tempPaths();
  try {
    mkdirSync(join(p.dir, 'repo'));
    writeFileSync(p.root, 'K=1\n');
    const r = moveEnv(p);
    assert.equal(r.ok, true);
    assert.ok(!existsSync(p.root));
    assert.equal(readFileSync(p.home, 'utf8'), 'K=1\n');
  } finally {
    rmSync(p.dir, { recursive: true, force: true });
  }
});

test('--move-env는 이미 있는 ~/.vibenote/.env를 덮어쓰지 않고, 옮길 파일이 없으면 실패로 알린다', () => {
  const p = tempPaths();
  try {
    mkdirSync(join(p.dir, 'repo'));
    mkdirSync(join(p.dir, 'home', '.vibenote'), { recursive: true });
    writeFileSync(p.root, 'K=root\n');
    writeFileSync(p.home, 'K=home\n');
    const both = moveEnv(p);
    assert.equal(both.ok, false);
    assert.equal(readFileSync(p.home, 'utf8'), 'K=home\n');
    assert.ok(existsSync(p.root));

    rmSync(p.root);
    rmSync(p.home);
    assert.equal(moveEnv(p).ok, false);
  } finally {
    rmSync(p.dir, { recursive: true, force: true });
  }
});

test('--move-env는 파일이 잠겨 옮기지 못하거나, 다른 드라이브에 복사한 뒤 지우지 못하면 예외 대신 할 일을 알린다', () => {
  const p = tempPaths();
  const fail = (code) => () => {
    throw Object.assign(new Error(code), { code });
  };
  try {
    mkdirSync(join(p.dir, 'repo'));
    writeFileSync(p.root, 'K=1\n');
    const locked = moveEnv(p, { renameSync: fail('EBUSY') });
    assert.equal(locked.ok, false);
    assert.match(locked.message, /EBUSY/);
    assert.ok(existsSync(p.root) && !existsSync(p.home));

    // 저장소와 홈이 다른 드라이브(EXDEV): 복사는 됐는데 원본을 지우지 못함 → 두 파일이 키 단위로 합쳐지므로 직접 지우라고 알린다
    const copied = moveEnv(p, { renameSync: fail('EXDEV'), unlinkSync: fail('EPERM') });
    assert.equal(copied.ok, false);
    assert.match(copied.message, /직접 지우/);
    assert.equal(readFileSync(p.home, 'utf8'), 'K=1\n');

    rmSync(p.home);
    const crossDrive = moveEnv(p, { renameSync: fail('EXDEV') });
    assert.equal(crossDrive.ok, true);
    assert.ok(!existsSync(p.root));
  } finally {
    rmSync(p.dir, { recursive: true, force: true });
  }
});
