// 주간 /progress 초안과 하네스 지표 (H-1)
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { codexStats, guardStats, issueWeekStats, specStatus } from '../lib/weekly.mjs';

test('spec 상태와 체크리스트 비율', () => {
  const md = '# F-21a\n| 항목 | 값 |\n|---|---|\n| 상태 | DONE (PR #118) <!-- DRAFT → … --> |\n\n- [x] 브랜치\n- [X] 테스트\n  - [ ] 하위 항목\n';
  assert.deepEqual(specStatus(md), { status: 'DONE', pr: 118, checked: 2, total: 3 });
  assert.deepEqual(specStatus('| 상태 | APPROVED |'), { status: 'APPROVED', pr: null, checked: 0, total: 0 });
  assert.equal(specStatus('').status, '?');
});

test('follow-up 이슈 영역별: 열림·기간 안 닫힘·7일 넘음', () => {
  const now = Date.parse('2026-11-25T00:00:00Z');
  const since = Date.parse('2026-11-18T00:00:00Z');
  const issue = (number, area, state, createdAt, closedAt) => ({
    number,
    state,
    createdAt,
    closedAt,
    labels: [{ name: 'follow-up' }, ...(area ? [{ name: area }] : [])],
  });
  const rows = issueWeekStats(
    [
      issue(24, 'area:A', 'OPEN', '2026-11-17T00:00:00Z'),
      issue(121, 'area:A', 'OPEN', '2026-11-24T00:00:00Z'),
      issue(90, 'area:A', 'CLOSED', '2026-11-10T00:00:00Z', '2026-11-20T00:00:00Z'),
      issue(80, 'area:A', 'CLOSED', '2026-11-01T00:00:00Z', '2026-11-10T00:00:00Z'),
      issue(29, 'area:common', 'OPEN', '2026-11-19T00:00:00Z'),
      issue(5, null, 'OPEN', '2026-11-24T00:00:00Z'),
    ],
    since,
    now,
  );
  assert.deepEqual(rows['area:A'], { open: 2, closed: 1, stale: [24] });
  assert.deepEqual(rows['area:common'], { open: 1, closed: 0, stale: [] });
  assert.deepEqual(rows['분류 필요'], { open: 1, closed: 0, stale: [] });
});

test('Codex: 재리뷰 횟수와 스레드 첫 지적의 우선순위', () => {
  const sha = (c) => c.repeat(40);
  const issueComments = [
    { user: { login: 'whans1996' }, body: `@codex review\n<!-- codex-review-request: ${sha('a')} -->` },
    { user: { login: 'whans1996' }, body: `@codex review\n<!-- codex-review-request: ${sha('b')} -->` },
    { user: { login: 'whans1996' }, body: `@codex review\n<!-- codex-review-request: ${sha('b')} -->\n<!-- codex-review-retry: ${sha('b')} -->` },
  ];
  const bot = { login: 'chatgpt-codex-connector[bot]' };
  const reviewComments = [
    { user: bot, body: '**<sub><sub>![P1 Badge](x)</sub></sub> a**' },
    { user: bot, body: '**<sub><sub>![P2 Badge](x)</sub></sub> b**' },
    { user: bot, body: '**<sub><sub>![P2 Badge](x)</sub></sub> c**' },
    { user: bot, body: '답글', in_reply_to_id: 1 },
    { user: { login: 'whans1996' }, body: '#130으로 이관' },
  ];
  assert.deepEqual(codexStats(issueComments, reviewComments), { rounds: 2, findings: { P1: 1, P2: 2 } });
});

test('가드 차단: 기간 안의 것만 사유별로', () => {
  const since = Date.parse('2026-11-18T00:00:00Z');
  const lines = [
    JSON.stringify({ at: '2026-11-20T00:00:00Z', reason: '강제 push는 금지입니다. 다른 PC…' }),
    JSON.stringify({ at: '2026-11-21T00:00:00Z', reason: '강제 push는 금지입니다. 다른 PC…' }),
    JSON.stringify({ at: '2026-11-22T00:00:00Z', reason: '.env 파일 내용을 읽을 수 없습니다. 필요한…' }),
    JSON.stringify({ at: '2026-11-10T00:00:00Z', reason: '오래된 것.' }),
    '깨진 줄',
  ];
  assert.deepEqual(guardStats(lines, since), {
    total: 3,
    byReason: [
      ['강제 push는 금지입니다', 2],
      ['.env 파일 내용을 읽을 수 없습니다', 1],
    ],
  });
});
