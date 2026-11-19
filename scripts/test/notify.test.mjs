// Mattermost 알림 메시지
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { buildMessage, changedNowKeys, senderKey, whoOf } from '../lib/notify.mjs';

const PCS = {
  'dh-ssafy': { owner: '김동환', github: 'whans1996' },
  'js-ssafy': { owner: '김준섭', github: 'JoonseobKim' },
};
const PR = (action, extra = {}) => ({
  action,
  pull_request: {
    number: 31,
    title: 'feat(F-08): 개념 추출',
    html_url: 'https://github.com/x/y/pull/31',
    draft: false,
    merged: false,
    user: { login: 'whans1996' },
    head: { ref: 'feature/F-08-concepts' },
    base: { ref: 'develop' },
    ...extra,
  },
});

test('작성자는 영역과 이름으로 표시한다', () => {
  assert.equal(whoOf('whans1996', PCS), 'A 김동환');
  assert.equal(whoOf('joonseobkim', PCS), 'B 김준섭');
  assert.equal(whoOf('someone', PCS), 'someone');
});

test('PR 열림·머지·닫힘', () => {
  assert.match(buildMessage('pull_request', PR('opened'), { pcs: PCS }), /^🔀 \*\*A 김동환\*\* \[PR #31 feat\(F-08\): 개념 추출\]\(.+\) 열림 \(`feature\/F-08-concepts` → `develop`\)$/);
  const merged = buildMessage('pull_request', PR('closed', { merged: true }), { pcs: PCS });
  assert.match(merged, /✅ 머지됨/);
  assert.match(merged, /git merge origin\/develop/);
  assert.match(buildMessage('pull_request', PR('closed'), { pcs: PCS }), / 닫힘 \(/);
  assert.equal(buildMessage('pull_request', PR('synchronize'), { pcs: PCS }), null);
});

test('NOW push는 바뀐 사람의 현재 기능과 다음 할 일', () => {
  const payload = {
    ref: 'refs/heads/feature/F-09-cards',
    commits: [{ added: [], modified: ['docs/handoff/NOW-js.md', 'backend/A.java'] }, { added: ['docs/handoff/NOW-zz.md'], modified: [] }],
  };
  assert.deepEqual(changedNowKeys(payload), ['js']);
  const now = '갱신: 2026-11-20 14:05\n\n## 현재 기능\n\nF-09 카드 목록 — AC2\n\n## 바로 다음 할 일\n\n1. FE 목록 화면\n';
  assert.equal(
    buildMessage('push', payload, { pcs: PCS, nowTexts: { js: now } }),
    '📝 **B 김준섭** 작업 상태 (`feature/F-09-cards`)\n- 현재: F-09 카드 목록 — AC2\n- 다음: FE 목록 화면',
  );
  assert.equal(buildMessage('push', { ref: 'refs/heads/x', commits: [] }, { pcs: PCS }), null);
});

test('Actions push payload에는 파일 목록이 없어 커밋 id로 조회한 파일을 쓴다 (Codex 리뷰)', () => {
  const payload = {
    ref: 'refs/heads/feature/F-09-cards',
    sender: { login: 'JoonseobKim' },
    commits: [
      { id: 'aaa', distinct: true },
      { id: 'bbb', distinct: false },
    ],
  };
  const files = { aaa: ['docs/handoff/NOW-js.md'], bbb: ['docs/handoff/NOW-dh.md'] };
  assert.deepEqual(changedNowKeys(payload, PCS, (c) => files[c.id]), ['js']);
  assert.deepEqual(changedNowKeys(payload, PCS), []);
});

test('develop 합치기로 딸려 온 상대 NOW 커밋은 알리지 않는다', () => {
  const merged = {
    ref: 'refs/heads/feature/F-08-concepts',
    sender: { login: 'whans1996' },
    commits: [
      { distinct: false, modified: ['docs/handoff/NOW-js.md'] },
      { distinct: true, modified: ['docs/handoff/NOW-dh.md'] },
    ],
  };
  assert.deepEqual(changedNowKeys(merged, PCS), ['dh']);
  // distinct 표시가 없어도 push한 사람의 NOW만 본다
  const noDistinct = { ...merged, commits: [{ modified: ['docs/handoff/NOW-js.md', 'docs/handoff/NOW-dh.md'] }] };
  assert.deepEqual(changedNowKeys(noDistinct, PCS), ['dh']);
});

test('알림은 행동한 사람의 웹훅으로 보낸다 (글쓴이 = 웹훅 만든 사람)', () => {
  assert.equal(senderKey({ sender: { login: 'joonseobkim' } }, PCS), 'js');
  assert.equal(senderKey({ sender: { login: 'whans1996' } }, PCS), 'dh');
  assert.equal(senderKey({ sender: { login: 'github-actions[bot]' } }, PCS), null);
  assert.equal(senderKey({}, PCS), null);
});
