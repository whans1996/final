// Codex 재리뷰 판정 중 GitHub 호출 없는 부분
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { isDocPath, onlyDocsChanged, unresolvedFindingAt } from '../lib/codex.mjs';

test('openapi.json은 문서가 아니다', () => {
  assert.ok(isDocPath('docs/handoff/NOW-dh.md'));
  assert.ok(!isDocPath('docs/api/openapi.json'));
  assert.ok(!isDocPath('backend/src/A.java'));
});

test('develop 합치기로 들어온 코드 파일은 재리뷰 대상이 아니다', () => {
  const files = ['docs/진행현황.md', 'scripts/lib/common.mjs', '.claude/hooks/session-start.mjs'];
  assert.ok(!onlyDocsChanged(files));
  assert.ok(onlyDocsChanged(files, (f) => !f.startsWith('docs/')));
  // PR 자신의 코드 변경이 하나라도 있으면 재리뷰
  assert.ok(!onlyDocsChanged([...files, 'backend/src/A.java'], (f) => f !== 'backend/src/A.java' && !f.startsWith('docs/')));
  assert.ok(!onlyDocsChanged([]));
});

test('이관 마커보다 이른 지적은 해결된 것으로 본다', () => {
  const findings = ['2026-11-19T01:00:00Z', '2026-11-19T02:00:00Z'];
  assert.equal(unresolvedFindingAt(findings, []), '2026-11-19T02:00:00Z');
  assert.equal(unresolvedFindingAt(findings, ['2026-11-19T03:00:00Z']), '');
  // 마커 뒤에 새 지적이 오면 다시 미해결
  assert.equal(unresolvedFindingAt([...findings, '2026-11-19T04:00:00Z'], ['2026-11-19T03:00:00Z']), '2026-11-19T04:00:00Z');
  assert.equal(unresolvedFindingAt([], ['2026-11-19T03:00:00Z']), '');
});
