// 상태 표시줄 (HX-6): 브랜치 읽기, CI 판정, 컨텍스트, 새로 고침 판정, 표시 문구 (gh·git 호출 없는 부분)
import assert from 'node:assert/strict';
import { mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { after, test } from 'node:test';
import { parseRoadmap, roadmapStatus, roadmapView } from '../lib/roadmap.mjs';
import { PR_REFRESH_MS, REFRESH_LOCK_MS, ROADMAP_REFRESH_MS, ciState, contextInfo, gitBranchAt, needsRefresh, readStatus, refreshPatch, roadmapSnapshot, statusLines, writeStatus } from '../lib/status.mjs';

const tmp = mkdtempSync(join(tmpdir(), 'vibenote-status-'));
after(() => rmSync(tmp, { recursive: true, force: true }));

const MD = `# 로드맵
- 연도: 2026

## 항목

| ID | 담당 | 트랙 | 시작 | 끝 | 의존 | 할 일 | 모델 |
|---|---|---|---|---|---|---|---|
| HP-3 | A | 4 하네스 | 11/27 | 11/28 | - | 표시 | Opus/high |
| K-3a | A | 3 카드 | 11/26 | 11/28 | - | 검수 | Opus/high |

## 넘겨주는 시점

| 날짜 | 주는 쪽 | 받는 쪽 | 항목 | 넘기는 것 | 풀리는 작업 |
|---|---|---|---|---|---|
| 12/01 | B | A | EV-1 | 기준선 점수 | AI 출력 PR 머지 |

## 공통 일정

| 날짜 | 일정 |
|---|---|
| 12/14 | 기능 동결 |
`;
const view = (branches = []) => {
  const r = parseRoadmap(MD);
  return roadmapView(r, roadmapStatus(r.items, { prs: [], branches }), '2026-11-26');
};

test('.git/HEAD를 직접 읽어 브랜치를 알아낸다 (worktree·detached 포함)', () => {
  const repo = join(tmp, 'repo');
  mkdirSync(join(repo, '.git'), { recursive: true });
  writeFileSync(join(repo, '.git', 'HEAD'), 'ref: refs/heads/feature/HP-3-session-display\n');
  assert.equal(gitBranchAt(repo), 'feature/HP-3-session-display');

  const wt = join(tmp, 'wt');
  const wtGit = join(tmp, 'main-git', 'worktrees', 'wt');
  mkdirSync(wt, { recursive: true });
  mkdirSync(wtGit, { recursive: true });
  writeFileSync(join(wt, '.git'), `gitdir: ${wtGit}\n`);
  writeFileSync(join(wtGit, 'HEAD'), '0123456789abcdef0123456789abcdef01234567\n');
  assert.equal(gitBranchAt(wt), '0123456');
  assert.equal(gitBranchAt(join(tmp, 'none')), null);
});

test('CI 판정: 실패가 하나라도 있으면 fail, 진행 중이면 pending, 모두 끝나 통과면 pass', () => {
  const run = (status, conclusion) => ({ __typename: 'CheckRun', status, conclusion });
  assert.equal(ciState([]), 'none');
  assert.equal(ciState(null), 'none');
  assert.equal(ciState([run('COMPLETED', 'SUCCESS'), run('COMPLETED', 'SKIPPED')]), 'pass');
  assert.equal(ciState([run('COMPLETED', 'SUCCESS'), run('IN_PROGRESS', null)]), 'pending');
  assert.equal(ciState([run('IN_PROGRESS', null), run('COMPLETED', 'FAILURE')]), 'fail');
  assert.equal(ciState([{ __typename: 'StatusContext', state: 'PENDING' }]), 'pending');
  assert.equal(ciState([{ __typename: 'StatusContext', state: 'SUCCESS' }]), 'pass');
});

test('컨텍스트 크기는 current_usage 합계이고 200k를 넘으면 /clear를 권한다', () => {
  const input = (tokens, extra = {}) => ({
    context_window: { used_percentage: 42.6, current_usage: { input_tokens: 10, cache_creation_input_tokens: tokens - 10, cache_read_input_tokens: 0, output_tokens: 500 } },
    ...extra,
  });
  assert.deepEqual(contextInfo(input(85_000)), { tokens: 85_000, pct: 43, over: false });
  assert.equal(contextInfo(input(200_000)).over, true);
  assert.equal(contextInfo(input(1000, { exceeds_200k_tokens: true })).over, true);
  // 첫 응답 전에는 비어 있다
  assert.deepEqual(contextInfo({ context_window: { current_usage: null } }), { tokens: 0, pct: null, over: false });
  assert.deepEqual(contextInfo({}), { tokens: 0, pct: null, over: false });
});

test('새로 고침: 캐시가 없거나 브랜치가 바뀌었거나 오래되면 하고, 진행 중이면 기다린다', () => {
  const now = 10_000_000;
  const fresh = { pr: { branch: 'b', at: now }, roadmap: { branch: 'b', at: now } };
  assert.equal(needsRefresh({}, 'b', now), true);
  assert.equal(needsRefresh(fresh, 'b', now), false);
  assert.equal(needsRefresh(fresh, 'c', now), true);
  assert.equal(needsRefresh(fresh, 'b', now + PR_REFRESH_MS + 1), true);
  assert.equal(needsRefresh({ ...fresh, refreshingAt: now }, 'c', now + 1000), false);
  assert.equal(needsRefresh({ ...fresh, refreshingAt: now }, 'c', now + REFRESH_LOCK_MS + 1), true);
});

test('캐시는 일부만 바꿔 쓰고 나머지 키를 지키며, 깨진 파일은 빈 캐시로 읽는다', () => {
  const file = join(tmp, 'status.json');
  writeStatus({ pr: { number: 1 } }, file);
  writeStatus({ roadmap: { key: 'dh' } }, file);
  assert.deepEqual(readStatus(file), { pr: { number: 1 }, roadmap: { key: 'dh' } });
  writeFileSync(file, '{깨짐');
  assert.deepEqual(readStatus(file), {});
});

test('캐시는 임시 파일에 쓴 뒤 바꿔 넣어, 읽는 쪽이 반쯤 쓴 파일을 보지 않고 임시 파일도 남지 않는다 (reviewer)', () => {
  const dir = join(tmp, 'atomic');
  const file = join(dir, 'status.json');
  writeStatus({ pr: { number: 1 } }, file);
  writeStatus({ roadmap: { key: 'dh' } }, file);
  assert.deepEqual(readdirSync(dir), ['status.json']);
  assert.deepEqual(JSON.parse(readFileSync(file, 'utf8')), { pr: { number: 1 }, roadmap: { key: 'dh' } });
});

test('새로 고침 결과: gh 실패는 "PR 없음"이 아니라 모름, 로드맵 계산이 던져도 PR은 쓰고 바로 다시 부르지 않는다 (reviewer)', () => {
  const at = 50_000_000;
  const branch = 'feature/HP-3-session-display';
  // gh 실패 + 같은 브랜치의 직전 값 없음 → 모름(error)
  const failed = refreshPatch({ prev: {}, branch, key: 'dh', at, pr: null, roadmapStale: false, roadmap: null });
  assert.deepEqual(failed.pr, { branch, at, error: true });
  assert.equal(failed.refreshingAt, null);
  const [, line] = statusLines({ pc: { id: 'dh-home', owner: '김동환', key: 'dh' }, branch, cache: failed, today: '2026-11-26' });
  assert.equal(line, '🔀 PR ?');
  // 같은 브랜치의 직전 값은 두고 시각만 바꾼다
  const prevPr = { branch, at: 1, number: 230, ci: 'pass' };
  assert.deepEqual(refreshPatch({ prev: { pr: prevPr }, branch, key: 'dh', at, pr: null }).pr, { ...prevPr, at });
  // PR 없음은 그대로
  assert.deepEqual(refreshPatch({ prev: {}, branch, key: 'dh', at, pr: { number: null } }).pr, { branch, at, number: null });

  // 로드맵 계산이 던지면 직전 값을 두고 시각만 바꾼다
  const boom = () => {
    throw new Error('형식 오류');
  };
  const prevRm = { at: 1, key: 'dh', branch, next: { id: 'HP-3' } };
  const kept = refreshPatch({ prev: { roadmap: prevRm }, branch, key: 'dh', at, pr: { number: 230 }, roadmapStale: true, roadmap: boom });
  assert.equal(kept.pr.number, 230);
  assert.deepEqual(kept.roadmap, { ...prevRm, at });
  // 직전 값도 없으면 빈 요약을 남겨 10분 동안 다시 부르지 않는다 (표시줄은 로드맵 칸을 비운다)
  const empty = refreshPatch({ prev: {}, branch, key: 'dh', at, pr: { number: 230 }, roadmapStale: true, roadmap: boom });
  assert.deepEqual(empty.roadmap, { at, key: 'dh', branch, next: null, handoff: null, error: true });
  assert.equal(needsRefresh({ ...empty }, branch, at + PR_REFRESH_MS - 1), false);
  assert.equal(needsRefresh({ ...empty, pr: { ...empty.pr, at: at + ROADMAP_REFRESH_MS } }, branch, at + ROADMAP_REFRESH_MS + 1), true);
  // 계산에 성공하면 새 요약
  assert.deepEqual(refreshPatch({ prev: {}, branch, key: 'dh', at, pr: { number: 230 }, roadmapStale: true, roadmap: () => ({ key: 'dh', branch, next: null }) }).roadmap, { at, key: 'dh', branch, next: null });
});

test('새로 고침은 표시줄과 같은 방식(.git/HEAD)으로 브랜치를 읽는다: detached에서 매번 다시 뜨지 않게 (reviewer)', () => {
  const src = readFileSync(new URL('../status-refresh.mjs', import.meta.url), 'utf8');
  assert.match(src, /gitBranchAt\(ROOT\)/);
  assert.doesNotMatch(src, /currentBranch\(/);
});

test('로드맵 요약: 지금 브랜치 항목과 가장 이른 넘겨주는 시점', () => {
  const snap = roadmapSnapshot(view(['feature/HP-3-session-display']), 'dh', 'feature/HP-3-session-display');
  assert.equal(snap.next.id, 'HP-3');
  assert.equal(snap.next.here, true);
  assert.equal(snap.next.state, 'doing');
  assert.deepEqual(snap.handoff, { date: '2026-12-01', text: '12/01 B→A EV-1 기준선 점수' });
  // develop에서는 의존이 풀린 가장 이른 항목
  const dev = roadmapSnapshot(view(), 'dh', 'develop');
  assert.equal(dev.next.id, 'K-3a');
  assert.equal(dev.next.here, false);
});

test('표시줄: 모델·사람·PC·브랜치·컨텍스트·사용량 / PR·CI·Codex·로드맵·넘겨주기', () => {
  const branch = 'feature/HP-3-session-display';
  const cache = {
    pr: { branch, at: 1, number: 230, state: 'OPEN', ci: 'pass', head: 'abcdef1234' },
    roadmap: roadmapSnapshot(view([branch]), 'dh', branch),
  };
  const input = {
    model: { display_name: 'Opus 5.5' },
    effort: { level: 'high' },
    context_window: { used_percentage: 43, current_usage: { input_tokens: 85_000 } },
    rate_limits: { five_hour: { used_percentage: 31.2 }, seven_day: { used_percentage: 12 } },
  };
  const pc = { id: 'dh-home', owner: '김동환', key: 'dh' };
  const [l1, l2] = statusLines({ input, pc, branch, cache, codex: { verdict: 'COMPLETE', prHead: 'abcdef1' }, today: '2026-11-26' });
  assert.equal(l1, `🤖 Opus 5.5 high · 👤 김동환 🖥️ dh-home · 🌿 ${branch} · 🧠 85k 43% · ⏳ 5h 31% 7d 12%`);
  assert.equal(l2, '🔀 PR #230 CI ✅ Codex ✅ · 🗺️ HP-3 🔨 ~11/28 · 📅 D-5 12/01 B→A EV-1 기준선 점수');

  // Codex 판정이 예전 head 것이면 ?, PR이 없으면 "PR 없음", 넘겨주기 D-2부터 🤝, 끝 날짜가 지나면 ⚠️
  const [, stale] = statusLines({ input, pc, branch, cache, codex: { verdict: 'COMPLETE', prHead: '1111111' }, today: '2026-11-29' });
  assert.match(stale, /Codex \? /);
  assert.match(stale, /HP-3 🔨 ~11\/28 ⚠️ 지남/);
  assert.match(stale, /🤝 D-2 12\/01/);
  const [, none] = statusLines({ input, pc, branch, cache: { ...cache, pr: { branch, at: 1, number: null } }, today: '2026-11-26' });
  assert.match(none, /^🔀 PR 없음 · /);
  // 다른 브랜치의 PR 캐시는 보이지 않는다
  const [, other] = statusLines({ input, pc, branch: 'feature/K-3a-x', cache, today: '2026-11-26' });
  assert.doesNotMatch(other, /PR #230/);
});

test('표시줄: 미등록 PC·입력 없음·200k 넘음', () => {
  const [l1, l2] = statusLines({ input: {}, pc: { id: null }, branch: 'develop', cache: {}, today: '2026-11-26' });
  assert.equal(l1, '🖥️ 미등록 PC → node scripts/setup.mjs --pc <id> · 🌿 develop');
  assert.equal(l2, undefined);
  const [big] = statusLines({ input: { exceeds_200k_tokens: true, context_window: { current_usage: { input_tokens: 210_000 } } }, pc: { id: 'dh-home', owner: '김동환' }, branch: 'x', today: '2026-11-26' });
  assert.match(big, /🧠 210k ⚠️ \/clear 권장/);
});
