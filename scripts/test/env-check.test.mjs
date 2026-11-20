// 환경 점검의 외부 명령 실행 (세션 시작 3초 제한)
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { run } from '../lib/env-check.mjs';

const node = (code) => `"${process.execPath}" -e "${code}"`;

test('외부 명령이 제한 시간을 넘으면 실패와 시간 초과를 함께 알린다 (#67)', async () => {
  const r = await run(node('setTimeout(()=>{},3000)'), 300);
  assert.equal(r.ok, false);
  assert.equal(r.timedOut, true);
});

test('제한 시간 안에 끝나면 출력과 성공을 돌려준다', async () => {
  const r = await run(node("console.log('hi')"), 5000);
  assert.deepEqual(r, { ok: true, out: 'hi', timedOut: false });
  const bad = await run(node('process.exit(3)'), 5000);
  assert.equal(bad.ok, false);
  assert.equal(bad.timedOut, false);
});
