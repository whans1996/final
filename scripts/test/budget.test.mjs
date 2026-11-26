// hook 시간 예산 (H-5)
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { SESSION_START_BUDGET_MS, createBudget, sessionStartBudgetMs } from '../lib/budget.mjs';

test('남은 시간과 단계별 제한 시간을 계산한다', () => {
  let t = 1000;
  const b = createBudget(10_000, { now: () => t });
  assert.equal(b.left(), 10_000);
  t += 4000;
  assert.equal(b.left(), 6000);
  assert.ok(b.has(6000));
  assert.ok(!b.has(6001));
  assert.equal(b.timeout(5000), 5000);
  assert.equal(b.timeout(5000, 3000), 3000);
  assert.equal(b.timeout(5000, 9000), 0);
  t += 20_000;
  assert.equal(b.left(), 0);
});

test('예산 안에 안 끝나는 작업은 기다리지 않고 대체값을 쓴다', async () => {
  const b = createBudget(50);
  const slow = new Promise((resolve) => setTimeout(() => resolve('slow'), 2000).unref());
  assert.equal(await b.within(slow, () => 'fallback'), 'fallback');
  assert.equal(await b.within(Promise.resolve('fast'), () => 'fallback'), 'fast');
  // 남은 시간이 reserve보다 적으면 바로 대체값
  assert.equal(await createBudget(1000).within(slow, () => 'none', 5000), 'none');
});

test('건너뛴 단계는 한 번씩만 모은다', () => {
  const b = createBudget(0);
  b.skip('자동 병합');
  b.skip('자동 병합');
  b.skip('이슈 조회');
  assert.deepEqual(b.skipped, ['자동 병합', '이슈 조회']);
});

test('세션 시작 예산은 환경변수로 바꿀 수 있고 잘못된 값은 무시한다', () => {
  assert.equal(sessionStartBudgetMs({}), SESSION_START_BUDGET_MS);
  assert.equal(sessionStartBudgetMs({ VIBENOTE_SESSION_BUDGET_MS: '0' }), 0);
  assert.equal(sessionStartBudgetMs({ VIBENOTE_SESSION_BUDGET_MS: 'x' }), SESSION_START_BUDGET_MS);
  assert.equal(sessionStartBudgetMs({ VIBENOTE_SESSION_BUDGET_MS: '' }), SESSION_START_BUDGET_MS);
  assert.ok(SESSION_START_BUDGET_MS < 15_000);
});
