// 상대 작업 알림: NOW 선택, 요약, 변화 판정 (git·gh 호출 없는 부분)
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { nowHeadline, peerDigest, pickLatestNow } from '../lib/peer.mjs';

const NOW = (stamp, feature = 'F-09 카드 목록 — AC2 진행') =>
  `# NOW\n\n갱신: ${stamp} · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-09-cards\n\n## 현재 기능\n\n${feature}\n\n## 바로 다음 할 일\n\n1. ConceptListTest 통과\n2. FE 목록 화면\n`;

test('원격과 작업 트리 중 갱신 시각이 늦은 NOW를 고른다', () => {
  const tree = { source: '작업 트리', text: NOW('2026-11-19 10:26'), at: 0 };
  const remote = { source: 'origin/feature/F-09-cards', text: NOW('2026-11-20 14:05'), at: 1 };
  assert.equal(pickLatestNow([tree, remote]).source, 'origin/feature/F-09-cards');
  assert.equal(pickLatestNow([{ ...tree, text: NOW('2026-11-21 09:00') }, remote]).source, '작업 트리');
  // 같으면 앞(작업 트리)
  assert.equal(pickLatestNow([tree, { ...remote, text: NOW('2026-11-19 10:26') }]).source, '작업 트리');
  assert.equal(pickLatestNow([{ source: 'x', text: null }]), null);
});

test('NOW 요약은 갱신 줄, 현재 기능 첫 줄, 다음 할 일 1번', () => {
  const h = nowHeadline(NOW('2026-11-20 14:05'));
  assert.match(h.stamp, /^갱신: 2026-11-20 14:05/);
  assert.equal(h.feature, 'F-09 카드 목록 — AC2 진행');
  assert.equal(h.next, 'ConceptListTest 통과');
  assert.deepEqual(nowHeadline(''), { stamp: '갱신: ?', feature: '(없음)', next: '(없음)' });
});

const BASE = {
  developSha: 'a1',
  peerNow: { source: '작업 트리', stamp: '갱신: 2026-11-19 10:26', feature: '없음', next: '-' },
  prs: { 30: { title: 'feat(F-09): 카드 목록', state: 'OPEN', head: 'feature/F-09-cards', sha: 's1' } },
};

test('직전 스냅샷이 없거나 변화가 없으면 알리지 않는다', () => {
  assert.deepEqual(peerDigest(null, BASE), []);
  assert.deepEqual(peerDigest(BASE, { ...BASE, develop: null }), []);
});

test('develop 새 커밋은 마이그레이션·API 변경과 함께 알리고, 내 커밋뿐이면 알리지 않는다', () => {
  const develop = {
    commits: [{ sha: 'b2', author: 'JoonseobKim', subject: 'Merge pull request #30' }],
    files: ['backend/src/main/resources/db/migration/V6__concept_wish.sql', 'docs/api/openapi.json'],
  };
  const [line] = peerDigest(BASE, { ...BASE, develop }, { myNames: ['whans1996'] });
  assert.match(line, /새 커밋 1개 \(새 마이그레이션 V6__concept_wish\.sql, openapi\.json 변경\)/);
  assert.match(line, /git merge origin\/develop/);
  const mine = { ...develop, commits: [{ sha: 'c3', author: 'whans1996', subject: 'Merge pull request #31' }] };
  assert.deepEqual(peerDigest(BASE, { ...BASE, develop: mine }, { myNames: ['whans1996'] }), []);
});

test('상대 NOW 갱신과 PR 열림·머지·push를 알린다', () => {
  const curr = {
    ...BASE,
    peerNow: { source: 'origin/feature/F-09-cards', stamp: '갱신: 2026-11-20 14:05', feature: 'F-09 AC2', next: 'FE 화면' },
    prs: {
      30: { ...BASE.prs[30], state: 'MERGED' },
      32: { title: 'feat(F-10): 카드 상세', state: 'OPEN', head: 'feature/F-10-card-detail', sha: 't1' },
    },
  };
  const lines = peerDigest(BASE, curr, { peerName: '김준섭' });
  assert.equal(lines.length, 3);
  assert.match(lines[0], /김준섭 NOW 갱신 \(origin\/feature\/F-09-cards\): F-09 AC2 \/ 다음: FE 화면/);
  assert.match(lines[1], /PR #30 .* 머지됨 → develop 합치기 대상/);
  assert.match(lines[2], /PR #32 .* 열림/);
  const pushed = { ...BASE, prs: { 30: { ...BASE.prs[30], sha: 's2' } } };
  assert.match(peerDigest(BASE, pushed)[0], /PR #30 .*새 커밋 push/);
  // 직전 PR 목록이 없으면(첫 확인) PR은 기준만 잡는다
  assert.deepEqual(peerDigest({ ...BASE, prs: null }, { ...BASE, prs: curr.prs }), []);
});
